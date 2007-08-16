/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class JTableSelectedElementProperty<S, E> extends AbstractProperty<S, E> {

    private Property<S, ? extends JTable> sourceProperty;
    private IdentityHashMap<S, SourceEntry> map = new IdentityHashMap<S, SourceEntry>();
    private static final Object NOREAD = new Object();
    private final Class<E> valueType;

    private final class SourceEntry implements PropertyStateListener, ListSelectionListener, PropertyChangeListener {
        private S source;
        private JTable cachedComponent;
        private Object cachedValue;
        private boolean ignoreChange;

        private SourceEntry(S source) {
            this.source = source;
            sourceProperty.addPropertyStateListener(source, this);
            updateCachedComponent();
            updateCachedValue();
        }

        private void cleanup() {
            if (cachedComponent != null) {
                cachedComponent.getSelectionModel().removeListSelectionListener(this);
                cachedComponent.removePropertyChangeListener("selectionModel", this);
            }

            sourceProperty.removePropertyStateListener(source, this);

            cachedComponent = null;
            cachedValue = null;
        }

        // flag -1 - validate all
        // flag  0 - source property changed value or readability
        // flag  1 - value changed (or selection model changed)
        private void validateCache(int flag) {
            if (flag != 0 && getJTableFromSource(source, false) != cachedComponent) {
                System.err.println("LOG: validateCache(): concurrent modification");
            }

            if (flag != 1) {
                Object value = (cachedComponent == null ? NOREAD : getSelectionIndex(cachedComponent));
                if (cachedValue != value && (cachedValue == null || !cachedValue.equals(value))) {
                    System.err.println("LOG: validateCache(): concurrent modification");
                }
            }
        }
        
        private void updateCachedComponent() {
            JTable comp = getJTableFromSource(source, true);

            if (comp != cachedComponent) {
                if (cachedComponent != null) {
                    cachedComponent.getSelectionModel().removeListSelectionListener(this);
                    cachedComponent.removePropertyChangeListener("selectionModel", this);
                }

                cachedComponent = comp;

                if (cachedComponent != null) {
                    cachedComponent.getSelectionModel().addListSelectionListener(this);
                    cachedComponent.addPropertyChangeListener("selectionModel", this);
                }
            }
        }

        private void updateCachedValue() {
            cachedValue = (cachedComponent == null ? NOREAD : getSelectionIndex(cachedComponent));
        }

        private boolean cachedIsReadable() {
            return cachedValue != NOREAD;
        }

        private boolean cachedIsWriteable() {
            return cachedComponent != null;
        }

        public void propertyStateChanged(PropertyStateEvent pe) {
            bindingPropertyChanged(pe);
        }

        public void valueChanged(ListSelectionEvent e) {
            listSelectionChanged();
        }

        public void propertyChange(PropertyChangeEvent pce) {
            listSelectionChanged();
        }
        
        private void bindingPropertyChanged(PropertyStateEvent pse) {
            boolean valueChanged = pse.getValueChanged() || pse.getReadableChanged();
            validateCache(0);
            Object oldValue = cachedValue;
            boolean wasWriteable = cachedIsWriteable();
            updateCachedComponent();
            updateCachedValue();
            notifyListeners(wasWriteable, oldValue, this);
        }

        private void listSelectionChanged() {
            if (ignoreChange) {
                return;
            }

            validateCache(1);
            Object oldValue = cachedValue;
            updateCachedValue();
            notifyListeners(cachedIsWriteable(), oldValue, this);
        }
    }

    private JTableSelectedElementProperty(Property<S, ? extends JTable> sourceProperty, Class<E> valueType) {
        if (sourceProperty == null) {
            throw new IllegalArgumentException("can't have null source property");
        }

        assert valueType != null;

        this.sourceProperty = sourceProperty;
        this.valueType = valueType;
    }

    public static final JTableSelectedElementProperty<JTable, Object> create() {
        return createForProperty(new ObjectProperty<JTable>());
    }

    public static final <S> JTableSelectedElementProperty<S, Object> createForProperty(Property<S, ? extends JTable> sourceProperty) {
        return createForProperty(sourceProperty, Object.class);
    }

    public static final <E> JTableSelectedElementProperty<JTable, E> create(Class<E> valueType) {
        return createForProperty(new ObjectProperty<JTable>(), valueType);
    }

    public static final <S, E> JTableSelectedElementProperty<S, E> createForProperty(Property<S, ? extends JTable> sourceProperty, Class<E> valueType) {
        return new JTableSelectedElementProperty<S, E>(sourceProperty, valueType);
    }

    private static int getSelectionIndex(JTable list) {
        assert list != null;
        int index = list.getSelectionModel().getLeadSelectionIndex();
        return list.getSelectionModel().isSelectedIndex(index) ? index : -1;
    }
    
    private static Object getListObject(JTable list, int index) {
        assert list != null;
        if (index == -1) {
            return null;
        }

        ListModel model = list.getModel();
        return model instanceof ListBindingManager ? ((ListBindingManager)model).getElement(index)
                                                   : model.getElementAt(index);
    }
    
    public Class<E> getWriteType(S source) {
        JTable component;

        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            component = entry.cachedComponent;
        } else {
            component = getJTableFromSource(source, true);
        }

        if (component == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return valueType;
    }

    public E getValue(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (entry.cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }

            return (E)getListObject(entry.cachedComponent, (Integer)entry.cachedValue);
        }

        JTable comp = getJTableFromSource(source, true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return (E)getListObject(comp, getSelectionIndex(comp));
    }

    public void setValue(S source, E value) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (entry.cachedComponent == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            try {
                entry.ignoreChange = true;
                //entry.cachedComponent.setSelected(value);
                Object oldValue = entry.cachedValue;
                entry.updateCachedValue();
                notifyListeners(entry.cachedIsWriteable(), oldValue, entry);
            } finally {
                entry.ignoreChange = false;
            }
        } else {
            JTable component = getJTableFromSource(source, true);
            if (component == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            //component.setSelected(value);
        }
    }

    public boolean isReadable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsReadable();
        }

        return (getJTableFromSource(source, true) != null);
    }

    public boolean isWriteable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsWriteable();
        }

        return (getJTableFromSource(source, true) != null);
    }

    private JTable getJTableFromSource(S source, boolean logErrors) {
        if (!sourceProperty.isReadable(source)) {
            if (logErrors) {
                System.err.println("LOG: getJTableFromSource(): unreadable source property");
            }
            return null;
        }

        JTable list = sourceProperty.getValue(source);
        if (list == null) {
            if (logErrors) {
                System.err.println("LOG: getJTableFromSource(): source property returned null");
            }
            return null;
        }
        
        return list;
    }

    protected final void listeningStarted(S source) {
        SourceEntry entry = map.get(source);
        if (entry == null) {
            entry = new SourceEntry(source);
            map.put(source, entry);
        }
    }

    protected final void listeningStopped(S source) {
        SourceEntry entry = map.remove(source);
        if (entry != null) {
            entry.cleanup();
        }
    }

    private static boolean didValueChange(Object oldValue, Object newValue) {
        return oldValue == null || newValue == null || !oldValue.equals(newValue);
    }

    private static Object toUNREADABLE(Object src) {
        return src == NOREAD ? UNREADABLE : src;
    }

    private void notifyListeners(boolean wasWriteable, Object oldValue, SourceEntry entry) {
        PropertyStateListener[] listeners = getPropertyStateListeners(entry.source);

        if (listeners == null || listeners.length == 0) {
            return;
        }

        oldValue = toUNREADABLE(oldValue);
        Object newValue = toUNREADABLE(entry.cachedValue);
        boolean valueChanged = didValueChange(oldValue, newValue);
        boolean writeableChanged = (wasWriteable != entry.cachedIsWriteable());

        if (!valueChanged && !writeableChanged) {
            return;
        }
        
        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        entry.source,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        entry.cachedIsWriteable());

        this.firePropertyStateChange(pse);
    }

    public String toString() {
        return "JTable.selectedElement";
    }

}
