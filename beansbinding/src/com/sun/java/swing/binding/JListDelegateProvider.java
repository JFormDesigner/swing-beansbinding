/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.ext.*;
import java.beans.*;
import javax.swing.*;
import sun.swing.binding.*;
import javax.swing.event.*;

/**
 * @author Shannon Hickey
 */
public final class JListDelegateProvider implements BeanDelegateProvider {

    private static final String PROPERTY_BASE = "selectedElement";
    private static final String IGNORE_ADJUSTING = PROPERTY_BASE + "_IGNORE_ADJUSTING";
    private static final String ON_ADJUSTING = PROPERTY_BASE + "_ON_ADJUSTING";

    public final class Delegate extends DelegateBase {
        private JList list;
        private Handler handler;
        private Object cachedElement;

        private Delegate(JList component, String property) {
            super(property);
            this.list = component;
        }

        public Object getSelectedElement() {
            return JListDelegateProvider.getSelectedElement(list);
        }
        
        public Object getSelectedElement_IGNORE_ADJUSTING() {
            return getSelectedElement();
        }

        public Object getSelectedElement_ON_ADJUSTING() {
            return getSelectedElement();
        }
        
        protected void listeningStarted() {
            handler = new Handler();
            cachedElement = JListDelegateProvider.getSelectedElement(list);
            list.addPropertyChangeListener("selectionModel", handler);
            list.getSelectionModel().addListSelectionListener(handler);
        }
        
        protected void listeningStopped() {
            list.getSelectionModel().removeListSelectionListener(handler);
            list.removePropertyChangeListener("selectionModel", handler);
            cachedElement = null;
            handler = null;
        }

        private class Handler implements ListSelectionListener, PropertyChangeListener {
            private void listSelectionChanged() {
                Object oldElement = cachedElement;
                cachedElement = getSelectedElement();
                firePropertyChange(oldElement, cachedElement);
            }

            public void valueChanged(ListSelectionEvent e) {
                if ((property == PROPERTY_BASE || property == IGNORE_ADJUSTING) && e.getValueIsAdjusting()) {
                    return;
                }

                listSelectionChanged();
            }
            
            public void propertyChange(PropertyChangeEvent pce) {
                listSelectionChanged();
            }
        }
    }

    private static Object getSelectedElement(JList list) {
        assert list != null;

        // PENDING(shannonh) - more cases to consider
        int index = list.getSelectionModel().getLeadSelectionIndex();
        index = list.getSelectionModel().isSelectedIndex(index) ? index : -1;
        
        if (index == -1) {
            return null;
        }

        ListModel model = list.getModel();
        return model instanceof ListBindingManager ? ((ListBindingManager)model).getElement(index)
                                                   : model.getElementAt(index);
    }
    
    public boolean providesDelegate(Class<?> type, String property) {
        property = property.intern();

        if (!JList.class.isAssignableFrom(type)) {
            return false;
        }

        return property == PROPERTY_BASE ||
               property == IGNORE_ADJUSTING ||
               property == ON_ADJUSTING;
                 
    }
    
    public Object createPropertyDelegate(Object source, String property) {
        if (!providesDelegate(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }
        
        return new Delegate((JList)source, property);
    }
    
    public Class<?> getPropertyDelegateClass(Class<?> type) {
        return JListDelegateProvider.Delegate.class;
    }
    
}
