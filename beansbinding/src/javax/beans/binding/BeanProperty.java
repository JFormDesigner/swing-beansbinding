/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.ConcurrentModificationException;
import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;

/**
 * @author Shannon Hickey
 */
public final class BeanProperty implements Property<Object, Object> {

    private final PropertyPath path;
    private Object source;
    private Object[] cache;
    private Object cachedValue;
    private Object cachedWriter;
    private PropertyChangeSupport support;
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
            updateCachedValue(true);
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

    private void maybeStartListening() {
        if (!isListening && getPropertyChangeListeners().length != 0) {
            startListening();
        }
    }

    private void startListening() {
        isListening = true;
        if (cache == null) {
            cache = new Object[path.length()];
        }

        cache[0] = UNREADABLE;
        updateCachedSources(0);
        updateCachedValue(false);
        updateCachedWriter(false);
    }

    private void maybeStopListening() {
        if (isListening && getPropertyChangeListeners().length == 0) {
            stopListening();
        }
    }

    private void stopListening() {
        isListening = false;

        if (changeHandler != null) {
            for (int i = 0; i < path.length(); i++) {
                unregisterListener(cache[i]);
                cache[i] = null;
            }
        }

        cachedValue = null;
        cachedWriter = null;
        changeHandler = null;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (support == null) {
            support = new PropertyChangeSupport(this);
        }

        support.addPropertyChangeListener(listener);

        maybeStartListening();
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (support == null) {
            support = new PropertyChangeSupport(this);
        }

        support.addPropertyChangeListener(propertyName, listener);

        maybeStartListening();
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (support == null) {
            return;
        }

        support.removePropertyChangeListener(listener);

        maybeStopListening();
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (support == null) {
            return;
        }

        support.removePropertyChangeListener(propertyName, listener);

        maybeStopListening();
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (support == null) {
            return new PropertyChangeListener[0];
        }

        return support.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (support == null) {
            return new PropertyChangeListener[0];
        }

        return support.getPropertyChangeListeners(propertyName);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (support == null || support.getPropertyChangeListeners().length == 0) {
            return;
        }

        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }

        support.firePropertyChange(propertyName, oldValue, newValue);
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
    }

    /**
     * @throws PropertyResolverException
     * @throws IllegalStateException
     */
    private void setProperty(Object object, String string, Object value) {
        if (object == null || object == UNREADABLE) {
            throw new IllegalStateException("Unwritable");
        }

        try {
            ignoreChange = true;

            Object writer = getWriter(object, string);
            if (writer == null) {
                System.err.println(hashCode() + ": LOG: setProperty(): missing write method");
                throw new IllegalStateException("Unwritable");
            }

            write(writer, object, string, value);
        } finally {
            ignoreChange = false;
        }
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
                unregisterListener(cache[0]);

                cache[0] = src;

                if (src == null) {
                    loggedYet = true;
                    System.err.println(hashCode() + ": LOG: updateCachedSources(): source is null");
                } else {
                    registerListener(src);
                }
            }

            index++;
        }

        for (int i = index; i < path.length(); i++) {
            Object old = cache[i];
            src = getProperty(cache[i - 1], path.get(i - 1));

            if (src != old) {
                unregisterListener(old);

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
                    registerListener(src);
                }
            }
        }
    }

    private void registerListener(Object object) {
        assert object != null;

        if (object != UNREADABLE) {
            if (object instanceof ObservableMap) {
                ((ObservableMap)object).addObservableMapListener(
                        getChangeHandler());
            } else if (object instanceof Property) {
                ((Property)object).addPropertyChangeListener(getChangeHandler());
            } else if (!(object instanceof Map)) {
                addPropertyChangeListener(object);
            }
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private void unregisterListener(Object object) {
        if (changeHandler != null && object!= null && object != UNREADABLE) {
            // PENDING: optimize this and cache
            if (object instanceof ObservableMap) {
                ((ObservableMap)object).removeObservableMapListener(
                        getChangeHandler());
            } else if (object instanceof Property) {
                ((Property)object).addPropertyChangeListener(getChangeHandler());
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

    private void updateCachedWriter(boolean notify) {
        Object oldValue = cachedWriter;
        boolean wasWritable = (cachedWriter != null);
        Object src = cache[path.length() - 1];

        if (src == null || src == UNREADABLE) {
            cachedWriter = null;
        } else {
            cachedWriter = getWriter(src, path.getLast());
            if (cachedWriter == null) {
                System.err.println(hashCode() + ": LOG: updateCachedWriter(): missing write method");
            }
        }
        
        if (notify) {
            boolean isWriteable = (cachedWriter != null);
            firePropertyChange("writable", wasWritable, isWriteable);
        }
    }

    private void updateCachedValue(boolean notify) {
        Object oldValue = cachedValue;
        boolean wasReadable = (cachedValue != UNREADABLE);

        Object src = cache[path.length() - 1];
        if (src == null || src == UNREADABLE) {
            cachedValue = UNREADABLE;
        } else {
            cachedValue = getProperty(cache[path.length() - 1], path.getLast());
            if (cachedValue == UNREADABLE) {
                System.err.println(hashCode() + ": LOG: updateCachedValue(): missing read method");
            }
        }

        if (notify) {
            firePropertyChange("value", toNull(oldValue), toNull(cachedValue));
            boolean isReadable = (cachedValue != UNREADABLE);
            firePropertyChange("readable", wasReadable, isReadable);
        }
    }

    private void cachedValueChanged(int index) {
        validateCache(index);
        updateCachedSources(index);
        updateCachedValue(true);
        updateCachedWriter(true);
    }

    private void mapValueChanged(ObservableMap map, Object key) {
        if (!ignoreChange) {
            int index = getSourceIndex(map);
            if (index != -1) {
                if (key.equals(path.get(index))) {
                    cachedValueChanged(index + 1);
                }
            } else {
                throw new AssertionError();
            }
        }
    }

    private ChangeHandler getChangeHandler() {
        if (changeHandler== null) {
            changeHandler = new ChangeHandler();
        }
        return changeHandler;
    }


    private final class ChangeHandler implements PropertyChangeListener,
            ObservableMapListener {
        public void propertyChange(PropertyChangeEvent e) {
            if (!ignoreChange) {
                int index = getSourceIndex(e.getSource());
                if (index != -1) {
                    String propertyName = e.getPropertyName();
                    if (propertyName == null ||
                            path.get(index).equals(propertyName)) {
                        Object newValue = e.getNewValue();
                        cachedValueChanged(index + 1);
                    }
                } else {
                    throw new AssertionError();
                }
            }
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
    }

}
