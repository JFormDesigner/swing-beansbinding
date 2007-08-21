/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import org.jdesktop.beansbinding.Binding;
import javax.beans.binding.BindingContext;
import javax.beans.binding.EventListenerRecorder;
import javax.beans.binding.TestBean;
import com.sun.java.util.ObservableCollections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.OLDBINDING.ParameterKeys;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;
import junit.framework.*;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreePath;

/**
 *
 * @author sky
 */
public class JTreeBindingHelperTest extends TestCase {
    private EventListenerRecorder<TreeModelListener> recorder;
    private JTree tree;
    private BindingContext context;
    private TestBean root;
    private Binding lbd;
    private TreeSelectionModel selectionModel;
    
    public JTreeBindingHelperTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        context = new BindingContext();
        root = new TestBean();
        tree = new JTree();
        selectionModel = tree.getSelectionModel();
        lbd = new Binding(root, null, tree, "root");
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(JTreeBindingHelperTest.class);
        return suite;
    }
    
    // PENDING: be sure and write a test to make sure root changes are handled
    // ok.
    
    public void testFeatureDescriptors() {
        TestUtils.verifyPreferredBindingProperty(this, tree,
                "root", "selectedElement", "selectedElements");
    }

    private void assertRecordEquals(String methodName, TreePath path, Object...args) {
        List<EventListenerRecorder.InvocationRecord> records = 
                recorder.getAndClearRecords();
        assertEquals(1, records.size());
        assertEquals(methodName, records.get(0).getMethodName());
        TreeModelEvent event = (TreeModelEvent)records.get(0).getArgs().get(0);
        assertEquals(path, new TreePath(event.getPath()));
        if (args.length > 0) {
            int[] indices = (int[])args[0];
            int[] eIndices = (int[])event.getChildIndices();
            assertEquals(indices.length, eIndices.length);
            for (int i = 0; i < indices.length; i++) {
                assertEquals(indices[i], eIndices[i]);
            }
            if (args.length > 1) {
                assertEquals(Arrays.asList((Object[])args[1]),
                        Arrays.asList(event.getChildren()));
            }
        }
    }
    
    private void createRecorder() {
        recorder = new EventListenerRecorder<TreeModelListener>(
                TreeModelListener.class);
        tree.getModel().addTreeModelListener(recorder.getEventListenerImpl());
    }
    
    public void testSelection() {
        lbd.addChildBinding("${value}", null).putParameter(ParameterKeys.TREE_NODE_CLASS, Object.class);
        context.addBinding(lbd);
        
        TestBean selectionBean = new TestBean();
        context.addBinding(
                tree, "${selectedElement}", selectionBean, "value");
        context.addBinding(
                tree, "${selectedElements}", selectionBean, "value2");
        context.bind();
 
        List<TestBean> rootChildren = new ArrayList<TestBean>(1);
        rootChildren = ObservableCollections.observableList(rootChildren);
        rootChildren.add(new TestBean());
        rootChildren.add(new TestBean());
        root.setValue(rootChildren);
        
        tree.expandPath(new TreePath(root));
        tree.setSelectionPath(new TreePath(root));
        assertEquals(selectionBean.getValue(), root);
        assertEquals(selectionBean.getValue2(), Arrays.asList(root));
        
        selectionBean.setValue(rootChildren.get(0));
        assertEquals(1, tree.getSelectionCount());
        assertEquals(tree.getSelectionPath(),
                new TreePath(root).pathByAddingChild(rootChildren.get(0)));
        assertEquals(Arrays.asList(rootChildren.get(0)),
                selectionBean.getValue2());
        
        selectionBean.setValue2(rootChildren);
        assertEquals(2, tree.getSelectionCount());
        TreePath[] selection = tree.getSelectionPaths();
        assertEquals(selection[0],
                new TreePath(root).pathByAddingChild(rootChildren.get(0)));
        assertEquals(selection[1],
                new TreePath(root).pathByAddingChild(rootChildren.get(1)));
        assertEquals(rootChildren.get(0), selectionBean.getValue());
    }
    
    public void testEmptyNode() {
        lbd.putParameter(ParameterKeys.EMPTY_NODE_TREATED_AS_LEAF, true);
        lbd.addChildBinding("${value}", null).putParameter(ParameterKeys.TREE_NODE_CLASS, Object.class);
        lbd.bind();
        TreeModel tm = tree.getModel();
        assertTrue(tm.isLeaf(root));
    }
    
    public void testNotification() {
        lbd.addChildBinding("${value}", null).putParameter(ParameterKeys.TREE_NODE_CLASS, Object.class);
        context.addBinding(lbd);
        context.bind();
        TreeModel tm = tree.getModel();
        createRecorder();
        assertEquals(root, tm.getRoot());
        assertEquals(0, tm.getChildCount(tm.getRoot()));
        
        List<TestBean> rootChildren = new ArrayList<TestBean>(1);
        rootChildren = ObservableCollections.observableList(rootChildren);
        root.setValue(rootChildren);
        assertRecordEquals("treeStructureChanged", new TreePath(root));

        rootChildren.add(new TestBean());
        assertRecordEquals("treeNodesInserted", new TreePath(root),
                new int[] { 0 }, new Object[] { rootChildren.get(0) });
        assertEquals(1, tm.getChildCount(tm.getRoot()));
        assertEquals(rootChildren.get(0), tm.getChild(tm.getRoot(), 0));
        assertFalse(tm.isLeaf(rootChildren.get(0)));
        assertEquals(0, tm.getChildCount(rootChildren.get(0)));

        List<TestBean> subChildren = new ArrayList<TestBean>(1);
        subChildren = ObservableCollections.observableList(subChildren);
        rootChildren.get(0).setValue(subChildren);
        assertRecordEquals("treeStructureChanged",
                new TreePath(root).pathByAddingChild(rootChildren.get(0)));
        
        subChildren.add(new TestBean());
        assertEquals(1, tm.getChildCount(rootChildren.get(0)));
        assertEquals(subChildren.get(0), tm.getChild(rootChildren.get(0), 0));
        assertRecordEquals("treeNodesInserted",
                new TreePath(root).pathByAddingChild(rootChildren.get(0)),
                new int[] { 0 }, new Object[] { subChildren.get(0) } );
        
        Object old = subChildren.remove(0);
        assertRecordEquals("treeNodesRemoved",
                new TreePath(root).pathByAddingChild(rootChildren.get(0)),
                new int[] { 0 }, new Object[] { old } );
    }

    public void testRootChildren() {
        lbd.addChildBinding("${value}", null).putParameter(ParameterKeys.TREE_NODE_CLASS, Object.class);
        context.addBinding(lbd);
        context.bind();
        TreeModel tm = tree.getModel();
        assertEquals(root, tm.getRoot());
        assertEquals(0, tm.getChildCount(tm.getRoot()));
        
        List<TestBean> rootChildren = new ArrayList<TestBean>(1);
        rootChildren = ObservableCollections.observableList(rootChildren);
        root.setValue(rootChildren);
        rootChildren.add(new TestBean());
        assertEquals(1, tm.getChildCount(tm.getRoot()));
        assertEquals(rootChildren.get(0), tm.getChild(tm.getRoot(), 0));
        assertFalse(tm.isLeaf(rootChildren.get(0)));

        List<TestBean> subChildren = new ArrayList<TestBean>(1);
        subChildren = ObservableCollections.observableList(subChildren);
        rootChildren.get(0).setValue(subChildren);
        subChildren.add(new TestBean());
        assertEquals(1, tm.getChildCount(rootChildren.get(0)));
        assertEquals(subChildren.get(0), tm.getChild(rootChildren.get(0), 0));
    }
    
    public void testRoot() {
        lbd.bind();
        assertEquals(root, tree.getModel().getRoot());
    }
}
