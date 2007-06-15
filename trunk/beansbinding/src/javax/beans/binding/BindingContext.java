/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import com.sun.el.lang.FunctionMapperImpl;
import com.sun.el.lang.VariableMapperImpl;
import com.sun.java.beans.binding.BindingFunctions;
import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.beans.binding.ext.PropertyDelegateFactory;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.MapELResolver;
import javax.el.VariableMapper;

/**
 * {@code BindingContext} manages a set of bindings.
 * <p>
 * {@code BindingContext} provides methods to bind and unbind the set of
 * {@code Binding}s it contains. The following example illustrates
 * adding a {@code Binding} to a {@code BindingContext} and
 * binding it:
 * <pre>
 *   BindingContext context = new BindingContext();
 *   context.addBinding(source, "${sourceExpression}", target, "targetPath");
 *   context.bind();
 * </pre>
 * {@code BindingContext} provides methods for tracking the state of the
 * {@code Binding}s it contains. The {@code getHasUncommittedValues}
 * method may be used to determine if any of the bound {@code Binding}s
 * contained in a {@code BindingContext} have an uncommited value. Similarly,
 * the {@code hasInvalidValues} method may be used to determine if any of
 * {@code Binding}s are currently invalid. Both of these properties are bound;
 * a {@code PropertyChangeListener} may be attached to the
 * {@code BindingContext} to track when either property changes.
 * <p>
 * {@code BindingContext} also supports a {@code BindingListener}.
 * {@code BindingListener}s are notified when conversion or validation of
 * a {@code Binding} fails. {@code BindingListener} provides a convenient
 * way to provide feedback to the user when an invalid value is input.
 *
 * @see Binding
 * @see BindingListener
 *
 * @author sky
 */
public class BindingContext {
    private final List<Binding> bound;
    private final List<Binding> unbound;
    Map<String, Binding> namedBindings;
    private final PropertyChangeSupport changeSupport;
    private final List<BindingListener> bindingListeners;
    private boolean uncommittedValues;
    private boolean invalidValues;
    private boolean hasEditedTargetValues;
    
    /**
     * List of resolvers.
     */
    private final List<ELResolver> resolvers;
    private final BindingFunctionMapper functionMapper;

    private ELContext context;

    
    /**
     * Creates a new {@code BindingContext}.
     */
    public BindingContext() {
        changeSupport = new PropertyChangeSupport(this);
        unbound = new LinkedList<Binding>();
        bound = new ArrayList<Binding>();
        bindingListeners = new CopyOnWriteArrayList<BindingListener>();
        resolvers = new ArrayList<ELResolver>(3);
        // PENDING: EL also has an ArrayELResolver. Should that be added too?
        resolvers.add(new MapELResolver());
        resolvers.add(new PropertyDelegateResolver());
        functionMapper = new BindingFunctionMapper();
        try {
            functionMapper.addFunction("bb", "listSize", 
                    BindingFunctions.class.getDeclaredMethod("listSize", List.class));
        } catch (SecurityException ex) {
            assert false;
        } catch (NoSuchMethodException ex) {
            assert false;
        }
    }
    
    void targetEdited(Binding binding) {
        setHasEditedTargetValues(true);
    }
    
    private void setHasEditedTargetValues(boolean value) {
        if (hasEditedTargetValues != value) {
            hasEditedTargetValues = value;
            changeSupport.firePropertyChange("hasEditedTargetValues", 
                    !value, value);
        }
    }

    /**
     * Sets the {@code hasEditedTargetValues} property to {@code false}. This
     * is typically invoked after a save operation has done.
     *
     * @see #getHasEditedTargetValues
     */
    public void clearHasEditedTargetValues() {
        setHasEditedTargetValues(false);
    }
    
