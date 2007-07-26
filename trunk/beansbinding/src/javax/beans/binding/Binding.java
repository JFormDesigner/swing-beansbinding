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
    private AutoUpdateStrategy strategy = AutoUpdateStrategy.READ_WRITE;
    private Validator<? super S> validator;
    private Converter<S, T> converter;
    private T sourceNullValue;
    private S targetNullValue;
    private T sourceUnreadableValue;
    private List<BindingListener> listeners;
    private boolean inSync;
    private PropertyStateListener psl;

    public enum AutoUpdateStrategy {
        READ,
        READ_ONCE,
        READ_WRITE
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

        if (strategy == null) {
            throw new IllegalArgumentException("Update strategy may not be null");
        }

        this.strategy = strategy;
    }

    public final AutoUpdateStrategy getAutoUpdateStrategy() {
        return strategy;
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

    public void bind() {
        throwIfBound();
        bound = true;

        if (strategy == AutoUpdateStrategy.READ_ONCE) {
            refresh();
        } else if (strategy == AutoUpdateStrategy.READ) {
            refresh();
            psl = new PSL();
            source.addPropertyStateListener(psl);
        } else {
            refresh();
            if (!inSync) {
                save();
            }
            psl = new PSL();
            source.addPropertyStateListener(psl);
            target.addPropertyStateListener(psl);
        }
    }

    public void unbind() {
        throwIfUnbound();
        if (psl != null) {
            source.removePropertyStateListener(psl);
            target.removePropertyStateListener(psl);
            psl = null;
        }
        bound = false;
    }

    public boolean isBound() {
        return bound;
    }

    public final boolean isInSync() {
        throwIfUnbound();
        return inSync;
    }

    public final void refresh() {
        throwIfUnbound();

        if (!target.isWriteable()) {
            inSync = false;

            for (BindingListener listener : listeners) {
                listener.targetUnwriteable(this);
            }

            return;
        }

        T targetValue;

        if (source.isReadable()) {
            S sourceValue = source.getValue();

            if (sourceValue == null) {
                targetValue = sourceNullValue;
            } else {
                // may throw ClassCastException or other RuntimeException here
                targetValue = convertForward(sourceValue);
            }
        } else {
            targetValue = sourceUnreadableValue;
        }

        target.setValue(targetValue);
        inSync = true;

        for (BindingListener listener : listeners) {
            listener.bindingInSync(this);
        }
    }

    public final void save() {
        throwIfUnbound();

        if (!target.isReadable()) {
            inSync = false;

            for (BindingListener listener : listeners) {
                listener.targetUnreadable(this);
            }

            return;
        }

        if (!source.isWriteable()) {
            inSync = false;

            for (BindingListener listener : listeners) {
                listener.sourceUnwriteable(this);
            }

            return;
        }

        S sourceValue = null;

        T targetValue = target.getValue();

        if (targetValue == null) {
            sourceValue = targetNullValue;
        } else {
            try {
                sourceValue = convertReverse(targetValue);
            } catch (ClassCastException cce) {
                throw cce;
            } catch (RuntimeException rte) {
                inSync = false;

                for (BindingListener listener : listeners) {
                    listener.conversionFailed(this, rte);
                }
                
                return;
            }

            if (validator != null) {
                Validator.Result vr = validator.validate(this, sourceValue);
                if (vr != null) {
                    inSync = false;

                    for (BindingListener listener : listeners) {
                        listener.validationFailed(this, vr);
                    }

                    return;
                }
            }
        }

        source.setValue(sourceValue);
        inSync = true;

        for (BindingListener listener : listeners) {
            listener.bindingInSync(this);
        }
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
               ", autoUpdateStrategy=" + strategy +
               ", validator=" + validator +
               ", converter=" + converter +
               ", sourceNullValue=" + sourceNullValue +
               ", targetNullValue=" + targetNullValue +
               ", sourceUnreadableValue=" + sourceUnreadableValue +
               ", bound=" + isBound();
    }

    private class PSL implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            System.out.println(pse);
        }
    }

}
