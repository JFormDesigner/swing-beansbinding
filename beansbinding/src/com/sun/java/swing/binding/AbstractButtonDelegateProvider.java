/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.ext.*;
import javax.swing.*;
import java.awt.event.*;
import java.beans.*;

/**
 * @author Shannon Hickey
 */
public final class AbstractButtonDelegateProvider implements BeanDelegateProvider {

    private static final String PROPERTY = "selected";

    public static final class Delegate extends DelegateBase {
        private AbstractButton button;
        private Handler handler;
        private boolean cachedSelected;

        private Delegate(AbstractButton button) {
            super(PROPERTY);
            this.button = button;
        }

        public boolean isSelected() {
            return button.isSelected();
        }

        public void setSelected(boolean selected) {
            button.setSelected(selected);
        }

        protected void listeningStarted() {
            handler = new Handler();
            button.addItemListener(handler);
            button.addPropertyChangeListener("model", handler);
        }

        protected void listeningStopped() {
            button.removeItemListener(handler);
            button.removePropertyChangeListener("model", handler);
            handler = null;
        }
        
        private class Handler implements ItemListener, PropertyChangeListener {
            private void buttonSelectedChanged() {
                boolean oldSelected = cachedSelected;
                cachedSelected = isSelected();
                firePropertyChange(oldSelected, cachedSelected);
            }
            
            public void itemStateChanged(ItemEvent ie) {
                buttonSelectedChanged();
            }

            public void propertyChange(PropertyChangeEvent pe) {
                buttonSelectedChanged();
            }
        }
    }

    public boolean providesDelegate(Class<?> type, String property) {
        return AbstractButton.class.isAssignableFrom(type) && property.intern() == PROPERTY;
    }

    public Object createPropertyDelegate(Object source, String property) {
        if (!providesDelegate(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }

        return new Delegate((AbstractButton)source);
    }

    public Class<?> getPropertyDelegateClass(Class<?> type) {
        return AbstractButton.class.isAssignableFrom(type) ?
            AbstractButtonDelegateProvider.Delegate.class :
            null;
    }

}