    /**
     * Returns whether any of the {@code Binding}s contained in this 
     * {@code BindingDescription} have had the target value edited. This
     * property is internally set to true when the target property of
     * a {@code Binding} chanages. The value of this is set to {@code false}
     * by invoking {@code clarHasEditedTargetValues}.
     *
     * @return whether any of the target properties have been changed
     *
     * @see #clearHasEditedTargetValues
     */
    public boolean getHasEditedTargetValues() {
        return hasEditedTargetValues;
    }

    void putNamed(String name, Binding binding) {
        if (namedBindings == null) {
            namedBindings = new HashMap<String, Binding>();
        }
        
        namedBindings.put(name, binding);
    }

    /**
     * Returns the binding with the given name, or {@code null}
     * if there is no such binding. Bindings without a name can not
     * be accessed by this method; it throws
     * {@code IllegalArgumentException} for a {@code null} name.
     *
     * @param name name of the binding to fetch
     * @return the binding with the given name, or {@code null}
     * @throws IllegalArgumentException for a {@code null} name
     */
    public final Binding getBinding(String name) {
        if (name == null) {
            throw new IllegalArgumentException("cannot fetch unnamed bindings");
        }

        return namedBindings == null ? null : namedBindings.get(name);
    }

    /**
     * Adds a {@code Binding} to this {@code BindingContext}.
     * The specified {@code Binding} must not be bound or be a child binding.
     * {@code Binding}s are bound by invoking the {@code bind} method.
     *
     * @param binding the {@code Binding} to add, must be
     *        {@code non-null}
     *
     * @throws IllegalArgumentException if {@code binding} is {@code null},
     *         has already been added to a context, is a child binding,
     *         or has the same name as an existing binding in the context
     * @throws IllegalStateException if {@code binding} is bound
     * @see #bind
     */
    public void addBinding(Binding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("Binding must be non-null");
        }
        if (binding.isBound()) {
            throw new IllegalStateException("Can not add bound Binding");
        }
        if (binding.getContext() != null) {
            throw new IllegalArgumentException("Can not add Binding to two different BindingContexts");
        }
        if (binding.getParentBinding() != null) {
            throw new IllegalArgumentException("Child binding cannot be added to a context");
        }

        String name = binding.getName();
        if (name != null) {
            if (getBinding(name) != null) {
                throw new IllegalArgumentException("Context already contains a binding with name \"" + name + "\"");
            } else {
                putNamed(name, binding);
            }
        }

