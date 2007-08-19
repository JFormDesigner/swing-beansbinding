/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Shannon Hickey
 */
public class Binding<SS, SV, TS, TV> {

    private String name;
    private SS sourceObject;
    private TS targetObject;
    private Property<SS, SV> sourceProperty;
    private Property<TS, TV> targetProperty;

    private boolean bound;
    private AutoUpdateStrategy strategy;
    private Validator<? super SV> validator;
    private Converter<SV, TV> converter;
    private TV sourceNullValue;
    private SV targetNullValue;
    private TV sourceUnreadableValue;
    private List<BindingListener> listeners;
    private PropertyStateListener psl;
    private boolean ignoreChange;
    private BindingGroup group;
    private boolean hasChangedTarget;

    public enum AutoUpdateStrategy {
        READ,
        READ_ONCE,
        READ_WRITE
    }

    public enum SyncFailureType {
        TARGET_UNWRITEABLE,
        SOURCE_UNWRITEABLE,
        TARGET_UNREADABLE,
        CONVERSION_FAILED,
        VALIDATION_FAILED
    }

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

        public SyncFailureType getType() {
            return type;
        }
        
        public RuntimeException getConversionException() {
            if (type != SyncFailureType.CONVERSION_FAILED) {
                throw new UnsupportedOperationException();
            }
            
            return (RuntimeException)reason;
        }

        public Validator.Result getValidationResult() {
            if (type != SyncFailureType.VALIDATION_FAILED) {
                throw new UnsupportedOperationException();
            }
            
            return (Validator.Result)reason;
        }

