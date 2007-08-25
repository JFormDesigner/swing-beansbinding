/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.ObjectProperty;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.beansbinding.PropertyStateListener;
import org.jdesktop.swingbinding.impl.AbstractColumnBinding;
import org.jdesktop.swingbinding.impl.ListBindingManager;
import org.jdesktop.swingbinding.impl.*;

/**
 * @author Shannon Hickey
 */
public final class JComboBoxBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {
    
    private ElementsProperty<TS, JComboBox> ep;
    private Handler handler = new Handler();
    private BindingComboBoxModel model;
    private JComboBox combo;
    
    protected JComboBoxBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JComboBox> targetJComboBoxProperty, String name) {
        super(strategy, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JComboBox>(targetJComboBoxProperty), name);
        ep = (ElementsProperty<TS, JComboBox>)getTargetProperty();
    }
    
    protected void bindImpl() {
        model = new BindingComboBoxModel();
        // order is important for the next two lines
        ep.addPropertyStateListener(null, handler);
        ep.installBinding(this);
        super.bindImpl();
    }
    
    protected void unbindImpl() {
        // order is important for the next two lines
        ep.uninstallBinding();
        ep.removePropertyStateListener(null, handler);
        model = null;
        super.unbindImpl();
    }
    
    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (!pse.getValueChanged()) {
                return;
            }
            
            Object newValue = pse.getNewValue();
            
            if (newValue == PropertyStateEvent.UNREADABLE) {
                combo.setModel(new DefaultComboBoxModel());
                combo = null;
                model.setElements(null);
            } else {
                combo = ep.getComponent();
                model.setElements((List<E>)newValue);
                combo.setModel(model);
            }
        }
    }

    private final class BindingComboBoxModel extends ListBindingManager implements ComboBoxModel  {
        private final List<ListDataListener> listeners;
        private Object selectedItem = null;
        private int selectedModelIndex = -1;

        public BindingComboBoxModel() {
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }

        public void setElements(List<?> elements) {
            super.setElements(elements);

            if (size() <= 0) {
                if (selectedModelIndex != -1) {
                    selectedModelIndex = -1;
                    selectedItem = null;
                }
            } else {
                if (selectedItem == null) {
                    selectedModelIndex = 0;
                    selectedItem = getElementAt(selectedModelIndex);
                }
            }
        }

        protected AbstractColumnBinding[] getColBindings() {
            return new AbstractColumnBinding[0];
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public void setSelectedItem(Object item) {
            // This is what DefaultComboBoxModel does (yes, yuck!)
            if ((selectedItem != null && !selectedItem.equals(item)) || selectedItem == null && item != null) {
                selectedItem = item;
                contentsChanged(-1, -1);
                selectedModelIndex = -1;
                if (item != null) {
                    int size = size();
                    for (int i = 0; i < size; i++) {
                        if (item.equals(getElementAt(i))) {
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
            // we're not expecting any value changes since we don't have any
            // detail bindings for JComboBox
        }

        protected void added(int index, int length) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index + length - 1);
            int size = listeners.size();
            for (int i = size - 1; i >= 0; i--) {
                listeners.get(i).intervalAdded(e);
            }

            if (size() == length && selectedItem == null) {
                setSelectedItem(getElementAt(0));
            }
        }

        protected void removed(int index, int length) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index + length - 1);
            int size = listeners.size();
            for (int i = size - 1; i >= 0; i--) {
                listeners.get(i).intervalRemoved(e);
            }
            
            if (selectedModelIndex >= index && selectedModelIndex < index + length) {
                if (size() == 0) {
                    setSelectedItem(null);
                } else {
                    setSelectedItem(getElementAt(Math.max(index - 1, 0)));
                }
            }
        }

        protected void changed(int row) {
            contentsChanged(row, row);
        }

        private void contentsChanged(int row0, int row1) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, row0, row1);
            int size = listeners.size();
            for (int i = size - 1; i >= 0; i--) {
                listeners.get(i).contentsChanged(e);
            }
        }
        
        public Object getElementAt(int index) {
            return getElement(index);
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
    }

}
