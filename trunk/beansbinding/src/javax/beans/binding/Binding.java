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

    public enum UpdateStrategy {
        READ,
        READ_ONCE,
        READ_WRITE
    }

    public Binding(String name, Property source, Property target) {
        this.name = name;
        this.source = source;
        this.target = target;
    }

    public Binding(Property source, Property targetProperty) {
    }

    public String getName() {
        return name;
    }

    public Object getSource() {
        return source;
    }

    public Property getTarget() {
        return target;
    }

    public String toString() {
        return getClass().getName() + " [" + paramString() + "]";
    }

    private String paramString() {
        return "name=" + getName() +
               ", source=" + getSource() +
               ", target=" + getTarget();
        
               //", updateStrategy=" + updateStrategy +
               //", bound=" + isBound() +
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
