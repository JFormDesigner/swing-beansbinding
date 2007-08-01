/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 *
 * @author Shannon Hickey
 */
public class BindingGroup {
    public void addBinding(Binding binding) {}
    public void addBinding(Property source, Property target) {}
    public void addBinding(String name, Property source, Property target) {}
    public void bind() {}
    public void unbind() {}
}
