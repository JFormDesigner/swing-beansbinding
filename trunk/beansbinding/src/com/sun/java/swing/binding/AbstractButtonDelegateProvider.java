/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.*;
import javax.beans.binding.ext.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.binding.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;

/**
 * @author Shannon Hickey
 */
public final class AbstractButtonDelegateProvider implements BeanDelegateProvider {

    private static final String PROPERTY = "selected";

    public static final class Delegate extends DelegateBase {
        private AbstractButton button;
        private ItemListener itemListener;

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
            itemListener = new Handler();
            button.addItemListener(itemListener);
        }

        protected void listeningStopped() {
            button.removeItemListener(itemListener);
            itemListener = null;
        }
        
        private class Handler implements ItemListener {
            public void itemStateChanged(ItemEvent ie) {
                firePropertyChange(!button.isSelected(), button.isSelected());
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
        return AbstractButtonDelegateProvider.Delegate.class;
    }

}
