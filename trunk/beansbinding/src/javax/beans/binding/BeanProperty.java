/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;

/**
 * @author Shannon Hickey
 */
public final class BeanProperty implements SourceableProperty<Object, Object> {

    private final PropertyPath path;
    private Object source;
    private Object[] cache;
    private Object cachedValue;
    private Object cachedWriter;
    private List<PropertyStateListener<Object>> listeners;
    private boolean isListening = false;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;
    private static final Object UNREADABLE = new Object();

    /**
     * @throws IllegalArgumentException for empty or {@code null} path.
     */
    public BeanProperty(String path) {
        this(path, null);
    }

    /**
     * @throws IllegalArgumentException for empty or {@code null} path.
     */
    public BeanProperty(String path, Object source) {
        this.path = PropertyPath.createPropertyPath(path);
        this.source = source;
    }

    public void setSource(Object source) {
        this.source = source;

        if (isListening) {
            cachedValueChanged(0);
        }
    };

    public Object getSource() {
        return source;
    }

    private Object getLastSource() {
        if (source == null) {
            System.err.println(hashCode() + ": LOG: getLastSource(): source is null");
            return null;
        }

        Object src = source;

        for (int i = 0; i < path.length() - 1; i++) {
            src = getProperty(src, path.get(i));
            if (src == null) {
                System.err.println(hashCode() + ": LOG: getLastSource(): missing source");
                return null;
            }

            if (src == UNREADABLE) {
                System.err.println(hashCode() + ": LOG: getLastSource(): missing read method");
                return UNREADABLE;
            }
        }

        return src;
    }

    public Class<?> getValueType() {
        if (isListening) {
            validateCache(-1);
            return getType(cache[path.length() - 1], path.getLast());
        }

        return getType(getLastSource(), path.getLast());
    }

    public Object getValue() {
        if (isListening) {
            validateCache(-1);

            if (cachedValue == UNREADABLE) {
                throw new IllegalStateException("Unreadable");
            }

            return cachedValue;
        }

        Object src = getLastSource();
        if (src == null || src == UNREADABLE) {
            throw new IllegalStateException("Unreadable");
        }

        src = getProperty(getLastSource(), path.getLast());
        if (src == UNREADABLE) {
            System.err.println(hashCode() + ": LOG: getValue(): missing read method");
            throw new IllegalStateException("Unreadable");
        }

        return src;
    }

    public void setValue(Object value) {
        if (isListening) {
            validateCache(-1);

            if (cachedWriter == null) {
                throw new IllegalStateException("Unwritable");
            }

            write(cachedWriter, cache[path.length() - 1], path.getLast(), value);

            Object oldValue = toNull(cachedValue);
            updateCachedValue();
            Object newValue = toNull(cachedValue);

            if (didValueChange(oldValue, newValue)) {
                notifyListeners(new PropertyStateEvent(this, false, false, true, oldValue, newValue));
            }
        } else {
            setProperty(getLastSource(), path.getLast(), value);
        }
    }

    public boolean isReadable() {
        if (isListening) {
            validateCache(-1);
            return cachedValue != UNREADABLE;
        }

        Object src = getLastSource();
        if (src == null || src == UNREADABLE) {
            return false;
        }

        Object reader = getReader(src, path.getLast());
        if (reader == null) {
            System.err.println(hashCode() + ": LOG: isReadable(): missing read method");
            return false;
        }

        return true;
    }

    public boolean isWriteable() {
        if (isListening) {
            validateCache(-1);
            return cachedWriter != null;
        }

        Object src = getLastSource();
        if (src == null || src == UNREADABLE) {
            return false;
        }

        Object writer = getWriter(src, path.getLast());
        if (writer == null) {
            System.err.println(hashCode() + ": LOG: isWritable(): missing write method");
            return false;
        }

        return true;
    }

    private void startListening() {
        isListening = true;
        if (cache == null) {
            cache = new Object[path.length()];
        }

        cache[0] = UNREADABLE;
        updateCachedSources(0);
        updateCachedValue();
        updateCachedWriter();
    }

    private void stopListening() {
        isListening = false;

        if (changeHandler != null) {
            for (int i = 0; i < path.length(); i++) {
                unregisterListener(cache[i], path.get(i));
                cache[i] = null;
            }
        }

        cachedValue = null;
        cachedWriter = null;
        changeHandler = null;
    }

    public void addPropertyStateListener(PropertyStateListener<Object> listener) {
        if (listeners == null) {
            listeners = new ArrayList<PropertyStateListener<Object>>(1);
        }

        listeners.add(listener);

        if (!isListening && listeners.size() != 0) {
            startListening();
        }
    }

