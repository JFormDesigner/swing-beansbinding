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
public final class JTableBinding<T> extends SwingBinding<List<T>, List> {

    private ElementsProperty<JTable> ep;
    private Handler handler = new Handler();
    private JTable table;
    private List<T> elements;
    private boolean editable;
    private boolean editableSet;
    private List<TableColumnBinding> columnBindings = new ArrayList<TableColumnBinding>();

    public JTableBinding(List<T> source, JTable target) {
        this(null, source, target);
    }

    public JTableBinding(Property<List<T>> source, JTable target) {
        this(null, source, target);
    }

    public JTableBinding(List<T> source, Property<? extends JTable> target) {
        this(null, source, target);
    }

    public JTableBinding(Property<List<T>> source, Property<? extends JTable> target) {
        this(null, source, target);
    }

    public JTableBinding(String name, List<T> source, JTable target) {
        super(name, new ObjectProperty(source), new ElementsProperty<JTable>(new ObjectProperty<JTable>(target)));
        setup();
    }

    public JTableBinding(String name, Property<List<T>> source, JTable target) {
        super(name, source, new ElementsProperty<JTable>(new ObjectProperty<JTable>(target)));
        setup();
    }

    public JTableBinding(String name, List<T> source, Property<? extends JTable> target) {
        super(name, new ObjectProperty(source), new ElementsProperty(target));
        setup();
    }

    public JTableBinding(String name, Property<List<T>> source, Property<? extends JTable> target) {
        super(name, source, new ElementsProperty(target));
        setup();
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

    public TableColumnBinding addColumnBinding(SourceableProperty<T, ?> source) {
        return addColumnBinding(null, source);
    }
    
    public TableColumnBinding addColumnBinding(String name, SourceableProperty<T, ?> source) {
        throwIfBound();
        if (source == null) {
            throw new IllegalArgumentException("can't have null source");
        }

        TableColumnBinding binding = new TableColumnBinding(name, source);
        columnBindings.add(binding);
        return binding;
    }

    public TableColumnBinding addColumnBinding(int index, SourceableProperty<T, ?> source) {
        return addColumnBinding(null, index, source);
    }
    
    public TableColumnBinding addColumnBinding(String name, int index, SourceableProperty<T, ?> source) {
        throwIfBound();
        if (source == null) {
            throw new IllegalArgumentException("can't have null source");
        }

        TableColumnBinding binding = new TableColumnBinding(name, source);
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

    private void setup() {
        prepareElementsProperty();
    }

    private void prepareElementsProperty() {
        ep = (ElementsProperty)getTarget();
        ep.addPropertyStateListener(handler);
    }

    private final class TableColumnProperty implements Property<Object> {
        private TableColumnBinding binding;

        public Class<? extends Object> getWriteType() {
            return binding.columnClass == null ? Object.class : binding.columnClass;
        }

        public Object getValue() {
            throw new UnsupportedOperationException();
        }

        public void setValue(Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean isReadable() {
            throw new UnsupportedOperationException();
        }

        public boolean isWriteable() {
            return true;
        }

        public void addPropertyStateListener(PropertyStateListener listener) {
            throw new UnsupportedOperationException();
        }

        public void removePropertyStateListener(PropertyStateListener listener) {
            throw new UnsupportedOperationException();
        }

        public PropertyStateListener[] getPropertyStateListeners() {
            throw new UnsupportedOperationException();
        }
    }
    
    public final class TableColumnBinding extends Binding {
        private Class<?> columnClass;
        private boolean editable;
        private boolean editableSet;
        private String columnName;

        public TableColumnBinding(String name, SourceableProperty<T, ?> prop) {
            super(name, prop, new TableColumnProperty());
            ((TableColumnProperty)getTarget()).binding = this;
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
            return columnName == null ? getSource().toString() : columnName;
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
                elements = (List<T>)newValue;
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
            ((SourceableProperty)cb.getSource()).setSource(elements.get(rowIndex));
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
