/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import java.awt.Component;
import javax.beans.binding.Binding;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author sky
 */
class JSliderBindingHelper extends AbstractBindingHelper {
    private static final String VALUE_PROPERTY = "value";
    private final JSlider slider;
    private final PropertyDelegate delegate;
    private Object value;
    private Binding.BindingController controller;
    private final ChangeListener handler;
    
    public JSliderBindingHelper(JSlider slider) {
        this.slider = slider;
        delegate = new PropertyDelegate();
        handler = new ChangeHandler();
    }

    public Object getPropertyDelegate() {
        return delegate;
    }
    
    private void setValue(int value) {
        slider.setValue(value);
    }

    private int getValue() {
        return slider.getValue();
    }
    
    private void sliderValueChanged() {
        Object lastValue = this.value;
        this.value = slider.getValue();
        delegate.firePropertyChange(VALUE_PROPERTY, lastValue, value);
    }
    
    protected boolean shouldCreateBindingTarget(String property) {
        return (property == VALUE_P);
    }

    public void bind(Binding.BindingController controller, String property) {
        throwIfNonNull(this.controller);
        assert (property == VALUE_P);
        value = slider.getValue();
        slider.addChangeListener(handler);
        this.controller = controller;
        updateComponentEnabledFromBinding();
    }

    public void unbind(Binding.BindingController controller, String property) {
        slider.removeChangeListener(handler);
        this.controller = null;
    }

    public void sourceValueStateChanged(Binding.BindingController controller, 
            String property) {
        updateComponentEnabledFromBinding();
    }

    protected Component getComponent() {
        return slider;
    }
    
    protected Binding getBinding() {
        return controller.getBinding();
    }

    
    public final class PropertyDelegate extends DelegateBase {
        public void setValue(int value) {
            JSliderBindingHelper.this.setValue(value);
        }
        
        public int getValue() {
            return JSliderBindingHelper.this.getValue();
        }
    }
    

    // PENDING: nuke this once BeanInfoAPT is working
    public static final class PropertyDelegateBeanInfo extends BindingBeanInfo {
        protected Class<?> getPropertyDelegateClass() {
            return PropertyDelegate.class;
        }

        protected Property[] getPreferredProperties() {
            return new Property[] {
                new Property("value", "The sliders current value")
            };
        }
    }
    
    
    private final class ChangeHandler implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            // PENDING: offer ability to conditionalize this
            sliderValueChanged();
        }
    }
}
