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
    private final boolean emptySourcePath;
    //private ChangeHandler changeHandler;
    private boolean ignoreChange;
    private Validator<V> validator;
    private HashMap<Class<?>, Converter<?, V>> converters;

    public BeanProperty(String path) {
        this(path, null);
    }

    public BeanProperty(String path, S source) {
        this.path = PropertyPath.createPropertyPath(path);

        if (this.path.length() > 0) {
            sources = new Object[path.length()];
            sources[0] = source;
            emptySourcePath = false;
        } else {
            sources = new Object[] { source };
            emptySourcePath = true;
        }
    }

    public Class<? extends V> getValueType() {
        // checkBoundPath();

        if (path.length() == 0) {
            return (Class<? extends V>)sources[0].getClass();
        }

//        if (bound) {
//            return getType(sources[sources.length - 1], path.get(path.length() - 1));
//        } else {

        Object value = sources[0];

        for (int i = 0; i < path.length() - 1; i++) {
            if (value == null) {
                return null;
            }

            value = getProperty(value, path.get(i));
        }

        if (value == null) {
            return null;
        }

        return (Class<? extends V>)getType(value, path.get(path.length() - 1));
    }

    public V getValue() {
//        if (bound) {
//            checkBoundPath();
//            if (emptySourcePath) {
//                return sources[0];
//            }
//            return getProperty(sources[sources.length - 1],
//                    path.get(path.length() - 1));
//        }

        Object value = sources[0];

        for (int i = 0; i < path.length(); i++) {
            if (value == null) {
                return null;
            }

            value = getProperty(value, path.get(i));
        }

        return (V)value;
    }

    public void setValue(V value) {
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
        return false;
    }

    private void startListening() {
        isListening = true;
    }

    private void stopListening() {
        isListening = false;
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

    protected void firePropertyChange(Object oldValue, Object newValue) {
        if (support == null || support.getPropertyChangeListeners().length == 0) {
            return;
        }

        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }

        support.firePropertyChange("value", oldValue, newValue);
    }

    public String toString() {
        String className = (sources[0] == null ? "" : sources[0].getClass().getName());
        return path == null ? className
                            : className + '.' + path;
    }

    public void setSource(S source) {
        boolean wasListening = isListening;
        V oldValue = null;

        if (wasListening) {
            stopListening();
            oldValue = getValue();
        }

        sources[0] = source;

        if (wasListening) {
            startListening();
            firePropertyChange(oldValue, getValue());
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

/*
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
                            sourceValueChanged(index, source);
                        } else {
                            sourceValueChanged(index + 1, newValue);
                        }
                    } // else case means a different property changes
                } else {
                }
            }
        }

        public void mapKeyValueChanged(ObservableMap map, Object key,
                Object lastValue) {
            mapValueChanged(map, key);
        }

        public void mapKeyAdded(ObservableMap map, Object key) {
            mapValueChanged(map, key);
        }

        public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
            mapValueChanged(map, key);
        }
    }
*/
}
