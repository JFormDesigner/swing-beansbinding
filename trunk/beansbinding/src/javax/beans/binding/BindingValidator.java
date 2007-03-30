/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * {@code BindingValidator} is responsible for validating the value
 * from the target of a {@code Binding}. Before the target value is set on the
 * source, the value is passed to the {@code
 * BindingValidator}. {@code BindingValidator} signals an invalid
 * value by returning a {@code non-null} value from the {@code
 * validate} method. All {@code ValidationResults} are forwarded to
 * any {@code BindingListener}s attached to the {@code BindingContext}.
 *
 * @see BindingContext
 * @see ValidationResult
 * @see BindingListener
 *
 * @author sky
 */ 
public abstract class BindingValidator {
    /**
     * Validates a value. An invalid value is identified by returning
     * a {@code non-null} value from this method.
     *
     * @param binding the binding
     * @param value the value to validate, may be {@code null}
     * @throws IllegalArgumentException if {@code binding} is {@code null}
     */
    public abstract ValidationResult validate(Binding binding, Object value);
}
