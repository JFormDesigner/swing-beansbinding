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
import javax.swing.table.*;
import java.util.HashMap;

/**
 * @author Shannon Hickey
 */
public final class JTableAdapterProvider implements BeanAdapterProvider {

    private static final String PROPERTY_BASE = "selectedElement";
    private static final String IGNORE_ADJUSTING = PROPERTY_BASE + "_IGNORE_ADJUSTING";

    public final class Adapter extends BeanAdapterBase {
        private JTable table;
        private Handler handler;
        private Object cachedElement;

        private Adapter(JTable table, String property) {
            super(property);
            this.table = table;
        }

        public Object getSelectedElement() {
            return JTableAdapterProvider.getSelectedElement(table);
        }
        
        public Object getSelectedElement_IGNORE_ADJUSTING() {
            return getSelectedElement();
        }

        protected void listeningStarted() {
            handler = new Handler();
            cachedElement = JTableAdapterProvider.getSelectedElement(table);
            table.addPropertyChangeListener("selectionModel", handler);
            table.getSelectionModel().addListSelectionListener(handler);
        }
        
        protected void listeningStopped() {
            table.getSelectionModel().removeListSelectionListener(handler);
            table.removePropertyChangeListener("selectionModel", handler);
            cachedElement = null;
            handler = null;
        }

        private class Handler implements ListSelectionListener, PropertyChangeListener {
            private void tableSelectionChanged() {
                Object oldElement = cachedElement;
                cachedElement = getSelectedElement();
                firePropertyChange(oldElement, cachedElement);
            }

            public void valueChanged(ListSelectionEvent e) {
                if (property == IGNORE_ADJUSTING && e.getValueIsAdjusting()) {
                    return;
                }

                tableSelectionChanged();
            }
            
            public void propertyChange(PropertyChangeEvent pce) {
                tableSelectionChanged();
            }
        }
    }

    private static Object getSelectedElement(JTable table) {
        assert table != null;

        // PENDING(shannonh) - more cases to consider
        int index = table.getSelectionModel().getLeadSelectionIndex();
        index = table.getSelectionModel().isSelectedIndex(index) ? index : -1;
        
        if (index == -1) {
            return null;
        }

        TableModel model = table.getModel();
        if (model instanceof ListBindingManager) {
            return ((ListBindingManager)model).getElement(index);
        } else {
            int columnCount = model.getColumnCount();
            // PENDING(shannonh) - need to support editing values in this map!
            HashMap map = new HashMap(columnCount);
            for (int i = 0; i < columnCount; i++) {
                // PENDING(shannonh) - find better identifiers
                // can't use plain column name since it might be a bad identifier
                // but can we convert it to a propert identifier?
                map.put("column" + i, model.getValueAt(index, i));
            }
            return map;
        }
    }
    
    public boolean providesAdapter(Class<?> type, String property) {
        property = property.intern();

        if (!JTable.class.isAssignableFrom(type)) {
            return false;
        }

        return property == PROPERTY_BASE ||
               property == IGNORE_ADJUSTING;
                 
    }
    
    public Object createAdapter(Object source, String property) {
        if (!providesAdapter(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }
        
        return new Adapter((JTable)source, property);
    }
    
    public Class<?> getAdapterClass(Class<?> type) {
        return JTable.class.isAssignableFrom(type) ?
            JTableAdapterProvider.Adapter.class :
            null;
    }
    
}
