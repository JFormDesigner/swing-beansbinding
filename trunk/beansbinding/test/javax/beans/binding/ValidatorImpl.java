/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import org.jdesktop.beans.binding.Binding;

class ValidatorImpl extends BindingValidator {
    private ValidationResult.Action type;
    
    ValidatorImpl(ValidationResult.Action type) {
        setType(type);
    }
    
    public void setType(ValidationResult.Action type) {
        this.type = type;
    }
    
    public ValidationResult validate(Binding binding, Object value) {
        return new ValidationResult(type);
    }
}