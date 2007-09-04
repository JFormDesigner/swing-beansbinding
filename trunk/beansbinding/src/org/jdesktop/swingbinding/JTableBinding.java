/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingListener;
import org.jdesktop.beansbinding.Binding.*;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.beansbinding.PropertyStateListener;
import org.jdesktop.swingbinding.impl.AbstractColumnBinding;
import org.jdesktop.swingbinding.impl.ListBindingManager;
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;

/**
 * Binds a {@code List} of objects to act as the elements of a {@code JTable}.
 * Each object in the source {@code List} represents one row in the {@code JTable}.
 * Mappings from properties of the source objects to columns are created by
 * adding {@code ColumnBindings} to this {@code JTableBinding}.
 * <p>
 * If the {@code List} is an instance of {@code ObservableList}, then changes
 * to the {@code List} are reflected in the table. {@code JTableBinding}
 * also listens to the properties specified for column bindings, for all rows,
 * and updates its display values in response to change.
 * <p>
 * This class is a subclass of {@code AutoBinding}. The update strategy
 * dictates how the binding responds to changes to the value of the source
 * {@code List} property itself. Strategies {@code READ_ONCE} and {@code READ}
 * are the only two that make sense ({@code READ_WRITE} acts like {@code READ})
 * since the table end of the binding never changes the list value. The update
 * strategy does not dictate how changes made to cell values are committed back
 * to the list; since the {@code JTable} is acting as a view for the live list values,
 * all changes are committed back immediately.
 * <p>
 * <a name="EDITABILITY">A cell</a> in the {@code JTable} is editable for any given row and
 * column when all of the following are true: the property specified for that column
 * by the column binding is editable for the object in that row, the "editable" property
 * of the {@code JTableBinding} is either {@code true} or unset, and the "editable"
 * property of the {@code ColumnBinding} is either {@code true} or unset.
 * <p>
 * {@code ColumnBindings} are managed by the {@code JTableBinding}. They are not
 * to be explicitly bound, unbound, added to a {@code BindingGroup}, or accessed
 * in a way that is not allowed for a managed binding. {@code BindingListeners}
 * added to a {@code ColumnBinding} are notified at the time an edited table value
 * is to be committed back to the source list. They receive notification of either
 * {@code synced} or {@code syncFailed}. {@code BindingListeners} added to the
 * {@code JTableBinding} itself are also notified of {@code sync} and {@code syncFailed}
 * for the {@code JTableBinding's ColumnBindings}.
 * <p>
 * Instances of {@code JTableBinding} are obtained by calling one of the
 * {@code createJTableBinding} methods in the {@code SwingBindings} class.
 * Here is an example of creating a binding from a list of {@code Person}
 * objects to a table:
 * <p>
 * <pre><code>
 *    // create the person list
 *    List<Person> people = createPersonList();
 *
 *    // create the binding from list to table
 *    JTableBinding tb = SwingBindings.createJTableBinding(READ, people, table);
 *
 *    // define the properties to be used for the columns
 *    BeanProperty firstNameP = BeanProperty.create("firstName");
 *    BeanProperty lastNameP = BeanProperty.create("lastName");
 *    BeanProperty ageP = BeanProperty.create("age");
 *
 *    // configure how the properties map to columns
 *    tb.addColumnBinding(firstNameP).setColumnName("First Name");
 *    tb.addColumnBinding(lastNameP).setColumnName("Last Name");
 *    tb.addColumnBinding(ageP).setColumnName("Age").setColumnClass(Integer.class);
 *
 *    // realize the binding
 *    tb.bind();
 * </code></pre>
 * <p>
 * In addition to binding the elements of a {@code JTable}, it is possible to
 * bind to the selection of a table. See the list of <a href="package-summary.html#SWING_PROPS">
 * interesting swing properties</a> in the package summary for more details.
 *
 * @param <E> the type of elements in the source list
 * @param <SS> the type of source object (on which the source property resolves to List)
 * @param <TS> the type of target object (on which the target property resolves to JTable}
 *
 * @author Shannon Hickey
 */
public final class JTableBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {

    private ElementsProperty<TS, JTable> ep;
    private Handler handler = new Handler();
    private BindingTableModel model;
    private JTable table;
    private boolean editable;
    private boolean editableSet;
    private List<ColumnBinding> columnBindings = new ArrayList<ColumnBinding>();

