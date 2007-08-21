/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.EventListener;

/**
 * @author Shannon Hickey
 */
public interface BindingListener extends EventListener {
    public void bindingBecameBound(Binding binding);
    public void bindingBecameUnbound(Binding binding);
    public void syncFailed(Binding binding, Binding.SyncFailure... failures);
    public void synced(Binding binding);
    public void sourceEdited(Binding binding);
    public void targetEdited(Binding binding);
}
