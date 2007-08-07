/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import javax.swing.*;
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

    public TableColumnBinding addColumnBinding(SourceableProperty source) {
        throwIfBound();
        if (source == null) {
            throw new IllegalArgumentException("can't have null source");
        }

        TableColumnBinding binding = new TableColumnBinding(source);
        columnBindings.add(binding);
        return binding;
    }

    public TableColumnBinding addColumnBinding(int index, SourceableProperty source) {
        throwIfBound();
        if (source == null) {
            throw new IllegalArgumentException("can't have null source");
        }

        TableColumnBinding binding = new TableColumnBinding(source);
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
        super.setAutoUpdateStrategy(AutoUpdateStrategy.READ);
        prepareElementsProperty();
    }

    private void prepareElementsProperty() {
        ep = (ElementsProperty)getTarget();
        ep.addPropertyStateListener(handler);
    }

    private final Property DUMMY_PROPERTY = new BeanProperty("DUMMY");

    public final class TableColumnBinding extends SwingBinding {
        private Class<?> columnClass;
        private boolean editable;

        public TableColumnBinding(SourceableProperty prop) {
            super(prop, DUMMY_PROPERTY);
        }

        public void setColumnClass(Class<?> columnClass) {
        }

        public Class<?> getColumnClass() {
            return null;
        }
        
        public void setEditable(boolean editable) {
        }

        public boolean isEditable() {
            return false;
        }

        public boolean isEditableSet() {
            return false;
        }
    }

    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (!pse.getValueChanged()) {
                return;
            }

            if (table != null) {
                System.out.println("uninstall table model");
                table = null;
                elements = null;
            }

            Object newValue = pse.getNewValue();

            if (newValue != null && newValue != PropertyStateEvent.UNREADABLE) {
                System.out.println("install table model");
                table = ep.getComponent();
                elements = (List<T>)newValue;
            }
        }
    }

}
