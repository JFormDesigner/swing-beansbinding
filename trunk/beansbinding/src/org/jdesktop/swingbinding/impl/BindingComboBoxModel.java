/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding.impl;

import org.jdesktop.swingbinding.*;
import org.jdesktop.swingbinding.JComboBoxBinding.DetailBinding;
import org.jdesktop.swingbinding.JComboBoxBinding.IDBinding;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import javax.swing.event.*;
import java.beans.*;

/**
 * @author Shannon Hickey
 */
public final class BindingComboBoxModel extends ListBindingManager implements ComboBoxModel  {
    private final List<ListDataListener> listeners;
    private Object selectedObject;
    private int selectedModelIndex;
    private AbstractColumnBinding detailBinding;
    private AbstractColumnBinding idBinding;
    private PropertyChangeSupport changeSupport;

    public BindingComboBoxModel(DetailBinding detailBinding, IDBinding idBinding) {
        this.detailBinding = detailBinding;
        this.idBinding = idBinding;
        listeners = new CopyOnWriteArrayList<ListDataListener>();
    }

    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }

        changeSupport.addPropertyChangeListener(propertyName, listener);
    }
    
    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }

        changeSupport.removePropertyChangeListener(propertyName, listener);
    }
    
    public Object getSelectedElement() {
        return selectedModelIndex == -1 ? selectedObject : getElement(selectedModelIndex);
    }

    public Object getSelectedElementID() {
        if (idBinding == null) {
            return getSelectedElement();
        }

        if (selectedModelIndex == -1) {
            return selectedObject;
        }

        try {
            idBinding.setSourceObjectInternal(getElement(selectedModelIndex));
            return idBinding.getSourceValueForTarget().getValue();
        } finally {
            idBinding.setSourceObjectInternal(null);
        }
    }

    public void setElements(List<?> elements) {
        super.setElements(elements);
        if (size() > 0 && selectedObject == null) {
            selectedObject = getElementAt(0);
            selectedModelIndex = 0;
        }
    }
    
    protected AbstractColumnBinding[] getColBindings() {
        return new AbstractColumnBinding[] {detailBinding};
    }
    
    public Object getSelectedItem() {
        return selectedObject;
    }
    
    public void setSelectedItem(Object anObject) {
        // This is what DefaultComboBoxModel does (yes, yuck!)
        if ((selectedObject != null && !selectedObject.equals(anObject)) ||
                selectedObject == null && anObject != null) {
            selectedObject = anObject;
            contentsChanged(-1, -1);
            
            selectedModelIndex = -1;
            if (anObject != null) {
                int size = size();
                for (int i = 0; i < size; i++) {
                    if (anObject.equals(getElementAt(i))) {
                        selectedModelIndex = i;
                        break;
                    }
                }
            }
        }
    }
    
    protected void allChanged() {
        contentsChanged(0, size());
    }
    
    protected void valueChanged(int row, int column) {
        contentsChanged(row, row);
        if (row == selectedModelIndex) {
            selectedObject = getElementAt(row);
            contentsChanged(-1, -1);
            if (changeSupport != null) {
                changeSupport.firePropertyChange("selectedElement", null, null);
            }
        }
    }
    
    protected void added(int index, int length) {
        ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index + length - 1);
        for (ListDataListener listener : listeners) {
            listener.intervalAdded(e);
        }
        
        if (size() == length && selectedObject == null && getElementAt(0) != null) {
            setSelectedItem(getElementAt(0));
        }
    }
    
    protected void removed(int index, List removedElements) {
        boolean removedSelected = false;
        int length = removedElements.size();
        
        try {
            for (Object element : removedElements) {
                detailBinding.setSourceObjectInternal(element);
                Object detail = detailBinding.getSourceValueForTarget().getValue();
                if (areObjectsEqual(detail, selectedObject)) {
                    removedSelected = true;
                    break;
                }
            }
        } finally {
            detailBinding.setSourceObjectInternal(null);
        }
        
        ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index + length - 1);
        for (ListDataListener listener : listeners) {
            listener.intervalRemoved(e);
        }
        
        if (removedSelected) {
            if (size() == 0) {
                setSelectedItem(null);
            } else {
                setSelectedItem(getElementAt(Math.max(index - 1, 0)));
            }
        }
    }
    
    protected void changed(int row) {
        contentsChanged(row, row);
        if (row == selectedModelIndex) {
            selectedObject = getElementAt(row);
            contentsChanged(-1, -1);
            if (changeSupport != null) {
                changeSupport.firePropertyChange("selectedElement", null, null);
            }
        }
    }
    
    private void contentsChanged(int row0, int row1) {
        ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, row0, row1);
        for (ListDataListener listener : listeners) {
            listener.contentsChanged(e);
        }
    }
    
    public Object getElementAt(int index) {
        return valueAt(index, 0);
    }
    
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }
    
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }
    
    public int getSize() {
        return size();
    }

    private static final boolean areObjectsEqual(Object o1, Object o2) {
        return ((o1 != null && o1.equals(o2)) ||
                (o1 == null && o2 == null));
    }
}
