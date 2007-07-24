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

    public void bind() {
        bound = true;
    }

    public void unbind() {
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

    protected final void throwIfNotBound() {
        if (!isBound()) {
            throw new IllegalStateException("Can not call this method on an unbound binding");
        }
    }

    public String toString() {
        return getClass().getName() + " [" + paramString() + "]";
    }

    private String paramString() {
        return "name=" + getName() +
               ", source=" + getSource() +
               ", target=" + getTarget() +
               ", bound=" + isBound();// +

               //", updateStrategy=" + updateStrategy +

               //", validator=" + validator +
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
