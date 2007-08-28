/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.EventListener;

/**
 * An abstract subclass of {@code BindingListener} that simplifies writing
 * {@code BindingListeners} by allowing you to extend this class and re-implement
 * only the methods you care about.
 *
 * @author Shannon Hickey
 */
public abstract class AbstractBindingListener implements BindingListener {

    /**
     * {@inheritDoc}
     */
    public void bindingBecameBound(Binding binding) {}

    /**
     * {@inheritDoc}
     */
    public void bindingBecameUnbound(Binding binding) {}

    /**
     * {@inheritDoc}
     */
    public void syncFailed(Binding binding, Binding.SyncFailure... failures) {}

    /**
     * {@inheritDoc}
     */
    public void synced(Binding binding) {}

    /**
     * {@inheritDoc}
     */
    public void sourceEdited(Binding binding) {}

    /**
     * {@inheritDoc}
     */
    public void targetEdited(Binding binding) {}

}
