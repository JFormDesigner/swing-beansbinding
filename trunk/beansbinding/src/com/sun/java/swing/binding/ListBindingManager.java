/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.Binding;
import javax.beans.binding.ELPropertyResolver;
import com.sun.java.util.ObservableList;
import com.sun.java.util.ObservableListListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.beans.binding.Binding.BindingController;

/**
 *
 * @author sky
 */
abstract class ListBindingManager implements ObservableListListener {
    private BindingController controller;
    private List<?> elements;
    private List<ColumnDescriptionManager> managers;
    private ColumnDescription[] columns;

    public ListBindingManager(BindingController controller) {
        this.controller = controller;
    }
    
    protected Binding.BindingController getController() {
        return controller;
    }
    
    public final void refresh() {
        setElements(getElements());
    }
    
    protected final ColumnDescription getValueColumnDescription(int column) {
        return this.columns[column];
    }

    private void calculateValueColumns(List<ColumnDescription> columns) {
        int max = -1;
        for (ColumnDescription column : columns) {
            max = Math.max(column.getColumn(), max);
        }
        this.columns = new ColumnDescription[max + 1];
        for (ColumnDescription column : columns) {
            if (column.isValue()) {
                this.columns[column.getColumn()] = column;
            }
        }
    }
    
    private List<ColumnDescriptionManager> createManagers(
            List<ColumnDescription> columns) {
        List<ColumnDescriptionManager> managers = 
                new ArrayList<ColumnDescriptionManager>(columns.size());
        for (ColumnDescription description : columns) {
            managers.add(new ColumnDescriptionManager(description));
        }
        return managers;
    }

    // PENDING: this needs to handle null and null binding.
    public final void setElements(List<?> elements) {
        int oldSize = size();
        if (this.elements != null) {
            boolean registerListeners;
            if (this.elements instanceof ObservableList) {
                ((ObservableList)this.elements).
                        removeObservableListListener(this);
            }
            for (ColumnDescriptionManager manager : managers) {
                manager.unbind();
            }
        }
        managers = null;
        int newSize = 0;
        if (elements == null) {
            elements = Collections.emptyList();
        }
        this.elements = elements;
        List<ColumnDescription> columns = new LinkedList<ColumnDescription>();
        createColumnDescriptions(columns);
        if (columns.size() == 0) {
            columns.add(new ColumnDescription(null, 0, true, Object.class));
        }
        calculateValueColumns(columns);
        
        newSize = size();
        boolean addListeners;
        if (elements instanceof ObservableList) {
            ((ObservableList)elements).addObservableListListener(this);
            addListeners = !((ObservableList)elements).supportsElementPropertyChanged();
        } else {
            addListeners = true;
        }
        
        if (addListeners) {
            managers = createManagers(columns);
            for (ColumnDescriptionManager manager : managers) {
                manager.bind(newSize);
            }
        }
        allChanged();
    }
    
    public final Object getElement(int index) {
        return elements.get(index);
    }

    public final List<?> getElements() {
        return elements;
    }
    
    public final int size() {
        return (elements == null) ? 0 : elements.size();
    }
    
    public final Object valueAt(int row, int column) {
        return valueAt(row, column, false);
    }
    
    public final Object valueAt(int row, int column, 
            boolean returnElementIfNoBinding) {
        if (managers != null) {
            // Make sure the necessary bindings have been registered
            for (ColumnDescriptionManager manager : managers) {
                manager.validateBinding(row);
            }
        }
        ColumnDescription cd = columns[column];
        if (cd != null) {
            ELPropertyResolver resolver = cd.getPropertyResolver();
            String path = resolver.getPath();
            if (path == null || "".equals(path)) {
                return getElement(row);
            }
            resolver.setSource(getElement(row));
            return controller.getValueForTarget(cd.getBinding(), resolver, cd.getTargetType());
        }
        if (returnElementIfNoBinding) {
            return getElement(row);
        }
        return null;
    }
    
    public final int columnCount() {
        return columns.length;
    }
        
    public final void listElementsAdded(ObservableList list, int index, int length) {
        if (managers != null) {
            for (ColumnDescriptionManager manager : managers) {
                manager.add(index, length);
            }
        }
        added(index, length);
    }
    
