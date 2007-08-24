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

/**
 * @author Shannon Hickey
 */
public final class JComboBoxBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {

    private ElementsProperty<TS, JComboBox> ep;
    private Handler handler = new Handler();
    private BindingComboBoxModel model;
    private JComboBox combo;
    private DetailBinding detailBinding;
    private IDBinding IDBinding;

    protected JComboBoxBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JComboBox> targetJComboBoxProperty, String name) {
        super(strategy, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JComboBox>(targetJComboBoxProperty), name);
        ep = (ElementsProperty<TS, JComboBox>)getTargetProperty();
        setDetailBinding(null);
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

    public DetailBinding setDetailBinding(Property<E, ?> detailProperty) {
        return setDetailBinding(detailProperty, null);
    }

    public DetailBinding setDetailBinding(Property<E, ?> detailProperty, String name) {
        throwIfBound();

        if (name == null && JComboBoxBinding.this.getName() != null) {
            name = JComboBoxBinding.this.getName() + ".DETAIL_BINDING";
        }

        detailBinding = detailProperty == null ?
                        new DetailBinding(ObjectProperty.<E>create(), name) :
                        new DetailBinding(detailProperty, name);
        return detailBinding;
    }

    public IDBinding setIDBinding(Property<E, ?> IDProperty) {
        return setIDBinding(IDProperty, null);
    }

    public IDBinding setIDBinding(Property<E, ?> IDProperty, String name) {
        throwIfBound();

        if (name == null && JComboBoxBinding.this.getName() != null) {
            name = JComboBoxBinding.this.getName() + ".ID_BINDING";
        }

        IDBinding = IDProperty == null ?
                    new IDBinding(ObjectProperty.<E>create(), name) :
                    new IDBinding(IDProperty, name);
        return IDBinding;
    }
    
    public DetailBinding getDetailBinding() {
        return detailBinding;
    }

    public IDBinding getIDBinding() {
        return IDBinding;
    }
    
    private final Property DETAIL_PROPERTY = new Property() {
        public Class<Object> getWriteType(Object source) {
            return Object.class;
        }

        public Object getValue(Object source) {
            throw new UnsupportedOperationException();
        }

        public void setValue(Object source, Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean isReadable(Object source) {
            throw new UnsupportedOperationException();
        }

        public boolean isWriteable(Object source) {
            return true;
        }

        public void addPropertyStateListener(Object source, PropertyStateListener listener) {
            throw new UnsupportedOperationException();
        }

        public void removePropertyStateListener(Object source, PropertyStateListener listener) {
            throw new UnsupportedOperationException();
        }

        public PropertyStateListener[] getPropertyStateListeners(Object source) {
            throw new UnsupportedOperationException();
        }
    };

    public final class IDBinding extends AbstractColumnBinding {

        public IDBinding(Property<E, ?> IDProperty, String name) {
            super(0, IDProperty, DETAIL_PROPERTY, name);
        }

        private void setSourceObjectInternal(Object object) {
            setManaged(false);
            try {
                setSourceObject(object);
            } finally {
                setManaged(true);
            }
        }
    }
    
    public final class DetailBinding extends AbstractColumnBinding {

        public DetailBinding(Property<E, ?> detailProperty, String name) {
            super(0, detailProperty, DETAIL_PROPERTY, name);
        }

        private void setSourceObjectInternal(Object object) {
            setManaged(false);
            try {
                setSourceObject(object);
            } finally {
                setManaged(true);
            }
        }
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

    private static final boolean areObjectsEqual(Object o1, Object o2) {
        return ((o1 != null && o1.equals(o2)) ||
                (o1 == null && o2 == null));
    }
    
    private final class BindingComboBoxModel extends ListBindingManager implements ComboBoxModel  {
        private final List<ListDataListener> listeners;
        private Object selectedObject;
        private int selectedModelIndex;

        public BindingComboBoxModel() {
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }

        public void setElements(List<?> elements) {
            super.setElements(elements);
            if (size() > 0 && selectedObject == null) {
                selectedObject = getElementAt(0);
                selectedModelIndex = 0;
            }
        }
        
        protected AbstractColumnBinding[] getColBindings() {
            return new AbstractColumnBinding[] {getDetailBinding()};
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
    }
}
