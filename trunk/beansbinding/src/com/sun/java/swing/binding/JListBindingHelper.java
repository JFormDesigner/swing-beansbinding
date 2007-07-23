/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.OLDBINDING.Binding;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.beans.OLDBINDING.Binding.BindingController;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author sky
 */
class JListBindingHelper extends AbstractListTableBindingSupport {
    private final JList list;
    private final PropertyDelegate delegate;
    private BindingListModel model;
    private Binding elementsBinding;
    private BindingController elementsBindingController;
    private BindingListCellRenderer renderer;
    // PENDING:
    private List<?> rendererBindings;//List<RendererBinding> rendererBindings;
    private boolean changingRenderer;

    public JListBindingHelper(JList list) {
        this.list = list;
        delegate = new PropertyDelegate();
        // PENDING: notice we never remove this
        list.addPropertyChangeListener(this);
        list.getSelectionModel().addListSelectionListener(this);
    }
    
    public Object getPropertyDelegate() {
        return delegate;
    }

    protected void bindElements(BindingController controller) {
        model = new BindingListModel(controller);
        elementsBinding = controller.getBinding();
        elementsBindingController = controller;
    }
    
    protected void unbindElements() {
        elementsBinding = null;
        elementsBindingController = null;
        if (model != null) {
            model.setElements(null);
            model = null;
            list.setModel(new DefaultListModel());
        }
        if (renderer != null) {
            renderer = null;
            setCellRenderer(null);
        }
    }

    protected void setElements(List<?> elements) {
        model.setElements(elements);
        if (list.getModel() != model) {
            list.setModel(model);
        }
    }
    
    protected List<?> getElements() {
        return (model == null) ? null : model.getElements();
    }
    
    protected Object getSelectedElement() {
        int index = list.getSelectedIndex();
        if (index != -1) {
            return model.getElement(index);
        }
        return null;
    }

    protected ListSelectionModel getSelectionModel() {
        return list.getSelectionModel();
    }

    protected PropertyDelegate getDelegate() {
        return delegate;
    }
    
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName() == "cellRenderer") {
            cellRendererChanged();
        } else {
            super.propertyChange(e);
        }
    }
    
    private void updateRenderer(boolean uninstall) {
        boolean resetRenderer = false;
        if (uninstall && renderer != null) {
            setCellRenderer(null);
            resetRenderer = true;
        }
        if (rendererBindings != null && rendererBindings.size() > 0) {
            resetBindingRenderer();
        } else if (resetRenderer) {
            setCellRenderer(createDefaultRenderer());
        }
    }
    
    private ListCellRenderer createDefaultRenderer() {
        ListCellRenderer listRenderer = (ListCellRenderer)UIManager.get(
                "List.cellRenderer");
        if (listRenderer == null) {
            listRenderer = new DefaultListCellRenderer();
        }
        return listRenderer;
    }
    
    private void resetBindingRenderer() {
        ListCellRenderer listRenderer = list.getCellRenderer();
        if (listRenderer == null) {
            listRenderer = createDefaultRenderer();
        }
        // PENDING:
        renderer = new BindingListCellRenderer(listRenderer);
        setCellRenderer(renderer);
    }
    
    private void setCellRenderer(ListCellRenderer renderer) {
        changingRenderer = true;
        list.setCellRenderer(renderer);
        changingRenderer = false;
    }

    private void cellRendererChanged() {
        if (!changingRenderer) {
            updateRenderer(false);
        }
    }


    private class BindingListModel extends ListBindingManager implements
            ListModel {
        private final List<ListDataListener> listeners;
        
        public BindingListModel(BindingController controller) {
            super(controller);
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }
        
        protected void createColumnDescriptions(
                List<ListBindingManager.ColumnDescription> columns) {
            Binding binding = getController().getBinding();
            if (binding != null) {
                Binding elementsBinding = binding;
                rendererBindings = null;//new ArrayList<RendererBinding>(1);
                for (Binding childBinding : elementsBinding.getChildBindings()) {
                    String targetPath = childBinding.getTargetPath();
                    if (targetPath == null) {
                        columns.add(new ColumnDescription(
                                childBinding, 0, true, Object.class));
                    }
                    // PENDING: tidy this up.
//                    PropertyPath path = PropertyPath.createPropertyPath(
//                            childBinding.getTargetPath());
//                    int pathCount = path.length();
//                    if (pathCount == 1 && path.get(0).equals("value")) {
//                        columns.add(new ColumnDescription(childBinding, 0, true));
//                    } else if (pathCount > 1 && path.get(0).equals("renderer")) {
//                        columns.add(new ColumnDescription(childBinding, 0, false));
//                        rendererBindings.add(new RendererBinding(childBinding));
//                    }
                }
           } else {
                rendererBindings = null;
            }
        }

        protected void allChanged() {
            contentsChanged(0, size());
            updateRenderer(true);
        }

        protected void valueChanged(int row, int column) {
            contentsChanged(row, row);
        }

        protected void added(int index, int length) {
            ListDataEvent e = new ListDataEvent(this,
                    ListDataEvent.INTERVAL_ADDED, index, index + length - 1);
            for (ListDataListener listener : listeners) {
                listener.intervalAdded(e);
            }
        }

        protected void removed(int index, int length) {
            ListDataEvent e = new ListDataEvent(this,
                    ListDataEvent.INTERVAL_REMOVED, index, index + length - 1);
            for (ListDataListener listener : listeners) {
                listener.intervalRemoved(e);
            }
        }

        protected void changed(int row) {
            contentsChanged(row, row);
        }

        private void contentsChanged(int row0, int row1) {
            ListDataEvent e = new ListDataEvent(this,
                    ListDataEvent.CONTENTS_CHANGED, row0, row1);
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
    

    private final class BindingListCellRenderer implements ListCellRenderer {
        private final ListCellRenderer renderer;
        
        BindingListCellRenderer(ListCellRenderer renderer) {
            this.renderer = renderer;
        }
        
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            // PENDING: resolve this
//            for (RendererBinding rb : rendererBindings) {
//                PropertyResolver sourceResolver = rb.getSourceResolver();
//                sourceResolver.setSource(model.getElement(index));
//                // PENDING: this is incomplete, it needs to handle other cases
//                // and value states.
//                PropertyResolver targetResolver = rb.getTargetResolver();
//                targetResolver.setSource(renderer);
//                Object sourceValue = BindingUtils.getSourceValue(
//                        rb.getBinding(), sourceResolver, 
//                        targetResolver.getTypeOfLastProperty());
//                targetResolver.setValueOfLastProperty(sourceValue);
//            }
            return renderer.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
        }
    }
    
    
//    private static final class RendererBinding {
//        private final Binding binding;
//        private final PropertyResolver sourceResolver;
//        private final PropertyResolver targetResolver;
//        
//        RendererBinding(Binding binding) {
//            this.binding = binding;
//            sourceResolver = PropertyResolver.createPropertyResolver(
//                    PropertyPath.createPropertyPath(binding.getSourcePath()));
//            PropertyPath targetPath = PropertyPath.createPropertyPath(
//                    binding.getTargetPath());
//            targetResolver = PropertyResolver.createPropertyResolver(
//                    targetPath.subPath(1, targetPath.length() - 1));
//        }
//        
//        public Binding getBinding() {
//            return binding;
//        }
//        
//        public PropertyResolver getSourceResolver() {
//            return sourceResolver;
//        }
//        
//        public PropertyResolver getTargetResolver() {
//            return targetResolver;
//        }
//    }
}
