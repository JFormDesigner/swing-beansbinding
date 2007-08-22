/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding.adapters;

import java.awt.Component;
import javax.beans.OLDBINDING.Binding;
import com.sun.java.swing.binding.ListBindingManager.ColumnDescription;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.beans.OLDBINDING.Binding.BindingController;
import javax.swing.CellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.OLDBINDING.ParameterKeys;
import javax.swing.border.LineBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author sky
 * @author Shannon Hickey
 * @author Jan Stola
 */
class JTableBindingHelper extends AbstractListTableBindingSupport {
    private final JTable table;
    private final PropertyDelegate delegate;
    private BindingTableModel model;
    private Binding elementsBinding;
    private int lastAccessedRow;
    private int lastAccessedColumn;
    private BindingController elementsBindingController;
    private EditingBindingTarget editingBindingTarget;
    private List<Object> selectedElements;

    private static boolean IS_JAVA_15 =
        System.getProperty("java.version").startsWith("1.5");

    public JTableBindingHelper(JTable table) {
        this.table = table;
        delegate = new PropertyDelegate();
        // PENDING: notice we never remove this.
        table.addPropertyChangeListener(this);
        table.getSelectionModel().addListSelectionListener(this);
    }
    
    public Object getPropertyDelegate() {
        return delegate;
    }
    
    protected void bindElements(BindingController controller) {
        throwIfNonNull(this.elementsBindingController);
        replaceGenericEditorIfNecessary();
        elementsBinding = controller.getBinding();
        elementsBindingController = controller;
        model = new BindingTableModel(controller);
    }
    
    protected void unbindElements() {
        unbindCellEditor(true);
        elementsBinding = null;
        elementsBindingController = null;
        if (model != null) {
            model.setElements(null);
            model = null;
            table.setModel(new DefaultTableModel());
        }
    }
    
    protected void setElements(List<?> elements) {
        model.setElements(elements);
        if (table.getModel() != model) {
            table.setModel(model);
        }
    }
    
    protected List<?> getElements() {
        return (model == null) ? null : model.getElements();
    }

    protected int viewToModel(int index) {
        // deal with sorting & filtering in 6.0 and up
        if (!IS_JAVA_15) {
            try {
                java.lang.reflect.Method m = table.getClass().getMethod("convertRowIndexToModel", int.class);
                index = (Integer)m.invoke(table, index);
            } catch (NoSuchMethodException nsme) {
                throw new AssertionError(nsme);
            } catch (IllegalAccessException iae) {
                throw new AssertionError(iae);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof Error) {
                    throw (Error)cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }
        }

        return index;
    }

    protected int modelToView(int index) {
        // deal with sorting & filtering in 6.0 and up
        if (!IS_JAVA_15) {
            try {
                java.lang.reflect.Method m = table.getClass().getMethod("convertRowIndexToView", int.class);
                index = (Integer)m.invoke(table, index);
            } catch (NoSuchMethodException nsme) {
                throw new AssertionError(nsme);
            } catch (IllegalAccessException iae) {
                throw new AssertionError(iae);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof Error) {
                    throw (Error)cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }
        }

        return index;
    }

    protected Object getSelectedElement() {
        int index = table.getSelectedRow();
        if (index != -1) {
            return model.getElement(viewToModel(index));
        }
        return null;
    }
    
    private void unbindCellEditor(boolean stopIfNecessary) {
        if (editingBindingTarget != null) {
            editingBindingTarget.cleanup(stopIfNecessary);
            editingBindingTarget = null;
        }
    }

    private void cellEditorChanged() {
        unbindCellEditor(false);
        if (table.getCellEditor() != null) {
            editingBindingTarget = new EditingBindingTarget(
                    table.getCellEditor(),
                    model.getValueAt(lastAccessedRow, lastAccessedColumn),
                    lastAccessedRow, lastAccessedColumn);
            Binding binding = model.getValueColumnDescription(lastAccessedColumn).
                    getBinding();
            elementsBindingController.bind(
                    binding,
                    getElements().get(lastAccessedRow),
                    editingBindingTarget,
                    "value",
                    true);
            editingBindingTarget.setBinding(binding);
        }
    }
    
    private void replaceGenericEditorIfNecessary() {
        TableCellEditor editor = table.getDefaultEditor(Object.class);
        if (!(editor instanceof GenericEditor)) {
            table.setDefaultEditor(Object.class, new GenericEditor());
        }
    }
    
