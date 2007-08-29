/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.List;
import java.util.ArrayList;
import java.beans.*;

/**
 * {@code Binding} is an abstract class that represents the concept of a
 * binding between two properties, typically of two objects, and contains
 * methods for explicitly syncing the values of the two properties. {@code Binding}
 * itself does no automatic syncing between property values. Subclasses
 * will typically keep the values in sync according to some strategy.
 * <p>
 * Some {@code Bindings} are managed, often by another {@code Binding}.
 * A managed {@code Binding} does not allow certain methods to be called by
 * the user. These methods are identified in the documentation.
 * {@code Binding} provides protected versions of these methods with the
 * suffix {@code "Unmanaged"} for the use of subclasses.
 * <p>
 * Any {@code PropertyResolutionExceptions} thrown by {@code Property}
 * objects used by this binding are allowed to flow through to the caller
 * of the {@code Binding} methods.
 *
 * @param <SS> the type of source object
 * @param <SV> the type of value that the source property represents
 * @param <TS> the type of target object
 * @param <SV> the type of value that the target property represents
 *
 * @author Shannon Hickey
 */
public abstract class Binding<SS, SV, TS, TV> {

    private String name;
    private SS sourceObject;
    private TS targetObject;
    private Property<SS, SV> sourceProperty;
    private Property<TS, TV> targetProperty;
    private Validator<? super SV> validator;
    private Converter<SV, TV> converter;
    private TV sourceNullValue;
    private SV targetNullValue;
    private TV sourceUnreadableValue;
    private List<BindingListener> listeners;
    private PropertyStateListener psl;
    private boolean hasEditedSource;
    private boolean hasEditedTarget;
    private boolean ignoreChange;
    private boolean isManaged;
    private boolean isBound;
    private PropertyChangeSupport changeSupport;

    /**
     * An enumeration representing the reasons a sync ({@code save} or {@code refresh})
     * can fail on a {@code Binding}.
     *
     * @see Binding#refresh
     * @see Binding#save
     */
    public enum SyncFailureType {
        
        /**
         * A {@code refresh} failed because the {@code Binding's} target property is unwriteable
         * for the {@code Binding's} target object.
         */
        TARGET_UNWRITEABLE,
        
        /**
         * A {@code save} failed because the {@code Binding's} source property is unwriteable
         * for the {@code Binding's} source object.
         */
        SOURCE_UNWRITEABLE,
        
        /**
         * A {@code save} failed because the {@code Binding's} target property is unreadable
         * for the {@code Binding's} target object.
         */
        TARGET_UNREADABLE,
        
        /**
         * A {@code save} failed due to a conversion failure on the value
         * returned by the {@code Binding's} target property for the {@code Binding's}
         * target object.
         */
        CONVERSION_FAILED,
        
        /**
         * A {@code save} failed due to a validation failure on the value
         * returned by the {@code Binding's} target property for the {@code Binding's}
         * target object.
         */
        VALIDATION_FAILED
    }

    /**
     * {@code SyncFailure} represents a failure to sync ({@code save} or {@code refresh}) a
     * {@code Binding}.
     */
    public static final class SyncFailure {
        private SyncFailureType type;
        private Object reason;

        private static SyncFailure TARGET_UNWRITEABLE = new SyncFailure(SyncFailureType.TARGET_UNWRITEABLE);
        private static SyncFailure SOURCE_UNWRITEABLE = new SyncFailure(SyncFailureType.SOURCE_UNWRITEABLE);
        private static SyncFailure TARGET_UNREADABLE = new SyncFailure(SyncFailureType.TARGET_UNREADABLE);

        private static SyncFailure conversionFailure(RuntimeException rte) {
            return new SyncFailure(rte);
        }

        private static SyncFailure validationFailure(Validator.Result result) {
            return new SyncFailure(result);
        }

        private SyncFailure(SyncFailureType type) {
            if (type == SyncFailureType.CONVERSION_FAILED || type == SyncFailureType.VALIDATION_FAILED) {
                throw new IllegalArgumentException();
            }

            this.type = type;
        }

        private SyncFailure(RuntimeException exception) {
            this.type = SyncFailureType.CONVERSION_FAILED;
            this.reason = exception;
        }

        private SyncFailure(Validator.Result result) {
            this.type = SyncFailureType.VALIDATION_FAILED;
            this.reason = result;
        }

