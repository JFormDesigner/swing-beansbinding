/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * @author Shannon Hickey
 */
public class Binding {

    private String name;
    private Property source;
    private Property target;

    private boolean bound;
    private AutoUpdateStrategy strategy;
    private Validator validator;
    private Converter<?,?> converter;
    private Object sourceNullValue;
    private Object targetNullValue;
    private Object sourceUnreadableValue;

    public enum AutoUpdateStrategy {
        READ,
        READ_ONCE,
        READ_WRITE
    }

    public Binding(Property source, Property target) {
        this(null, source, target);
    }

    public Binding(String name, Property source, Property target) {
        this.name = name;
        this.source = source;
        this.target = target;
    }

    public final String getName() {
        return name;
    }

    public final Property getSource() {
        return source;
    }

    public final Property getTarget() {
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

    public final void setValidator(Validator validator) {
        throwIfBound();
        this.validator = validator;
    }

    public final Validator getValidator() {
        return validator;
    }

    public final void setConverter(Converter<?, ?> converter) {
        throwIfBound();
        this.converter = converter;
    }

    public final Converter<?, ?> getConverter() {
        return converter;
    }

    public final void setSourceNullValue(Object value) {
        throwIfBound();
        sourceNullValue = value;
    }

    public final Object getSourceNullValue() {
        return sourceNullValue;
    }

    public final void setTargetNullValue(Object value) {
        throwIfBound();
        targetNullValue = value;
    }

    public final Object getTargetNullValue() {
        return targetNullValue;
    }

    public final void setSourceUnreadableValue(Object value) {
        throwIfBound();
        sourceUnreadableValue = value;
    }

    public final Object getSourceUnreadableValue() {
        return sourceUnreadableValue;
    }

    public void bind() {
        throwIfBound();
        bound = true;
    }

    public void unbind() {
        throwIfUnbound();
        bound = false;
    }

    public boolean isBound() {
        return bound;
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

}
