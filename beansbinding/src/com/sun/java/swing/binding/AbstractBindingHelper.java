/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import java.awt.Component;
import javax.beans.binding.Binding;
import javax.beans.binding.ext.BindingTarget;
import javax.beans.binding.ext.BindingTargetProvider;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.beans.binding.Binding.BindingController;
import javax.swing.binding.ParameterKeys;

/**
 *
 * @author sky
 */
abstract class AbstractBindingHelper implements BindingTarget {
    public abstract Object getPropertyDelegate();
    
    static final String ELEMENTS_P = "elements";
    static final String SELECTED_ELEMENTS_P = "selectedElements";
    static final String SELECTED_ELEMENT_P = "selectedElement";
    
    static final String JCHECK_BOX_SELECTED_P = "selected";
    
    static final String JCOMBO_BOX_SELECTED_ELEMENT_P = "selectedElementProperty";

    static final String JTEXT_COMPONENT_TEXT_P = "text";
    
    static final String JTREE_ROOT_P = "root";

    static final String VALUE_P = "value";
    
    protected boolean shouldCreateBindingTarget(String property) {
        return false;
    }

    public void bind(BindingController controller,
            String property) {
    }
        
    public void unbind(BindingController controller,
            String property) {
    }

    public void sourceValueStateChanged(BindingController controller,
            String property) {
    }
    
    protected void throwIfNonNull(BindingController controller) {
        if (controller != null) {
            throw new IllegalStateException(
                    "Can not bind to an already bound component");
        }
    }
    
    protected Binding getBinding() {
        return null;
    }
    
    protected Component getComponent() {
        return null;
    }

    protected void updateComponentEnabledFromBinding() {
        if (getDisableOnIncompletePath()) {
            if (getBinding().getSourceValueState() ==
                    Binding.ValueState.INCOMPLETE_PATH) {
                setComponentEnabled(false);
            } else {
                setComponentEnabled(true);
            }
        }
    }
    
    protected void setComponentEnabled(boolean enable) {
        getComponent().setEnabled(enable);
    }

    protected boolean getDisableOnIncompletePath() {
        return getBinding().getParameter(
                ParameterKeys.DISABLE_ON_INCOMPLETE_PATH, Boolean.TRUE);
    }
    
    
    public abstract class DelegateBase implements BindingTargetProvider {
        private final PropertyChangeSupport changeSupport;
        
        public DelegateBase() {
            changeSupport = new PropertyChangeSupport(this);
        }
        
        public BindingTarget createBindingTarget(String property) {
            if (shouldCreateBindingTarget(property)) {
                return AbstractBindingHelper.this;
            }
            return null;
        }
        
        public void addPropertyChangeListener(PropertyChangeListener l) {
            changeSupport.addPropertyChangeListener(l);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener l) {
            changeSupport.removePropertyChangeListener(l);
        }
        
        public void addPropertyChangeListener(String key,
                PropertyChangeListener l) {
            changeSupport.addPropertyChangeListener(key, l);
        }
        
        public void removePropertyChangeListener(String key,
                PropertyChangeListener l) {
            changeSupport.removePropertyChangeListener(key, l);
        }
        
        protected void firePropertyChange(String key, Object oldValue, Object newValue) {
            changeSupport.firePropertyChange(key, oldValue, newValue);
        }
    }
}