        binding.setContext(this);
        unbound.add(binding);
    }
    
    /**
     * Creates and adds {@code Binding} to this {@code BindingContext}.
     *
     * @param source the source of the binding
     * @param sourceExpression El expression specifying the "property" of the source
     * @param target the target of the binding
     * @param targetPath path to the paroperty of the target
     * @return the {@code Binding}
     */
    public Binding addBinding(Object source, String sourceExpression,
                              Object target, String targetPath) {

        Binding binding = new Binding(source, sourceExpression, target, targetPath);
        addBinding(binding);
        return binding;
    }

    /**
     * Creates and adds {@code Binding} to this {@code BindingContext}.
     *
     * @param name a name for the binding
     * @param source the source of the binding
     * @param sourceExpression El expression specifying the "property" of the source
     * @param target the target of the binding
     * @param targetPath path to the paroperty of the target
     * @return the {@code Binding}
     * @throws IllegalArgumentException if the context already has a binding
     *         with the given name
     */
    public Binding addBinding(String name, Object source,
                              String sourceExpression, Object target,
                              String targetPath) {

        Binding binding = new Binding(name, source, sourceExpression, target, targetPath);
        addBinding(binding);
        return binding;
    }
    
    /**
     * Returns a {@code Binding}.
     *
     * @param binding the {@code Binding} to remove
     * @throws NullPointerException if {@code binding} is {@code null}
     * @throws IllegalStateException if {@code binding} is {@code bound}
     * @throws IllegalArgumentException if {@code binding} is not in this
     *         context
     */
    public void removeBinding(Binding binding) {
        if (binding.isBound()) {
            throw new IllegalStateException("Must unbind before removing");
        }
        if (!unbound.contains(binding)) {
            throw new IllegalArgumentException("Unknown Binding");
        }

        String name = binding.getName();
        if (name != null) {
            assert namedBindings != null;
            namedBindings.remove(name);
        }
        
        binding.setContext(null);
        unbound.remove(binding);
    }
    
    /**
     * Binds the set of unbound {@code Binding}s that have been
     * added to this {@code BindingContext}.
     *
     * @throws PropertyResolverException if {@code PropertyResolver} throws an
     *         exception; refer to {@code PropertyResolver} for the conditions
     *         under which an exception is thrown
     *
     * @see #unbind
     */
    public void bind() {
        List<Binding> toBind = new ArrayList<Binding>(unbound);
        for (Binding binding : toBind) {
            binding.bind();
        }
    }
    
    /**
     * Unbinds the set of {@code Binding}s that have been added
     * to this {@code BindingContext} and bound.
     *
     * @throws PropertyResolverException if {@code PropertyResolver} throws an
     *         exception; refer to {@code PropertyResolver} for the conditions
     *         under which an exception is thrown
     */
    public void unbind() {
        List<Binding> toUnbind = new ArrayList<Binding>(bound);
        for (Binding binding : toUnbind) {
            binding.unbind();
        }
    }

    /**
     * Adds a {@code BindingListener} to this {@code BindingContext}.
     *
     * @param listener the {@code BindingListener} to add
     */
    public void addBindingListener(BindingListener listener) {
        bindingListeners.add(listener);
    }
    
    /**
     * Removes a {@code BindingListener} from this {@code BindingContext}.
     *
     * @param listener the {@code BindingListener} to remove
     */
    public void removeBindingListener(BindingListener listener) {
        bindingListeners.remove(listener);
    }

    /**
     * Returns the {@code BindingValidator} for the specified 
     * {@code Binding}.
     *
     * @param binding the {@code Binding} to obtain the
     *        {@code BindingValidator} for
     * @return the {@code BindingValidator}
     * @throws NullPointerException if {@code binding} is {@code null}
     */
    public BindingValidator getValidator(Binding binding) {
        // PENDING: allow BindingContext to have a registry.
        if (binding == null) {
            throw new NullPointerException();
        }
        return null;
    }

    void notifyValidationListeners(Binding binding,
            ValidationResult validationResult) {
        for (BindingListener l : bindingListeners) {
            l.validationFailed(binding, validationResult);
        }
    }
    
    /**
     * Commits any uncommited values in the {@code Bindings} managed by
     * this {@code BindingContext}. This is a convenience method that may
     * be used to make sure all edited values are committed. This 
     * invokes {@code setSourceValueFromTargetValue()} on any {@code Bindings}
     * managed by this {@code BindingContext} that have target value state of
     * {@code UNCOMMITTED}. 
     *
     * @throws PropertyResolverException if {@code PropertyResolver} throws an
     *         exception; refer to {@code PropertyResolver} for the conditions
     *         under which an exception is thrown
     */
    public void commitUncommittedValues() {
        List<Binding> childBindings;
        List<Binding> bindings = new ArrayList<Binding>(this.bound);
        for (Binding binding : bindings) {
            // PENDING: need to set a property so that don't try and update state
            // while doing this
            commitUncommittedValues(binding);
            childBindings = binding.getBindings();
            for (Binding childBinding : childBindings) {
                commitUncommittedValues(childBinding);
            }
        }
    }
    
    private void commitUncommittedValues(Binding binding) {
        if (binding.getTargetValueState() == Binding.ValueState.UNCOMMITTED) {
            binding.setSourceValueFromTargetValue();
        }
    }

    /**
     * Adds a {@code PropertyChangeListener} to this {@code BindingContext}.
     *
     * @param listener the PropertyChangeListener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    /**
     * Removes a {@code PropertyChangeListener} from this
     * {@code BindingContext}.
     *
     * @param listener the PropertyChangeListener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /**
     * Adds a {@code PropertyChangeListener} to this {@code BindingContext}.
     *
     * @param property the name of the property the
     *        {@code PropertyChangeListener} is interested in
     * @param listener the {@code PropertyChangeListener} to add
     */
    public void addPropertyChangeListener(String property,
            PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(property, listener);
    }
    
    /**
     * Removes a {@code PropertyChangeListener} from this
     * {@code BindingContext}.
     *
     * @param property the name of the property the
     *        {@code PropertyChangeListener} is interested in
     * @param listener the {@code PropertyChangeListener} to remove
     */
    public void removePropertyChangeListener(String property,
                                             PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(property, listener);
    }

    private void setHasUncommittedValues(boolean uncommittedValues) {
        if (uncommittedValues != this.uncommittedValues) {
            this.uncommittedValues = uncommittedValues;
            changeSupport.firePropertyChange("hasUncommittedValues",
                    !uncommittedValues, uncommittedValues);
        }
    }

    /**
     * Returns {@code true} if any the {@code Bindings} managed by this
     * {@code BindingContext} have a source or target value state of
     * {@code UNCOMMITTED}.
     *
     * @return {@code true} if a value is uncommited
     */
    public boolean getHasUncommittedValues() {
        return uncommittedValues;
    }
    
    private void setHasInvalidValues(boolean invalidValues) {
        if (this.invalidValues != invalidValues) {
            this.invalidValues = invalidValues;
            changeSupport.firePropertyChange("hasInvalidValues",
                    !invalidValues, invalidValues);
        }
    }

    /**
     * Returns {@code true} if any of the {@code Bindings} managed by
     * this {@code BindingContext} have a target value state of
     * {@code INVALID}.
     *
     * @return {@code true} if a target value state is invalid
     */
    public boolean getHasInvalidValues() {
        return invalidValues;
    }

    /**
     * Adds an {@code ELResover} to the list of resolvers used in resolving
     * values. Adding a resolver does not effect existing {@code Binding}s.
     *
     * @param resolver the resolver to add
     *
     * @throws IllegalArgumentException if {@code resolver} is {@code null}
     */
    public final void addResolver(ELResolver resolver) {
        throwIfNull(resolver);
        resolvers.add(resolver);
        context = null;
    }
    
    /**
     * Removes an {@code ELResover} from the list of resolvers used in resolving
     * values. Removing a resolver does not effect existing {@code Binding}s.
     *
     * @param resolver the resolver to remove
     *
     * @throws IllegalArgumentException if {@code resolver} is {@code null}
     */
    public final void removeResolver(ELResolver resolver) {
        throwIfNull(resolver);
        resolvers.remove(resolver);
        context = null;
    }
    
    /**
     * Adds the specified method to the list functions available to the
     * expression. Adding a function does not effect existing {@code Binding}s.
     *
     * @param prefix the prefix of the function, or "" if no prefix;
     *     for example, {@code "fn"} in {@code ${fn:method()}}, or
     *     {@code ""} in {@code ${method()}}.
     * @param localName the short name of the function. For example,
     *     {@code "method"} in {@code ${fn:method()}}.
     * @param m the method
     * @throws IllegalArgumentException if any of the arguments are 
     *         {@code null}
     */
    public final void addFunction(String prefix, String localName, Method m) {
        throwIfNull(prefix, localName, m);
        functionMapper.addFunction(prefix, localName, m);
        // No need to set the context to null
    }

    /**
     * Adds the specified method to the list functions available to the
     * expression. Removing a function does not effect existing {@code Binding}s.
     *
     * @param prefix the prefix of the function, or "" if no prefix;
     *     for example, {@code "fn"} in {@code ${fn:method()}}, or
     *     {@code ""} in {@code ${method()}}.
     * @param localName the short name of the function. For example,
     *     {@code "method"} in {@code ${fn:method()}}.
     * @param m the method
     * @throws IllegalArgumentException if any of the arguments are 
     *         {@code null}
     */
    public final void removeFunction(String prefix, String localName, 
            Method m) {
        throwIfNull(prefix, localName, m);
        functionMapper.removeFunction(prefix, localName, m);
        // No need to set the context to null
    }
    
    /**
     * Returns an iterator over the known {@code FeatureDescriptor}s for the
     * specified object. This method is primarily intended for builders.
     * <p>
     * Preferred properties are identified by {@code FeatureDescriptor}s that
     * return a value of {@code Boolean.TRUE} from {@code 
     * getValue(PropertyDelegateProvider.PREFERRED_BINDING_PROPERTY)}.
     *
     * @param obj the object to query for {@code FeatureDescriptor}s
     * @return an {@code Iterable} over the known {@code FeatureDescriptor}s for
     *         the specified object; this never returns {@code null}
     *
     * @throws IllegalArgumentException if {@code obj} is {@code null}
     */
    public final Iterable<FeatureDescriptor> getFeatureDescriptors(Object obj) {
        throwIfNull(obj);
        return new FeatureDescriptorIterable(obj);
    }
    
    ELContext getContext() {
        if (context == null) {
            context = new Context();
        }
        return context;
    }
    

    static void throwIfNull(Object...args) {
        for (Object arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("Argument must be non-null");
            }
        }
    }

    void bindingBecameBound(Binding binding) {
        unbound.remove(binding);
        bound.add(binding);
        // No need to updateState here, that'll be triggered when the Binding
        // updates the values.
    }

    void bindingBecameUnbound(Binding binding) {
        bound.remove(binding);
        unbound.add(binding);
        updateState();
    }

    void bindingValueStateChanged(Binding binding) {
        updateState();
    }

    private void updateState() {
        boolean[] state = new boolean[2];
        state[0] = false;
        state[1] = false;
        List<Binding> childBindings = new ArrayList<Binding>(1);
        for (Binding binding : bound) {
            if (updateState0(binding, state)) {
                break;
            }
            childBindings = binding.getBindings();
            for (Binding childBinding : childBindings) {
                if (updateState0(childBinding, state)) {
                    break;
                }
            }
        }
        setHasUncommittedValues(state[0]);
        setHasInvalidValues(state[1]);
    }
    
    private boolean updateState0(Binding binding, boolean[] state) {
        if (binding.isBound()) {
            Binding.ValueState targetState = binding.getTargetValueState();
            Binding.ValueState sourceState = binding.getSourceValueState();
            if (!state[0]) {
                if (sourceState == Binding.ValueState.UNCOMMITTED ||
                        targetState == Binding.ValueState.UNCOMMITTED) {
                    state[0] = true;
                    if (state[1]) {
                        return true;
                    }
                }
            }
            if (!state[1]) {
                if (targetState == Binding.ValueState.INVALID) {
                    state[1] = true;
                    if (state[0]) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    void converterFailed(Binding binding, Exception conversionException) {
        for (BindingListener listener : bindingListeners) {
            listener.converterFailed(binding, conversionException);
        }
    }
    
    /**
     * Returns a string representing the state of this binding
     * context. This method is intended to be used only for
     * debugging purposes, and the content and format of the returned
     * string may vary between implementations. The returned string
     * may be empty but may not be <code>null</code>.
     *
     * @return a string representation of this binding description
     */
    public String toString() {
        return "Binding " +
                "[uncommittedValues=" + uncommittedValues +
                " invalidValues=" + invalidValues + 
                " bindings=" + bound +
                " hasEditedTargetValues=" + hasEditedTargetValues +
                " resolvers=" + resolvers +
                "]";
    }


    private static final class PropertyDelegateResolver extends BeanELResolver {
        public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, 
                Object base) {
            Iterator<FeatureDescriptor> superDescriptors = super.
                    getFeatureDescriptors(context, base);
            
            if (base != null) {
                List<Class<?>> pdTypes = PropertyDelegateFactory.
                        getPropertyDelegateClass(base.getClass());
                if (pdTypes.size() > 0) {
                    Map<String,FeatureDescriptor> dMap = new 
                            HashMap<String,FeatureDescriptor>();
                    while (superDescriptors.hasNext()) {
                        FeatureDescriptor d = superDescriptors.next();
                        dMap.put(d.getName(), d);
                    }
                    for (Class<?> type : pdTypes) {
                        for (FeatureDescriptor d : getDescriptors(type)) {
                            dMap.put(d.getName(), d);
                        }
                    }
                    return dMap.values().iterator();
                }
            }
            return superDescriptors;
        }
        
        private List<FeatureDescriptor> getDescriptors(Class<?> type) {
            BeanInfo info = null;
            try {
                info = Introspector.getBeanInfo(type);
            } catch (Exception ex) {
            }
            if (info == null) {
                return Collections.emptyList();
            }
            ArrayList<FeatureDescriptor> list = new ArrayList<FeatureDescriptor>(
                    info.getPropertyDescriptors().length);
            for (PropertyDescriptor pd: info.getPropertyDescriptors()) {
                // PENDING: The following properties come from EL, are they
                // needed?
                if (pd.getPropertyType() != null) {
                    pd.setValue("type", pd.getPropertyType());
                }
                pd.setValue("resolvableAtDesignTime", Boolean.TRUE);
                list.add(pd);
            }
            return list;
        }
        
        private Object baseOrDelegate(Object base, Object property) {
            if (base != null && property instanceof String) {
                Object delegate = PropertyDelegateFactory.getPropertyDelegate(
                        base, (String) property);
                if (delegate != null) {
                    return delegate;
                }
            }
            return base;
        }

        public void setValue(ELContext context, Object base, Object property, Object val) {
            super.setValue(context, baseOrDelegate(base, property), property, val);
        }

        public boolean isReadOnly(ELContext context, Object base, Object property) {
            return super.isReadOnly(context, baseOrDelegate(base, property), 
                    property);
        }

        public Object getValue(ELContext context, Object base, Object property) {
            return super.getValue(context, baseOrDelegate(base, property), 
                    property);
        }

        public Class<?> getType(ELContext context, Object base, Object property) {
            return super.getType(context, baseOrDelegate(base, property), 
                    property);
        }

        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            if (base == null){
                return null;
            }
            return Object.class;
        }
    }

    
    private final class Context extends ELContext {
        private final ELResolver resolver;
        private final VariableMapper mapper;
        
        Context() {
            CompositeELResolver compositeResolver = new CompositeELResolver();
            for (ELResolver resolver : resolvers) {
                compositeResolver.add(resolver);
            }
            resolver = compositeResolver;
            mapper = new VariableMapperImpl();
        }
        
        public ELResolver getELResolver() {
            return resolver;
        }

        public FunctionMapper getFunctionMapper() {
            return functionMapper;
        }

        public VariableMapper getVariableMapper() {
            return mapper;
        }
    }
    
    
    private final class BindingFunctionMapper extends FunctionMapperImpl {
        public void removeFunction(String prefix, String localName, Method m) {
            if (this.functions != null) {
                Function f = new Function(prefix, localName, m);
                this.functions.remove(prefix+":"+localName);
            }
        }
    }
    
    
    private final class FeatureDescriptorIterable implements Iterable<FeatureDescriptor> {
        private final Object obj;
        
        FeatureDescriptorIterable(Object obj) {
            this.obj = obj;
        }
        
        public Iterator<FeatureDescriptor> iterator() {
            ELContext context = getContext();
            return context.getELResolver().getFeatureDescriptors(context, obj);
        }
    }
}
