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
public final class JListAdapterProvider implements BeanAdapterProvider {

    private static final String PROPERTY_BASE = "selectedElement";
    private static final String IGNORE_ADJUSTING = PROPERTY_BASE + "_IGNORE_ADJUSTING";

    public final class Adapter extends BeanAdapterBase {
        private JList list;
        private Handler handler;
        private Object cachedElement;

        private Adapter(JList list, String property) {
            super(property);
            this.list = list;
        }

        public Object getSelectedElement() {
            return JListAdapterProvider.getSelectedElement(list);
        }
        
        public Object getSelectedElement_IGNORE_ADJUSTING() {
            return getSelectedElement();
        }

        protected void listeningStarted() {
            handler = new Handler();
            cachedElement = JListAdapterProvider.getSelectedElement(list);
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
                if (property == IGNORE_ADJUSTING && e.getValueIsAdjusting()) {
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
    
    public boolean providesAdapter(Class<?> type, String property) {
        property = property.intern();

        if (!JList.class.isAssignableFrom(type)) {
            return false;
        }

        return property == PROPERTY_BASE ||
               property == IGNORE_ADJUSTING;
                 
    }
    
    public Object createAdapter(Object source, String property) {
        if (!providesAdapter(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }
        
        return new Adapter((JList)source, property);
    }
    
    public Class<?> getAdapterClass(Class<?> type) {
        return JList.class.isAssignableFrom(type) ? 
            JListAdapterProvider.Adapter.class :
            null;
    }
    
}
