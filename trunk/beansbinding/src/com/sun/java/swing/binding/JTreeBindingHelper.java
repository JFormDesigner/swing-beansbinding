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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.beans.binding.Binding.BindingController;
import javax.el.Expression;
import javax.swing.JTree;
import javax.swing.binding.SwingBindingSupport;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 *
 * @author sky
 */
final class JTreeBindingHelper extends AbstractBindingHelper implements 
        TreeSelectionListener {
    private final JTree tree;
    private final Map<Object,BindingNode> nodeMap;
    private final Map<Class<?>,Binding> elementMap;
    private final BindingTreeModel model;
    private BindingController controller;
    private BindingNode root;
    private PropertyDelegate delegate;
    private List<Object> selectedElements;
    private Object selectedElement;
    private boolean changingSelection;
    private boolean emptyNodesTreatedAsLeafs;

    
    public JTreeBindingHelper(JTree tree) {
        this.tree = tree;
        elementMap = new HashMap<Class<?>,Binding>();
        nodeMap = new HashMap<Object,BindingNode>();
        root = new BindingNode(null, null);
        model = new BindingTreeModel();
        delegate = new PropertyDelegate();
        tree.addTreeSelectionListener(this);
    }

    public Object getPropertyDelegate() {
        return delegate;
    }
    
    protected boolean shouldCreateBindingTarget(String property) {
        return (property == JTREE_ROOT_P);
    }

    public void bind(BindingController controller, String property) {
        if (property == JTREE_ROOT_P) {
            bindRoot(controller);
        }
    }

    public void unbind(BindingController controller,
            String property) {
        if (property == JTREE_ROOT_P) {
            unbindRoot();
        }
    }
    
    private void bindRoot(BindingController controller) {
        if (this.controller != null) {
            throw new IllegalStateException("Already bound");
        }
        this.controller = controller;
        emptyNodesTreatedAsLeafs = controller.getBinding().getParameterValue(
                SwingBindingSupport.EmptyNodeTreatedAsLeafParameter.class, Boolean.FALSE);
        calculateChildMapping(controller.getBinding());
        if (tree.getModel() != model) {
            tree.setModel(model);
        }
    }
    
    private void unbindRoot() {
        this.controller = null;
        tree.setModel(null);
        elementMap.clear();
    }
    
    private void setRoot(Object root) {
        Set<BindingNode> nodes = new HashSet<BindingNode>(nodeMap.values());
        for (BindingNode node : nodes) {
            node.dispose();
        }
        BindingNode node = new BindingNode(null, root);
        nodeMap.put(root, node);
        this.root = node;
        model.fireRootChanged();
    }
    
    private void updateSelectElements() {
        TreePath[] selectedPaths = tree.getSelectionModel().getSelectionPaths();
        if (selectedPaths == null) {
            selectedElements = new ArrayList<Object>(1);
        } else {
            selectedElements = new ArrayList<Object>(selectedPaths.length);
            for (TreePath path : selectedPaths) {
                selectedElements.add(path.getLastPathComponent());
            }
        }
        selectedElements = Collections.unmodifiableList(selectedElements);
        selectedElement = (selectedElements.size() == 0) ? null :
            selectedElements.get(0);
    }
    
    private BindingNode getBindingNode(Object e) {
        return nodeMap.get(e);
    }

    private void createBindingNodeIfNecessary(BindingNode parentBindingNode,
            Object child) {
        BindingNode childNode = getBindingNode(child);
        if (childNode == null) {
            childNode = new BindingNode(parentBindingNode, child);
            nodeMap.put(child, childNode);
        }
    }
        
    private Binding getElementsBinding(Object source) {
        if (source != null) {
            return getElementsBinding(source.getClass());
        }
        return null;
    }
    
    private Binding getElementsBinding(Class<?> type) {
        Binding binding = elementMap.get(type);
        if (binding == null) {
            Class<?> superType = type.getSuperclass();
            if (superType == null) {
                binding = null;
            } else {
                binding = getElementsBinding(superType);
            }
            elementMap.put(type, binding);
        }
        return binding;
    }
    
    private void nodeStructureChanged(BindingNode node) {
        model.fireTreeStructureChanged(node.getPathToRoot());
    }

    private void nodeElementsAdded(BindingNode node, int index, int length) {
        model.fireTreeNodesInserted(node.getPathToRoot(), index, length);
    }
    
    private void nodeElementsRemoved(BindingNode bindingNode, int index,
            List oldElements) {
        model.fireTreeNodesRemoved(bindingNode.getPathToRoot(), index,
                oldElements);
    }
    
    private void nodeElementReplaced(BindingNode bindingNode, int index,
            Object oldElement) {
        model.handleReplace(bindingNode, index, oldElement);
    }
    
    private void nodeElementPropertyChanged(BindingNode parent, int index) {
        model.fireTreeNodesChanged(parent, index);
    }

    private void calculateChildMapping(Binding binding) {
        elementMap.clear();
        for (Binding childBinding : binding.getBindings()) {
            Class<?> type = childBinding.getParameterValue(
                    SwingBindingSupport.TreeNodeClassParameter.class, null);
            if (type == null) {
                throw new IllegalArgumentException(
                        "Must specify a class the binding is applicable to " +
                        "using the key SwingBindingSupport.TreeNodeClassKey");
            }
            elementMap.put(type, childBinding);
        }
    }

    public void valueChanged(TreeSelectionEvent e) {
        if (!changingSelection) {
            Object lastSelectedElement = selectedElement;
            List<Object> lastSelectedElements = selectedElements;
            selectedElements = null;
            selectedElement = null;
            delegate.firePropertyChange(SELECTED_ELEMENT_P,
                    lastSelectedElement, null);
            delegate.firePropertyChange(SELECTED_ELEMENTS_P,
                    lastSelectedElements, null);
        }
    }

    private void setSelectedElements(List<?> selection) {
        changingSelection = true;
        Object lastSelectedElement = selectedElement;
        List<Object> oldSelection = selectedElements;
        List<Object> newSelection;
        TreeSelectionModel selectionModel = tree.getSelectionModel();
        selectionModel.clearSelection();
        if (selection == null || selection.size() == 0) {
            newSelection = Collections.emptyList();
        } else {
            newSelection = new ArrayList<Object>(selection.size());
            for (Object elem : selection) {
                BindingNode node = getBindingNode(elem);
                if (node != null) {
                    newSelection.add(elem);
                    selectionModel.addSelectionPath(node.getPathToRoot());
                }
            }
        }
        selectedElement = (newSelection.size() == 0) ? null : newSelection.get(0);
        selectedElements = Collections.unmodifiableList(newSelection);
        changingSelection = false;
        delegate.firePropertyChange(SELECTED_ELEMENT_P, lastSelectedElement,
                selectedElement);
        delegate.firePropertyChange(SELECTED_ELEMENTS_P, oldSelection,
                selectedElements);
    }

    private List<?> getSelectedElements() {
        if (selectedElements == null) {
            updateSelectElements();
        }
        return selectedElements;
    }

    private Object getSelectedElement() {
        if (selectedElement == null) {
            updateSelectElements();
        }
        return selectedElement;
    }

    private boolean getEmptyNodesTreatedAsLeafs() {
        return emptyNodesTreatedAsLeafs;
    }
    

    
    private class BindingTreeModel implements TreeModel {
        private final List<TreeModelListener> listeners;
        private BindingNode removeParent;
        private int removeHole;
        
        public BindingTreeModel() {
            listeners = new CopyOnWriteArrayList<TreeModelListener>();
        }
        
        public Object getRoot() {
            return root.getSource();
        }

        public Object getChild(Object parent, int index) {
            BindingNode parentBindingNode = getBindingNode(parent);
            if (parentBindingNode == removeParent && index >= removeHole) {
                index--;
            }
            Object child = parentBindingNode.getElements().get(index);
            createBindingNodeIfNecessary(parentBindingNode, child);
            return child;
        }

        public int getChildCount(Object parent) {
            BindingNode node = getBindingNode(parent);
            List<?> elements = node.getElements();
            if (node == removeParent) {
                return elements.size() - 1;
            }
            return (elements == null) ? 0 : elements.size();
        }

        public boolean isLeaf(Object node) {
            boolean isLeaf = getBindingNode(node).isLeaf();
            if (!isLeaf) {
                if (getEmptyNodesTreatedAsLeafs() &&
                        getChildCount(node) == 0) {
                    return true;
                }
            }
            return isLeaf;
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
            // PENDING: need to create Binding and set back
        }

        public int getIndexOfChild(Object parent, Object child) {
            BindingNode node = getBindingNode(parent);
            int index = node.getElements().indexOf(child);
            if (node == removeParent && index > removeHole) {
                index--;
            }
            return index;
        }

        public void addTreeModelListener(TreeModelListener l) {
            listeners.add(l);
        }

        public void removeTreeModelListener(TreeModelListener l) {
            listeners.remove(l);
        }

        private void fireTreeStructureChanged(TreePath treePath) {
            TreeModelEvent e = new TreeModelEvent(this, treePath);
            for (TreeModelListener l : listeners) {
                l.treeStructureChanged(e);
            }
        }

        private void fireTreeNodesInserted(TreePath treePath, int index, int length) {
            int[] indices = new int[length];
            Object[] children = new Object[length];
            List<?> elements = getBindingNode(treePath.getLastPathComponent()).
                    getElements();
            for (int i = 0; i < length; i++) {
                indices[i] = index + i;
                children[i] = elements.get(index + i);
            }
            TreeModelEvent e = new TreeModelEvent(this, treePath, indices,
                    children);
            for (TreeModelListener l : listeners) {
                l.treeNodesInserted(e);
            }
        }

        private void fireTreeNodesRemoved(TreePath treePath, int index,
                List oldElements) {
            int length = oldElements.size();
            int[] indices = new int[length];
            Object[] children = new Object[length];
            for (int i = 0; i < length; i++) {
                indices[i] = index + i;
                children[i] = oldElements.get(i);
            }
            TreeModelEvent e = new TreeModelEvent(this, treePath, indices,
                    children);
            for (TreeModelListener l : listeners) {
                l.treeNodesRemoved(e);
            }
        }

        private void handleReplace(BindingNode node, int index,
                Object oldElement) {
            removeParent = node;
            removeHole = index;
            fireTreeNodesRemoved(node.getPathToRoot(), index,
                    Arrays.asList(oldElement));
            removeParent = null;
            removeHole = -1;
            fireTreeNodesInserted(node.getPathToRoot(), index, 1);
        }

        private void fireTreeNodesChanged(BindingNode parent, int index) {
            TreeModelEvent e;
            if (parent == root) {
                e = new TreeModelEvent(this, parent.getPathToRoot(), null, null);
            } else {
                int[] indices = new int[] { index };
                Object[] children = new Object[] { parent.getElements().get(index) };
                
                e = new TreeModelEvent(this, parent.getPathToRoot(), indices,
                        children);
            }
            for (TreeModelListener l : listeners) {
                l.treeNodesChanged(e);
            }
        }

        private void fireRootChanged() {
            fireTreeStructureChanged(null);
        }
    }

    
    private final class BindingNode extends ELPropertyResolver.Delegate
            implements ObservableListListener {
        private final BindingNode parent;
        private final Object source;
        private boolean loaded;
        private List<?> elements;
        private ELPropertyResolver resolver;
        
        private BindingNode(BindingNode parent, Object source) {
            this.parent = parent;
            this.source = source;
            // PENDING: should add PCL to track changes
        }
        
        boolean isLeaf() {
            if (elements == null) {
                return (getElementsBinding(source) == null);
            }
            return false;
        }
        
        private Object getSource() {
            return source;
        }
        
        public TreePath getPathToRoot() {
            if (parent == null) {
                return new TreePath(source);
            }
            return parent.getPathToRoot().pathByAddingChild(source);
        }
        
        public BindingNode getParent() {
            return parent;
        }
        
        private void setElements(List<?> elements) {
            if (this.elements != null) {
                if (this.elements instanceof ObservableList) {
                    ((ObservableList)this.elements).
                            removeObservableListListener(this);
                }
                disposeChildren();
            }
            this.elements = elements;
            if (elements instanceof ObservableList) {
                ((ObservableList)elements).addObservableListListener(this);
            }
            if (loaded) {
                nodeStructureChanged(this);
            }
        }
        
        public List<?> getElements() {
            if (!loaded) {
                loadElements();
            }
            return elements;
        }
        
        private void loadElements() {
            Binding binding = getElementsBinding(source);
            if (binding != null) {
                resolver = controller.createResolver();
                resolver.setSource(source);
                resolver.setPath(binding.getSourceExpression());
                resolver.bind();
                resolver.setDelegate(this);
                valueChanged(resolver);
            }
            loaded = true;
        }
        
        private void dispose() {
            nodeMap.remove(this);
            if (this.elements instanceof ObservableList) {
                ((ObservableList)this.elements).
                        removeObservableListListener(this);
            }
            this.elements = null;
            if (resolver != null) {
                resolver.unbind();
                resolver = null;
            }
        }
        
        private void disposeChildren() {
            if (elements != null) {
                for (Object elem : elements) {
                    BindingNode childNode = getBindingNode(elem);
                    if (childNode != null) {
                        childNode.disposeChildren();
                        childNode.dispose();
                    }
                }
            }
        }

        public void valueChanged(ELPropertyResolver resolver) {
            if (resolver.getEvaluationResultType() == 
                    Expression.Result.Type.SINGLE_VALUE) {
                setElements((List<?>)resolver.getValueOfLastProperty());
            }
        }

        public void listElementsAdded(ObservableList list, int index,
                int length) {
            nodeElementsAdded(this, index, length);
        }

        public void listElementsRemoved(ObservableList list, int index,
                List oldElements) {
            for (Object elem : oldElements) {
                BindingNode node = getBindingNode(elem);
                if (node != null) {
                    node.disposeChildren();
                    node.dispose();
                }
            }
            nodeElementsRemoved(this, index, oldElements);
        }

        public void listElementReplaced(ObservableList list, int index,
                Object oldElement) {
            BindingNode node = getBindingNode(oldElement);
            if (node != null) {
                node.disposeChildren();
                node.dispose();
            }
            nodeElementReplaced(this, index, oldElement);
        }

        public void listElementPropertyChanged(ObservableList list, int index) {
            nodeElementPropertyChanged(this, index);
        }
    }


    public final class PropertyDelegate extends DelegateBase {
        public void setRoot(Object root) {
            JTreeBindingHelper.this.setRoot(root);
        }
        
        public Object getRoot() {
            return root;
        }

        public void setSelectedElements(List<?> elements) {
            JTreeBindingHelper.this.setSelectedElements(elements);
        }
        
        public List<?> getSelectedElements() {
            return JTreeBindingHelper.this.getSelectedElements();
        }

        public void setSelectedElement(Object element) {
            if (element == null) {
                JTreeBindingHelper.this.setSelectedElements(
                        Collections.emptyList());
            } else {
                JTreeBindingHelper.this.setSelectedElements(
                        Arrays.asList(element));
            }
        }
        
        public Object getSelectedElement() {
            return JTreeBindingHelper.this.getSelectedElement();
        }
    }
    

    // PENDING: nuke this once BeanInfoAPT is working
    public static final class PropertyDelegateBeanInfo extends BindingBeanInfo {
        protected Class<?> getPropertyDelegateClass() {
            return PropertyDelegate.class;
        }

        protected BindingBeanInfo.Property[] getPreferredProperties() {
            return new Property[] {
                new Property("root", "The root of the tree"),
                new Property("selectedElement", "The selected element"),
                new Property("selectedElements", "The selected elements")
            };
        }
    }
}
