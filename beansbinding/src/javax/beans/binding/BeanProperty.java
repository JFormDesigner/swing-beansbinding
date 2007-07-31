/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

/*
 *   TO DO LIST:
 *
 *   - Re-think use of PropertyResolutionException.
 *     Many of the cases should be AssertionErrors, because they shouldn't happen.
 *     For the others, we should either use an Error subclass to indicate they're
 *     unrecoverable, or we need to try to leave the object in a consistent state.
 *     This is very difficult in methods like updateCachedSources where an
 *     exception can occur at any time while processing the chain.
 *
 *   - Do testing with applets/security managers.
 *
 *   - Introspector/reflection doesn't work for non-public classes. EL handles this
 *     by trying to find a version of the method in a public superclass/interface.
 *     Looking at the code for Introspector (also used by EL), I got the idea that
 *     it already does something like this. Investigate why EL handles this in an
 *     extra step, and decide what we need to do in this class.
 *
 *   - Add option to turn on validation. For now it's hard-coded to be off.
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
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class BeanProperty<S, V> extends AbstractProperty<V> implements SourceableProperty<S, V> {

    private final PropertyPath path;
    private S source;
    private Object[] cache;
    private Object cachedValue;
    private Object cachedWriter;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;
    private static final Object NOREAD = new Object();

    /**
     * @throws IllegalArgumentException for empty or {@code null} path.
     */
    public BeanProperty(String path) {
        this(path, null);
    }

    /**
     * @throws IllegalArgumentException for empty or {@code null} path.
     */
    public BeanProperty(String path, S source) {
        this.path = PropertyPath.createPropertyPath(path);
        this.source = source;
    }

    public void setSource(S source) {
        this.source = source;

        if (isListening()) {
            cachedValueChanged(0);
        }
    };

    public S getSource() {
        if (isListening()) {
            validateCache(-1);
        }

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

            if (src == NOREAD) {
                System.err.println(hashCode() + ": LOG: getLastSource(): missing read method");
                return NOREAD;
            }
        }

        return src;
    }

    public Class<? extends V> getWriteType() {
        if (isListening()) {
            validateCache(-1);

            if (cachedWriter == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            return (Class<? extends V>)getType(cache[path.length() - 1], path.getLast());
        }

        return (Class<? extends V>)getType(getLastSource(), path.getLast());
    }

    public V getValue() {
        if (isListening()) {
            validateCache(-1);

            if (cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }

            return (V)cachedValue;
        }

        Object src = getLastSource();
        if (src == null || src == NOREAD) {
            throw new UnsupportedOperationException("Unreadable");
        }

        src = getProperty(getLastSource(), path.getLast());
        if (src == NOREAD) {
            System.err.println(hashCode() + ": LOG: getValue(): missing read method");
            throw new UnsupportedOperationException("Unreadable");
        }

        return (V)src;
    }

    public void setValue(V value) {
        if (isListening()) {
            validateCache(-1);

            if (cachedWriter == null) {
                throw new UnsupportedOperationException("Unwritable");
            }

            write(cachedWriter, cache[path.length() - 1], path.getLast(), value);

            Object oldValue = cachedValue;
            updateCachedValue();
            notifyListeners(cachedIsWriteable(), oldValue);
        } else {
            setProperty(getLastSource(), path.getLast(), value);
        }
    }

    private boolean cachedIsReadable() {
        return cachedValue != NOREAD;
    }

    public boolean isReadable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsReadable();
        }

        Object src = getLastSource();
        if (src == null || src == NOREAD) {
            return false;
        }

        Object reader = getReader(src, path.getLast());
        if (reader == null) {
            System.err.println(hashCode() + ": LOG: isReadable(): missing read method");
            return false;
        }

        return true;
    }

    private boolean cachedIsWriteable() {
        return cachedWriter != null;
    }

    public boolean isWriteable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsWriteable();
        }

        Object src = getLastSource();
        if (src == null || src == NOREAD) {
            return false;
        }

        Object writer = getWriter(src, path.getLast());
        if (writer == null) {
            System.err.println(hashCode() + ": LOG: isWritable(): missing write method");
            return false;
        }

        return true;
    }

    protected final void listeningStarted() {
        if (isListening()) {
            return;
        }

        if (cache == null) {
            cache = new Object[path.length()];
        }

        cache[0] = NOREAD;
        updateCachedSources(0);
        updateCachedValue();
        updateCachedWriter();
    }

    protected final void listeningStopped() {
        if (!isListening()) {
            return;
        }

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

    private boolean didValueChange(Object oldValue, Object newValue) {
        return oldValue == null || newValue == null || !oldValue.equals(newValue);
    }

    private void notifyListeners(boolean wasWriteable, Object oldValue) {
        PropertyStateListener[] listeners = getPropertyStateListeners();

        if (listeners == null || listeners.length == 0) {
            return;
        }

        oldValue = toUNREADABLE(oldValue);
        Object newValue = toUNREADABLE(cachedValue);
        boolean valueChanged = didValueChange(oldValue, newValue);
        boolean writeableChanged = (wasWriteable != cachedIsWriteable());

        if (!valueChanged && !writeableChanged) {
            return;
        }
        
        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        cachedIsWriteable());

        for (PropertyStateListener listener : listeners) {
            listener.propertyStateChanged(pse);
        }
    }

    public String toString() {
        String className = (source == null ? "" : source.getClass().getName());
        return getClass().getName() + "[" + className + path + "]";
    }

    /**
     * @throws PropertyResolutionException
     */
    private BeanInfo getBeanInfo(Object object) {
        assert object != null;

        try {
            return Introspector.getBeanInfo(object.getClass(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException ie) {
            throw new PropertyResolutionException("Exception while introspecting " + object.getClass().getName(),
                                                  source, path.toString(), ie);
        }
    }

    /**
     * @throws PropertyResolutionException
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
     * @throws PropertyResolutionException
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

        throw new PropertyResolutionException("Exception invoking method " + method + " on " + object,
                                              source, path.toString(), reason);
    }

    private Object getReader(Object object, String string) {
        assert object != null;

        if (object instanceof Map) {
            return object;
        }

        if (object instanceof Property) {
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
     * @throws PropertyResolutionException
     */
    private Object read(Object reader, Object object, String string) {
        assert reader != null;

        if (reader instanceof Map) {
            assert reader == object;
            return ((Map)reader).get(string);
        }

        if (reader instanceof Property) {
            assert reader == object;
            return ((Property)reader).getValue();
        }
        
        return invokeMethod((Method)reader, object);
    }

    /**
     * @throws PropertyResolutionException
     */
    private Object getProperty(Object object, String string) {
        if (object == null || object == NOREAD) {
            return NOREAD;
        }

        Object reader = getReader(object, string);
        if (reader == null) {
            return NOREAD;
        }
        
        return read(reader, object, string);
    }

    private static String capitalize(String name) {
	return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * @throws PropertyResolutionException
     */
    private Class<?> getType(Object object, String string) {
        if (object == null || object == NOREAD) {
            throw new UnsupportedOperationException("Unwritable");
        }

        if (object instanceof Map) {
            return Object.class;
        }

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        if (pd == null || pd.getWriteMethod() == null) {
            System.err.println(hashCode() + ": LOG: missing write method");
            throw new UnsupportedOperationException("Unwritable");
        }

        return pd.getPropertyType();
    }

    private Object getWriter(Object object, String string) {
        assert object != null;

        if (object instanceof Map) {
            return object;
        }

        if (object instanceof Property) {
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
     * @throws PropertyResolutionException
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
                ((Property)writer).setValue(value);
                return;
            }
            
            invokeMethod((Method)writer, object, value);
        } finally {
            ignoreChange = false;
        }
    }

    /**
     * @throws PropertyResolutionException
     * @throws IllegalStateException
     */
    private void setProperty(Object object, String string, Object value) {
        if (object == null || object == NOREAD) {
            throw new UnsupportedOperationException("Unwritable");
        }

        Object writer = getWriter(object, string);
        if (writer == null) {
            System.err.println(hashCode() + ": LOG: setProperty(): missing write method");
            throw new UnsupportedOperationException("Unwritable");
        }

        write(writer, object, string, value);
    }

    private Object toUNREADABLE(Object src) {
        return src == NOREAD ? UNREADABLE : src;
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
                } else if (src == NOREAD) {
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

        if (object != NOREAD) {
            if (object instanceof ObservableMap) {
                ((ObservableMap)object).addObservableMapListener(
                        getChangeHandler());
            } else if (object instanceof Property) {
                ((Property)object).addPropertyStateListener(getChangeHandler());
            } else if (!(object instanceof Map)) {
                addPropertyChangeListener(object);
            }
        }
    }

    /**
     * @throws PropertyResolutionException
     */
    private void unregisterListener(Object object, String string) {
        if (changeHandler != null && object!= null && object != NOREAD) {
            // PENDING: optimize this and cache
            if (object instanceof ObservableMap) {
                ((ObservableMap)object).removeObservableMapListener(
                        getChangeHandler());
            } else if (object instanceof Property) {
                ((Property)object).addPropertyStateListener(getChangeHandler());
            } else if (!(object instanceof Map)) {
                removePropertyChangeListener(object);
            }
        }
    }

    /**
     * @throws PropertyResolutionException
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
     * @throws PropertyResolutionException
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
            throw new PropertyResolutionException("Unable to remove listener from " + object,
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

    private boolean wrapsLiteral(Object o) {
        assert o != null;

        return o instanceof String ||
               o instanceof Byte ||
               o instanceof Character ||
               o instanceof Boolean ||
               o instanceof Short ||
               o instanceof Integer ||
               o instanceof Long ||
               o instanceof Float ||
               o instanceof Double;
    }

    // need special match method because when using reflection
    // to get a primitive value, the value is always wrapped in
    // a new object
    private boolean match(Object a, Object b) {
        if (a == b) {
            return true;
        }

        if (a == null) {
            return false;
        }

        if (wrapsLiteral(a)) {
            return a.equals(b);
        }

        return false;
    }

    private void validateCache(int ignore) {
        // PENDING(shannonh) - add API to turn this on
        if (true) {
            return;
        }

        for (int i = 0; i < path.length() - 1; i++) {
           if (i == ignore - 1) {
               continue;
           }
            
            Object src = cache[i];

            if (src == NOREAD) {
                return;
            }

            Object next = getProperty(src, path.get(i));

            if (!match(next, cache[i + 1])) {
                throw new ConcurrentModificationException();
            }
        }

        if (path.length() != ignore) {
            Object next = getProperty(cache[path.length() - 1], path.getLast());
            if (!match(cachedValue, next)) {
                throw new ConcurrentModificationException();
            }

            Object src = cache[path.length() - 1];
            Object writer;
            if (src == null || src == NOREAD) {
                writer = null;
            } else {
                writer = getWriter(cache[path.length() - 1], path.getLast());
            }

            if (cachedWriter != writer && (cachedWriter == null || !cachedWriter.equals(writer))) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private void updateCachedWriter() {
        Object src = cache[path.length() - 1];
        if (src == null || src == NOREAD) {
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
        if (src == null || src == NOREAD) {
            cachedValue = NOREAD;
        } else {
            cachedValue = getProperty(cache[path.length() - 1], path.getLast());
            if (cachedValue == NOREAD) {
                System.err.println(hashCode() + ": LOG: updateCachedValue(): missing read method");
            }
        }
    }

    private void cachedValueChanged(int index) {
        validateCache(index);

        boolean wasWriteable = cachedIsWriteable();
        Object oldValue = cachedValue;

        updateCachedSources(index);
        updateCachedValue();
        if (index != path.length()) {
            updateCachedWriter();
        }

        notifyListeners(wasWriteable, oldValue);
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

    private void bindingPropertyChanged(PropertyStateEvent pse) {
        if (ignoreChange) {
            return;
        }

        int index = getSourceIndex(pse.getSource());

        if (index == -1) {
            throw new AssertionError();
        }

        boolean valueChanged = pse.getValueChanged();

        if (index == path.length() - 1) {
            validateCache(index + 1);

            boolean writeableChanged = pse.getWriteableChanged();

            if (writeableChanged && valueChanged) {
                boolean wasWriteable = cachedIsWriteable();
                Object oldValue = cachedValue;
                updateCachedValue();
                updateCachedWriter();
                notifyListeners(wasWriteable, oldValue);
            } else if (valueChanged) {
                Object writer = getWriter(pse.getSource(), path.getLast());
                if (cachedWriter != writer) {
                    throw new ConcurrentModificationException();
                }
                Object oldValue = cachedValue;
                updateCachedValue();
                notifyListeners(cachedIsWriteable(), oldValue);
            } else {
                Object value = pse.getSource().getValue();
                if (cachedValue != value) {
                    throw new ConcurrentModificationException();
                }
                boolean wasWriteable = cachedIsWriteable();
                updateCachedWriter();
                notifyListeners(wasWriteable, cachedValue);
            }
        } else if (valueChanged) {
            cachedValueChanged(index + 1);
            return;
        }
    }

    private ChangeHandler getChangeHandler() {
        if (changeHandler == null) {
            changeHandler = new ChangeHandler();
        }
        return changeHandler;
    }


    private final class ChangeHandler implements PropertyChangeListener,
                                                 ObservableMapListener,
                                                 PropertyStateListener {

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

        public void propertyStateChanged(PropertyStateEvent pe) {
            bindingPropertyChanged(pe);
        }
    }

}
