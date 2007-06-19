/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import com.sun.java.swing.binding.ListBindingManager.ColumnDescription;
import java.awt.Component;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.beans.binding.Binding;
import javax.beans.binding.ELPropertyResolver;
import javax.beans.binding.ext.PropertyDelegateProvider;
import javax.el.Expression;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.binding.SwingBindingSupport;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author sky
 */
class JComboBoxBindingHelper extends AbstractBindingHelper {
    private final JComboBox comboBox;
    private final PropertyDelegate delegate;
    private BindingModel model;
    private Binding.BindingController controller;
    private Binding valueBinding;
    private Binding.BindingController selectedElementController;
    private ELPropertyResolver selectedElementResolver;

    // PENDING: disable on incomplete path
    // PENDING: this should support not replacing the model.
    public JComboBoxBindingHelper(JComboBox comboBox) {
        this.comboBox = comboBox;
        delegate = new PropertyDelegate();
    }

    public Object getPropertyDelegate() {
        return delegate;
    }

    protected boolean shouldCreateBindingTarget(String property) {
        return (property == ELEMENTS_P ||
                property == JCOMBO_BOX_SELECTED_ELEMENT_P);
    }

    public void bind(Binding.BindingController controller, String property) {
        if (property == ELEMENTS_P) {
            throwIfNonNull(this.controller);
            this.controller = controller;
            String selectedElementPath = controller.getBinding().getValue(
                    SwingBindingSupport.ComboBoxSelectedObjectPropertyParameter, null);
            if (selectedElementPath != null) {
                selectedElementResolver = controller.createResolver();
                selectedElementResolver.setPath(selectedElementPath);
            } else {
                selectedElementResolver = null;
            }
            model = new BindingModel(controller);
            comboBox.setModel(model);
        } else if (property == JCOMBO_BOX_SELECTED_ELEMENT_P) {
            throwIfNonNull(this.selectedElementController);
            selectedElementController = controller;
            updateComponentEnabledFromBinding();
        }
    }

    public void unbind(Binding.BindingController controller, String property) {
        if (property == ELEMENTS_P) {
            this.controller = null;
            if (comboBox.getModel() == model) {
                comboBox.setModel(new DefaultComboBoxModel());
            }
            valueBinding = null;
            model.setElements(null);
            model = null;
        } else if (property == JCOMBO_BOX_SELECTED_ELEMENT_P) {
            selectedElementController = null;
        }
    }

    public void sourceValueStateChanged(Binding.BindingController controller,
            String property) {
        if (property == JCOMBO_BOX_SELECTED_ELEMENT_P) {
            updateComponentEnabledFromBinding();
        }
    }

    protected void setElements(List<?> elements) {
        model.setElements(elements);
        if (comboBox.getModel() != model) {
            comboBox.setModel(model);
        }
    }

    protected List<?> getElements() {
        return model.getElements();
    }
    
    protected Binding getBinding() {
        return selectedElementController.getBinding();
    }
    
    protected Component getComponent() {
        return comboBox;
    }

    private boolean areObjectsEqual(Object o1, Object o2) {
        return ((o1 != null && o1.equals(o2)) ||
                (o1 == null && o2 == null));
    }

