/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.OLDBINDING;

import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;
import javax.beans.OLDBINDING.ext.PropertyDelegateFactory;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@code ReflectionPropertyResolver} is used to resolve a dot separated list of
 * properties against an object. Once bound, {@code ReflectionPropertyResolver}
 * listens for changes to the source, notifying the delegate any time
 * a change occurs along any element in the path. This class is used internally 
 * by {@code Binding}, most developers need not use this class.
 * <p>
 * All properties are resolved using the following (in order):
 * <ol>
 * <li>If the object is a {@code Map}, the property is accessed using the
 *     {@code Map} method {@code get(Object)}.
 * <li>If a {@code PropertyDelegate} has been registered with the
 *     {@code PropertyDelegateFactory}, the property delegate is used.
 * <li>Reflection is used to access the property.
 * </ol>
 * <p>
 * If the property is not found after trying all these options, a
 * {@code PropertyResolverException} is thrown.
 * <p>
 * A bound {@code ReflectionPropertyResolver} tracks changes using the following
 * algorithm:
 * <ol>
 *   <li>If the object is an {@code ObservableMap}, an
 *       {@code ObservableMapListener} is used.
 *   <li>Otherwise the {@code
 *       addPropertyChangeListener(String,PropertyChangeListener)} method
 *       is looked up using reflection. If it exists, a {@code
 *       PropertyChangeListener} is added for the appropriate property.
 *   <li>If {@code addPropertyChangeListener(String,PropertyChangeListener)}
 *       does not exist, then {@code addPropertyChangeListener(PropertyChangeListener)}
 *       is looked up using reflection. If it exists, a {@code
 *       PropertyChangeListener} is added for the appropriate property.
 * </ol>
 * If the object is not an {@code ObservableMap}, or does not have
 * the {@code addPropertyChangeListener} method, no listener is added and
 * changes to the property are not tracked.
 * <p>
 * Once bound, any time an observable property changes along the property path
 * the delegate is notified. An observable property is a property for which
 * a {@code PropertyChangeListener} cound be added, or the object is an
 * {@code ObservableMap}.
 * <p>
 * Because of the dynamic nature of this class an exception may be thrown at
 * any point. Any exceptions encountered in trying to resolve a value are
 * wrapped in {@code PropertyResolverException}.
 *
 * @author sky
 */
// PENDING: better document the cases under which a PRE is thrown
// PENDING: this may not completely handle a null source, check on
//          getValue()/setValue()/bind()
final class ReflectionPropertyResolver {
    private final PropertyPath path;
    private final Object[] sources;
    private final boolean emptySourcePath;
    private boolean bound;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;
    private Delegate delegate;


    /**
     * Creates a {@code ReflectionPropertyResolver} from the specified path.
     *
     * @param path the path to create the {@code ReflectionPropertyResolver} from
     * @throws IllegalArgumentException if {@code path} is {@code null}
     */
    public static ReflectionPropertyResolver create(String path) {
        return create(null, path);
    }

    /**
     * Creates a {@code ReflectionPropertyResolver} for the specified object and path.
     *
     * @param source the source object the path is relative to
     * @param path the path identify the property to access
     * @throws IllegalArgumentException if {@code path} or {@code source}
     *         is {@code null}
     */
    public static ReflectionPropertyResolver create(Object source, String path) {
        return new ReflectionPropertyResolver(source, PropertyPath.createPropertyPath(path));
    }
    
    
    ReflectionPropertyResolver(Object source, PropertyPath path) {
        if (path == null) {
            throw new IllegalArgumentException();
        }
        this.path = path;
        if (path.length() > 0) {
            sources = new Object[path.length()];
            sources[0] = source;
            emptySourcePath = false;
        } else {
            sources = new Object[] { source };
            emptySourcePath = true;
        }
    }

    /**
     * Sets the object the path is relative to.
     *
     * @param object the object the path is relative to
     * @throws IllegalStateException if bound
     */
    public void setSource(Object object) {
        if (bound) {
            throw new IllegalStateException("Can't set source when bound");
        }
        sources[0] = object;
    }

    /**
     * Returns the object the path is relative to.
     *
     * @return the object the path is relative to
     */
    public Object getSource() {
        return sources[0];
    }
    
    /**
     * Returns the path.
     *
     * @return the path
     */
    public PropertyPath getPath() {
        return path;
    }
    
    /**
     * Sets the delegate that is notified any time an observable property
     * along the path changes.
     *
     * @param delegate the delegate
     * @throws IllegalStateException if the current delegate is
     *         {@code non-null}
     */
    public void setDelegate(Delegate delegate) {
        if (this.delegate != null) {
            throw new IllegalStateException(
            "ReflectionPropertyResolver can only have one delegate, and a delegate " +
            "is already registered.");
        }
        this.delegate = delegate;
    }
    