    public final void listElementsRemoved(ObservableList list, int index,
            List elements) {
        if (managers != null) {
            for (ColumnDescriptionManager manager : managers) {
                manager.remove(index, elements.size());
            }
        }
        removed(index, elements.size());
    }
    
    public final void listElementReplaced(ObservableList list, int index,
            Object oldElement) {
        if (managers != null) {
            for (ColumnDescriptionManager manager : managers) {
                manager.replaced(index);
            }
        }
        changed(index);
    }
    
    public final void listElementPropertyChanged(ObservableList list, int index) {
        changed(index);
    }

    protected abstract void createColumnDescriptions(List<ColumnDescription> columns);
    
    protected abstract void allChanged();

    protected abstract void valueChanged(int row, int column);

    protected abstract void added(int index, int length);

    protected abstract void removed(int index, int length);

    protected abstract void changed(int row);
    
    
    protected final class ColumnDescription {
        private final Binding binding;
        private final int column;
        private final boolean isValue;
        private final Class<?> targetType;
        private ELPropertyResolver resolver;

        protected ColumnDescription(Binding binding, int column,
                boolean isValue, Class<?> targetType) {
            this.binding = binding;
            this.column = column;
            this.isValue = isValue;
            this.targetType = targetType;
        }
        
        // NOTE: This may be null!
        public Binding getBinding() {
            return binding;
        }
        
        private int getColumn() {
            return column;
        }
        
        private boolean isValue() {
            return isValue;
        }
        
        private ELPropertyResolver getPropertyResolver() {
            if (resolver == null) {
                resolver = createPropertyResolver();
            }
            return resolver;
        }

        private ELPropertyResolver createPropertyResolver() {
            ELPropertyResolver resolver = getController().createResolver();
            if (binding != null) {
                resolver.setPath(binding.getSourceExpression());
            }
            return resolver;
        }

        public Class<?> getTargetType() {
            return targetType;
        }
        
        public String toString() {
            return "ColumnDescription [" +
                    " binding=" + binding + "," +
                    " column=" + column + "," +
                    " isValue=" + isValue + 
                    " targetType=" + targetType +
                    "]";
        }
    }
    
    
    private final class ColumnDescriptionManager {
        private final ColumnDescription description;
        private List<EntryWrapper> wrappers;
        
        ColumnDescriptionManager(ColumnDescription description) {
            this.description = description;
        }
        
        public void bind(int newSize) {
            wrappers = new ArrayList<EntryWrapper>(newSize);
            for (int i = 0; i < newSize; i++) {
                wrappers.add(null);
            }
        }
        
        public void unbind() {
            if (wrappers != null) {
                for (EntryWrapper wrapper : wrappers) {
                    if (wrapper != null) {
                        wrapper.unbind();
                    }
                }
                wrappers = null;
            }
        }

        public void validateBinding(int row) {
            if (wrappers.get(row) == null) {
                EntryWrapper wrapper = new EntryWrapper(getElement(row));
                wrappers.set(row, wrapper);
            }
        }
        
        void wrapperChanged(EntryWrapper wrapper) {
            int row = wrappers.indexOf(wrapper);
            ListBindingManager.this.valueChanged(row, description.getColumn());
        }

        private void add(int index, int length) {
            if (wrappers != null) {
                for (int i = 0; i < length; i++) {
                    wrappers.add(index, null);
                }
            }
        }

        private void remove(int index, int length) {
            if (wrappers != null) {
                while (length-- > 0) {
                    EntryWrapper wrapper = wrappers.remove(index);
                    if (wrapper != null) {
                        wrapper.unbind();
                    }
                }
            }
        }

        private void replaced(int index) {
            if (wrappers != null) {
                EntryWrapper wrapper = wrappers.get(index);
                if (wrapper != null) {
                    wrapper.unbind();
                }
                wrappers.set(index, null);
            }
        }
        
        public String toString() {
            return "ColumnDescriptionManager [description=" + description + "]";
        }

        
        private final class EntryWrapper extends ELPropertyResolver.Delegate {
            private ELPropertyResolver resolver;
            
            EntryWrapper(Object source) {
                resolver = description.createPropertyResolver();
                resolver.setSource(source);
                resolver.bind();
                resolver.setDelegate(this);
            }
            
            public void unbind() {
                resolver.unbind();
            }
            
            public void valueChanged(ELPropertyResolver resolver) {
                wrapperChanged(this);
            }
        }
    }
}
