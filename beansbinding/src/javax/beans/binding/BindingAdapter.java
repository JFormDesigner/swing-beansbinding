/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.EventListener;

/**
 * @author Shannon Hickey
 */
public abstract class BindingAdapter implements BindingListener {
    public void targetUnwriteable(Binding<?, ?> binding) {}

    public void targetUnreadable(Binding<?, ?> binding) {}
    public void sourceUnwriteable(Binding<?, ?> binding) {}

    public void targetEdited(Binding<?, ?> binding) {}

    public void conversionFailed(Binding<?, ?> binding, RuntimeException exception) {}
    public void validationFailed(Binding<?, ?> binding, Validator.Result result) {}

    public void bindingSynced(Binding<?, ?> binding) {}
}