    public void propertyChange(PropertyChangeEvent e) {
        String property = e.getPropertyName();
        if (property == "tableCellEditor") {
            cellEditorChanged();
        } else {
            super.propertyChange(e);
        }
    }

    protected ListSelectionModel getSelectionModel() {
        return table.getSelectionModel();
    }

    protected PropertyDelegate getDelegate() {
        return delegate;
    }
    
    
    // Work around for bug in JTable$GenericEditor where getCellEditorValue
    // won't return the current value, only the last value when stop was
    // invoked.
    private final static class GenericEditor extends DefaultCellEditor {
        private boolean ignoreStop;
        
        GenericEditor() {
            super(new JTextField());
            ((JComponent)getComponent()).setBorder(new LineBorder(Color.BLACK));
        }

        public Object getCellEditorValue() {
            ignoreStop = true;
            stopCellEditing();
            ignoreStop = false;
            return super.getCellEditorValue();
        }

        protected void fireEditingStopped() {
            if (!ignoreStop) {
                super.fireEditingStopped();
            }
        }
    }


    private final class BindingTableModel extends ListBindingManager implements TableModel  {
        private final List<TableModelListener> listeners;
        
        BindingTableModel(BindingController controller) {
            super(controller);
            listeners = new CopyOnWriteArrayList<TableModelListener>();
        }
        
        protected void createColumnDescriptions(
                List<ListBindingManager.ColumnDescription> columns) {
            // PENDING: this should be done once
            if (elementsBinding != null) {
                Boolean tableEditable =
                        elementsBinding.getParameter(ParameterKeys.EDITABLE, true);

                for (Binding binding : elementsBinding.getChildBindings()) {
                    Integer column = binding.getParameter(ParameterKeys.COLUMN, null);
                    if (column == null) {
                        throw new IllegalArgumentException(
                                "JTable element binding must identify the column by way of " +
                                "SwingBindingSupport.TableColumnKey");
                    }
                    boolean isRenderer = false;
                    //binding.getValue(SwingBindingSupport.TableRendererKey, 
                    //        Boolean.FALSE);
                    boolean isEditor = false;//binding.getValue(SwingBindingSupport.TableEditorKey, 
                            //Boolean.FALSE);
                    boolean isValue = (!isRenderer && !isEditor);
                    Class<?> type = binding.getParameter(ParameterKeys.COLUMN_CLASS,
                            Object.class);

                    // PENDING: this should be false if no writable property
                    Boolean columnEditable =
                            binding.getParameter(ParameterKeys.EDITABLE, null);
                    if (columnEditable == null) {
                        columnEditable = tableEditable;
                    }

                    if ((isValue && (isRenderer || isEditor)) ||
                            (isRenderer && (isValue || isEditor)) ||
                            (isEditor) && (isValue || isRenderer) ||
                            (!isValue && !isRenderer && !isEditor)) {
                        throw new IllegalArgumentException(
                                "JTable element binding must include one of: " +
                                "SwingBindingSupport.TableValueKey, " +
                                "SwingBindingSupport.TableRendererKey " +
                                "SwingBindingSupport.TableEditorKey");
                    }
                    // PENDING: renderer and editor binding
                    if (isValue) {
                        columns.add(new ColumnDescription(
                                binding, column, true, type, columnEditable));
                    }
                }
            }
        }

        private int getColumn(String path) {
            int value = 0;
            for (int i = 0, max = path.length(); i < max; i++) {
                char aChar = path.charAt(i);
                if (aChar >= '0' && aChar <= '9') {
                    value = value * 10 + (aChar - '0');
                } else {
                    return -1;
                }
            }
            return value;
        }
        
        public int getRowCount() {
            return size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            lastAccessedRow = rowIndex;
            lastAccessedColumn = columnIndex;
            return valueAt(rowIndex, columnIndex);
        }

        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            // PENDING: this needs to set value back in source
            if (editingBindingTarget != null &&
                    editingBindingTarget.getRow() == rowIndex &&
                    editingBindingTarget.getColumn() == columnIndex) {
                editingBindingTarget.setValue0(value);
            }
        }

        public Class<?> getColumnClass(int columnIndex) {
            ColumnDescription cd = getValueColumnDescription(columnIndex);
            if (cd != null) {
                return cd.getTargetType();
            }
            return Object.class;
        }

