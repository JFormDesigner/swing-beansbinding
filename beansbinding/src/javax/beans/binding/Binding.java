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
    private UpdateStrategy strategy;
    private Validator validator;

    public enum UpdateStrategy {
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

    public final void setUpdateStrategy(UpdateStrategy strategy) {
        throwIfBound();

        if (strategy == null) {
            throw new IllegalArgumentException("Update strategy may not be null");
        }

        this.strategy = strategy;
    }

    public final UpdateStrategy getUpdateStrategy() {
        return strategy;
    }

    public final void setValidator(Validator validator) {
        throwIfBound();
        this.validator = validator;
    }

    public final Validator getValidator() {
        return validator;
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
               ", updateStrategy=" + strategy +
               ", validator=" + validator +
               ", bound=" + isBound();// +
                





               //", converter=" + converter +
               //", valueForIncompleteTargetPath=" + incompleteTargetPathValue +
               //", valueForIncompleteSourcePath=" + incompleteSourcePathValue +
               //", sourceValueState=" + sourceValueState +
               //", targetValueState=" + targetValueState +
               //", childBindings=" + childBindings +
               //", keepUncommited=" + keepUncommitted +
               //", parameters=" + parameters;
    }

}
