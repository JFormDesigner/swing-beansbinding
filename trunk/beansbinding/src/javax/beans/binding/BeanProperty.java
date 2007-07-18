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
            System.err.println("LOG: source is null");
            return null;
        }

        Object src = source;

        for (int i = 0; i < path.length() - 1; i++) {
            src = getProperty(src, path.get(i));
            if (src == null) {
                System.err.println("LOG: missing source");
                return null;
            }

            if (src == UNREADABLE) {
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

        Object src = getProperty(getLastSource(), path.getLast());
        if (src == UNREADABLE) {
            throw new IllegalStateException("Unreadable");
        }

        return src;
    }

    public void setValue(Object value) {
        int pathLength = path.length();

        if (isListening) {
            validateCache(-1);
            setProperty(cache[pathLength - 1], path.getLast(), value);
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

        PropertyDescriptor pd = getPropertyDescriptor(src, path.getLast());
        if (pd == null || pd.getReadMethod() == null) {
            System.err.println("LOG: missing read method");
            return false;
        }

        return true;
    }

    public boolean isWriteable() {
        Object src;

        if (isListening) {
            validateCache(-1);
            src = cache[path.length() - 1];
        } else {
            src = getLastSource();
        }

        if (src == null || src == UNREADABLE) {
            return false;
        }

        PropertyDescriptor pd = getPropertyDescriptor(src, path.getLast());
        if (pd == null || pd.getWriteMethod() == null) {
            System.err.println("LOG: missing write method");
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
    private Object getProperty(Object object, String string) {
        if (object == null || object == UNREADABLE) {
            return UNREADABLE;
        }

        if (object instanceof Map) {
            return ((Map)object).get(string);
        }

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        Method readMethod = null;
        if (pd == null || (readMethod = pd.getReadMethod()) == null) {
            System.err.println("LOG: Missing read method");
            return UNREADABLE;
        }

        Exception reason = null;
        try {
            return readMethod.invoke(object);
        } catch (IllegalArgumentException ex) {
            reason = ex;
        } catch (IllegalAccessException ex) {
            reason = ex;
        } catch (InvocationTargetException ex) {
            reason = ex;
        }

        throw new PropertyResolverException("Exception reading property " + string + " on " + object,
                                            source, path.toString(), reason);
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
            System.err.println("LOG: missing read or write method");
            throw new IllegalStateException("Unreadable and unwritable");
        }

        return pd.getPropertyType();
    }

    /**
     * @throws PropertyResolverException
     * @throws IllegalStateException
     */
    private void setProperty(Object object, String propertyName, Object value) {
        if (object == null || object == UNREADABLE) {
            throw new IllegalStateException("Unwritable");
        }

        try {
            ignoreChange = true;

            if (object instanceof Map) {
                ((Map)object).put(propertyName, value);
                return;
            }

            PropertyDescriptor pd = getPropertyDescriptor(object, propertyName);
            Method writeMethod = null;
            if (pd == null || (writeMethod = pd.getWriteMethod()) == null) {
                System.err.println("missing write method");
                throw new IllegalStateException("Unwritable");
            }

            Exception reason;
            try {
                writeMethod.invoke(object, value);
                return;
            } catch (IllegalArgumentException ex) {
                reason = ex;
            } catch (InvocationTargetException ex) {
                reason = ex;
            } catch (IllegalAccessException ex) {
                reason = ex;
            }

            throw new PropertyResolverException("Exception reading property " + propertyName + " on " + object,
                    source, path.toString(), reason);
        } finally {
            ignoreChange = false;
        }
    }

    private Object toNull(Object src) {
        return src == UNREADABLE ? null : src;
    }

    private void updateCachedSources(int index) {
        boolean loggedYet = false;

        if (index == 0) {
            if (cache[0] != source) {
                unregisterListener(cache[0]);

                cache[0] = source;

                if (source == null) {
                    loggedYet = true;
                    System.err.println("LOG: source is null");
                } else {
                    registerListener(source);
                }
            }

            index++;
        }

        for (int i = index; i < path.length(); i++) {
            Object old = cache[i];
            Object sourceValue = getProperty(cache[i - 1], path.get(i - 1));

            if (sourceValue != old) {
                unregisterListener(old);

                cache[i] = sourceValue;

                if (sourceValue == null) {
                    if (!loggedYet) {
                        loggedYet = true;
                        System.err.println("LOG: missing source");
                    }
                } else if (sourceValue == UNREADABLE) {
                    if (!loggedYet) {
                        loggedYet = true;
                    }
                } else {
                    registerListener(sourceValue);
                }
            }
        }
    }

    private void registerListener(Object source) {
        assert source != null;

        if (source != UNREADABLE) {
            if (source instanceof ObservableMap) {
                ((ObservableMap)source).addObservableMapListener(
                        getChangeHandler());
            } else {
                addPropertyChangeListener(source);
            }
        }
    }

    private static String gs(Object source) {
        if (source == null) {
            return "null";
        } else if (source == UNREADABLE) {
            return "UNREADABLE";
        } else {
            return source.getClass().getName();
        }
    }
    
    /**
     * @throws PropertyResolverException
     */
    private void unregisterListener(Object source) {
        if (changeHandler != null && source!= null && source != UNREADABLE) {
            // PENDING: optimize this and cache
            if (source instanceof ObservableMap) {
                ((ObservableMap)source).removeObservableMapListener(
                        getChangeHandler());
            } else {
                removePropertyChangeListener(source);
            }
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private void addPropertyChangeListener(Object source) {
        EventSetDescriptor ed = getEventSetDescriptor(source);
        Method addPCMethod = null;

        if (ed == null || (addPCMethod = ed.getAddListenerMethod()) == null) {
            System.err.println("LOG: can't add listener to source " + source);
            return;
        }
        
        Exception reason = null;
        try {
            addPCMethod.invoke(source, getChangeHandler());
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
            throw new PropertyResolverException("Unable to register listener on " + source,
                    source, path.toString(), reason);
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private void removePropertyChangeListener(Object source) {
        EventSetDescriptor ed = getEventSetDescriptor(source);
        Method removePCMethod = null;

        if (ed == null || (removePCMethod = ed.getRemoveListenerMethod()) == null) {
            System.err.println("LOG: can't remove listener from source");
            return;
        }
        
        Exception reason = null;
        try {
            removePCMethod.invoke(source, getChangeHandler());
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
            throw new PropertyResolverException("Unable to remove listener from " + source,
                    source, path.toString(), reason);
        }
    }

    private int getSourceIndex(Object source) {
        for (int i = 0; i < cache.length; i++) {
            if (cache[i] == source) {
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
            
            Object source = cache[i];

            if (source == UNREADABLE) {
                return;
            }

            Object next = getProperty(source, path.get(i));

            if (next != cache[i + 1]) {
                throw new ConcurrentModificationException();
            }
        }

        if (path.length() != ignore) {
            Object next = getProperty(cache[path.length() - 1], path.getLast());
            if (cachedValue != next) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private void updateCachedValue(boolean notify) {
        Object oldValue = cachedValue;
        cachedValue = getProperty(cache[path.length() - 1], path.getLast());
        if (notify) {
            firePropertyChange("value", toNull(oldValue), toNull(cachedValue));
        }
    }

    private void cachedValueChanged(int index) {
        validateCache(index);
        updateCachedSources(index);
        updateCachedValue(true);
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
                Object source = e.getSource();
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
