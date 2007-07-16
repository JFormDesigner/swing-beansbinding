/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.ConcurrentModificationException;
import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;

public final class BeanProperty implements Property<Object, Object> {

    private final PropertyPath path;
    private Object source;
    private Object[] cache;
    private Object cachedValue;
    private PropertyChangeSupport support;
    private boolean isListening = false;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;

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

    public Class<?> getValueType() {
        int pathLength = path.length();

        if (isListening) {
            validateCache(-1);
            return getType(cache[pathLength - 1], path.getLast());
        }

        Object src = source;

        for (int i = 0; i < pathLength - 1; i++) {
            if (src == null) {
                return null;
            }

            src = getProperty(src, path.get(i));
        }

        return getType(src, path.getLast());
    }

    public Object getValue() {
        int pathLength = path.length();

        if (isListening) {
            validateCache(-1);
            return cachedValue;
        }

        Object src = source;

        for (int i = 0; i < pathLength; i++) {
            if (src == null) {
                return null;
            }

            src = getProperty(src, path.get(i));
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
            Object src = source;
            
            for (int i = 0; i < pathLength - 1; i++) {
                if (src == null) {
                    return;
                }
                
                src = getProperty(src, path.get(i));
            }
            
            setProperty(src, path.get(pathLength - 1), value);
        }
    }

    public boolean isReadable() {
        return false;
    }

    public boolean isWriteable() {
        return false;
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
    private Object getProperty(Object object, String string) {
        if (object == null) {
            return null;
        }

        if (object instanceof Map) {
            return ((Map)object).get(string);
        }

        try {
            PropertyDescriptor pd =
                new PropertyDescriptor(string, object.getClass(),
                                       "is" + capitalize(string), null);
            Method readMethod = pd.getReadMethod();
            if (readMethod != null) {
                Exception reason;
                try {
                    return readMethod.invoke(object);
                } catch (IllegalArgumentException ex) {
                    reason = ex;
                } catch (IllegalAccessException ex) {
                    reason = ex;
                } catch (InvocationTargetException ex) {
                    reason = ex;
                }

                throw new PropertyResolverException(
                        "Exception getting value " + string + " " + object,
                        source, path.toString(), reason);
            } else {
                throw new PropertyResolverException(
                        "Unable to find read method " + string + " " + object,
                        source, path.toString());
            }
        } catch (IntrospectionException ex) {
            throw new PropertyResolverException(
                    "IntrospectionException getting read method " + string + " " + object,
                    source, path.toString(), ex);
        }
    }

    private static String capitalize(String name) {
	return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * @throws PropertyResolverException
     */
    private Class<?> getType(Object object, String string) {
        if (object == null) {
            return null;
        }

        if (object instanceof Map) {
            // PENDING: can we get type information at run time?
            return Object.class;
        }

        try {
            PropertyDescriptor pd
                = new PropertyDescriptor(string, object.getClass(),
                                         null, "set" + capitalize(string));
            Method writeMethod = pd.getWriteMethod();
            if (writeMethod != null) {
                return writeMethod.getParameterTypes()[0];
            } else {
                throw new PropertyResolverException(
                        "Unable to determine type " + string + " " + object,
                        source, path.toString());
            }
        } catch (IntrospectionException ex) {
            throw new PropertyResolverException(
                    "IntrospectionException getting read method " + string + " " + object,
                    source, path.toString(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void setProperty(Object object, String propertyName, Object value) {
        if (object == null)  {
            return;
        }

        try {
            ignoreChange = true;

            if (object instanceof Map) {
                ((Map)object).put(propertyName, value);
                return;
            }

            // set value
            PropertyDescriptor pd = new PropertyDescriptor(
                    propertyName, object.getClass(), null, 
                    "set" + capitalize(propertyName));
            Method setMethod = pd.getWriteMethod();
            if (setMethod != null) {
                Exception reason;
                try {
                    setMethod.invoke(object, value);
                    return;
                } catch (IllegalArgumentException ex) {
                    reason = ex;
                } catch (InvocationTargetException ex) {
                    reason = ex;
                } catch (IllegalAccessException ex) {
                    reason = ex;
                }
                throw new PropertyResolverException(
                        "Unable to set value " + propertyName + " on " + object +
                        " value=" + value,
                        source, path.toString(), reason);
            } else {
                throw new PropertyResolverException(
                        "Unable to find setter " + propertyName + " on " + object,
                        source, path.toString());
            }
        } catch (IntrospectionException ex) {
            throw new PropertyResolverException(
                    "Introspection exception " + propertyName + " on " + object,
                    source, path.toString(), ex);
        } finally {
            ignoreChange = false;
        }
    }

    private void updateListeners(int index) {
        Object sourceValue;
        
        if (index == 0) {
            sourceValue = source;
        } else {
            sourceValue = getProperty(cache[index - 1], path.get(index - 1));
        }
        
        for (int i = index, max = path.length(); i < max; i++) {
            if (sourceValue != cache[i]) {
                unregisterListener(cache[i], path.get(i));
                cache[i] = sourceValue;

                if (sourceValue != null) {
                    registerListener(sourceValue, path.get(i));
                }
            }

            if (i + 1 < max) {
                sourceValue = getProperty(sourceValue, path.get(i));
            }
        }
    }

    private void registerListener(Object source, String property) {
        if (source != null) {
            if (source instanceof ObservableMap) {
                ((ObservableMap)source).addObservableMapListener(
                        getChangeHandler());
            } else {
                addPropertyChangeListener(source, property);
            }
        }
    }

    /**
     * @throws PropertyResolverException
     */
    private void unregisterListener(Object source, String property) {
        if (changeHandler != null && source != null) {
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
