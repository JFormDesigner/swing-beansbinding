/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.EventListenerRecorder;
import javax.beans.binding.TestBean;
import com.sun.java.util.BindingCollections;
import java.awt.Component;
import org.jdesktop.beansbinding.Binding;
import javax.swing.ListCellRenderer;
import javax.swing.OLDBINDING.ParameterKeys;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import junit.framework.*;
import javax.beans.binding.BindingContext;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;

/**
 *
 * @author sky
 */
public class JListBindingHelperTest extends TestCase {
    private JList jlist;
    private List<TestBean> values;
    private BindingContext context;
    private Binding lbd;
    
    public JListBindingHelperTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(JListBindingHelperTest.class);
        
        return suite;
    }

    protected void setUp() throws Exception {
        super.setUp();
        jlist = new JList();
        context = new BindingContext();
        values = new ArrayList<TestBean>();
        values.add(new TestBean());
        values.add(new TestBean());
        lbd = new Binding(values, null, jlist, "elements");
    }
    
    public void testFeatureDescriptors() {
        TestUtils.verifyPreferredBindingProperty(this, new JList(),
                "elements", "selectedElement", "selectedElements");
    }

    public void testModelSize() {
        context.addBinding(lbd);
        context.bind();
        assertEquals(2, jlist.getModel().getSize());
    }
    
    public void testModelElement() {
        context.addBinding(lbd);
        context.bind();
        for (int i = 0; i < values.size(); i++) {
            assertEquals(values.get(i), jlist.getModel().getElementAt(i));
        }
    }
    
    public void testModelValueBinding() {
        lbd.addChildBinding(new Binding("${value}", null));
        context.addBinding(lbd);
        values.get(0).setValue("x");
        values.get(1).setValue("y");
        context.bind();
        assertEquals("x", values.get(0).getValue());
        assertEquals("y", values.get(1).getValue());
        assertEquals("x", jlist.getModel().getElementAt(0));
        assertEquals("y", jlist.getModel().getElementAt(1));
    }
    
    public void testElements() {
        lbd.bind();
        assertEquals(values.get(0), jlist.getModel().getElementAt(0));
        assertEquals(values.get(1), jlist.getModel().getElementAt(1));
    }
    
    public void testObservableList() {
        values = BindingCollections.observableList(values);
        lbd = new Binding(values, null, jlist, "elements");
        lbd.addChildBinding(new Binding("${value}", null));
        context.addBinding(lbd);
        context.bind();
        EventListenerRecorder<ListDataListener> r = new 
                EventListenerRecorder<ListDataListener>(ListDataListener.class);
        jlist.getModel().addListDataListener(r.getEventListenerImpl());

        values.add(new TestBean());
        List<EventListenerRecorder.InvocationRecord> records = r.getAndClearRecords();
        assertEquals(1, records.size());
        assertEquals("intervalAdded", records.get(0).getMethodName());
        ListDataEvent e = (ListDataEvent)records.get(0).getArgs().get(0);
        assertEquals(2, e.getIndex0());
        assertEquals(2, e.getIndex1());
        
        values.remove(0);
        records = r.getAndClearRecords();
        assertEquals(1, records.size());
        assertEquals("intervalRemoved", records.get(0).getMethodName());
        e = (ListDataEvent)records.get(0).getArgs().get(0);
        assertEquals(0, e.getIndex0());
        assertEquals(0, e.getIndex1());

        jlist.getModel().getElementAt(0);
        values.get(0).setValue("xxx");
        assertEquals("xxx", jlist.getModel().getElementAt(0));
        records = r.getAndClearRecords();
        assertEquals(1, records.size());
        assertEquals("contentsChanged", records.get(0).getMethodName());
        e = (ListDataEvent)records.get(0).getArgs().get(0);
        assertEquals(0, e.getIndex0());
        assertEquals(0, e.getIndex1());
    }

    // PENDING: need renderer target to get this working.
//    public void testRendererBinding() {
//        MyRenderer r = new MyRenderer();
//        jlist.setCellRenderer(r);
//        lbd.addBinding(new Binding("value2", "renderer.value"));
//        context.addBinding(lbd);
//        context.bind();
//        
//        values.get(0).setValue2("0");
//        values.get(1).setValue2("1");
//        
//        jlist.getCellRenderer().getListCellRendererComponent(
//                jlist, null, 0, true, true);
//        assertEquals("0", r.getValue());
//
//        jlist.getCellRenderer().getListCellRendererComponent(
//                jlist, null, 1, true, true);
//        assertEquals("1", r.getValue());
//    }
    
    
    public static class MyRenderer implements ListCellRenderer {
        private Object value;

        public void setValue(Object value) {
            this.value = value;
        }
        public Object getValue() {
            return value;
        }
        
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            return null;
        }
    }
}