    private void setSelectedElement(Object toSelect) {
        List<?> elements = model.getElements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            Object e = elements.get(i);
            if (areObjectsEqual(e, toSelect)) {
                model.setSelectedItem(model.valueAt(i, 0, true));
                return;
            }
        }
        // If we get here, the item isn't contained in the list. Clear the
        // selection
        model.setSelectedItem(null);
    }

    private Object getSelectedElement() {
        Object selected = model.getSelectedItem();
        for (int i = model.size() - 1; i >= 0; i--) {
            if (areObjectsEqual(model.valueAt(i, 0, true), selected)) {
                return model.getElements().get(i);
            }
        }
        // Can't find a match
        return null;
    }

    private void setSelectedElementProperty(Object id) {
        if (selectedElementResolver != null) {
            for (int i = 0; i < model.getSize(); i++) {
                Object element = model.getElement(i);
                selectedElementResolver.setSource(element);
                Expression.Result result = selectedElementResolver.evaluate();
                if (result.getType() == Expression.Result.Type.SINGLE_VALUE &&
                        areObjectsEqual(result.getResult(), id)) {
                    model.setSelectedItem(model.valueAt(i, 0, true));
                    return;
                }
            }
        }
        // If we get here, the item isn't contained in the list. Clear the
        // selection
        model.setSelectedItem(null);
    }

    private Object getSelectedElementProperty() {
        if (selectedElementResolver != null) {
            Object selected = model.getSelectedItem();
            for (int i = model.getSize() - 1; i >= 0; i--) {
                Object value = model.valueAt(i, 0, true);
                if (areObjectsEqual(value, selected)) {
                    selectedElementResolver.setSource(model.getElement(i));
                    Expression.Result result = selectedElementResolver.evaluate();
                    if (result.getType() == Expression.Result.Type.SINGLE_VALUE) {
                        return result.getResult();
                    }
                }
            }
        }
        return null;
    }
    
    private void selectionChanged() {
        delegate.firePropertyChange(SELECTED_ELEMENT_P, null, null);
        delegate.firePropertyChange(JCOMBO_BOX_SELECTED_ELEMENT_P, null, null);
    }

    
    private final class BindingModel extends ListBindingManager implements 
            ComboBoxModel {
        private final List<ListDataListener> listeners;
        private Object selectedObject;

        BindingModel(Binding.BindingController controller) {
            super(controller);
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }

        protected void createColumnDescriptions(
                List<ListBindingManager.ColumnDescription> columns) {
            valueBinding = null;
            for (Binding childBinding : getController().getBinding().getChildBindings()) {
                // PENDING: this only supports one, so it should throw
                // otherwise.
                valueBinding = childBinding;
                columns.add(new ColumnDescription(childBinding, 0, true,
                        Object.class));
            }
        }

        protected void allChanged() {
            contentsChanged(0, size());
        }

        protected void valueChanged(int row, int column) {
            contentsChanged(row, row);
        }

        protected void added(int index, int length) {
            ListDataEvent e = new ListDataEvent(this,
                    ListDataEvent.INTERVAL_ADDED, index, index + length - 1);
            for (ListDataListener listener : listeners) {
                listener.intervalAdded(e);
            }
        }

        protected void removed(int index, int length) {
            ListDataEvent e = new ListDataEvent(this,
                    ListDataEvent.INTERVAL_REMOVED, index, index + length - 1);
            for (ListDataListener listener : listeners) {
                listener.intervalRemoved(e);
            }
        }

        protected void changed(int row) {
            contentsChanged(row, row);
        }

        public int getSize() {
            return size();
        }

        public Object getElementAt(int index) {
            return valueAt(index, 0, true);
        }

        public void setSelectedItem(Object anObject) {
            // This is what DefaultComboBoxModel does (yes, yuck!)
            if ((selectedObject != null && !selectedObject.equals( anObject )) ||
                    selectedObject == null && anObject != null) {
                selectedObject = anObject;
                contentsChanged(-1, -1);
                selectionChanged();
            }
        }

        public Object getSelectedItem() {
            return selectedObject;
        }

        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }

        private void contentsChanged(int row0, int row1) {
            ListDataEvent e = new ListDataEvent(this,
                    ListDataEvent.CONTENTS_CHANGED, row0, row1);
            for (ListDataListener listener : listeners) {
                listener.contentsChanged(e);
            }
        }
    }

    
    public final class PropertyDelegate extends DelegateBase {
        public void setElements(List<?> elements) {
            JComboBoxBindingHelper.this.setElements(elements);
        }
        
        public List<?> getElements() {
            return JComboBoxBindingHelper.this.getElements();
        }

        public void setSelectedElement(Object element) {
            JComboBoxBindingHelper.this.setSelectedElement(element);
        }
        
        public Object getSelectedElement() {
            return JComboBoxBindingHelper.this.getSelectedElement();
        }
        
        public void setSelectedElementProperty(Object id) {
            JComboBoxBindingHelper.this.setSelectedElementProperty(id);
        }
        
        public Object getSelectedElementProperty() {
            return JComboBoxBindingHelper.this.getSelectedElementProperty();
        }
    }


    // PENDING: nuke this once BeanInfoAPT is working
    public static final class PropertyDelegateBeanInfo extends BindingBeanInfo {
        protected Class<?> getPropertyDelegateClass() {
            return PropertyDelegate.class;
        }

        protected Property[] getPreferredProperties() {
            return new Property[] {
                new Property("elements", "The conents of the combobox"),
                new Property("selectedElement", "The selected element of the combobox"),
                new Property("selectedElementProperty", "PENDING: nuke this!"),
            };
        }
    }
}