    /**
     * Constructs an instance of {@code JTableBinding}.
     *
     * @param strategy the update strategy
     * @param sourceObject the source object
     * @param sourceProperty a property on the source object that resolves to the list of elements
     * @param targetObject the target object
     * @param targetProperty a property on the target object that resolves to a {@code JTable}
     * @param name a name for the {@code Binding}
     * @throws IllegalArgumentException if the source property or target property is {@code null}
     */
    protected JTableBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty, String name) {
        super(strategy, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JTable>(targetJTableProperty), name);
        ep = (ElementsProperty<TS, JTable>)getTargetProperty();
    }

    protected void bindImpl() {
        model = new BindingTableModel();
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
    
    /**
     * Sets whether or not the cells of the table should be editable.
     * See this <a href="#EDITABILITY">paragraph</a> in the class level
     * documentation on editability.
     *
     * @param editable whether or not the cells of the table should be editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        this.editableSet = true;
    }

    /**
     * Returns whether or not the cells of the table should be editable.
     * See this <a href="#EDITABILITY">paragraph</a> in the class level
     * documentation on editability.
     *
     * @return whether or not the cells of the table should be editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Returns whether or not {@code setEditable} has been called on
     * this {@code JTableBinding}.
     *
     * @return whether or not {@code setEditable} has been called on this
     *         {@code JTableBinding}
     * @deprecated Unused and to be removed in a future version.
     */
    @Deprecated
    public boolean isEditableSet() {
        return editableSet;
    }

    /**
     * Creates a column binding and adds it to the end of the list of column bindings
     * maintained by this {@code JTableBinding}.
     * <p>
     * The list of column bindings dictates the columns to be displayed in the
     * {@code JTable}, with a column binding's order in the list determining its
     * table model index.
     *
     * @param columnProperty the property with which to fetch cell values from the
     *                       elements of the source list
     * @return the column binding
     */
    public ColumnBinding addColumnBinding(Property<E, ?> columnProperty) {
        return addColumnBinding(columnProperty, null);
    }

    /**
     * Creates a named column binding and adds it to the end of the list of column bindings
     * maintained by this {@code JTableBinding}.
     * <p>
     * The list of column bindings dictates the columns to be displayed in the
     * {@code JTable}, with a column binding's order in the list determining its
     * table model index.
     *
     * @param columnProperty the property with which to fetch cell values from the
     *                       elements of the source list
     * @param name a name for the column binding
     * @return the column binding
     */
    public ColumnBinding addColumnBinding(Property<E, ?> columnProperty, String name) {
        throwIfBound();

        if (columnProperty == null) {
            throw new IllegalArgumentException("can't have null column property");
        }

        if (name == null && JTableBinding.this.getName() != null) {
            name = JTableBinding.this.getName() + ".COLUMN_BINDING";
        }

        ColumnBinding binding = new ColumnBinding(columnBindings.size(), columnProperty, name);
        columnBindings.add(binding);
        return binding;
    }

    /**
     * Creates a column binding and inserts it at the given index into the list
     * of column bindings maintained by this {@code JTableBinding}.
     * <p>
     * The list of column bindings dictates the columns to be displayed in the
     * {@code JTable}, with a column binding's order in the list determining its
     * table model index.
     *
     * @param index the index at which to insert the column binding
     * @param columnProperty the property with which to fetch cell values from the
     *                       elements of the source list
     * @return the column binding
     */
    public ColumnBinding addColumnBinding(int index, Property<E, ?> columnProperty) {
        return addColumnBinding(index, columnProperty, null);
    }

    /**
     * Creates a column binding and inserts it at the given index into the list
     * of column bindings maintained by this {@code JTableBinding}.
     * <p>
     * The list of column bindings dictates the columns to be displayed in the
     * {@code JTable}, with a column binding's order in the list determining its
     * table model index.
     *
     * @param index the index at which to insert the column binding
     * @param columnProperty the property with which to fetch cell values from the
     *                       elements of the source list
     * @param name a name for the column binding
     * @return the column binding
     */
    public ColumnBinding addColumnBinding(int index, Property<E, ?> columnProperty, String name) {
        throwIfBound();

        if (columnProperty == null) {
            throw new IllegalArgumentException("can't have null column property");
        }

        if (name == null && JTableBinding.this.getName() != null) {
            name = JTableBinding.this.getName() + ".COLUMN_BINDING";
        }
        
        ColumnBinding binding = new ColumnBinding(index, columnProperty, name);
        columnBindings.add(index, binding);
        adjustIndices(index + 1, true);
        return binding;
    }

    public boolean removeColumnBinding(ColumnBinding binding) {
        throwIfBound();
        boolean retVal = columnBindings.remove(binding);

        if (retVal) {
            adjustIndices(binding.getColumn(), false);
        }

        return retVal;
    }
    
    public ColumnBinding removeColumnBinding(int index) {
        throwIfBound();
        ColumnBinding retVal = columnBindings.remove(index);
        
        if (retVal != null) {
            adjustIndices(index, false);
        }

        return retVal;
    }

    public ColumnBinding getColumnBinding(int index) {
        return columnBindings.get(index);
    }

    public List<ColumnBinding> getColumnBindings() {
        return Collections.unmodifiableList(columnBindings);
    }

    private void adjustIndices(int start, boolean up) {
        int size = columnBindings.size();
        for (int i = start; i < size; i++) {
            ColumnBinding cb = columnBindings.get(i);
            cb.adjustColumn(cb.getColumn() + (up ? 1 : -1));
        }
    }
    
    private final class ColumnProperty extends Property {
        private ColumnBinding binding;

        public Class<? extends Object> getWriteType(Object source) {
            return binding.columnClass == null ? Object.class : binding.columnClass;
        }

        public Object getValue(Object source) {
            if (binding.isBound()) {
                return binding.editingObject;
            }

            throw new UnsupportedOperationException();
        }

        public void setValue(Object source, Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean isReadable(Object source) {
            return binding.isBound();
        }

        public boolean isWriteable(Object source) {
            return true;
        }

        public void addPropertyStateListener(Object source, PropertyStateListener listener) {
        }

        public void removePropertyStateListener(Object source, PropertyStateListener listener) {
        }

        public PropertyStateListener[] getPropertyStateListeners(Object source) {
            return new PropertyStateListener[0];
        }
    }

    public final class ColumnBinding extends AbstractColumnBinding {
        private Class<?> columnClass;
        private boolean editable;
        private boolean editableSet;
        private String columnName;
        private Object editingObject;

        private ColumnBinding(int column, Property<E, ?> columnProperty, String name) {
            super(column, columnProperty, new ColumnProperty(), name);
            ((ColumnProperty) getTargetProperty()).binding = this;
        }

        private void setEditingObject(Object editingObject) {
            this.editingObject = editingObject;
        }
        
        private void adjustColumn(int newCol) {
            setColumn(newCol);
        }

        public ColumnBinding setColumnName(String name) {
            JTableBinding.this.throwIfBound();
            this.columnName = name;
            return this;
        }

        public ColumnBinding setColumnClass(Class<?> columnClass) {
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

        /**
         * Sets whether or not the cells of the column should be editable.
         * See this <a href="JTableBinding.html#EDITABILITY">paragraph</a> in the class level
         * documentation on editability.
         *
         * @param editable whether or not the cells of the column should be editable
         */
        public ColumnBinding setEditable(boolean editable) {
            this.editable = editable;
            this.editableSet = true;
            return this;
        }

        /**
         * Returns whether or not the cells of the column should be editable.
         * See this <a href="JTableBinding.html#EDITABILITY">paragraph</a> in the class level
         * documentation on editability.
         *
         * @return whether or not the cells of the column should be editable
         */
        public boolean isEditable() {
            return editable;
        }

        /**
         * Returns whether or not {@code setEditable} has been called on
         * this {@code ColumnBinding}.
         *
         * @return whether or not {@code setEditable} has been called on this
         *         {@code ColumnBinding}
         * @deprecated Unused and to be removed in a future version.
         */
        @Deprecated
        public boolean isEditableSet() {
            return editableSet;
        }

        private void bindUnmanaged0() {
            bindUnmanaged();
        }
        
        private void unbindUnmanaged0() {
            unbindUnmanaged();
        }

        private SyncFailure saveUnmanaged0() {
            return saveUnmanaged();
        }

        private void setSourceObjectUnmanaged0(Object source) {
            setSourceObjectUnmanaged(source);
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

        protected AbstractColumnBinding[] getColBindings() {
            AbstractColumnBinding[] bindings = new AbstractColumnBinding[getColumnBindings().size()];
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
            ColumnBinding cb = JTableBinding.this.getColumnBinding(columnIndex);
            cb.setSourceObjectUnmanaged0(this.getElement(rowIndex));
            cb.setEditingObject(value);
            cb.bindUnmanaged0();
            SyncFailure failure = cb.saveUnmanaged0();
            cb.unbindUnmanaged0();
            cb.setEditingObject(null);
            cb.setSourceObjectUnmanaged0(null);
            if (failure == null) {
                BindingListener[] listeners;
                listeners = cb.getBindingListeners();
                for (BindingListener listener : listeners) {
                    listener.synced(cb);
                }
                listeners = JTableBinding.this.getBindingListeners();
                for (BindingListener listener : listeners) {
                    listener.synced(cb);
                }
            } else {
                BindingListener[] listeners;
                listeners = cb.getBindingListeners();
                for (BindingListener listener : listeners) {
                    listener.syncFailed(cb, failure);
                }
                listeners = JTableBinding.this.getBindingListeners();
                for (BindingListener listener : listeners) {
                    listener.syncFailed(cb, failure);
                }
            }
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
            ColumnBinding binding = JTableBinding.this.getColumnBinding(columnIndex);
            return binding.getColumnName() == null ? binding.getSourceProperty().toString() : binding.getColumnName();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (JTableBinding.this.isEditableSet() && !JTableBinding.this.isEditable()) {
                return false;
            }

            ColumnBinding binding = JTableBinding.this.getColumnBinding(columnIndex);
            if (binding.isEditableSet() && !binding.isEditable()) {
                return false;
            }

            return binding.getSourceProperty().isWriteable(getElement(rowIndex));
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
