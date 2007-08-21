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
import java.util.concurrent.CopyOnWriteArrayList;
import sun.swing.binding.ColumnBinding;
import sun.swing.binding.ListBindingManager;

/**
 * @author Shannon Hickey
 */
public final class JTableBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {

    private ElementsProperty<TS, JTable> ep;
    private Handler handler = new Handler();
    private BindingTableModel model;
    private JTable table;
    private boolean editable;
    private boolean editableSet;
    private List<TableColumnBinding> columnBindings = new ArrayList<TableColumnBinding>();

    protected JTableBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty, String name) {
        super(strategy, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JTable>(targetJTableProperty), name);
        ep = (ElementsProperty<TS, JTable>)getTargetProperty();
    }

    protected boolean bindImpl() {
        model = new BindingTableModel();
        ep.addPropertyStateListener(null, handler);
        ep.installBinding(this);
        return super.bindImpl();
    }

    protected boolean unbindImpl() {
        ep.uninstallBinding();
        ep.removePropertyStateListener(null, handler);
        model = null;
        return super.unbindImpl();
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
        return addColumnBinding(columnProperty, null);
    }

    public TableColumnBinding addColumnBinding(Property<E, ?> columnProperty, String name) {
        throwIfBound();

        if (columnProperty == null) {
            throw new IllegalArgumentException("can't have null column property");
        }

        TableColumnBinding binding = new TableColumnBinding(columnBindings.size(), columnProperty, name);
        columnBindings.add(binding);
        return binding;
    }

    public TableColumnBinding addColumnBinding(int index, Property<E, ?> columnProperty) {
        return addColumnBinding(index, columnProperty, null);
    }

    public TableColumnBinding addColumnBinding(int index, Property<E, ?> columnProperty, String name) {
        throwIfBound();

        if (columnProperty == null) {
            throw new IllegalArgumentException("can't have null column property");
        }

        TableColumnBinding binding = new TableColumnBinding(index, columnProperty, name);
        columnBindings.add(index, binding);
        adjustIndices(index + 1, true);
        return binding;
    }

    public boolean removeColumnBinding(TableColumnBinding binding) {
        throwIfBound();
        boolean retVal = columnBindings.remove(binding);

        if (retVal) {
            adjustIndices(binding.getColumn(), false);
        }

        return retVal;
    }
    
    public TableColumnBinding removeColumnBinding(int index) {
        throwIfBound();
        TableColumnBinding retVal = columnBindings.remove(index);
        
        if (retVal != null) {
            adjustIndices(index, false);
        }

        return retVal;
    }

    public TableColumnBinding getColumnBinding(int index) {
        return columnBindings.get(index);
    }

    public List<TableColumnBinding> getColumnBindings() {
        return Collections.unmodifiableList(columnBindings);
    }

    private void adjustIndices(int start, boolean up) {
        int size = columnBindings.size();
        for (int i = start; i < size; i++) {
            TableColumnBinding cb = columnBindings.get(i);
            cb.adjustColumn(cb.getColumn() + (up ? 1 : -1));
        }
    }
    
    private final class TableColumnProperty extends Property {
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

    public final class TableColumnBinding extends ColumnBinding {
        private Class<?> columnClass;
        private boolean editable;
        private boolean editableSet;
        private String columnName;

        public TableColumnBinding(int column, Property<E, ?> columnProperty, String name) {
            super(column, columnProperty, new TableColumnProperty(), name);
            ((TableColumnProperty)getTargetProperty()).binding = this;
        }

        private void adjustColumn(int newCol) {
            setColumn(newCol);
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

            Object newValue = pse.getNewValue();

            if (newValue == PropertyStateEvent.UNREADABLE) {
                table.setModel(new DefaultTableModel());
                table = null;
                model.setElements(null);
            } else {
                table = ep.getComponent();
                model.setElements((List<E>)newValue);
                table.setModel(model);
            }
        }
    }

    private final class BindingTableModel extends ListBindingManager implements TableModel  {
        private final List<TableModelListener> listeners;

        public BindingTableModel() {
            listeners = new CopyOnWriteArrayList<TableModelListener>();
        }

        protected ColumnBinding[] getColBindings() {
            ColumnBinding[] bindings = new ColumnBinding[getColumnBindings().size()];
            bindings = getColumnBindings().toArray(bindings);
            return bindings;
        }

        public int getRowCount() {
            return size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return valueAt(rowIndex, columnIndex);
        }

        public void setValueAt(Object value, int rowIndex, int columnIndex) {
        }

        public Class<?> getColumnClass(int columnIndex) {
            Class<?> klass = JTableBinding.this.getColumnBinding(columnIndex).getColumnClass();
            return klass == null ? Object.class : klass;
        }

        protected void allChanged() {
            fireTableModelEvent(new TableModelEvent(this, 0, Integer.MAX_VALUE));
        }

        protected void valueChanged(int row, int column) {
            fireTableModelEvent(new TableModelEvent(this, row, row, column));
        }

        protected void added(int row, int length) {
            fireTableModelEvent(new TableModelEvent(this, row, row + length - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
        }

        protected void removed(int row, int length) {
            fireTableModelEvent(new TableModelEvent(this, row, row + length - 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
        }

        protected void changed(int row) {
            fireTableModelEvent(new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS));
        }

        public String getColumnName(int columnIndex) {
            TableColumnBinding binding = JTableBinding.this.getColumnBinding(columnIndex);
            return binding.getColumnName() == null ? binding.getSourceProperty().toString() : binding.getColumnName();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        public void removeTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        private void fireTableModelEvent(TableModelEvent e) {
            for (TableModelListener listener : listeners) {
                listener.tableChanged(e);
            }
        }

        public int getColumnCount() {
            return columnCount();
        }
    }
}