    /**
     * Returns the current delegate.
     *
     * @return the current delegate
     */
    public Delegate getDelegate() {
        return delegate;
    }
    
    /**
     * Sets the value of the last property identified in the path, relative
     * to the source object. For example, if the current path is
     * {@code "firstName"}, this is equivalent to
     * {@code getSource().setFirstName(value)} or
     * {@code getSource().put("firstName", value)}.
     * <p>
     * Note: This does <b>not</b> notify the delegate that the value has
     * changed.
     *
     * @param value the value to set
     * @throws IllegalStateException if not bound and the value of one
     *         of the properties along the path is {@code null}
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public void setValueOfLastProperty(Object value) {
        // PENDING: this doesn't look like it deals with an empty path
        if (bound) {
            checkBoundPath();
            setProperty(sources[sources.length - 1],
                    path.get(sources.length - 1),
                    value);
        } else {
            Object source = sources[0];
            for (int i = 0; i < path.length() - 1; i++) {
                if (source == null) {
                    throw new IllegalStateException();
                }
                source = getProperty(source, path.get(i));
            }
            if (source == null) {
                throw new IllegalStateException();
            }
            setProperty(source, path.get(sources.length - 1), value);
        }
    }
    
    private void checkBoundPath() {
        for (int i = 0; i < sources.length; i++) {
            if (sources[i] == null) {
                throw new IllegalStateException();
            }
        }
    }
    
    /**
     * Returns the value of the last property identified in the path, relative
     * to the source object.
     *
     * @return the value
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public Object getValueOfLastProperty() {
        if (bound) {
            checkBoundPath();
            if (emptySourcePath) {
                return sources[0];
            }
            return getProperty(sources[sources.length - 1],
                    path.get(path.length() - 1));
        }
        Object value = sources[0];
        for (int i = 0; i < path.length(); i++) {
            if (value == null) {
                throw new IllegalStateException();
            }
            value = getProperty(value, path.get(i));
        }
        return value;
    }
    
    /**
     * Returns the object the last element of the path is resolved against.
     *
     * @throws IllegalStateException if one of properties along the path
     *         resolves to {@code null}
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public Object getLastSource() {
        if (bound) {
            checkBoundPath();
            return sources[sources.length - 1];
        }
        Object source = sources[0];
        for (int i = 0; i < path.length() - 1; i++) {
            if (source == null) {
                throw new IllegalStateException();
            }
            source = getProperty(source, path.get(i));
        }
        return source;
    }
    
    /**
     * Returns {@code true} if all the values along the path resolve
     * to a {@code non-null} value.
     *
     * @return {@code true} if all the values along the path resolve to
     *         a {@code non-null} value
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public boolean hasAllPathValues() {
        if (bound) {
            for (int i = 0; i < sources.length; i++) {
                if (sources[i] == null) {
                    return false;
                }
            }
        } else {
            Object value = sources[0];
            for (int i = 0; i < path.length(); i++) {
                if (value == null) {
                    return false;
                }
                value = getProperty(value, path.get(i));
            }
        }
        return true;
    }
    
    /**
     * Attaches listeners on the objects along the path as described
     * in the class documentation. Any time a value along the path changes
     * the delegate is notified.
     *
     * @throws IllegalStateException if already bound
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public void bind() {
        if (bound) {
            throw new IllegalStateException("already bound");
        }
        bound = true;
        if (path.length() > 0) {
            updateListeners(0, sources[0], true);
        }
    }
    
    /**
     * Removes listeners installed from invoking {@code bind}.
     *
     * @throws IllegalStateException if not bound
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public void unbind() {
        if (!bound) {
            throw new IllegalStateException("already unbound");
        }
        bound = false;
        if (!emptySourcePath) {
            if (changeHandler != null) {
                for (int i = 0; i < sources.length; i++) {
                    unregisterListener(sources[i], path.get(i));
                }
            }
            for (int i = 1; i < sources.length; i++) {
                sources[i] = null;
            }
        }
    }
    
    /**
     * Returns the type of the last property.
     *
     * @return the type of the last property
     *
     * @throws IllegalStateException if not all path elements are available
     * @throws PropertyResolverException if a property along the path can
     *         not be resolved
     */
    public Class<?> getTypeOfLastProperty() {
        checkBoundPath();
        if (path.length() == 0) {
            return sources[0].getClass();
        }
        if (bound) {
            return getType(sources[sources.length - 1], path.get(path.length() - 1));
        } else {
            Object value = sources[0];
            for (int i = 0; i < path.length() - 1; i++) {
                if (value == null) {
                    throw new IllegalStateException();
                }
                value = getProperty(value, path.get(i));
            }
            if (value == null) {
                throw new IllegalStateException();
            }
            return getType(value, path.get(path.length() - 1));
        }
    }
    
