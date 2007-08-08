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
public class Binding<S, T> {

    private String name;
    private Property<S> source;
    private Property<T> target;

    private boolean bound;
    private AutoUpdateStrategy strategy;
    private Validator<? super S> validator;
    private Converter<S, T> converter;
    private T sourceNullValue;
    private S targetNullValue;
    private T sourceUnreadableValue;
    private List<BindingListener> listeners;
    private PropertyStateListener psl;
    private boolean ignoreChange;
    private BindingGroup group;

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
            this.failure = failure;
        }

        public boolean failed() {
            return failure != null;
        }

        public V getValue() {
            if (value == null) {
                throw new UnsupportedOperationException();
            }

            return value;
        }

        public SyncFailure getFailure() {
            if (failure == null) {
                throw new UnsupportedOperationException();
            }
            
            return failure;
        }

        public String toString() {
            return value == null ? "failure: " + failure : "value: " + value;
        }
    }

    public Binding(Property<S> source, Property<T> target) {
        this(null, source, target);
    }

    public Binding(String name, Property<S> source, Property<T> target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("source and target must be non-null");
        }

        if (source == target) {
            throw new IllegalArgumentException("can't bind a property to itself");
        }

        this.name = name;
        this.source = source;
        this.target = target;
    }

    public final String getName() {
        return name;
    }

    public final Property<S> getSource() {
        return source;
    }

    public final Property<T> getTarget() {
        return target;
    }

    public final void setAutoUpdateStrategy(AutoUpdateStrategy strategy) {
        throwIfBound();
        this.strategy = strategy;
    }

    public final AutoUpdateStrategy getAutoUpdateStrategy() {
        if (strategy != null) {
            return strategy;
        } else if (group != null) {
            return group.getAutoUpdateStrategy();
        } else {
            return AutoUpdateStrategy.READ_WRITE;
        }
    }

    public final void setValidator(Validator<? super S> validator) {
        throwIfBound();
        this.validator = validator;
    }

    public final Validator<? super S> getValidator() {
        return validator;
    }

    public final void setConverter(Converter<S, T> converter) {
        throwIfBound();
        this.converter = converter;
    }

    public final Converter<S, T> getConverter() {
        return converter;
    }

    public final void setSourceNullValue(T value) {
        throwIfBound();
        sourceNullValue = value;
    }

    public final T getSourceNullValue() {
        return sourceNullValue;
    }

    public final void setTargetNullValue(S value) {
        throwIfBound();
        targetNullValue = value;
    }

    public final S getTargetNullValue() {
        return targetNullValue;
    }

    public final void setSourceUnreadableValue(T value) {
        throwIfBound();
        sourceUnreadableValue = value;
    }

    public final T getSourceUnreadableValue() {
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

    public final ValueResult<T> getSourceValueForTarget() {
        if (!target.isWriteable()) {
            return new ValueResult<T>(SyncFailure.TARGET_UNWRITEABLE);
        }

        T value;

        if (source.isReadable()) {
            S rawValue = source.getValue();

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

        return new ValueResult<T>(value);
    }

    public final ValueResult<S> getTargetValueForSource() {
        if (!target.isReadable()) {
            return new ValueResult<S>(SyncFailure.TARGET_UNREADABLE);
        }

        if (!source.isWriteable()) {
            return new ValueResult<S>(SyncFailure.SOURCE_UNWRITEABLE);
        }

        S value = null;
        T rawValue = target.getValue();

        if (rawValue == null) {
            value = targetNullValue;
        } else {
            try {
                value = convertReverse(rawValue);
            } catch (ClassCastException cce) {
                throw cce;
            } catch (RuntimeException rte) {
                return new ValueResult<S>(SyncFailure.conversionFailure(rte));
            }

            if (validator != null) {
                Validator.Result vr = validator.validate(value);
                if (vr != null) {
                    return new ValueResult<S>(SyncFailure.validationFailure(vr));
                }
            }
        }

        return new ValueResult<S>(value);
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
            source.addPropertyStateListener(psl);
            target.addPropertyStateListener(psl);
        } else if (strat == AutoUpdateStrategy.READ) {
            refresh();
            psl = new PSL();
            source.addPropertyStateListener(psl);
            target.addPropertyStateListener(psl);
        } else {
            tryRefreshThenSave();
            psl = new PSL();
            source.addPropertyStateListener(psl);
            target.addPropertyStateListener(psl);
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
        source.removePropertyStateListener(psl);
        target.removePropertyStateListener(psl);
        psl = null;
    }

    public final boolean isBound() {
        return bound;
    }

    void setBindingGroup(BindingGroup group) {
        this.group = group;
    }

    public final BindingGroup getBindingGroup() {
        return group;
    }

    private final void synced() {
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

        ValueResult<T> vr = getSourceValueForTarget();
        if (vr.failed()) {
            return vr.getFailure();
        }

        try {
            ignoreChange = true;
            target.setValue(vr.getValue());
        } finally {
            ignoreChange = false;
        }

        return null;
    }
    
    protected final SyncFailure simpleSave() {
        throwIfUnbound();

        ValueResult<S> vr = getTargetValueForSource();
        if (vr.failed()) {
            return vr.getFailure();
        }

        try {
            ignoreChange = true;
            source.setValue(vr.getValue());
        } finally {
            ignoreChange = false;
        }

        return null;
    }

    private final T convertForward(S value) {
        if (converter == null) {
            Class<? extends T> targetType = target.getWriteType();
            return targetType.cast(value);
        }

        return converter.convertForward(value);
    }

    private final S convertReverse(T value) {
        if (converter == null) {
            Class<? extends S> sourceType = source.getWriteType();
            return sourceType.cast(value);
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
               ", source=" + source +
               ", target=" + target +
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

            if (pse.getSource() == source) {
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