        protected void allChanged() {
            fireTableModelEvent(new TableModelEvent(
                    this, 0, Integer.MAX_VALUE));
        }

        protected void valueChanged(int row, int column) {
            fireTableModelEvent(new TableModelEvent(
                    this, row, row, column));
        }

        protected void added(int row, int length) {
            fireTableModelEvent(new TableModelEvent(
                    this, row, row + length - 1, TableModelEvent.ALL_COLUMNS,
                    TableModelEvent.INSERT));
        }

        protected void removed(int row, int length) {
            fireTableModelEvent(new TableModelEvent(
                    this, row, row + length - 1, TableModelEvent.ALL_COLUMNS,
                    TableModelEvent.DELETE));
        }

        protected void changed(int row) {
            fireTableModelEvent(new TableModelEvent(
                    this, row, row, TableModelEvent.ALL_COLUMNS));
        }

        public String getColumnName(int columnIndex) {
            ColumnDescription d = getValueColumnDescription(columnIndex);
            if (d != null) {
                Binding binding = d.getBinding();
                if (binding != null) {
                    String path = binding.getSourceExpression();
                    if (path != null) {
                        String name = path;
                        if (path.startsWith("${")) {
                            path = path.substring(2);
                            if (path.endsWith("}")) {
                                path = path.substring(0, path.length() - 1);
                                if (path.indexOf('.') != -1) {
                                    path = path.substring(path.indexOf('.') + 1);
                                    name = capitalize(path);
                                } else {
                                    name = capitalize(path);
                                }
                            }
                        }
                        return name;
                    }
                }
            }
            return "";
        }
        
        private String capitalize(String title) {
            StringBuilder builder = new StringBuilder(title);
            boolean lastWasUpper = false;
            for (int i = 0; i < builder.length(); i++) {
                char aChar = builder.charAt(i);
                if (i == 0) {
                    builder.setCharAt(i, Character.toUpperCase(aChar));
                    lastWasUpper = true;
                } else if (Character.isUpperCase(aChar)) {
                    if (!lastWasUpper) {
                        builder.insert(i, ' ');
                    }
                    lastWasUpper = true;
                    i++;
                } else {
                    lastWasUpper = false;
                }
            }
            return builder.toString();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            ColumnDescription cd = getValueColumnDescription(columnIndex);
            if (cd != null) {
                // PENDING: this should return false if no writable property
                return cd.isEditable();
            }

            // PENDING: this should return false if no writable property
            return true;
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
    

    public final class EditingBindingTarget extends DelegateBase
            implements CellEditorListener {
        private final CellEditor editor;
        private final int row;
        private final int column;
        private Binding binding;
        private boolean isEditing;
        private final Object iValue;
        private Object value;

        EditingBindingTarget(CellEditor editor, Object value,
                int row, int column) {
            this.editor = editor;
            this.row = row;
            this.column = column;
            editor.addCellEditorListener(this);
            this.value = value;
            this.iValue = value;
            isEditing = true;
        }
        
        private int getRow() {
            return row;
        }

        private int getColumn() {
            return column;
        }

        void setBinding(Binding binding) {
            this.binding = binding;
        }

        public void setValue(Object value) {
            // PENDING: nothing to do here, need special editors to be able
            // to handle this.
            // NOTE: if we do end up doing something with this, we'll need
            // to ignore the first call, as it's sent by the Binding when
            // bound.
        }
        
        void setValue0(Object value) {
            this.value = value;
            firePropertyChange("value", null, null);
        }
        
        public Object getValue() {
            if (isEditing) {
                return editor.getCellEditorValue();
            }
            return value;
        }
        
        public void editingStopped(ChangeEvent e) {
            // Force a property change
            isEditing = false;
            value = editor.getCellEditorValue();
        }
        
        public void editingCanceled(ChangeEvent e) {
            isEditing = false;
        }
        
        private void cleanup(boolean stopIfNecessary) {
            if (isEditing && stopIfNecessary) {
                editor.stopCellEditing();
            }
            if (binding != null) {
                editor.removeCellEditorListener(this);
                isEditing = false;
                Binding binding = this.binding;
                this.binding = null;
                elementsBindingController.unbindOnCommit(binding);
                firePropertyChange("value", null, null);
            }
        }
    }
}
