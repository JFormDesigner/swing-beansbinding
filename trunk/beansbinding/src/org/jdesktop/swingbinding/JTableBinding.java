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
 * adding {@link org.jdesktop.swingbinding.JTableBinding.ColumnBinding ColumnBindings}
 * to this {@code JTableBinding}.
 * <p>
 * If the {@code List} is an instance of {@code ObservableList}, then changes
 * to the {@code List} are reflected in the {@code JTable}. {@code JTableBinding}
 * also listens to the properties specified for {@code ColumnBindings}, for all rows,
 * and updates its display values in response to change.
 * <p>
 * Instances of {@code JTableBinding} are obtained by calling one of the
 * {@code createJTableBinding} methods in the {@code SwingBindings} class. There
 * are methods for creating a {@code JTableBinding} using direct references to a
 * {@code List} and/or {@code JTable} and methods for creating a {@code JTableBinding} by
 * providing the {@code List} and/or {@code JTable} as {@code Property} instances
 * that derive the {@code List} and/or {@code JTable} from binding's the source/target objects.
 * <p>
 * {@code JTableBinding} works by installing a custom model on the target {@code JTable},
 * at bind time if the {@code JTable} property is readable, or whenever it becomes
 * readable after binding. This model is uninstalled when the property becomes unreadable
 * or the binding is unbound. It is also uninstalled, and installed on the replacement,
 * when the value of the {@code JTable} property changes. When the model is uninstalled from a
 * {@code JTable}, the {@code JTable's} model is replaced with an empty {@code DefaultTableModel}
 * so that it is left functional.
 * <p>
 * This class is a subclass of {@code AutoBinding}. The update strategy dictates how
 * the binding applies the value of the source {@code List} property to the model
 * used for the {@code JTable}. At bind time, if the source {@code List} property and
 * the target {@code JTable} property are both readable, the source {@code List}
 * becomes the source of elements for the model. If the strategy is {@code READ_ONCE}
 * then there is no further automatic syncing after this point, including if the
 * target {@code JTable} property changes or becomes readable; the new {@code JTable} gets the model,
 * but no elements. If the strategy is {@code READ}, however, the {@code List} is synced
 * to the model every time the source {@code List} property changes value, or the
 * target {@code JTable} property changes value or becomes readable. For
 * {@code JTableBinding}, the {@code READ_WRITE} strategy behaves identical to {@code READ}.
 * <p>
 * <a name="EDITABILITY">A cell</a> in the {@code JTable} is editable for any given row and
 * column when all of the following are true: the property specified for that column
 * by its {@code ColumnBinding} is editable for the object in that row, the {@code "editable"} property
 * of the {@code JTableBinding} is {@code true} (the default), and the {@code "editable"}
 * property of the {@code ColumnBinding} is {@code true} (the default).
 * <p>
 * Note that the {@code JTable} target of a {@code JTableBinding} is acting as a view for the
 * live {@code List} of elements in the source {@code List}. As such, all successful changes made
 * to cell values are committed back immediately to the source {@code List}.
 * <p>
 * {@code ColumnBindings} are managed by the {@code JTableBinding}. They are not
 * to be explicitly bound, unbound, added to a {@code BindingGroup}, or accessed
 * in a way that is not allowed for a managed binding. {@code BindingListeners}
 * added to a {@code ColumnBinding} are notified at the time an edited table value
 * is to be committed back to the source {@code List}. They receive notification of either
 * {@code synced} or {@code syncFailed}. {@code BindingListeners} added to the
 * {@code JTableBinding} itself are also notified of {@code sync} and {@code syncFailed}
 * for the {@code JTableBinding's ColumnBindings}.
 * <p>
 * Here is an example of creating a binding from a {@code List} of {@code Person}
 * objects to a {@code JTable}:
 * <p>
 * <pre><code>
 *    // create the person List
 *    List<Person> people = createPersonList();
 *
 *    // create the binding from List to JTable
 *    JTableBinding tb = SwingBindings.createJTableBinding(READ, people, jTable);
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
 * bind to the selection of a {@code JTable}. When binding to the selection of a {@code JTable}
 * backed by a {@code JTableBinding}, the selection is always in terms of elements
 * from the source {@code List}. See the list of <a href="package-summary.html#SWING_PROPS">
 * interesting swing properties</a> in the package summary for more details.
 *
 * @param <E> the type of elements in the source {@code List}
 * @param <SS> the type of source object (on which the source property resolves to {@code List})
 * @param <TS> the type of target object (on which the target property resolves to {@code JTable})
 *
 * @author Shannon Hickey
 */
public final class JTableBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {

    private ElementsProperty<TS, JTable> ep;
    private Handler handler = new Handler();
    private BindingTableModel model;
    private JTable table;
    private boolean editable = true;
    private List<ColumnBinding> columnBindings = new ArrayList<ColumnBinding>();

    /**
     * Constructs an instance of {@code JTableBinding}.
     *
     * @param strategy the update strategy
     * @param sourceObject the source object
     * @param sourceProperty a property on the source object that resolves to the {@code List} of elements
     * @param targetObject the target object
     * @param targetProperty a property on the target object that resolves to a {@code JTable}
     * @param name a name for the {@code JTableBinding}
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
     * The default for this property is {@code true}.
     * See this <a href="#EDITABILITY">paragraph</a> in the class level
     * documentation on editability.
     *
     * @param editable whether or not the cells of the table should be editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Returns whether or not the cells of the table should be editable.
     * The default for this property is {@code true}.
     * See this <a href="#EDITABILITY">paragraph</a> in the class level
     * documentation on editability.
     *
     * @return whether or not the cells of the table should be editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Creates a {@code ColumnBinding} and adds it to the end of the list of {@code ColumnBindings}
     * maintained by this {@code JTableBinding}.
     * <p>
     * The list of {@code ColumnBindings} dictates the columns to be displayed in the
     * {@code JTable}, with a {@code ColumnBinding's} order in the list determining its
     * table model index.
     *
     * @param columnProperty the property with which to derive cell values from the
     *                       elements of the source {@code List}
     * @return the {@code ColumnBinding}
     * @throws IllegalArgumentException if {@code columnProperty} is {@code null}
     * @see org.jdesktop.swingbinding.JTableBinding.ColumnBinding
     */
    public ColumnBinding addColumnBinding(Property<E, ?> columnProperty) {
        return addColumnBinding(columnProperty, null);
    }

    /**
     * Creates a named {@code ColumnBinding} and adds it to the end of the list of {@code ColumnBindings}
     * maintained by this {@code JTableBinding}.
     * <p>
     * The list of {@code ColumnBindings} dictates the columns to be displayed in the
     * {@code JTable}, with a {@code ColumnBinding's} order in the list determining its
     * table model index.
     *
     * @param columnProperty the property with which to derive cell values from the
     *                       elements of the source {@code List}
     * @param name a name for the column binding
     * @return the {@code ColumnBinding}
     * @throws IllegalArgumentException if {@code columnProperty} is {@code null}
     * @see org.jdesktop.swingbinding.JTableBinding.ColumnBinding
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
     * Creates a {@code ColumnBinding} and inserts it at the given index into the list
     * of {@code ColumnBindings} maintained by this {@code JTableBinding}.
     * <p>
     * The list of {@code ColumnBindings} dictates the columns to be displayed in the
     * {@code JTable}, with a {@code ColumnBinding's} order in the list determining its
     * table model index.
     *
     * @param index the index at which to insert the {@code ColumnBinding}
     * @param columnProperty the property with which to derive cell values from the
     *                       elements of the source {@code List}
     * @return the {@code ColumnBinding}
     * @throws IllegalArgumentException if {@code columnProperty} is {@code null}
     * @see org.jdesktop.swingbinding.JTableBinding.ColumnBinding
     */
    public ColumnBinding addColumnBinding(int index, Property<E, ?> columnProperty) {
        return addColumnBinding(index, columnProperty, null);
    }

    /**
     * Creates a {@code ColumnBinding} and inserts it at the given index into the list
     * of {@code ColumnBindings} maintained by this {@code JTableBinding}.
     * <p>
     * The list of {@code ColumnBindings} dictates the columns to be displayed in the
     * {@code JTable}, with a {@code ColumnBinding's} order in the list determining its
     * table model index.
     *
     * @param index the index at which to insert the {@code ColumnBinding}
     * @param columnProperty the property with which to derive cell values from the
     *                       elements of the source {@code List}
     * @param name a name for the {@code ColumnBinding}
     * @return the {@code ColumnBinding}
     * @throws IllegalArgumentException if {@code columnProperty} is {@code null}
     * @see org.jdesktop.swingbinding.JTableBinding.ColumnBinding
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

    /**
     * Removes the given {@code ColumnBinding} from the list maintained
     * by this {@code JTableBinding}.
     * <p>
     * The list of {@code ColumnBindings} dictates the columns to be displayed in the
     * {@code JTable}, with a {@code ColumnBinding's} order in the list determining its
     * table model index.
     *
     * @param binding the {@code ColumnBinding} to remove
     * @see #addColumnBinding(Property, String)
     */
    public boolean removeColumnBinding(ColumnBinding binding) {
        throwIfBound();
        boolean retVal = columnBindings.remove(binding);

        if (retVal) {
            adjustIndices(binding.getColumn(), false);
        }

        return retVal;
    }

    /**
     * Removes the {@code ColumnBinding} with the given index from the list maintained
     * by this {@code JTableBinding}.
     * <p>
     * The list of {@code ColumnBindings} dictates the columns to be displayed in the
     * {@code JTable}, with a {@code ColumnBinding's} order in the list determining its
     * table model index.
     *
     * @param index the index of the {@code ColumnBinding} to remove
     * @see #addColumnBinding(Property, String)
     */
    public ColumnBinding removeColumnBinding(int index) {
        throwIfBound();
        ColumnBinding retVal = columnBindings.remove(index);
        
        if (retVal != null) {
            adjustIndices(index, false);
        }

        return retVal;
    }

    /**
     * Returns the {@code ColumnBinding} with the given index in the list maintained
     * by this {@code JTableBinding}.
     * <p>
     * The list of {@code ColumnBindings} dictates the columns to be displayed in the
     * {@code JTable}, with a {@code ColumnBinding's} order in the list determining its
     * table model index.
     *
     * @param index the index of the {@code ColumnBinding} to return
     * @return the {@code ColumnBinding} at the given index
     * @see #addColumnBinding(Property, String)
     */
    public ColumnBinding getColumnBinding(int index) {
        return columnBindings.get(index);
    }

    /**
     * Returns an unmodifiable copy of the list of {@code ColumnBindings} maintained
     * by this {@code JTableBinding}.
     * <p>
     * The list of {@code ColumnBindings} dictates the columns to be displayed in the
     * {@code JTable}, with a {@code ColumnBinding's} order in the list determining its
     * table model index.
     *
     * @return the list of {@code ColumnBindings}
     * @see #addColumnBinding(Property, String)
     */
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

    /**
     * {@code ColumnBinding} represents a binding between a property of the elements
     * in the {@code JTableBinding's} source {@code List}, and a column in the table. Each
     * {@code ColumnBinding} added to a {@code JTableBinding} represents a column
     * to be displayed by the {@code JTable}. A value for any given row in a column
     * is aquired by fetching the value of the associated {@code ColumnBinding's}
     * source property for the element in the source {@code List} representing that row.
     * <p>
     * A {@code Converter} may be specified on a {@code ColumnBinding}, as may be
     * a {@code Validator}. Validation occurs at the time a cell value is to be
     * committed back to the source {@code List}.
     * <p>
     * {@code BindingListeners} registered on
     * a {@code ColumnBinding} are notified of successful {@code sync} or
     * {@code syncFailure}. These notifications are also sent to the
     * {@code JTableBinding's} {@code BindingListeners}.
     * <p>
     * {@code ColumnBindings} are managed by their {@code JTableBinding}. They are not
     * to be explicitly bound, unbound, added to a {@code BindingGroup}, or accessed
     * in a way that is not allowed for a managed binding.
     *
     * @see org.jdesktop.swingbinding.JTableBinding#addColumnBinding(Property, String)
     */
    public final class ColumnBinding extends AbstractColumnBinding {
        private Class<?> columnClass;
        private boolean editable = true;
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

        /**
         * Sets a name for the column represented by this {@code ColumnBinding}.
         * This is used to initialize the table's column header name. If
         * {@code null} is specified, the {@code toString()} value of the
         * {@code ColumnBinding's} source property is used.
         *
         * @param name the name
         * @return the {@code ColumnBinding} itself, to allow for method chaining
         * @see javax.swing.table.TableModel#getColumnName
         */
        public ColumnBinding setColumnName(String name) {
            JTableBinding.this.throwIfBound();
            this.columnName = name;
            return this;
        }

        /**
         * Sets the column class to be used by {@code JTable} to determine
         * the renderer and editor for the column represented by this
         * {@code ColumnBinding}.
         *
         * @param columnClass the column class
         * @return the {@code ColumnBinding} itself, to allow for method chaining
         * @see javax.swing.table.TableModel#getColumnClass
         */
        public ColumnBinding setColumnClass(Class<?> columnClass) {
            JTableBinding.this.throwIfBound();
            this.columnClass = columnClass;
            return this;
        }

        /**
         * Returns the column class to be used by {@code JTable} to determine
         * the renderer and editor for the column represented by this
         * {@code ColumnBinding}.
         *
         * @see setColumnClass
         * @see javax.swing.table.TableModel#getColumnClass
         */
        public Class<?> getColumnClass() {
            return columnClass == null ? Object.class : columnClass;
        }

        /**
         * Returns the name for the column represented by this {@code ColumnBinding}.
         * This is used to initialize the table's column header name.  If no name
         * has been specified, or if it has been set to {@code null}, the
         * {@code toString()} value of the {@code ColumnBinding's} source property is returned.
         *
         * @return the name for the column
         * @see #setColumnName
         * @see javax.swing.table.TableModel#getColumnName
         */
        public String getColumnName() {
            return columnName == null ? getSourceProperty().toString() : columnName;
        }

        /**
         * Sets whether or not the cells of the column should be editable.
         * The default for this property is {@code true}.
         * See this <a href="JTableBinding.html#EDITABILITY">paragraph</a> in the class level
         * documentation on editability.
         *
         * @param editable whether or not the cells of the column should be editable
         * @return the {@code ColumnBinding} itself, to allow for method chaining
         */
        public ColumnBinding setEditable(boolean editable) {
            this.editable = editable;
            return this;
        }

        /**
         * Returns whether or not the cells of the column should be editable.
         * The default for this property is {@code true}.
         * See this <a href="JTableBinding.html#EDITABILITY">paragraph</a> in the class level
         * documentation on editability.
         *
         * @return whether or not the cells of the column should be editable
         */
        public boolean isEditable() {
            return editable;
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
            if (!JTableBinding.this.isEditable()) {
                return false;
            }

            ColumnBinding binding = JTableBinding.this.getColumnBinding(columnIndex);
            if (!binding.isEditable()) {
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