    public void removePropertyStateListener(PropertyStateListener<Object> listener) {
        if (listeners == null) {
            return;
        }

        listeners.remove(listener);

        if (isListening && listeners.size() == 0) {
            stopListening();
        }
    }

    public PropertyStateListener<Object>[] getPropertyStateListeners() {
        if (listeners == null) {
            return (PropertyStateListener<Object>[])(new PropertyStateListener[0]);
        }

        PropertyStateListener[] ret = new PropertyStateListener[listeners.size()];
        ret = listeners.toArray(ret);
        return (PropertyStateListener<Object>[])ret;
    }

    private boolean didValueChange(Object oldValue, Object newValue) {
        return oldValue == null || newValue == null || !oldValue.equals(newValue);
    }

    private void notifyListeners(PropertyStateEvent<Object> pe) {
        if (listeners == null || listeners.size() == 0) {
            return;
        }

        for (PropertyStateListener<Object> listener : listeners) {
            listener.propertyStateChanged(pe);
        }
    }

    public String toString() {
        String className = (source == null ? "" : source.getClass().getName());
        return className + path;
    }

    /**
     * @throws PropertyResolverException
     */
    private BeanInfo getBeanInfo(Object object) {
        assert object != null;

        try {
            return Introspector.getBeanInfo(object.getClass(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException ie) {
            throw new PropertyResolverException("Exception while introspecting " + object.getClass().getName(),
                                                source, path.toString(), ie);
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private PropertyDescriptor getPropertyDescriptor(Object object, String string) {
        assert object != null;

        PropertyDescriptor[] pds = getBeanInfo(object).getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (!(pd instanceof IndexedPropertyDescriptor) && pd.getName().equals(string)) {
                return pd;
            }
        }

        return null;
    }

    private EventSetDescriptor getEventSetDescriptor(Object object) {
        assert object != null;
        
        EventSetDescriptor[] eds = getBeanInfo(object).getEventSetDescriptors();
        for (EventSetDescriptor ed : eds) {
            if (ed.getListenerType() == PropertyChangeListener.class) {
                return ed;
            }
        }

        return null;
    }

    /**
     * @throws PropertyResolverException
     */
    private Object invokeMethod(Method method, Object object, Object... args) {
        Exception reason = null;

        try {
            return method.invoke(object, args);
        } catch (IllegalArgumentException ex) {
            reason = ex;
        } catch (IllegalAccessException ex) {
            reason = ex;
        } catch (InvocationTargetException ex) {
            reason = ex;
        }

        throw new PropertyResolverException("Exception invoking method " + method + " on " + object,
                                            source, path.toString(), reason);
    }

    private Object getReader(Object object, String string) {
        assert object != null;

        if (object instanceof Map) {
            return object;
        }

        if (object instanceof Property && string.equals("value")) {
            Property prop = (Property)object;
            if (!prop.isReadable()) {
                return null;
            }

            return prop;
        }

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        Method readMethod = null;
        return pd == null ? null : pd.getReadMethod();
    }

    /**
     * @throws PropertyResolverException
     */
    private Object read(Object reader, Object object, String string) {
        assert reader != null;

        if (reader instanceof Map) {
            assert reader == object;
            return ((Map)reader).get(string);
        }

        if (reader instanceof Property) {
            assert reader == object;
            assert string.equals("value");
            return ((Property)reader).getValue();
        }
        
        return invokeMethod((Method)reader, object);
    }

    /**
     * @throws PropertyResolverException
     */
    private Object getProperty(Object object, String string) {
        if (object == null || object == UNREADABLE) {
            return UNREADABLE;
        }

        Object reader = getReader(object, string);
        if (reader == null) {
            return UNREADABLE;
        }
        
        return read(reader, object, string);
    }

    private static String capitalize(String name) {
	return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * @throws PropertyResolverException
     */
    private Class<?> getType(Object object, String string) {
        if (object == null || object == UNREADABLE) {
            throw new IllegalStateException("Unreadable and unwritable");
        }

        if (object instanceof Map) {
            return Object.class;
        }

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        if (pd == null) {
            System.err.println(hashCode() + ": LOG: missing read or write method");
            throw new IllegalStateException("Unreadable and unwritable");
        }

        return pd.getPropertyType();
    }

    private Object getWriter(Object object, String string) {
        assert object != null;

        if (object instanceof Map) {
            return object;
        }

        if (object instanceof Property && string.equals("value")) {
            Property prop = (Property)object;
            if (!prop.isWriteable()) {
                return null;
            }

            return prop;
        }

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        Method writeMethod = null;
        return pd == null ? null : pd.getWriteMethod();
    }

    /**
     * @throws PropertyResolverException
     */
    private void write(Object writer, Object object, String string, Object value) {
        assert writer != null;

        try {
            ignoreChange = true;
            if (writer instanceof Map) {
                assert writer == object;
                ((Map)writer).put(string, value);
                return;
            }
            
            if (writer instanceof Property) {
                assert writer == object;
                assert string.equals("value");
                ((Property)writer).setValue(value);
                return;
            }
            
            invokeMethod((Method)writer, object, value);
        } finally {
            ignoreChange = false;
        }
    }

    /**
     * @throws PropertyResolverException
     * @throws IllegalStateException
     */
    private void setProperty(Object object, String string, Object value) {
        if (object == null || object == UNREADABLE) {
            throw new IllegalStateException("Unwritable");
        }

        Object writer = getWriter(object, string);
        if (writer == null) {
            System.err.println(hashCode() + ": LOG: setProperty(): missing write method");
            throw new IllegalStateException("Unwritable");
        }

        write(writer, object, string, value);
    }

    private Object toNull(Object src) {
        return src == UNREADABLE ? null : src;
    }

    private void updateCachedSources(int index) {
        boolean loggedYet = false;

        Object src;

        if (index == 0) {
            src = source;

            if (cache[0] != src) {
                unregisterListener(cache[0], path.get(0));

                cache[0] = src;

                if (src == null) {
                    loggedYet = true;
                    System.err.println(hashCode() + ": LOG: updateCachedSources(): source is null");
                } else {
                    registerListener(src, path.get(0));
                }
            }

            index++;
        }

        for (int i = index; i < path.length(); i++) {
            Object old = cache[i];
            src = getProperty(cache[i - 1], path.get(i - 1));

            if (src != old) {
                unregisterListener(old, path.get(i));

                cache[i] = src;

                if (src == null) {
                    if (!loggedYet) {
                        loggedYet = true;
                        System.err.println(hashCode() + ": LOG: updateCachedSources(): missing source");
                    }
                } else if (src == UNREADABLE) {
                    if (!loggedYet) {
                        loggedYet = true;
                        System.err.println(hashCode() + ": LOG: updateCachedSources(): missing read method");
                    }
                } else {
                    registerListener(src, path.get(i));
                }
            }
        }
    }

    private void registerListener(Object object, String string) {
        assert object != null;

        if (object != UNREADABLE) {
            if (object instanceof ObservableMap) {
                ((ObservableMap)object).addObservableMapListener(
                        getChangeHandler());
            } else if (object instanceof Property && string.equals("value")) {
                ((Property)object).addPropertyStateListener(getChangeHandler());
            } else if (!(object instanceof Map)) {
                addPropertyChangeListener(object);
            }
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private void unregisterListener(Object object, String string) {
        if (changeHandler != null && object!= null && object != UNREADABLE) {
            // PENDING: optimize this and cache
            if (object instanceof ObservableMap) {
                ((ObservableMap)object).removeObservableMapListener(
                        getChangeHandler());
            } else if (object instanceof Property && string.equals("value")) {
                ((Property)object).addPropertyStateListener(getChangeHandler());
            } else if (!(object instanceof Map)) {
                removePropertyChangeListener(object);
            }
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private void addPropertyChangeListener(Object object) {
        EventSetDescriptor ed = getEventSetDescriptor(object);
        Method addPCMethod = null;

        if (ed == null || (addPCMethod = ed.getAddListenerMethod()) == null) {
            System.err.println(hashCode() + ": LOG: addPropertyChangeListener(): can't add listener");
            return;
        }

        invokeMethod(addPCMethod, object, getChangeHandler());
    }

    /**
     * @throws PropertyResolverException
     */
    private void removePropertyChangeListener(Object object) {
        EventSetDescriptor ed = getEventSetDescriptor(object);
        Method removePCMethod = null;

        if (ed == null || (removePCMethod = ed.getRemoveListenerMethod()) == null) {
            System.err.println(hashCode() + ": LOG: removePropertyChangeListener(): can't remove listener from source");
            return;
        }
        
        Exception reason = null;
        try {
            removePCMethod.invoke(object, getChangeHandler());
        } catch (SecurityException ex) {
            reason = ex;
        } catch (IllegalArgumentException ex) {
            reason = ex;
        } catch (InvocationTargetException ex) {
            reason = ex;
        } catch (IllegalAccessException ex) {
            reason = ex;
        }

        if (reason != null) {
            throw new PropertyResolverException("Unable to remove listener from " + object,
                    source, path.toString(), reason);
        }
    }

    private int getSourceIndex(Object object) {
        for (int i = 0; i < cache.length; i++) {
            if (cache[i] == object) {
                return i;
            }
        }

        return -1;
    }

    private void validateCache(int ignore) {
        for (int i = 0; i < path.length() - 1; i++) {
           if (i == ignore - 1) {
               continue;
           }
            
            Object src = cache[i];

            if (src == UNREADABLE) {
                return;
            }

            Object next = getProperty(src, path.get(i));

            if (next != cache[i + 1]) {
                throw new ConcurrentModificationException();
            }
        }

        if (path.length() != ignore) {
            Object next = getProperty(cache[path.length() - 1], path.getLast());
            if (cachedValue != next) {
                throw new ConcurrentModificationException();
            }

            Object src = cache[path.length() - 1];
            Object writer;
            if (src == null || src == UNREADABLE) {
                writer = null;
            } else {
                writer = getWriter(cache[path.length() - 1], path.getLast());
            }

            if (cachedWriter != writer) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private void updateCachedWriter() {
        Object src = cache[path.length() - 1];
        if (src == null || src == UNREADABLE) {
            cachedWriter = null;
        } else {
            cachedWriter = getWriter(src, path.getLast());
            if (cachedWriter == null) {
                System.err.println(hashCode() + ": LOG: updateCachedWriter(): missing write method");
            }
        }
    }

    private void updateCachedValue() {
        Object src = cache[path.length() - 1];
        if (src == null || src == UNREADABLE) {
            cachedValue = UNREADABLE;
        } else {
            cachedValue = getProperty(cache[path.length() - 1], path.getLast());
            if (cachedValue == UNREADABLE) {
                System.err.println(hashCode() + ": LOG: updateCachedValue(): missing read method");
            }
        }
    }

    private void cachedValueChanged(int index) {
        validateCache(index);

        boolean wasReadable = (cachedValue != UNREADABLE);
        boolean wasWriteable = (cachedWriter != null);
        Object oldValue = toNull(cachedValue);

        updateCachedSources(index);
        updateCachedValue();
        if (index != path.length()) {
            updateCachedWriter();
        }

        boolean isReadable = (cachedValue != UNREADABLE);
        boolean isWriteable = (cachedWriter != null);
        Object newValue = toNull(cachedValue);
        boolean valueChanged = didValueChange(oldValue, newValue);

        PropertyStateEvent<Object> pse
                = new PropertyStateEvent<Object>(this,
                                                 wasReadable != isReadable,
                                                 wasWriteable != isWriteable,
                                                 valueChanged,
                                                 valueChanged ? oldValue : null,
                                                 valueChanged ? newValue : null);

        notifyListeners(pse);
    }

    private void mapValueChanged(ObservableMap map, Object key) {
        if (ignoreChange) {
            return;
        }
        
        int index = getSourceIndex(map);

        if (index == -1) {
            throw new AssertionError();
        }

        if (key.equals(path.get(index))) {
            cachedValueChanged(index + 1);
        }
    }

    private void propertyValueChanged(PropertyChangeEvent pce) {
        if (ignoreChange) {
            return;
        }

        int index = getSourceIndex(pce.getSource());

        if (index == -1) {
            throw new AssertionError();
        }

        String propertyName = pce.getPropertyName();
        if (propertyName == null || path.get(index).equals(propertyName)) {
            cachedValueChanged(index + 1);
        }
    }

    private void bindingPropertyChanged(PropertyStateEvent<? extends Object> pe) {
        if (ignoreChange) {
            return;
        }

        int index = getSourceIndex(pe.getSource());

        if (index == -1) {
            throw new AssertionError();
        }

        if (index == path.length() - 1) {
        } else if (pe.getReadableChanged() || pe.getValueChanged()) {
            cachedValueChanged(index + 1);
            return;
        }

        // PENDING(shannonh) - special case when property is at the end of the path
    }

    private ChangeHandler getChangeHandler() {
        if (changeHandler== null) {
            changeHandler = new ChangeHandler();
        }
        return changeHandler;
    }


    private final class ChangeHandler implements PropertyChangeListener,
                                                 ObservableMapListener,
                                                 PropertyStateListener<Object> {

        public void propertyChange(PropertyChangeEvent e) {
           propertyValueChanged(e);
        }

        public void mapKeyValueChanged(ObservableMap map, Object key, Object lastValue) {
            mapValueChanged(map, key);
        }

        public void mapKeyAdded(ObservableMap map, Object key) {
            mapValueChanged(map, key);
        }

        public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
            mapValueChanged(map, key);
        }

        public void propertyStateChanged(PropertyStateEvent<? extends Object> pe) {
            bindingPropertyChanged(pe);
        }

    }

}
