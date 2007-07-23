/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.OLDBINDING;

import com.sun.el.ExpressionFactoryImpl;
import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.beans.binding.ext.PropertyDelegateFactory;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.Expression;
import javax.el.Expression.ResolvedList;
import javax.el.Expression.ResolvedProperty;
import javax.el.Expression.ResolvedObject;
import javax.el.Expression.Result;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;

/**
 *
 * @author sky
 */
public final class ELPropertyResolver {
    /**
     * Installed on all objects in the path to listen for changes.
     */
    private final ChangeHandler changeHandler;
    
    /**
     * The context used for evaluation.
     */
    private final ELContext context;
    
    /*
     * Whether this resolver is bound.
     */
    private boolean bound;

    /**
     * The source to resolve against.
     */
    private Object source;
    
    /**
     * The path to resolve, relative to source.
     */
    private String path;

    /**
     * Value expression for the current path, only valid if bound.
     */
    private ValueExpression expression;

    /**
     * Set of objects listeners have been installed on.
     */
    private Set<RegisteredListener> registeredListeners;
    
    /**
     * Last set of objects listeners have been installed on.
     */
    private Set<RegisteredListener> lastRegisteredListeners;
    
    /**
     * If true, the delegate isn't notified of the change.
     */
    private boolean ignoreChange;

    /**
     * If bound, current result from execution.
     */
    private Object result;
    
    /**
     * If bound, this gives the current result type.
     */
    private Result.Type resultType;
    
    /**
     * Delegate for this resolver.
     */
    private Delegate delegate;

    
    public ELPropertyResolver(ELContext context) {
        this.context = context;
        changeHandler = new ChangeHandler();
        registeredListeners = new HashSet<RegisteredListener>(1);
    }

    public ELPropertyResolver(ELContext context, Object source, String path) {
        this(context);
        this.source = source;
        this.path = path;
    }
    
    /**
     * Sets the object the path is relative to.
     *
     * @param source the object the path is relative to
     * @throws IllegalStateException if bound
     */
    public void setSource(Object source) {
        if (bound) {
            throw new IllegalStateException("Can't set source when bound");
        }
        this.source = source;
    }

    /**
     * Returns the object the path is relative to.
     *
     * @return the object the path is relative to
     */
    public Object getSource() {
        return source;
    }
    
    /**
     * Sets the path.
     *
     * @param path the path
     */
    public void setPath(String path) {
        if (bound) {
            throw new IllegalStateException("Can't set path when bound");
        }
        this.path = path;
    }

