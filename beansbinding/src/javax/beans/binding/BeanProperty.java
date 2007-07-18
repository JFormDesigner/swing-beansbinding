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
                System.err.println("LOG: missing read method in chain");
                return null;
            }
        }

        return src;
    }

    public Class<?> getValueType() {
        int pathLength = path.length();

        if (isListening) {
            validateCache(-1);
            return getType(cache[pathLength - 1], path.getLast());
        }

        Object src = getLastSource();
        if (src == null) {
            throw new IllegalStateException("Unreadable and unwritable");
        }

        src = getType(src, path.getLast());
        if (src == null) {
            System.err.println("LOG: missing read or write method");
            throw new IllegalStateException("Unreadable and unwritable");
        }

        return (Class<?>)src;
    }

    public Object getValue() {
        int pathLength = path.length();

        if (isListening) {
            validateCache(-1);
            return cachedValue;
        }

        Object src = getLastSource();
        if (src == null) {
            throw new IllegalStateException("Unreadable");
        }

        src = getProperty(src, path.getLast());
        if (src == UNREADABLE) {
            System.err.println("LOG: missing read method");
            throw new IllegalStateException("Unreadable");
        }

        return src;
    }

    public void setValue(Object value) {
        int pathLength = path.length();

        if (isListening) {
            validateCache(-1);
            setProperty(cache[pathLength - 1], path.getLast(), value);
            updateCachedValue();
        } else {
            if (source == null) {
                System.err.println("LOG: source is null");
                throw new IllegalStateException("Unwritable");
            }

            Object src = getLastSource();
            if (src == null) {
                throw new IllegalStateException("Unwritable");
            }
            
            setProperty(src, path.getLast(), value);
        }
    }

    public boolean isReadable() {
        if (isListening) {
        }

        Object src = getLastSource();
        if (src == null) {
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
        if (isListening) {
        }

        Object src = getLastSource();
        if (src == null) {
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
        updateListeners(0);
        cachedValue = getProperty(cache[path.length() - 1], path.getLast());
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
                unregisterListener(cache[i], path.get(i));
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
    private PropertyDescriptor getPropertyDescriptor(Object object, String string) {
        BeanInfo info;

        try {
            info = Introspector.getBeanInfo(object.getClass(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException ie) {
            throw new PropertyResolverException("Exception accessing " + object.getClass().getName() + "." + string,
                                                source, path.toString(), ie);
        }

        PropertyDescriptor[] pds = info.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (!(pd instanceof IndexedPropertyDescriptor) && pd.getName().equals(string)) {
                return pd;
            }
        }

        return null;
    }

    /**
     * @throws PropertyResolverException
     */
    private Object getProperty(Object object, String string) {
        if (object == null || object == UNREADABLE) {
            return null;
        }

        if (object instanceof Map) {
            return ((Map)object).get(string);
        }

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        Method readMethod = null;
        if (pd == null || (readMethod = pd.getReadMethod()) == null) {
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

        throw new PropertyResolverException("Exception reading " + object.getClass().getName() + "." + string,
                                            source, path.toString(), reason);
    }

    private static String capitalize(String name) {
	return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * @throws PropertyResolverException
     */
    private Class<?> getType(Object object, String string) {
        assert object != null;

        if (object instanceof Map) {
            return Object.class;
        }

        PropertyDescriptor pd = getPropertyDescriptor(object, string);
        if (pd == null) {
            return null;
        }

        return pd.getPropertyType();
    }

    /**
     * @throws PropertyResolverException
     * @throws IllegalStateException
     */
    private void setProperty(Object object, String propertyName, Object value) {
        assert object != null;

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

            throw new PropertyResolverException("Exception writing " + object.getClass().getName() + "." + propertyName,
                                                source, path.toString(), reason);
        } finally {
            ignoreChange = false;
        }
    }

    private void updateListeners(int index) {
        boolean loggedYet = false;

        if (index == 0) {
            if (cache[0] != source) {
                unregisterListener(cache[0], path.get(0));

                cache[0] = source;

                if (source == null) {
                    loggedYet = true;
                    System.err.println("LOG: source is null");
                } else {
                    registerListener(source, path.get(0));
                }
            }

            index++;
        }

        for (int i = index; i < path.length(); i++) {
            Object old = cache[i];
            Object sourceValue = getProperty(cache[i - 1], path.get(i - 1));
            if (sourceValue != old) {
                unregisterListener(old, path.get(i));
                
                cache[i] = sourceValue;
                
                if (sourceValue == null) {
                    if (!loggedYet) {
                        loggedYet = true;
                        System.err.println("LOG: missing source");
                    }
                } else if (sourceValue == UNREADABLE) {
                    if (!loggedYet) {
                        loggedYet = true;
                        System.err.println("LOG: missing read method");
                    }
                } else {
                    registerListener(sourceValue, path.get(i));
                }
            }
        }
    }

    private void registerListener(Object source, String property) {
        System.out.println("Added listener for " + gs(source) + "." + property);
        if (source != null && source != UNREADABLE) {
            if (source instanceof ObservableMap) {
                ((ObservableMap)source).addObservableMapListener(
                        getChangeHandler());
            } else {
                addPropertyChangeListener(source, property);
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
    private void unregisterListener(Object source, String property) {
        System.out.println("Removed listener for " + gs(source) + "." + property);
        if (changeHandler != null && source != null && source != UNREADABLE) {
            // PENDING: optimize this and cache
            if (source instanceof ObservableMap) {
                ((ObservableMap)source).removeObservableMapListener(
                        getChangeHandler());
            } else {
                removePropertyChangeListener(source, property);
            }
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private void addPropertyChangeListener(Object source, String property) {
        // PENDING: optimize this and cache
        Exception reason = null;
        try {
            Method addPCL = source.getClass().getMethod(
                    "addPropertyChangeListener",
                    String.class, PropertyChangeListener.class);
            addPCL.invoke(source, property, getChangeHandler());
        } catch (SecurityException ex) {
            reason = ex;
        } catch (IllegalArgumentException ex) {
            reason = ex;
        } catch (InvocationTargetException ex) {
            reason = ex;
        } catch (IllegalAccessException ex) {
            reason = ex;
        } catch (NoSuchMethodException ex) {
            // No addPCL(String,PCL), look for addPCL(PCL)
            try {
                Method addPCL = source.getClass().getMethod(
                        "addPropertyChangeListener",
                        PropertyChangeListener.class);
                addPCL.invoke(source, getChangeHandler());
            } catch (SecurityException ex2) {
                reason = ex2;
            } catch (IllegalArgumentException ex2) {
                reason = ex2;
            } catch (InvocationTargetException ex2) {
                reason = ex2;
            } catch (IllegalAccessException ex2) {
                reason = ex2;
            } catch (NoSuchMethodException ex2) {
                // No addPCL(String,PCL), or addPCL(PCL), should log.
            }
        }
        if (reason != null) {
            throw new PropertyResolverException(
                    "Unable to register propertyChangeListener " + property + " " + source,
                    source, path.toString(), reason);
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private void removePropertyChangeListener(Object source, String property) {
        Exception reason = null;
        try {
            Method removePCL = source.getClass().getMethod(
                    "removePropertyChangeListener",
                    String.class, PropertyChangeListener.class);
            removePCL.invoke(source, property, changeHandler);
        } catch (SecurityException ex) {
            reason = ex;
        } catch (IllegalArgumentException ex) {
            reason = ex;
        } catch (InvocationTargetException ex) {
            reason = ex;
        } catch (IllegalAccessException ex) {
            reason = ex;
        } catch (NoSuchMethodException ex) {
            // No removePCL(String,PCL), try removePCL(PCL)
            try {
                Method removePCL = source.getClass().getMethod(
                        "removePropertyChangeListener",
                        PropertyChangeListener.class);
                removePCL.invoke(source, changeHandler);
            } catch (SecurityException ex2) {
                reason = ex2;
            } catch (IllegalArgumentException ex2) {
                reason = ex2;
            } catch (InvocationTargetException ex2) {
                reason = ex2;
            } catch (IllegalAccessException ex2) {
                reason = ex2;
            } catch (NoSuchMethodException ex2) {
            }
        }
        if (reason != null) {
            throw new PropertyResolverException(
                    "Unable to remove propertyChangeListener " + property + " " + source,
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

            if (source == null) {
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

    private void updateCachedValue() {
        Object oldValue = cachedValue;
        cachedValue = getProperty(cache[path.length() - 1], path.getLast());
        firePropertyChange("value", oldValue, cachedValue);
    }

    private void cachedValueChanged(int index) {
        validateCache(index);
        int pathLength = path.length();
        updateListeners(index);
        updateCachedValue();
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
