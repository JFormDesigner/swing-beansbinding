/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.OLDBINDING.ext;

import javax.beans.OLDBINDING.Binding.BindingController;

/**
 * {@code BindingTarget} allows the target of a binding operation to
 * control the binding.
 * <p>
 * A bound {@code Binding} checks the target if it
 * implements {@code BindingTargetProvider}. If the target implements
 * {@code BindingTargetProvider}, then {@code createBindingTarget} is
 * invoked on it supplying a {@code BindingController}.
 * <p>
 * {@code BindingTarget} is primarily useful for targets that need to
 * customize how the binding works, such as a {@code JTable} binding.
 * 
 * @author sky
 */
public interface BindingTarget {
    /**
     * Notifies this {@code BindingTarget} that it has become the target of
     * an active binding.
     *
     * @param controller describes the binding
     * @param property the property being bound to
     *
     * @throws NullPointerException if {@code controller} or
     *         {@code property} is {@code null}
     * @throws IllegalArgumentException if {@code property} is not a valid
     *         property, or the binding does not contain necessary information
     *         for this {@code BindingTarget}
     * @throws IllegalStateException if already bound
     */
    public void bind(BindingController controller, String property);

    /**
     * Notifies this {@code BindingTarget} that it is no longer the target of
     * an active binding.
     *
     * @param controller describes the binding
     * @param property the property being bound to
     *
     * @throws NullPointerException if {@code controller} or
     *         {@code property} is {@code null}
     * @throws IllegalArgumentException if {@code property} is not a valid
     *         property
     * @throws IllegalStateException if not bound
     */
    public void unbind(BindingController controller, String property);

    /**
     * Notification that the value state of the source has changed.
     *
     * @param controller identifies the binding
     * @param property the property
     */
    public void sourceValueStateChanged(BindingController controller,
            String property);
}