        /**
         * Returns the type of failure.
         *
         * @return the type of failure
         */
        public SyncFailureType getType() {
            return type;
        }

        /**
         * Returns the exception that occurred during conversion if
         * this failure represents a conversion failure. Throws
         * {@code UnsupportedOperationException} otherwise.
         *
         * @return the exception that occurred during conversion
         * @throws UnsupportedOperationException if the type of failure
         *         is not {@code SyncFailureType.CONVERSION_FAILED}
         */
        public RuntimeException getConversionException() {
            if (type != SyncFailureType.CONVERSION_FAILED) {
                throw new UnsupportedOperationException();
            }
            
            return (RuntimeException)reason;
        }

        /**
         * Returns the result that was returned from the
         * {@code Binding's} validator if this failure represents a
         * validation failure. Throws {@code UnsupportedOperationException} otherwise.
         *
         * @return the result that was returned from the {@code Binding's} validator
         * @throws UnsupportedOperationException if the type of failure
         *         is not {@code SyncFailureType.VALIDATION_FAILED}
         */
        public Validator.Result getValidationResult() {
            if (type != SyncFailureType.VALIDATION_FAILED) {
                throw new UnsupportedOperationException();
            }
            
            return (Validator.Result)reason;
        }

        /**
         * Returns a string representation of the {@code SyncFailure}. This
         * method is intended to be used for debugging purposes only, and
         * the content and format of the returned string may vary between
         * implementations. The returned string may be empty but may not
         * be {@code null}.
         *
         * @return a string representation of this {@code SyncFailure}
         */
        public String toString() {
            return type + (reason == null ? "" : ": " + reason.toString());
        }
    }

    /**
     * Encapsulates the result from calling
     * {@link org.jdesktop.beansbinding.Binding#getSourceValueForTarget} or
     * {@link org.jdesktop.beansbinding.Binding#getTargetValueForSource}, which
     * can either be a successful value or a failure.
     */
    public static final class ValueResult<V> {
        private V value;
        private SyncFailure failure;

        private ValueResult(V value) {
            this.value = value;
        }

        private ValueResult(SyncFailure failure) {
            if (failure == null) {
                throw new AssertionError();
            }

            this.failure = failure;
        }

        /**
         * Returns {@code true} if this {@code ValueResult} represents
         * a failure and {@code false} otherwise.
         *
         * @return {@code true} if this {@code ValueResult} represents
         *         a failure and {@code false} otherwise
         * @see #getFailure
         */
        public boolean failed() {
            return failure != null;
        }

        /**
         * Returns the resulting value if this {@code ValueResult} does
         * not represent a failure and throws {@code UnsupportedOperationException}
         * otherwise.
         *
         * @return the resulting value
         * @throws UnsupportedOperationException if this {@code ValueResult} represents a failure
         * @see #failed
         */
        public V getValue() {
            if (failed()) {
                throw new UnsupportedOperationException();
            }

            return value;
        }

        /**
         * Returns the failure if this {@code ValueResult} represents
         * a failure and throws {@code UnsupportedOperationException}
         * otherwise.
         *
         * @return the failure
         * @throws UnsupportedOperationException if this {@code ValueResult} does not represent a failure
         * @see #failed
         */
        public SyncFailure getFailure() {
            if (!failed()) {
                throw new UnsupportedOperationException();
            }
            
            return failure;
        }

        /**
         * Returns a string representation of the {@code ValueResult}. This
         * method is intended to be used for debugging purposes only, and
         * the content and format of the returned string may vary between
         * implementations. The returned string may be empty but may not
         * be {@code null}.
         *
         * @return a string representation of this {@code ValueResult}
         */
        public String toString() {
            return value == null ? "failure: " + failure : "value: " + value;
        }
    }

    /**
     * Create an instance of {@code Binding} between two properties of two objects.
     *
     * @param sourceObject the source object
     * @param sourceProperty a property on the source object
     * @param targetObject the target object
     * @param targetProperty a property on the target object
     * @param name a name for the {@code Binding}
     * @throws IllegalArgumentException if the source property or target property is {@code null}
     */
    protected Binding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        setSourceProperty(sourceProperty);
        setTargetProperty(targetProperty);