    private void sourceValueChanged(int index, Object value) {
        updateListeners(index, value, false);
        if (delegate != null) {
            delegate.valueChanged(this);
        }
    }
    
    private void mapValueChanged(ObservableMap map, Object key) {
        if (!ignoreChange) {
            int index = getSourceIndex(map);
            if (index != -1) {
                if (key.equals(path.get(index))) {
                    sourceValueChanged(index + 1, map.get(key));
                }
            } else {
                // PENDING: Shouldn't get here, implies listener fired after
                // we removed ourself. Log or assert.
            }
        }
    }
    
    private void updateListeners(int index, Object value,
            boolean initialBind) {
        Object sourceValue = value;
//        Object sourceValue = (value == null) ? sources[index] : value;
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
    
    private Object getPropertyDelegate(Object source, String property) {
        return PropertyDelegateFactory.getPropertyDelegate(
                source, property);
    }
    
    private Class<?> getType(Object object, String string) {
        if (object == null) {
            return null;
        }
        if (object instanceof Map) {
            // PENDING: can we get type information at run time?
            return Object.class;
        }
        
        // Check if there is a delegate that has the property.
        Object delegate = PropertyDelegateFactory.
                getPropertyDelegate(object, string);
        if (delegate != null) {
            return getType0(delegate, string);
        }
        // No delegate, try real object.
        return getType0(object, string);
    }

    /**
     * @throws PropertyResolverException
     */
    private Class<?> getType0(Object object, String string) {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(string,
                    object.getClass(), "is" + capitalize(string), null);
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
    
    private Object getProperty(Object object, String string) {
        if (object == null) {
            return null;
        }
        if (object instanceof Map) {
            return ((Map)object).get(string);
        }
        
        // Check if there is a delegate that has the property.
        Object delegate = PropertyDelegateFactory.
                getPropertyDelegate(object, string);
        if (delegate != null) {
            return getProperty0(delegate, string);
        }
        // No delegate, try real object.
        return getProperty0(object, string);
    }

    /**
     * @throws PropertyResolverException
     */
    private Object getProperty0(Object object, String string) {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(string,
                    object.getClass(), "is" + capitalize(string), null);
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
    
    @SuppressWarnings("unchecked")
    private void setProperty(Object object, String propertyName, Object value) {
        ignoreChange = true;
        if (object instanceof Map) {
            ((Map)object).put(propertyName, value);
        } else if (object != null) {
            Object delegate = PropertyDelegateFactory.
                    getPropertyDelegate(object, propertyName);
            if (delegate != null) {
                setProperty0(delegate, propertyName, value);
            } else {
                setProperty0(object, propertyName, value);
            }
        }
        ignoreChange = false;
    }
    
    /**
     * @throws PropertyResolverException
     */
    private void setProperty0(Object object, String propertyName,
            Object value) {
        try {
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
        }
    }
    
    private void registerListener(Object source, String property) {
        if (source != null) {
            if (source instanceof ObservableMap) {
                ((ObservableMap)source).addObservableMapListener(
                        getChangeHandler());
            } else {
                Object delegate = getPropertyDelegate(source, property);
                if (delegate != null) {
                    addPropertyChangeListener(delegate, property);
                } else {
                    addPropertyChangeListener(source, property);
                }
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
    private void unregisterListener(Object source, String property) {
        if (changeHandler != null && source != null) {
            // PENDING: optimize this and cache
            if (source instanceof ObservableMap) {
                ((ObservableMap)source).removeObservableMapListener(
                        getChangeHandler());
            } else {
                Object delegate = getPropertyDelegate(source, property);
                if (delegate != null) {
                    removePropertyChangeListener(delegate, property);
                } else {
                    removePropertyChangeListener(source, property);
                }
            }
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
        for (int i = 0; i < sources.length; i++) {
            if (sources[i] != null) {
                Object delegate = getPropertyDelegate(
                        sources[i], path.get(i));
                if (delegate == source) {
                    return i;
                }
            }
        }
        return -1;
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


    /**
     * A {@code Delegate} is notified once a {@code ReflectionPropertyResolver} is
     * bound and a value of an observable property changes.
     */
    public static abstract class Delegate {
        /**
         * Notification that a property has changed.
         *
         * @param resolver the {@code ReflectionPropertyResolver} the delegate was
         *        added to
         */
        public abstract void valueChanged(ReflectionPropertyResolver resolver);
    }
}
