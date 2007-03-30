/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding.ext;

import javax.beans.binding.Binding;
import javax.beans.binding.Binding.BindingController;

/**
 *
 * @author sky
 */
public class TestBeanBindingTarget implements BindingTarget {
    private BindingController controller;
    private String property;
    private boolean isBound;
    
    public void bind(BindingController controller, String property) {
        if (isBound) {
            throw new RuntimeException("bind invoked when alread bound");
        }
        this.controller = controller;
        this.property = property;
        isBound = true;
    }

    public void unbind(BindingController controller, String property) {
        if (!isBound) {
            throw new RuntimeException("unbind invoked without bind");
        }
        this.controller = null;
        this.property = null;
        isBound = false;
    }
    
    public boolean isBound() {
        return isBound;
    }
    
    public BindingController getController() {
        return controller;
    }
    
    public String getProperty() {
        return property;
    }

    public void sourceValueStateChanged(Binding.BindingController controller, String property) {
    }
}
