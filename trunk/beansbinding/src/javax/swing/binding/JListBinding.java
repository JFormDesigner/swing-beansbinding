/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Shannon Hickey
 */
public final class JListBinding<E, SS, TS> extends Binding<SS, List<E>, TS, List> {

    private ElementsProperty<TS, JList> ep;
    private Handler handler = new Handler();
    private BindingListModel model;
    private JList list;
    private ListDetailBinding detailBinding;

    public static <E> JListBinding<E, List<E>, JList> createDirectBinding(List<E> sourceList, JList targetJList) {
        return createDirectBinding(null, sourceList, targetJList);
    }

    public static <E> JListBinding<E, List<E>, JList> createDirectBinding(String name, List<E> sourceList, JList targetJList) {
        return new JListBinding<E, List<E>, JList>(name, sourceList, new ObjectProperty<List<E>>(), targetJList, new ObjectProperty<JList>());
    }

    public static <E, SS> JListBinding<E, SS, JList> createDirectBinding(SS sourceObject, Property<SS, List<E>> sourceListProperty, JList targetJList) {
        return createDirectBinding(null, sourceObject, sourceListProperty, targetJList);
    }

    public static <E, SS> JListBinding<E, SS, JList> createDirectBinding(String name, SS sourceObject, Property<SS, List<E>> sourceListProperty, JList targetJList) {
        return new JListBinding<E, SS, JList>(name, sourceObject, sourceListProperty, targetJList, new ObjectProperty<JList>());
    }

    public static <E, TS> JListBinding<E, List<E>, TS> createDirectBinding(List<E> sourceList, TS targetObject, Property<TS, ? extends JList> targetJListProperty) {
        return createDirectBinding(null, sourceList, targetObject, targetJListProperty);
    }

    public static <E, TS> JListBinding<E, List<E>, TS> createDirectBinding(String name, List<E> sourceList, TS targetObject, Property<TS, ? extends JList> targetJListProperty) {
        return new JListBinding<E, List<E>, TS>(name, sourceList, new ObjectProperty<List<E>>(), targetObject, targetJListProperty);
    }

    public JListBinding(SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JList> targetJListProperty) {
        this(null, sourceObject, sourceListProperty, targetObject, targetJListProperty);
    }

    public JListBinding(String name, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JList> targetJListProperty) {
        super(name, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JList>(targetJListProperty));
        ep = (ElementsProperty<TS, JList>)getTargetProperty();
        setDetailBinding(null);
    }

    protected final void bindImpl() {
        model = new BindingListModel();
        ep.addPropertyStateListener(null, handler);
        ep.installBinding(this);
        super.bindImpl();
    }

    protected final void unbinImpl() {
        super.unbindImpl();
        ep.uninstallBinding();
        ep.removePropertyStateListener(null, handler);
        model = null;
    }

    public ListDetailBinding setDetailBinding(Property<E, ?> detailProperty) {
        return detailProperty == null ?
            setDetailBinding("AUTO_DETAIL", new ObjectProperty<E>()) :
            setDetailBinding(null, detailProperty);
    }

    public ListDetailBinding setDetailBinding(String name, Property<E, ?> detailProperty) {
        throwIfBound();

        if (detailProperty == null) {
            throw new IllegalArgumentException("can't have null detail property");
        }

        detailBinding = new ListDetailBinding(name, detailProperty);
        return detailBinding;
    }

    public ListDetailBinding getDetailBinding() {
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

    public final class ListDetailBinding extends ColumnBinding {

        public ListDetailBinding(String name, Property<E, ?> detailProperty) {
            super(name, 0, detailProperty, DETAIL_PROPERTY);
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

        protected ColumnBinding[] getColBindings() {
            return new ColumnBinding[] {getDetailBinding()};
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
