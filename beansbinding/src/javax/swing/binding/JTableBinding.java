/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;

/**
 * @author Shannon Hickey
 */
public final class JTableBinding<E, SS, TS> extends Binding<SS, List<E>, TS, List> {

    private ElementsProperty<TS, JTable> ep;
    private Handler handler = new Handler();
    private JTable table;
    private List<E> elements;
    private boolean editable;
    private boolean editableSet;
    private List<TableColumnBinding> columnBindings = new ArrayList<TableColumnBinding>();

    public static <E> JTableBinding<E, List<E>, JTable> createDirectBinding(List<E> source, JTable target) {
        return createDirectBinding(null, source, target);
    }

    public static <E> JTableBinding<E, List<E>, JTable> createDirectBinding(String name, List<E> sourceList, JTable targetTable) {
        return new JTableBinding<E, List<E>, JTable>(name, sourceList, new ObjectProperty<List<E>>(), targetTable, new ObjectProperty<JTable>());
    }

    public static <E, SS> JTableBinding<E, SS, JTable> createDirectBinding(SS sourceObject, Property<SS, List<E>> sourceListProperty, JTable targetTable) {
        return createDirectBinding(null, sourceObject, sourceListProperty, targetTable);
    }

    public static <E, SS> JTableBinding<E, SS, JTable> createDirectBinding(String name, SS sourceObject, Property<SS, List<E>> sourceListProperty, JTable targetTable) {
        return new JTableBinding<E, SS, JTable>(name, sourceObject, sourceListProperty, targetTable, new ObjectProperty<JTable>());
    }

    public static <E, TS> JTableBinding<E, List<E>, TS> createDirectBinding(List<E> sourceList, TS targetObject, Property<TS, ? extends JTable> targetTableProperty) {
        return createDirectBinding(null, sourceList, targetObject, targetTableProperty);
    }

    public static <E, TS> JTableBinding<E, List<E>, TS> createDirectBinding(String name, List<E> sourceList, TS targetObject, Property<TS, ? extends JTable> targetTableProperty) {
        return new JTableBinding<E, List<E>, TS>(name, sourceList, new ObjectProperty<List<E>>(), targetObject, targetTableProperty);
    }

    public JTableBinding(SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JTable> targetTableProperty) {
        this(null, sourceObject, sourceListProperty, targetObject, targetTableProperty);
    }

    public JTableBinding(String name, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JTable> targetTableProperty) {
        super(name, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JTable>(targetTableProperty));
        ep = (ElementsProperty<TS, JTable>)getTargetProperty();
        ep.addPropertyStateListener(null, handler);
    }

    protected final void bindImpl() {
        ep.installBinding(this);
        super.bindImpl();
    }

    protected final void unbinImpl() {
        ep.uninstallBinding();
        super.unbindImpl();
    }
    
    public void setEditable(boolean editable) {
        throwIfBound();
        this.editable = editable;
        this.editableSet = true;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isEditableSet() {
        return editableSet;
    }

    public TableColumnBinding addColumnBinding(Property<E, ?> columnProperty) {
        return addColumnBinding(null, columnProperty);
    }
    
    public TableColumnBinding addColumnBinding(String name, Property<E, ?> columnProperty) {
        throwIfBound();

        if (columnProperty == null) {
            throw new IllegalArgumentException("can't have null column property");
        }

        TableColumnBinding binding = new TableColumnBinding(name, columnProperty);
        columnBindings.add(binding);
        return binding;
    }

    public TableColumnBinding addColumnBinding(int index, Property<E, ?> columnProperty) {
        return addColumnBinding(null, index, columnProperty);
    }
    
    public TableColumnBinding addColumnBinding(String name, int index, Property<E, ?> columnProperty) {
        throwIfBound();

        if (columnProperty == null) {
            throw new IllegalArgumentException("can't have null column property");
        }

        TableColumnBinding binding = new TableColumnBinding(name, columnProperty);
        columnBindings.add(index, binding);
        return binding;
    }

    public boolean removeColumnBinding(TableColumnBinding binding) {
        throwIfBound();
        return columnBindings.remove(binding);
    }
    
    public TableColumnBinding removeColumnBinding(int index) {
        throwIfBound();
        return columnBindings.remove(index);
    }

    public TableColumnBinding getColumnBinding(int index) {
        return columnBindings.get(index);
    }

    public List<TableColumnBinding> getColumnBindings() {
        return Collections.unmodifiableList(columnBindings);
    }

    private final class TableColumnProperty implements Property {
        private TableColumnBinding binding;

        public Class<? extends Object> getWriteType(Object source) {
            return binding.columnClass == null ? Object.class : binding.columnClass;
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
    }
    
    public final class TableColumnBinding extends Binding {
        private Class<?> columnClass;
        private boolean editable;
        private boolean editableSet;
        private String columnName;

        public TableColumnBinding(String name, Property<E, ?> columnProperty) {
            super(name, null, columnProperty, null, new TableColumnProperty());
            ((TableColumnProperty)getTargetProperty()).binding = this;
        }

        public TableColumnBinding setColumnName(String name) {
            JTableBinding.this.throwIfBound();
            this.columnName = name;
            return this;
        }

        public TableColumnBinding setColumnClass(Class<?> columnClass) {
            JTableBinding.this.throwIfBound();
            this.columnClass = columnClass;
            return this;
        }

        public Class<?> getColumnClass() {
            return columnClass == null ? Object.class : columnClass;
        }

        public String getColumnName() {
            return columnName == null ? getSourceProperty().toString() : columnName;
        }
        
        public TableColumnBinding setEditable(boolean editable) {
            JTableBinding.this.throwIfBound();
            this.editable = editable;
            this.editableSet = true;
            return this;
        }

        public boolean isEditable() {
            return editable;
        }

        public boolean isEditableSet() {
            return editableSet;
        }
    }

    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (!pse.getValueChanged()) {
                return;
            }

            if (table != null) {
                table.setModel(null);
                table = null;
                elements = null;
            }

            Object newValue = pse.getNewValue();

            if (newValue != null && newValue != PropertyStateEvent.UNREADABLE) {
                table = ep.getComponent();
                elements = (List<E>)newValue;
                table.setModel(new BindingTableModel());
            }
        }
    }

    private final class BindingTableModel implements TableModel {
        public int getRowCount() {
            return elements.size();
        }

        public int getColumnCount() {
            return columnBindings.size();
        }

        public String getColumnName(int columnIndex) {
            return columnBindings.get(columnIndex).getColumnName();
        }

        public Class<?> getColumnClass(int columnIndex) {
            return columnBindings.get(columnIndex).getColumnClass();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            TableColumnBinding cb = columnBindings.get(columnIndex);
            cb.setSourceObject(elements.get(rowIndex));
            return cb.getSourceValueForTarget().getValue();
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        public void addTableModelListener(TableModelListener l) {
        }

        public void removeTableModelListener(TableModelListener l) {
        }
    }

}
