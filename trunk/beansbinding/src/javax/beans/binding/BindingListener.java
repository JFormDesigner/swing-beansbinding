/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.EventListener;

/**
 * {@code BindingListener}s are notified when either conversion or validation
 *  fails on a {@code Binding}.
 *
 * @see BindingConverter
 * @see BindingValidator
 * 
 * @author sky
 */
public interface BindingListener extends EventListener {
    /**
     * Notification that a {@code BindingValidator} has returned a {@code non-null}
     * value signaling a validation failure.
     *
     * @param binding the {@code Binding}
     * @param result the {@code ValidationResult}
     */
    public void validationFailed(Binding binding, ValidationResult result);
    
    /**
     * Notifcation that a {@code BindingConverter} threw an exception when
     * converting a value.
     *
     * @param binding the {@code Binding}
     * @param exception the exception the converter threw
     */
    public void converterFailed(Binding binding, Exception exception);
}
