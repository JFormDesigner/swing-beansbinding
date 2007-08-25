/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
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
public final class JListBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {

    private ElementsProperty<TS, JList> ep;
    private Handler handler = new Handler();
    private BindingListModel model;
    private JList list;
    private DetailBinding detailBinding;

    protected JListBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JList> targetJListProperty, String name) {
        super(strategy, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JList>(targetJListProperty), name);
        ep = (ElementsProperty<TS, JList>)getTargetProperty();
        setDetailBinding(null);
    }

    protected void bindImpl() {
        model = new BindingListModel();
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

        if (name == null && JListBinding.this.getName() != null) {
            name = JListBinding.this.getName() + ".DETAIL_BINDING";
        }

        detailBinding = detailProperty == null ?
                        new DetailBinding(ObjectProperty.<E>create(), name) :
                        new DetailBinding(detailProperty, name);
        return detailBinding;
    }

    public DetailBinding getDetailBinding() {
        return detailBinding;
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

    public final class DetailBinding extends AbstractColumnBinding {

        private DetailBinding(Property<E, ?> detailProperty, String name) {
            super(0, detailProperty, DETAIL_PROPERTY, name);
        }

    }

    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (!pse.getValueChanged()) {
                return;
            }

            Object newValue = pse.getNewValue();

            if (newValue == PropertyStateEvent.UNREADABLE) {
                list.setModel(new DefaultListModel());
                list = null;
                model.setElements(null);
            } else {
                list = ep.getComponent();
                model.setElements((List<E>)newValue);
                list.setModel(model);
            }
        }
    }

    private final class BindingListModel extends ListBindingManager implements ListModel  {
        private final List<ListDataListener> listeners;

        public BindingListModel() {
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }

        protected AbstractColumnBinding[] getColBindings() {
            return new AbstractColumnBinding[] {getDetailBinding()};
        }

        protected void allChanged() {
            contentsChanged(0, size());
        }

        protected void valueChanged(int row, int column) {
            contentsChanged(row, row);
        }

        protected void added(int index, int length) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index + length - 1);
            for (ListDataListener listener : listeners) {
                listener.intervalAdded(e);
            }
        }

        protected void removed(int index, int length) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index + length - 1);
            for (ListDataListener listener : listeners) {
                listener.intervalRemoved(e);
            }
        }

        protected void changed(int row) {
            contentsChanged(row, row);
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
