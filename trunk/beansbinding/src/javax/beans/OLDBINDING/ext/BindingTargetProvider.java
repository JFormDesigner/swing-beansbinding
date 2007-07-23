/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding.ext;

/**
 * {@code BindingTargetProvider} may be implemented by the target (or
 * property delegate) of a binding. {@code BindingTargetProvider} allows
 * a target to provide a {@code BindingTarget} that can control the binding.
 *
 * @see BindingTarget
 *
 * @author sky
 */
public interface BindingTargetProvider {
    /**
     * Returns the {@code BindingTarget} for the specified property, or
     * {@code null} if a {@code BindingTarget} is not applicable for the
     * specified property.
     *
     * @param property the name of the property
     * @return the {@code BindingTarget}, or {@code null} if a
     *         {@code BindingTarget} should not be used
     */
    public BindingTarget createBindingTarget(String property);
}