        this.sourceObject = sourceObject;
        this.sourceProperty = sourceProperty;
        this.targetObject = targetObject;
        this.targetProperty = targetProperty;
        this.name = name;
    }

    /**
     * Sets the source property on the {@code Binding}.
     * This method may not be called on a bound binding.
     *
     * @param sourceProperty the source property
     * @throws IllegalArgumentException if the source property is {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isBound()
     */
    protected final void setSourceProperty(Property<SS, SV> sourceProperty) {
        throwIfBound();
        if (sourceProperty == null) {
            throw new IllegalArgumentException("source property can't be null");
        }
        this.sourceProperty = sourceProperty;
    }
    
    /**
     * Sets the target property on the {@code Binding}.
     * This method may not be called on a bound binding.
     *
     * @param targetProperty the target property
     * @throws IllegalArgumentException if the target property is {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isBound()
     */
    protected final void setTargetProperty(Property<TS, TV> targetProperty) {
        throwIfBound();
        if (targetProperty == null) {
            throw new IllegalArgumentException("target property can't be null");
        }
        this.targetProperty = targetProperty;
    }
    
    /**
     * Returns the {@code Binding's} name, which may be {@code null}.
     *
     * @return the {@code Binding's} name, or {@code null}
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the {@code Binding's} source property, which may not be {@code null}.
     *
     * @return the {@code Binding's} source property, {@code non-null}
     */
    public final Property<SS, SV> getSourceProperty() {
        return sourceProperty;
    }

    /**
     * Returns the {@code Binding's} target property, which may not be {@code null}.
     *
     * @return the {@code Binding's} target property, {@code non-null}
     */
    public final Property<TS, TV> getTargetProperty() {
        return targetProperty;
    }

    /**
     * Returns the {@code Binding's} source object, which may be {@code null}.
     *
     * @return the {@code Binding's} source object, or {@code null}
     */
    public final SS getSourceObject() {
        return sourceObject;
    }

    /**
     * Returns the {@code Binding's} target object, which may be {@code null}.
     *
     * @return the {@code Binding's} target object, or {@code null}
     */
    public final TS getTargetObject() {
        return targetObject;
    }

    /**
     * Sets the {@code Binding's} source object, which may be {@code null}.
     * This method may not be called on a managed or bound binding.
     *
     * @param sourceObject the source object, or {@code null}
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    public final void setSourceObject(SS sourceObject) {
        throwIfManaged();
        setSourceObjectUnmanaged(sourceObject);
    }

    /**
     * A protected version of {@code setSourceObject} that allows managed
     * subclasses to set the source object without throwing an exception
     * for being managed.
     *
     * @param sourceObject the source object, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    protected final void setSourceObjectUnmanaged(SS sourceObject) {
        throwIfBound();
        this.sourceObject = sourceObject;
    }

    /**
     * Sets the {@code Binding's} target object, which may be {@code null}.
     * This method may not be called on a managed or bound binding.
     *
     * @param targetObject the target object, or {@code null}
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    public final void setTargetObject(TS targetObject) {
        throwIfManaged();
        setTargetObjectUnmanaged(targetObject);
    }

    /**
     * A protected version of {@code setTargetObject} that allows managed
     * subclasses to set the target object without throwing an exception
     * for being managed.
     *
     * @param targetObject the target object, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isManaged()
     * @see #isBound()
     */
    protected final void setTargetObjectUnmanaged(TS targetObject) {
        throwIfBound();
        this.targetObject = targetObject;
    }

    /**
     * Sets the {@code Validator} for the {@code Binding}, which may be {@code null}.
     * This method may not be called on a bound binding.
     * <p>
     * See the documentation on {@link #getTargetValueForSource} for details on how
     * a {@code Binding's Validator} is used.
     *
     * @param validator the {@code Validator}, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isBound()
     */
    public final void setValidator(Validator<? super SV> validator) {
        throwIfBound();
        this.validator = validator;
    }

    /**
     * Returns the {@code Binding's Validator}, which may be {@code null}.
     *
     * @return the {@code Binding's Validator}, or {@code null}
     * @see #setValidator
     */
    public final Validator<? super SV> getValidator() {
        return validator;
    }

    /**
     * Sets the {@code Converter} for the {@code Binding}, which may be {@code null}.
     * This method may not be called on a bound binding.
     * <p>
     * See the documentation on {@link #getTargetValueForSource} and
     * {@link #getSourceValueForTarget} for details on how
     * a {@code Binding's Covnerter} is used.
     *
     * @param converter the {@code Converter}, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     * @see #isBound()
     */
    public final void setConverter(Converter<SV, TV> converter) {
        throwIfBound();
        this.converter = converter;
    }

    /**
     * Returns the {@code Binding's Converter}, which may be {@code null}.
     *
     * @return the {@code Binding's Converter}, or {@code null}
     * @see #setConverter
     */
    public final Converter<SV, TV> getConverter() {
        return converter;
    }

    /**
     * Sets the value to be returned by {@link #getSourceValueForTarget}
     * when the source property returns {@code null} for the source object.
     * The default for this property is {@code null}.
     * This method may not be called on a bound binding.
     *
     * @param value the value, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     */
    public final void setSourceNullValue(TV value) {
        throwIfBound();
        sourceNullValue = value;
    }

    /**
     * Returns the value to be returned by {@link #getSourceValueForTarget}
     * when the source property returns {@code null} for the source object.
     * The default for this property is {@code null}.
     *
     * @return the value that replaces a source value of {@code null}, or {@code null}
     *         if there is no replacement
     * @see #setSourceNullValue
     */
    public final TV getSourceNullValue() {
        return sourceNullValue;
    }

    /**
     * Sets the value to be returned by {@link #getTargetValueForSource}
     * when the target property returns {@code null} for the target object.
     * The default for this property is {@code null}.
     * This method may not be called on a bound binding.
     *
     * @param value the value, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     */
    public final void setTargetNullValue(SV value) {
        throwIfBound();
        targetNullValue = value;
    }

    /**
     * Returns the value to be returned by {@link #getTargetValueForSource}
     * when the target property returns {@code null} for the target object.
     * The default for this property is {@code null}.
     *
     * @return the value that replaces a target value of {@code null}, or {@code null}
     *         if there is no replacement
     * @see #setTargetNullValue
     */
    public final SV getTargetNullValue() {
        return targetNullValue;
    }

    /**
     * Sets the value to be returned by {@link #getSourceValueForTarget}
     * when the source property is unreadable for the source object.
     * The default for this property is {@code null}.
     * This method may not be called on a bound binding.
     *
     * @param value the value, or {@code null}
     * @throws IllegalStateException if the {@code Binding} is bound
     */
    public final void setSourceUnreadableValue(TV value) {
        throwIfBound();
        sourceUnreadableValue = value;
    }

    /**
     * Returns the value to be returned by {@link #getSourceValueForTarget}
     * when the source property is unreadable for the source object.
     * The default for this property is {@code null}.
     *
     * @return the value that replaces an unreadable source value, or {@code null}
     *         if there is no replacement
     * @see #setSourceNullValue
     */
    public final TV getSourceUnreadableValue() {
        return sourceUnreadableValue;
    }

    /**
     * Adds a {@code BindingListener} to be notified of changes to this {@code Binding}.
     * Does nothing if the listener is {@code null}. If a listener is added more than once,
     * notifications are sent to that listener once for every time that it has
     * been added. The ordering of listener notification is unspecified.
     *
     * @param listener the listener to add
     */
    public final void addBindingListener(BindingListener listener) {
        if (listener == null) {
            return;
        }

        if (listeners == null) {
            listeners = new ArrayList<BindingListener>();
        }

        listeners.add(listener);
    }

    /**
     * Removes a {@code BindingListener} from the {@code Binding}. Does
     * nothing if the listener is {@code null} or is not one of those registered.
     * If the listener being removed was registered more than once, only one
     * occurrence of the listener is removed from the list of listeners.
     * The ordering of listener notification is unspecified.
     *
     * @param listener the listener to remove
     * @see #addBindingListener
     */
    public final void removeBindingListener(BindingListener listener) {
        if (listener == null) {
            return;
        }

        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Returns the list of {@code BindingListeners} registered on this
     * {@code Binding}. Order is undefined. Returns an empty array if there are
     * no listeners.
     *
     * @return the list of {@code BindingListeners} registered on this {@code Binding}
     * @see #addBindingListener
     */
    public final BindingListener[] getBindingListeners() {
        if (listeners == null) {
            return new BindingListener[0];
        }

        BindingListener[] ret = new BindingListener[listeners.size()];
        ret = listeners.toArray(ret);
        return ret;
    }

    public final ValueResult<TV> getSourceValueForTarget() {
        if (!targetProperty.isWriteable(targetObject)) {
            return new ValueResult<TV>(SyncFailure.TARGET_UNWRITEABLE);
        }

        TV value;

        if (sourceProperty.isReadable(sourceObject)) {
            SV rawValue = sourceProperty.getValue(sourceObject);

            if (rawValue == null) {
                value = sourceNullValue;
            } else {
                // may throw ClassCastException or other RuntimeException here;
                // allow it to be propogated back to the user of Binding
                value = convertForward(rawValue);
            }
        } else {
            value = sourceUnreadableValue;
        }

        return new ValueResult<TV>((TV)value);
    }

    public final ValueResult<SV> getTargetValueForSource() {
        if (!targetProperty.isReadable(targetObject)) {
            return new ValueResult<SV>(SyncFailure.TARGET_UNREADABLE);
        }

        if (!sourceProperty.isWriteable(sourceObject)) {
            return new ValueResult<SV>(SyncFailure.SOURCE_UNWRITEABLE);
        }

        SV value = null;
        TV rawValue = targetProperty.getValue(targetObject);

        if (rawValue == null) {
            value = targetNullValue;
        } else {
            try {
                value = convertReverse(rawValue);
            } catch (ClassCastException cce) {
                throw cce;
            } catch (RuntimeException rte) {
                return new ValueResult<SV>(SyncFailure.conversionFailure(rte));
            }

            if (validator != null) {
                Validator.Result vr = validator.validate(value);
                if (vr != null) {
                    return new ValueResult<SV>(SyncFailure.validationFailure(vr));
                }
            }
        }

        return new ValueResult<SV>((SV)value);
    }

    public final void bind() {
        throwIfManaged();
        bindUnmanaged();
    }
    
    protected final void bindUnmanaged() {
        throwIfBound();

        hasEditedSource = false;
        hasEditedTarget = false;
        isBound = true;

        psl = new PSL();
        sourceProperty.addPropertyStateListener(sourceObject, psl);
        targetProperty.addPropertyStateListener(targetObject, psl);

        bindImpl();

        if (listeners != null) {
            for (BindingListener listener : listeners) {
                listener.bindingBecameBound(this);
            }
        }
    }

    protected abstract void bindImpl();

    public final void unbind() {
        throwIfManaged();
        unbindUnmanaged();
    }
    
    protected final void unbindUnmanaged() {
        throwIfUnbound();

        unbindImpl();

        sourceProperty.removePropertyStateListener(sourceObject, psl);
        targetProperty.removePropertyStateListener(targetObject, psl);
        psl = null;

        isBound = false;
        hasEditedSource = false;
        hasEditedTarget = false;

        if (listeners != null) {
            for (BindingListener listener : listeners) {
                listener.bindingBecameUnbound(this);
            }
        }
    }
    
    protected abstract void unbindImpl();

    public final boolean isBound() {
        return isBound;
    }
    
    public final boolean getHasEditedSource() {
        throwIfUnbound();
        return hasEditedSource;
    }

    public final boolean getHasEditedTarget() {
        throwIfUnbound();
        return hasEditedTarget;
    }

    protected final void setManaged(boolean isManaged) {
        this.isManaged = isManaged;
    }

    protected final boolean isManaged() {
        return isManaged;
    }

    protected final void notifySynced() {
        if (listeners == null) {
            return;
        }

        for (BindingListener listener : listeners) {
            listener.synced(this);
        }
    }

    protected final void notifySyncFailed(SyncFailure... failures) {
        if (listeners == null) {
            return;
        }

        for (BindingListener listener : listeners) {
            listener.syncFailed(this, failures);
        }
    }

    private final SyncFailure notifyAndReturn(SyncFailure failure) {
        if (failure == null) {
            notifySynced();
        } else {
            notifySyncFailed(failure);
        }

        return failure;
    }

    public final SyncFailure refreshAndNotify() {
        return notifyAndReturn(refresh());
    }

    public final SyncFailure saveAndNotify() {
        return notifyAndReturn(save());
    }

    public final SyncFailure refresh() {
        throwIfManaged();
        return refreshUnmanaged();
    }
    
    protected final SyncFailure refreshUnmanaged() {
        throwIfUnbound();

        ValueResult<TV> vr = getSourceValueForTarget();
        if (vr.failed()) {
            return vr.getFailure();
        }

        try {
            ignoreChange = true;
            targetProperty.setValue(targetObject, vr.getValue());
        } finally {
            ignoreChange = false;
        }

        notifySourceEdited(false);
        notifyTargetEdited(false);
        return null;
    }

    public final SyncFailure save() {
        throwIfManaged();
        return saveUnmanaged();
    }
    
    protected final SyncFailure saveUnmanaged() {
        throwIfUnbound();

        ValueResult<SV> vr = getTargetValueForSource();
        if (vr.failed()) {
            return vr.getFailure();
        }

        try {
            ignoreChange = true;
            sourceProperty.setValue(sourceObject, vr.getValue());
        } finally {
            ignoreChange = false;
        }

        notifySourceEdited(false);
        notifyTargetEdited(false);
        return null;
    }

    private final Class<?> noPrimitiveType(Class<?> klass) {
        if (!klass.isPrimitive()) {
            return klass;
        }

        if (klass == Byte.TYPE) {
            return Byte.class;
        } else if (klass == Short.TYPE) {
            return Short.class;
        } else if (klass == Integer.TYPE) {
            return Integer.class;
        } else if (klass == Long.TYPE) {
            return Long.class;
        } else if (klass == Boolean.TYPE) {
            return Boolean.class;
        } else if (klass == Character.TYPE) {
            return Character.class;
        } else if (klass == Float.TYPE) {
            return Float.class;
        } else if (klass == Double.TYPE) {
            return Double.class;
        }

        throw new AssertionError();
    }

    private final TV convertForward(SV value) {
        if (converter == null) {
            Class<?> targetType = noPrimitiveType(targetProperty.getWriteType(targetObject));
            return (TV)targetType.cast(Converter.defaultConvert(value, targetType));
        }

        return converter.convertForward(value);
    }

    private final SV convertReverse(TV value) {
        if (converter == null) {
            Class<?> sourceType = noPrimitiveType(sourceProperty.getWriteType(sourceObject));
            return (SV)sourceType.cast(Converter.defaultConvert(value, sourceType));
        }

        return converter.convertReverse(value);
    }

    /**
     * Throws an UnsupportedOperationException if the {@code Binding} is managed.
     * Useful for calling at the beginning of method implementations that
     * shouldn't be called on managed {@code Bindings}
     *
     * @throws UnsupportedOperationException if the {@code Binding} is managed
     * @see #isManaged()
     */
    protected final void throwIfManaged() {
        if (isManaged()) {
            throw new UnsupportedOperationException("Can not call this method on a managed binding");
        }
    }
    
    /**
     * Throws an IllegalStateException if the {@code Binding} is bound.
     * Useful for calling at the beginning of method implementations that
     * shouldn't be called when the {@code Binding} is bound.
     *
     * @throws IllegalStateException if the {@code Binding} is bound.
     */
    protected final void throwIfBound() {
        if (isBound()) {
            throw new IllegalStateException("Can not call this method on a bound binding");
        }
    }

    /**
     * Throws an IllegalStateException if the {@code Binding} is unbound.
     * Useful for calling at the beginning of method implementations that should
     * only be called when the {@code Binding} is bound.
     *
     * @throws IllegalStateException if the {@code Binding} is unbound.
     */
    protected final void throwIfUnbound() {
        if (!isBound()) {
            throw new IllegalStateException("Can not call this method on an unbound binding");
        }
    }

    /**
     * Returns a string representation of the {@code Binding}. This
     * method is intended to be used for debugging purposes only, and
     * the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return a string representation of this {@code Binding}
     */
    public String toString() {
        return getClass().getName() + " [" + paramString() + "]";
    }

    private String paramString() {
        return "name=" + getName() +
               ", sourceObject=" + sourceObject +
               ", sourceProperty=" + sourceProperty +
               ", targetObject=" + targetObject +
               ", targetProperty" + targetProperty +
               ", validator=" + validator +
               ", converter=" + converter +
               ", sourceNullValue=" + sourceNullValue +
               ", targetNullValue=" + targetNullValue +
               ", sourceUnreadableValue=" + sourceUnreadableValue +
               ", hasChangedSource=" + hasEditedSource +
               ", hasChangedTarget=" + hasEditedTarget +
               ", bound=" + isBound();
    }
    
    private void sourceChanged(PropertyStateEvent pse) {
        if (!pse.getValueChanged()) {
            return;
        }

        notifySourceEdited(true);

        if (listeners != null) {
            for (BindingListener listener : listeners) {
                listener.sourceEdited(this);
            }
        }

        sourceChangedImpl(pse);
    }

    protected void sourceChangedImpl(PropertyStateEvent pse) {
    }

    private void targetChanged(PropertyStateEvent pse) {
        if (!pse.getValueChanged()) {
            return;
        }

        notifyTargetEdited(true);

        if (listeners != null) {
            for (BindingListener listener : listeners) {
                listener.targetEdited(this);
            }
        }

        targetChangedImpl(pse);
    }

    protected void targetChangedImpl(PropertyStateEvent pse) {
    }

    private void notifySourceEdited(boolean newValue) {
        boolean old = hasEditedSource;
        hasEditedSource = newValue;
        if (changeSupport != null) {
            changeSupport.firePropertyChange("hasEditedSource", old, hasEditedSource);
        }
    }
    
    private void notifyTargetEdited(boolean newValue) {
        boolean old = hasEditedTarget;
        hasEditedTarget = newValue;
        if (changeSupport != null) {
            changeSupport.firePropertyChange("hasEditedTarget", old, hasEditedTarget);
        }
    }

    /**
     * Adds a {@code PropertyChangeListener} to be notified when any property of
     * this {@code Binding} changes. Does nothing if the listener is
     * {@code null}. If a listener is added more than once, notifications are
     * sent to that listener once for every time that it has been added.
     * The ordering of listener notification is unspecified.
     * <p>
     * {@code Binding} fires property change notification for the following
     * properties:
     * <p>
     * <ul>
     *    <li>{@code hasEditedSource}
     *    <li>{@code hasEditedTarget}
     * </ul>
     * <p>
     * For other types of {@code Binding} notifications register a
     * {@code BindingListener}.
     *
     * @param listener the listener to add
     * @see #addBindingListener
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }

        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a {@code PropertyChangeListener} to be notified when the property identified
     * by the {@code propertyName} argument changes on this {@code Binding}.
     * Does nothing if the property name or listener is {@code null}.
     * If a listener is added more than once, notifications are
     * sent to that listener once for every time that it has been added.
     * The ordering of listener notification is unspecified.
     * <p>
     * {@code Binding} fires property change notification for the following
     * properties:
     * <p>
     * <ul>
     *    <li>{@code hasEditedSource}
     *    <li>{@code hasEditedTarget}
     * </ul>
     * <p>
     * For other types of {@code Binding} notifications register a
     * {@code BindingListener}.
     *
     * @param property the name of the property to listen for changes on
     * @param listener the listener to add
     */
    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }

        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Removes a {@code PropertyChangeListener} from the {@code Binding}. Does
     * nothing if the listener is {@code null} or is not one of those registered.
     * If the listener being removed was registered more than once, only one
     * occurrence of the listener is removed from the list of listeners.
     * The ordering of listener notification is unspecified.
     *
     * @param listener the listener to remove
     * @see #addPropertyChangeListener
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }

        changeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Removes a {@code PropertyChangeListener} from the {@code Binding} for the given
     * property name. Does nothing if the property name or listener is
     * {@code null} or the listener is not one of those registered.
     * If the listener being removed was registered more than once, only one
     * occurrence of the listener is removed from the list of listeners.
     * The ordering of listener notification is unspecified.
     * 
     * @param propertyName the name of the property to remove the listener for
     * @param listener the listener to remove
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }

        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Returns the list of {@code PropertyChangeListeners} registered on this
     * {@code Binding}. Order is undefined. Returns an empty array if there are
     * no listeners.
     *
     * @return the list of {@code PropertyChangeListeners} registered on this {@code Binding}
     * @see #addPropertyChangeListener
     */
    public final PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        
        return changeSupport.getPropertyChangeListeners();
    }

    /**
     * Returns the list of {@code PropertyChangeListeners} registered on this
     * {@code Binding} for the given property name. Order is undefined. Returns an empty array
     * if there are no listeners registered for the property name.
     *
     * @param propertyName the property name to retrieve the listeners for
     * @return the list of {@code PropertyChangeListeners} registered on this {@code Binding}
     *         for the given property name
     * @see #addPropertyChangeListener(String, PropertyChangeListener)
     */
    public final PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        
        return changeSupport.getPropertyChangeListeners(propertyName);
    }
    
    private class PSL implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (ignoreChange) {
                return;
            }

            if (pse.getSourceProperty() == sourceProperty && pse.getSourceObject() == sourceObject) {
                sourceChanged(pse);
            } else {
                targetChanged(pse);
            }
        }
    }

}
