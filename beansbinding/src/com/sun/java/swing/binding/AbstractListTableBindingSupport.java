/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.beans.OLDBINDING.Binding.BindingController;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author sky
 */
// PENDING: rename!
abstract class AbstractListTableBindingSupport extends AbstractBindingHelper
        implements ListSelectionListener, PropertyChangeListener {
    private List<Object> selectedElements;
    
    public AbstractListTableBindingSupport() {
    }

    public void bind(BindingController controller,
            String property) {
        if (property == ELEMENTS_P) {
            bindElements(controller);
        }
    }

    public void unbind(BindingController controller,
            String property) {
        if (property == ELEMENTS_P) {
            unbindElements();
        }
    }
    
    protected abstract void bindElements(BindingController controller);
    
    protected abstract void unbindElements();
    
    protected boolean shouldCreateBindingTarget(String property) {
        return (property == ELEMENTS_P ||
                property == SELECTED_ELEMENTS_P);
    }

    private void updateSelectedElements() {
        ListSelectionModel selectionModel = getSelectionModel();
        if (selectionModel != null) {
            int min = selectionModel.getMinSelectionIndex();
            int max = selectionModel.getMaxSelectionIndex();
            List<Object> newSelection;
            if (min < 0 || max < 0) {
                newSelection = new ArrayList<Object>(0);
            } else {
                min = Math.min(getElements().size() - 1, min);
                max = Math.min(getElements().size() - 1, max);
                newSelection = new ArrayList<Object>(max - min);
                for (int i = min; i <= max; i++) {
                    if (selectionModel.isSelectedIndex(i)) {
                        newSelection.add(getElement(i));
                    }
                }
            }
            this.selectedElements = newSelection;
        } else {
            this.selectedElements = new ArrayList<Object>(0);
        }
    }

    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName() == "selectionModel") {
            selectionModelChanged((ListSelectionModel)e.getOldValue(),
                    (ListSelectionModel)e.getNewValue());
        }
    }

    protected void selectionModelChanged(ListSelectionModel oldModel,
            ListSelectionModel newModel) {
        oldModel.removeListSelectionListener(this);
        newModel.addListSelectionListener(this);
    }

    protected void selectionChanged() {
        List<Object> oldSelection = this.selectedElements;
        this.selectedElements = null;
        getDelegate().firePropertyChange(SELECTED_ELEMENTS_P,
                oldSelection, this.selectedElements);
        getDelegate().firePropertyChange(SELECTED_ELEMENT_P,
                null, null);
    }

    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            selectionChanged();
        }
    }

    protected Object getElement(int i) {
        return getElements().get(viewToModel(i));
    }

    protected abstract void setElements(List<?> elements);

    protected abstract List<?> getElements();

    protected void setSelectedElements(List<?> selection) {
        List<Object> oldSelection = this.selectedElements;
        List<Object> newSelection;
        ListSelectionModel selectionModel = getSelectionModel();
        if (selection == null) {
            newSelection = new ArrayList<Object>(0);
            selectionModel.clearSelection();
        } else {
            newSelection = new ArrayList<Object>(selection);
            selectionModel.setValueIsAdjusting(true);
            selectionModel.clearSelection();
            for (Object elem : newSelection) {
                int index = getElements().indexOf(elem);
                if (index != -1) {
                    index = modelToView(index);
                    selectionModel.addSelectionInterval(index, index);
                }
            }
            selectionModel.setValueIsAdjusting(false);
        }
        this.selectedElements = newSelection;
        // PENDING: is this necessary? Seems like changing the selection
        // will trigger seletionChanged to be invoked, which fires too.
        getDelegate().firePropertyChange(SELECTED_ELEMENTS_P, oldSelection,
                newSelection);
        getDelegate().firePropertyChange(SELECTED_ELEMENT_P, null, null);

    }

    protected int viewToModel(int index) {
        return index;
    }

    protected int modelToView(int index) {
        return index;
    }
    
    protected abstract ListSelectionModel getSelectionModel();

    protected abstract PropertyDelegate getDelegate();

    protected List<?> getSelectedElements() {
        if (selectedElements == null) {
            updateSelectedElements();
        }
        return selectedElements;
    }

    protected abstract Object getSelectedElement();
    

    public final class PropertyDelegate extends DelegateBase {
        public void setElements(List<?> elements) {
            AbstractListTableBindingSupport.this.setElements(elements);
        }
        
        public List<?> getElements() {
            return AbstractListTableBindingSupport.this.getElements();
        }

        public void setSelectedElements(List<?> elements) {
            AbstractListTableBindingSupport.this.setSelectedElements(elements);
        }
        
        public List<?> getSelectedElements() {
            return AbstractListTableBindingSupport.this.getSelectedElements();
        }

        public void setSelectedElement(Object element) {
            if (element == null) {
                AbstractListTableBindingSupport.this.setSelectedElements(
                        Collections.emptyList());
            } else {
                AbstractListTableBindingSupport.this.setSelectedElements(
                        Arrays.asList(element));
            }
        }
        
        public Object getSelectedElement() {
            return AbstractListTableBindingSupport.this.getSelectedElement();
        }
    }


    // PENDING: nuke this once BeanInfoAPT is working
    public static final class PropertyDelegateBeanInfo extends BindingBeanInfo {
        protected Class<?> getPropertyDelegateClass() {
            return PropertyDelegate.class;
        }

        protected Property[] getPreferredProperties() {
            return new Property[] {
                new Property("elements", "The contents to display"),
                new Property("selectedElement", "The selected element"),
                new Property("selectedElements", "The selected elements")
            };
        }
    }
}
