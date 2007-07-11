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
import java.util.HashMap;
import java.util.Collections;
import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;

public final class BeanProperty<S, V> implements Property<S, V> {

    private final PropertyPath path;
    private final Object[] sources;
    private PropertyChangeSupport support;
    private boolean isListening = false;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;
    private Validator<V> validator;
    private HashMap<Class<?>, Converter<?, V>> converters;

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
        sources = new Object[this.path.length()];
        sources[0] = source;
    }

    public Class<? extends V> getValueType() {
        if (isListening) {
            return (Class<? extends V>)getType(sources[sources.length - 1],
                                               path.get(path.length() - 1));
        }
        
        int i = 0;
        Object source = sources[i];
        
        for (; i < path.length() - 1; i++) {
            if (source == null) {
                return null;
            }
            
            source = getProperty(source, path.get(i));
        }
        
        return (Class<? extends V>)getType(source, path.get(i));
    }

    public V getValue() {
        if (isListening) {
            return (V)getProperty(sources[sources.length - 1],
                                  path.get(path.length() - 1));
        }
        
        Object source = sources[0];
        
        for (int i = 0; i < path.length(); i++) {
            if (source == null) {
                return null;
            }
            
            source = getProperty(source, path.get(i));
        }
        
        return (V)source;
    }

    public void setValue(V value) {
        if (isListening) {
            setProperty(sources[sources.length - 1],
                        path.get(sources.length - 1), value);
        } else {
            Object source = sources[0];
            
            for (int i = 0; i < path.length() - 1; i++) {
                if (source == null) {
                    return;
                }
                
                source = getProperty(source, path.get(i));
            }
            
            setProperty(source, path.get(sources.length - 1), value);
        }
    }

    public boolean isReadable() {
        return false;
    }

    public boolean isWriteable() {
        return false;
    }

    public boolean isObservable() {
        return false;
    }

    public boolean isComplete() {
        if (isListening) {
            return sources[sources.length - 1] != null;
        }
        
        Object source = sources[0];
        
        for (int i = 0; i < path.length(); i++) {
            if (source == null) {
                return false;
            }
            
            source = getProperty(source, path.get(i));
        }

        return true;
    }

    private void startListening() {
        isListening = true;
        updateListeners(0, sources[0], true);
    }

    private void stopListening() {
        isListening = false;
        // TBD TBD TBD TBD
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (support == null) {
            support = new PropertyChangeSupport(this);
        }

        support.addPropertyChangeListener(listener);

        if (!isListening && getPropertyChangeListeners().length != 0) {
            startListening();
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (support == null) {
            return;
        }

        support.removePropertyChangeListener(listener);

        if (isListening && getPropertyChangeListeners().length == 0) {
            stopListening();
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (support == null) {
            return new PropertyChangeListener[0];
        }

        return support.getPropertyChangeListeners();
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
        String className = (sources[0] == null ? "" : sources[0].getClass().getName());
        return path == null ? className
                            : className + '.' + path;
    }

    public void setSource(S source) {
        sources[0] = source;

        if (isListening) {
            V oldValue = getValue();
            boolean wasComplete = isComplete();
            updateListeners(0, sources[0], false);
            firePropertyChange("complete", wasComplete, isComplete());
            firePropertyChange("value", oldValue, getValue());
        }
    };

    public S getSource() {
        return (S)sources[0];
    }
    
    public void setValidator(Validator<V> validator) {
        this.validator = validator;
    }

    public Validator<V> getValidator() {
        return validator;
    }

    public <F> void putConverter(Class<F> otherType, Converter<F, V> converter) {
        if (otherType == null) {
            throw new IllegalArgumentException("Must supply type");
        }

        if (converter == null) {
            if (converters != null) {
                converters.remove(otherType);
            }
        } else {
            if (converters == null) {
                converters = new HashMap<Class<?>, Converter<?, V>>();
            }
            converters.put(otherType, converter);
        }
    }

    public <F> Converter<F, V> getConverter(Class<F> otherType) {
        if (converters == null) {
            return null;
        }

        return (Converter<F, V>)converters.get(otherType);
    }

    public Map<Class<?>, Converter<?, V>> getConverters() {
        return Collections.unmodifiableMap(converters);
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
                        sources[0], path.toString(), reason);
            } else {
                throw new PropertyResolverException(
                        "Unable to find read method " + string + " " + object,
                        sources[0], path.toString());
            }
        } catch (IntrospectionException ex) {
            throw new PropertyResolverException(
                    "IntrospectionException getting read method " + string + " " + object,
                    sources[0], path.toString(), ex);
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
                                         "is" + capitalize(string), null);
            Method readMethod = pd.getReadMethod();
            if (readMethod != null) {
                return readMethod.getReturnType();
            } else {
                throw new PropertyResolverException(
                        "Unable to determine type " + string + " " + object,
                        sources[0], path.toString());
            }
        } catch (IntrospectionException ex) {
            throw new PropertyResolverException(
                    "IntrospectionException getting read method " + string + " " + object,
                    sources[0], path.toString(), ex);
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
                        sources[0], path.toString(), reason);
            } else {
                throw new PropertyResolverException(
                        "Unable to find setter " + propertyName + " on " + object,
                        sources[0], path.toString());
            }
        } catch (IntrospectionException ex) {
            throw new PropertyResolverException(
                    "Introspection exception " + propertyName + " on " + object,
                    sources[0], path.toString(), ex);
        } finally {
            ignoreChange = false;
        }
    }

    private void updateListeners(int index, Object value, boolean initialBind) {
        Object sourceValue = value;

        if (initialBind) {
            // forces installing listener (if necessary)
            sources[0] = null;
        }

        for (int i = index, max = path.length(); i < max; i++) {
            if (sourceValue != sources[i]) {
                unregisterListener(sources[i], path.get(i));
                sources[i] = sourceValue;

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
                    sources[0], path.toString(), reason);
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
                    sources[0], path.toString(), reason);
        }
    }

    private int getSourceIndex(Object source) {
        for (int i = 0; i < sources.length; i++) {
            if (sources[i] == source) {
                return i;
            }
        }

        return -1;
    }

    private void sourceValueChanged(int index, Object value, Object oldValue) {
        // PENDING - this is incomplete - still doesn't work when called from a property change listener
        // without the new value
        Object old = (index == path.length() ? oldValue : getValue());
        boolean wasComplete = isComplete();
        updateListeners(index, value, false);
        firePropertyChange("complete", wasComplete, isComplete());
        firePropertyChange("value", old, getValue());
    }

    private void mapValueChanged(ObservableMap map, Object key, Object lastValue) {
        if (!ignoreChange) {
            int index = getSourceIndex(map);
            if (index != -1) {
                if (key.equals(path.get(index))) {
                    sourceValueChanged(index + 1, map.get(key), lastValue);
                }
            } else {
                // PENDING: Shouldn't get here, implies listener fired after
                // we removed ourself. Log or assert.
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
                        if (newValue == null) {
                            // A PropertyChangeEvent with a null value can mean
                            // two things: either the new value is null, or it's
                            // too expensive to calculate the new value. This
                            // assumes it's the later.
                            sourceValueChanged(index, source, e.getOldValue());
                        } else {
                            sourceValueChanged(index + 1, newValue, e.getOldValue());
                        }
                    } // else case means a different property changes
                } else {
                }
            }
        }

        public void mapKeyValueChanged(ObservableMap map, Object key, Object lastValue) {
            mapValueChanged(map, key, lastValue);
        }

        public void mapKeyAdded(ObservableMap map, Object key) {
            mapValueChanged(map, key, null);
        }

        public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
            mapValueChanged(map, key, null);
        }
    }

}
