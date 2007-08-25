/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding.adapters;

import javax.swing.*;
import java.awt.event.*;
import java.beans.*;
import org.jdesktop.beansbinding.ext.BeanAdapterProvider;
import org.jdesktop.swingbinding.impl.BindingComboBoxModel;

/**
 * @author Shannon Hickey
 */
public final class JComboBoxAdapterProvider implements BeanAdapterProvider {

    private static final String SELECTED_ELEMENT_P = "selectedElement";
    private static final String SELECTED_ELEMENT_ID_P = "selectedElementID";

    public static final class Adapter extends BeanAdapterBase {
        private JComboBox combo;
        private Handler handler;
        private Object cachedElementOrID;

        private Adapter(JComboBox combo, String property) {
            super(property);
            this.combo = combo;
        }

        private boolean isID() {
            return property == SELECTED_ELEMENT_ID_P;
        }
        
        public Object getSelectedElement() {
            return JComboBoxAdapterProvider.getSelectedElement(combo);
        }

        public Object getSelectedElementID() {
            return JComboBoxAdapterProvider.getSelectedElementID(combo);
        }

        protected void listeningStarted() {
            handler = new Handler();
            cachedElementOrID = isID() ? getSelectedElementID() : getSelectedElement();
            combo.addActionListener(handler);
            ComboBoxModel model = combo.getModel();
            if (model instanceof BindingComboBoxModel) {
                ((BindingComboBoxModel)model).addPropertyChangeListener(property, handler);
            }
            combo.addPropertyChangeListener("model", handler);
        }

        protected void listeningStopped() {
            combo.removeActionListener(handler);
            ComboBoxModel model = combo.getModel();
            if (model instanceof BindingComboBoxModel) {
                ((BindingComboBoxModel)model).removePropertyChangeListener(property, handler);
            }
            combo.removePropertyChangeListener("model", handler);
            handler = null;
            cachedElementOrID = null;
        }

        private class Handler implements ActionListener, PropertyChangeListener {
            private void comboSelectionChanged() {
                Object oldValue = cachedElementOrID;
                cachedElementOrID = isID() ?
                    JComboBoxAdapterProvider.getSelectedElementID(combo) :
                    JComboBoxAdapterProvider.getSelectedElement(combo);
                firePropertyChange(oldValue, cachedElementOrID);
            }

            public void actionPerformed(ActionEvent ae) {
                comboSelectionChanged();
            }

            public void propertyChange(PropertyChangeEvent pce) {
                String propertyName = pce.getPropertyName();

                if (propertyName == "model") {
                    ComboBoxModel model = (ComboBoxModel)pce.getOldValue();
                    if (model instanceof BindingComboBoxModel) {
                        ((BindingComboBoxModel)model).removePropertyChangeListener(property, this);
                    }
                    model = (ComboBoxModel)pce.getNewValue();
                    if (model instanceof BindingComboBoxModel) {
                        ((BindingComboBoxModel)model).addPropertyChangeListener(property, this);
                    }
                }

                comboSelectionChanged();
            }
        }
    }

    private static Object getSelectedElement(JComboBox combo) {
        ComboBoxModel model = combo.getModel();

        return model instanceof BindingComboBoxModel ?
            ((BindingComboBoxModel)model).getSelectedElement() :
            model.getSelectedItem();
    }
    
    private static Object getSelectedElementID(JComboBox combo) {
        ComboBoxModel model = combo.getModel();

        return model instanceof BindingComboBoxModel ?
            ((BindingComboBoxModel)model).getSelectedElementID() :
            model.getSelectedItem();
    }
    
    public boolean providesAdapter(Class<?> type, String property) {
        if (!JComboBox.class.isAssignableFrom(type)) {
            return false;
        }

        return property == SELECTED_ELEMENT_P ||
               property == SELECTED_ELEMENT_ID_P;
    }

    public Object createAdapter(Object source, String property) {
        if (!providesAdapter(source.getClass(), property)) {
            throw new IllegalArgumentException();
        }

        return new Adapter((JComboBox)source, property);
    }

    public Class<?> getAdapterClass(Class<?> type) {
        return JComboBox.class.isAssignableFrom(type) ?
            JComboBoxAdapterProvider.Adapter.class :
            null;
    }

}
