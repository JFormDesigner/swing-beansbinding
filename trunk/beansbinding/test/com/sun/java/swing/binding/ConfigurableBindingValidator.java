/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.Binding;
import javax.beans.binding.BindingValidator;
import javax.beans.binding.ValidationResult;

/**
 *
 * @author sky
 */
public class ConfigurableBindingValidator extends BindingValidator{
    private ValidationResult.Action type;

    public void setType(ValidationResult.Action type) {
        this.type = type;
    }
    
    public ValidationResult validate(Binding binding, Object value) {
        return new ValidationResult(type);
    }
}