    /**
     * Returns the path.
     *
     * @return the path
     */
    public String getPath() {
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
            "PropertyResolver can only have one delegate, and a delegate " +
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
     *
     * @param value the value to set
     *
     * @throws IllegalStateException if not bound, path is empty, or
     *         the current path is incomplete or the expression is read only
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public void setValueOfLastProperty(Object value) {
        throwIfNotBound();
        if (emptyPath()) {
            throw new IllegalStateException("Can not set value on empty path");
        }
        if (resultType == Result.Type.INCOMPLETE_PATH) {
            throw new IllegalStateException(
                    "Can't set value of incomplete path");
        }
        if (expression.isReadOnly(context)) {
            throw new IllegalStateException("Expression is readonly");
        }
        // PENDING: deal with exceptions
        ignoreChange = true;
        try {
            expression.setValue(context, value);
        } finally {
            ignoreChange = false;
        }
        // Reevaluate so that result is correct again.
        reevaluate();
    }

    /**
     * Returns the value of the last property identified in the path, relative
     * to the source object.
     *
     * @return the value
     *
     * @throws IllegalStateException if not bound, or the expression is
     *         incomplete
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public Object getValueOfLastProperty() {
        throwIfNotBound();
        if (resultType == Expression.Result.Type.INCOMPLETE_PATH) {
            throw new IllegalStateException("Incomplete path");
        }
        return result;
    }
    
    /**
     * Returns the evaluation result type. This categorizes the state of
     * evaluating the expression.
     *
     * @return {@code true} if all the values along the path resolve to
     *         a {@code non-null} value
     * @throws IllegalStateException if not bound, or the expression is
     *         incomplete
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public Expression.Result.Type getEvaluationResultType() {
        throwIfNotBound();
        if (emptyPath()) {
            return Expression.Result.Type.SINGLE_VALUE;
        }
        return resultType;
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
        if (emptyPath()) {
            // PENDING: this should probably throw and be communicated in
            // some other way.
            return null;
        }
        if (!bound) {
            createExpressionAndFactory();
            Class<?> type = expression.getType(context);
            expression = null;
            return type;
        } else {
            if (resultType == Expression.Result.Type.INCOMPLETE_PATH) {
                throw new IllegalStateException("Path is currently incomplete");
            }
            return expression.getType(context);
        }
    }
    
    /**
     * Attaches listeners on the objects along the path as described
     * in the class documentation. Any time a value along the path changes
     * the delegate is notified.
     *
     * @throws IllegalStateException if already bound, or the path is not empty
     *         and the source is {@code null}
     * @throws PropertyResolverException as described in the class
     *         documentation
     */
    public void bind() {
        if (bound) {
            throw new IllegalStateException("already bound");
        }
        if (!emptyPath() && getSource() == null) {
            throw new IllegalStateException("Must provide non-null source");
        }
        bound = true;
        if (!emptyPath()) {
            createExpressionAndFactory();
            reevaluate();
        } else {
            result = source;
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
        throwIfNotBound();
        bound = false;
        for (RegisteredListener rl : registeredListeners) {
            removeListener(rl);
        }
        registeredListeners.clear();
        result = null;
        expression = null;
        resultType = null;
    }
    
    /**
     * Evaluates the expression, returning the result.
     *
     * @return the result of the evaluation
     */
    public Expression.Result evaluate() {
        if (emptyPath()) {
            List<ResolvedObject> emptyList = Collections.emptyList();
            return new Expression.Result(Expression.Result.Type.SINGLE_VALUE, 
                    getSource(), emptyList);
        } else {
            if (bound) {
                return expression.getResult(context);
            } else {
                createExpressionAndFactory();
                Expression.Result result = expression.getResult(context);
                expression = null;
                return result;
            }
        }
    }
    
    private void createExpressionAndFactory() {
        // PENDING: do I really need to create a new factory every time through?
        ExpressionFactoryImpl factory = new ExpressionFactoryImpl();
        expression = factory.createValueExpression(context, path, Object.class);
        expression.setSource(getSource());
    }
    
    private void reevaluate() {
        lastRegisteredListeners = registeredListeners;
        registeredListeners = new HashSet<RegisteredListener>(
                lastRegisteredListeners.size());
        
        Expression.Result result = null;
        Exception e = null;
        try {
            result = expression.getResult(context);
        } catch (ELException ele) {
            e = ele;
        }
        if (e != null) {
            throw new PropertyResolverException("Error evaluating expression", 
                    getSource(), getPath(), e);
        }
        List<Expression.ResolvedObject> pairs = result.getResolvedObjects();
        for (Expression.ResolvedObject pair : pairs) {
            registerListener(pair);
        }
        resultType = result.getType();
        this.result = result.getResult();
        
        // Uninstall all listeners that are no longer along the path.
        for (RegisteredListener listener : lastRegisteredListeners) {
            removeListener(listener);
        }
        lastRegisteredListeners = null;
    }
    
    private void registerListener(ResolvedObject o) {
        if (o instanceof ResolvedProperty) {
            ResolvedProperty resolved = (ResolvedProperty)o;
            Object source = resolved.getSource();
            Object property = resolved.getProperty();
            if (source != null && property instanceof String) {
                if (source instanceof ObservableMap) {
                    RegisteredListener rl = new RegisteredListener(
                            source, (String) property);
                    if (!registeredListeners.contains(rl)) {
                        if (!lastRegisteredListeners.remove(rl)) {
                            ((ObservableMap)source).addObservableMapListener(
                                    getChangeHandler());
                        }
                        registeredListeners.add(rl);
                    }
                } else if (source != null) {
                    String sProperty = (String)property;
                    Object delegate = getPropertyDelegate(source, sProperty);
                    Object pclSource = (delegate != null) ? delegate : source;
                    RegisteredListener rl = new RegisteredListener(
                            pclSource, sProperty);
                    if (!registeredListeners.contains(rl)) {
                        if (!lastRegisteredListeners.remove(rl)) {
                            addPropertyChangeListener(pclSource, sProperty);
                        }
                        registeredListeners.add(rl);
                    }
                }
            }
        } else if (o instanceof ResolvedList) {
            ResolvedList resolved = (ResolvedList)o;
            // PENDING: if there are properties relative to this list, a
            // listener should be installed on it.
        }
    }
    
    private void removeListener(RegisteredListener rl) {
        Object source = rl.getSource();
        if (source instanceof ObservableMap) {
            ((ObservableMap)source).removeObservableMapListener(
                    getChangeHandler());
        } else {
            removePropertyChangeListener(source, rl.getProperty());
        }
    }
    
    private ChangeHandler getChangeHandler() {
        return changeHandler;
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
            // PENDING: log
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
                // PENDING: object is not observable, log
            }
        }
        if (reason != null) {
            throw new PropertyResolverException(
                    "Unable to register propertyChangeListener " + property + " " + source,
                    source, path, reason);
        }
    }
    
