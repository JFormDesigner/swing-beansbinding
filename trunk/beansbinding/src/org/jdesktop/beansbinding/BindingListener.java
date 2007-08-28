/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.EventListener;

/**
 * {@code BindingListeners} are registered on {@code Bindings} or {@code BindingGroups}
 * to listen for changes to the state of {@code Bindings}
 *
 * @see Binding
 * @see BindingGroup
 *
 * @author Shannon Hickey
 */
public interface BindingListener extends EventListener {
    
    /**
     * Notification that a {@code Binding} has been bound.
     *
     * @param binding the {@code Binding}
     */
    public void bindingBecameBound(Binding binding);

    /**
     * Notification that a {@code Binding} has been unbound.
     *
     * @param binding the {@code Binding}
     */
    public void bindingBecameUnbound(Binding binding);

    /**
     * Notification that the {@code Binding} attempted to sync the source and
     * target, but the sync failed.
     *
     * @param binding the {@code Binding}
     * @param failures the reasons the sync failed
     */
    public void syncFailed(Binding binding, Binding.SyncFailure... failures);

    /**
     * Notification that the source and target of a {@code Binding} have
     * been made in sync.
     *
     * @param binding the {@code Binding}
     */
    public void synced(Binding binding);

    /**
     * Notification that the source property of a {@code Binding} has fired
     * a {@code PropertyStateEvent} indicating that its value has changed
     * for the {@code Binding's} source object.
     *
     * @param binding the {@code Binding}
     */
    public void sourceEdited(Binding binding);

    /**
     * Notification that the target property of a {@code Binding} has fired
     * a {@code PropertyStateEvent} indicating that its value has changed
     * for the {@code Binding's} target object.
     *
     * @param binding the {@code Binding}
     */
    public void targetEdited(Binding binding);
}