        public String toString() {
            return type + (reason == null ? "" : ": " + reason.toString());
        }
    }

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

        public boolean failed() {
            return failure != null;
        }

        public V getValue() {
            if (failed()) {
                throw new UnsupportedOperationException();
            }

            return value;
        }

        public SyncFailure getFailure() {
            if (!failed()) {
                throw new UnsupportedOperationException();
            }
            
            return failure;
        }

        public String toString() {
            return value == null ? "failure: " + failure : "value: " + value;
        }
    }

    protected Binding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        if (sourceProperty == null || targetProperty == null) {
            throw new IllegalArgumentException("source and target properties must be non-null");
        }

        this.sourceObject = sourceObject;
        this.sourceProperty = sourceProperty;
        this.targetObject = targetObject;
        this.targetProperty = targetProperty;
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public final Property<SS, SV> getSourceProperty() {
        return sourceProperty;
    }

    public final Property<TS, TV> getTargetProperty() {
        return targetProperty;
    }

    public final SS getSourceObject() {
        return sourceObject;
    }

    public final TS getTargetObject() {
        return targetObject;
    }

    public final void setSourceObject(SS sourceObject) {
        throwIfBound();
        this.sourceObject = sourceObject;
    }

    public final void setTargetObject(TS targetObject) {
        throwIfBound();
        this.targetObject = targetObject;
    }

    public final void setAutoUpdateStrategy(AutoUpdateStrategy strategy) {
        throwIfBound();
        this.strategy = strategy;
    }

    public final AutoUpdateStrategy getAutoUpdateStrategy() {
        AutoUpdateStrategy retVal = strategy;

        if (retVal == null && group != null) {
            retVal = group.getAutoUpdateStrategy();
        }

        return retVal == null ? AutoUpdateStrategy.READ_WRITE : retVal;
    }

    public final void setValidator(Validator<? super SV> validator) {
        throwIfBound();
        this.validator = validator;
    }

    public final Validator<? super SV> getValidator() {
        return validator;
    }

    public final void setConverter(Converter<SV, TV> converter) {
        throwIfBound();
        this.converter = converter;
    }

    public final Converter<SV, TV> getConverter() {
        return converter;
    }

    public final void setSourceNullValue(TV value) {
        throwIfBound();
        sourceNullValue = value;
    }

    public final TV getSourceNullValue() {
        return sourceNullValue;
    }

    public final void setTargetNullValue(SV value) {
        throwIfBound();
        targetNullValue = value;
    }

    public final SV getTargetNullValue() {
        return targetNullValue;
    }

    public final void setSourceUnreadableValue(TV value) {
        throwIfBound();
        sourceUnreadableValue = value;
    }

    public final TV getSourceUnreadableValue() {
        return sourceUnreadableValue;
    }

    public final void addBindingListener(BindingListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<BindingListener>();
        }

        listeners.add(listener);
    }

    public final void removeBindingListener(BindingListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

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

    private final void tryRefreshThenSave() {
        SyncFailure refreshFailure = simpleRefresh();
        if (refreshFailure == null) {
            synced();
        } else {
            SyncFailure saveFailure = simpleSave();
            if (saveFailure == null) {
                synced();
            } else {
                syncFailed(refreshFailure, saveFailure);
            }
        }
    }

    private final void trySaveThenRefresh() {
        SyncFailure saveFailure = simpleSave();
        if (saveFailure == null) {
            synced();
        } else if (saveFailure.getType() == SyncFailureType.CONVERSION_FAILED || saveFailure.getType() == SyncFailureType.VALIDATION_FAILED) {
            syncFailed(saveFailure);
        } else {
            SyncFailure refreshFailure = simpleRefresh();
            if (refreshFailure == null) {
                synced();
            } else {
                syncFailed(saveFailure, refreshFailure);
            }
        }
    }

    public final void bind() {
        throwIfBound();
        
        if (sourceProperty == targetProperty && sourceObject == targetObject) {
            throw new IllegalStateException("can't bind the same property on the same objects");
        }
        
        bound = true;
        bindImpl();
        if (group != null) {
            group.bindingBound(this);
        }
    }

    protected void bindImpl() {
        AutoUpdateStrategy strat = getAutoUpdateStrategy();
        
        if (strat == AutoUpdateStrategy.READ_ONCE) {
            refresh();
            psl = new PSL();
            sourceProperty.addPropertyStateListener(sourceObject, psl);
            targetProperty.addPropertyStateListener(targetObject, psl);
        } else if (strat == AutoUpdateStrategy.READ) {
            refresh();
            psl = new PSL();
            sourceProperty.addPropertyStateListener(sourceObject, psl);
            targetProperty.addPropertyStateListener(targetObject, psl);
        } else {
            tryRefreshThenSave();
            psl = new PSL();
            sourceProperty.addPropertyStateListener(sourceObject, psl);
            targetProperty.addPropertyStateListener(targetObject, psl);
        }
    }

    public final void unbind() {
        throwIfUnbound();
        bound = false;
        unbindImpl();
        if (group != null) {
            group.bindingUnbound(this);
        }
    }

    protected void unbindImpl() {
        sourceProperty.removePropertyStateListener(sourceObject, psl);
        targetProperty.removePropertyStateListener(targetObject, psl);
        psl = null;
    }

    public final boolean isBound() {
        return bound;
    }

    public final boolean getHasChangedTarget() {
        return hasChangedTarget;
    }
    
    void setBindingGroup(BindingGroup group) {
        this.group = group;
    }

    public final BindingGroup getBindingGroup() {
        return group;
    }

    private final void synced() {
        hasChangedTarget = false;

        if (listeners == null) {
            return;
        }

        for (BindingListener listener : listeners) {
            listener.synced(this);
        }
    }

    private final void syncFailed(SyncFailure... failures) {
        if (listeners == null) {
            return;
        }

        for (BindingListener listener : listeners) {
            listener.syncFailed(this, failures);
        }
    }

    private final void sourceChanged() {
        if (listeners == null) {
            return;
        }

        for (BindingListener listener : listeners) {
            listener.sourceChanged(this);
        }
    }

    private final void targetChanged() {
        hasChangedTarget = true;

        if (listeners == null) {
            return;
        }

        for (BindingListener listener : listeners) {
            listener.targetChanged(this);
        }
    }

    private final SyncFailure notifyAndReturn(SyncFailure failure) {
        if (failure == null) {
            synced();
        } else {
            syncFailed(failure);
        }

        return failure;
    }

    public final SyncFailure refresh() {
        return notifyAndReturn(simpleRefresh());
    }

    public final SyncFailure save() {
        return notifyAndReturn(simpleSave());
    }

    protected final SyncFailure simpleRefresh() {
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

        return null;
    }
    
    protected final SyncFailure simpleSave() {
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

        return null;
    }

    private final Class<?> noPrimitiveType(Class<?> klass) {
        if (!klass.isPrimitive()) {
            return klass;
        }

        if (klass == Short.TYPE) {
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
            return (TV)targetType.cast(value);
        }

        return converter.convertForward(value);
    }

    private final SV convertReverse(TV value) {
        if (converter == null) {
            Class<?> sourceType = noPrimitiveType(sourceProperty.getWriteType(sourceObject));
            return (SV)sourceType.cast(value);
        }

        return converter.convertReverse(value);
    }

    protected final void throwIfBound() {
        if (isBound()) {
            throw new IllegalStateException("Can not call this method on a bound binding");
        }
    }

    protected final void throwIfUnbound() {
        if (!isBound()) {
            throw new IllegalStateException("Can not call this method on an unbound binding");
        }
    }

    public String toString() {
        return getClass().getName() + " [" + paramString() + "]";
    }

    private String paramString() {
        return "name=" + getName() +
               ", sourceObject=" + sourceObject +
               ", sourceProperty=" + sourceProperty +
               ", targetObject=" + targetObject +
               ", targetProperty" + targetProperty +
               ", autoUpdateStrategy=" + getAutoUpdateStrategy() +
               ", validator=" + validator +
               ", converter=" + converter +
               ", sourceNullValue=" + sourceNullValue +
               ", targetNullValue=" + targetNullValue +
               ", sourceUnreadableValue=" + sourceUnreadableValue +
               ", bound=" + isBound();
    }

    private class PSL implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (ignoreChange) {
                return;
            }

            AutoUpdateStrategy strat = getAutoUpdateStrategy();

            if (pse.getSourceProperty() == sourceProperty && pse.getSourceObject() == sourceObject) {
                if (strat == AutoUpdateStrategy.READ_ONCE) {
                    if (pse.getValueChanged()) {
                        sourceChanged();
                    }
                } else if (strat == AutoUpdateStrategy.READ) {
                    if (pse.getValueChanged()) {
                        refresh();
                    }
                } else if (strat == AutoUpdateStrategy.READ_WRITE) {
                    if (pse.getValueChanged()) {
                        tryRefreshThenSave();
                    } else if (pse.getWriteableChanged() && pse.isWriteable()) {
                        save();
                    }
                }
            } else {
                if (strat == AutoUpdateStrategy.READ_ONCE) {
                    if (pse.getValueChanged()) {
                        targetChanged();
                    }
                } else if (strat == AutoUpdateStrategy.READ) {
                    if (pse.getWriteableChanged() && pse.isWriteable()) {
                        if (refresh() == null) {
                            return;
                        }
                    }

                    if (pse.getValueChanged()) {
                        targetChanged();
                    }
                } else if (strat == AutoUpdateStrategy.READ_WRITE) {
                    if (pse.getWriteableChanged() && pse.isWriteable()) {
                        tryRefreshThenSave();
                    } else {
                        trySaveThenRefresh();
                    }
                }
            }
        }
    }

}
