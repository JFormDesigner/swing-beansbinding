/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.ext.*;
import javax.swing.*;
import javax.swing.event.*;
import java.beans.*;

/**
 * @author Shannon Hickey
 */
public final class JSliderDelegateProvider implements BeanDelegateProvider {

    private static final String PROPERTY_BASE = "value";
    private static final String IGNORE_ADJUSTING = PROPERTY_BASE + "_IGNORE_ADJUSTING";

    public static final class Delegate extends DelegateBase {
        private JSlider slider;
        private Handler handler;
        private int cachedValue;

        private Delegate(JSlider slider, String property) {
            super(property);
            this.slider = slider;
        }

        public int getValue() {
            return slider.getValue();
        }

        public int getValue_IGNORE_ADJUSTING() {
            return getValue();
        }

        public void setValue(int value) {
            slider.setValue(value);
        }
        
        public void setValue_ON_ADJUSTING(int value) {
            setValue(value);
        }

        protected void listeningStarted() {
            handler = new Handler();
            cachedValue = getValue();
            slider.addChangeListener(handler);
            slider.addPropertyChangeListener("model", handler);
        }

        protected void listeningStopped() {
            slider.removeChangeListener(handler);
            slider.removePropertyChangeListener("model", handler);
            handler = null;
        }
        
        private class Handler implements ChangeListener, PropertyChangeListener {
            private void sliderValueChanged() {
                int oldValue = cachedValue;
                cachedValue = getValue();
                firePropertyChange(oldValue, cachedValue);
            }

            public void stateChanged(ChangeEvent ce) {
                if (property == IGNORE_ADJUSTING && slider.getValueIsAdjusting()) {
                    return;
                }

                sliderValueChanged();
            }

            public void propertyChange(PropertyChangeEvent pe) {
                sliderValueChanged();
            }
        }
    }

    public boolean providesDelegate(Class<?> type, String property) {
        if (!JSlider.class.isAssignableFrom(type)) {
            return false;
        }

        return property == PROPERTY_BASE ||
               property == IGNORE_ADJUSTING;
    }

    public Object createPropertyDelegate(Object source, String property) {
        if (!providesDelegate(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }

        return new Delegate((JSlider)source, property);
    }

    public Class<?> getPropertyDelegateClass(Class<?> type) {
        return JSlider.class.isAssignableFrom(type) ?
            JSliderDelegateProvider.Delegate.class :
            null;
    }

}