    /**
     * @throws PropertyResolverException
     */
    private void removePropertyChangeListener(Object source, String property) {
        Exception reason = null;
        boolean removed = false;
        if (property != null) {
            try {
                Method removePCL = source.getClass().getMethod(
                        "removePropertyChangeListener",
                        String.class, PropertyChangeListener.class);
                removePCL.invoke(source, property, getChangeHandler());
                removed = true;
            } catch (SecurityException ex) {
                reason = ex;
            } catch (IllegalArgumentException ex) {
                reason = ex;
            } catch (InvocationTargetException ex) {
                reason = ex;
            } catch (IllegalAccessException ex) {
                reason = ex;
            } catch (NoSuchMethodException ex) {
                // PENDING: log
            }
        }
        if (reason == null && !removed) {
            try {
                Method removePCL = source.getClass().getMethod(
                        "removePropertyChangeListener",
                        PropertyChangeListener.class);
                removePCL.invoke(source, getChangeHandler());
            } catch (SecurityException ex2) {
                reason = ex2;
            } catch (IllegalArgumentException ex2) {
                reason = ex2;
            } catch (InvocationTargetException ex2) {
                reason = ex2;
            } catch (IllegalAccessException ex2) {
                reason = ex2;
            } catch (NoSuchMethodException ex2) {
                // PENDING: log
            }
        }
        if (reason != null) {
            throw new PropertyResolverException(
                    "Unable to remove propertyChangeListener " + property + " " + source,
                    source, path, reason);
        }
    }

    private Object getPropertyDelegate(Object source, String property) {
        return PropertyDelegateFactory.getPropertyDelegate(
                source, property);
    }

    private boolean emptyPath() {
        return (path == null || "".equals(path));
    }

    private void propertyChanged() {
        if (!ignoreChange) {
            reevaluate();
            if (delegate != null) {
                delegate.valueChanged(this);
            }
        }
    }

    private void throwIfNotBound() {
        if (!bound) {
            throw new IllegalStateException("Must bind before using this");
        }
    }

    /**
     * Returns {@code true} if this resolver is bound.
     *
     * @return whether this resolver is bound
     */
    public boolean isBound() {
        return bound;
    }
    
    
    private final class ChangeHandler implements ObservableMapListener,
            PropertyChangeListener {
        private void sourceChanged(Object source, String property) {
            if (property != null) {
                property = property.intern();
            }
            for (RegisteredListener rl : registeredListeners) {
                if (rl.getSource() == source && (property == null || 
                        rl.getProperty() == property)) {
                    propertyChanged();
                    break;
                }
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            sourceChanged(e.getSource(), e.getPropertyName());
        }

        public void mapKeyValueChanged(ObservableMap map, Object key,
                Object lastValue) {
            if (key instanceof String) {
                sourceChanged(map, (String) key);
            }
        }

        public void mapKeyAdded(ObservableMap map, Object key) {
            if (key instanceof String) {
                sourceChanged(map, (String) key);
            }
        }

        public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
            if (key instanceof String) {
                sourceChanged(map, (String) key);
            }
        }
    }
    
    
    
    private static final class RegisteredListener {
        private final Object source;
        private final String property;
        
        RegisteredListener(Object source) {
            this(source, null);
        }
        
        RegisteredListener(Object source, String property) {
            this.source = source;
            if (property != null) {
                property = property.intern();
            }
            this.property = property;
        }
        
        public Object getSource() {
            return source;
        }
        
        public String getProperty() {
            return property;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof RegisteredListener) {
                RegisteredListener orl = (RegisteredListener) obj;
                return (orl.source == source && orl.property == property);
            }
            return false;
        }

        public int hashCode() {
            int result = 17;
            result = 37 * result + source.hashCode();
            if (property != null) {
                result = 37 * result + property.hashCode();
            }
            return result;
        }

        public String toString() {
            return "RegisteredListener [" +
                    " source=" + source +
                    " property=" + property + 
                    "]";
        }
    }

  
    /**
     * A {@code Delegate} is notified once a {@code PropertyResolver} is
     * bound and a value of an observable property changes.
     */
    public static abstract class Delegate {
        /**
         * Notification that a property has changed.
         *
         * @param resolver the {@code PropertyResolver} the delegate was
         *        added to
         */
        public abstract void valueChanged(ELPropertyResolver resolver);
    }
}
