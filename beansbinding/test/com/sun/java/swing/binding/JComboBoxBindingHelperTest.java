/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import com.sun.java.util.ObservableCollections;
import java.util.ArrayList;
import javax.beans.binding.TestBean;
import javax.swing.OLDBINDING.ParameterKeys;
import junit.framework.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jdesktop.beansbinding.Binding;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author sky
 */
public class JComboBoxBindingHelperTest extends TestCase {
    private List<TestBean> elements;
    private JComboBox cb;
    
    public JComboBoxBindingHelperTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        elements = ObservableCollections.observableList(new ArrayList<TestBean>(3));
        elements.add(new TestBean());
        elements.add(new TestBean());
        elements.add(new TestBean());
        cb = new JComboBox();
    }

    protected void tearDown() throws Exception {
    }
    
    public void testFeatureDescriptors() {
        TestUtils.verifyPreferredBindingProperty(this, new JComboBox(),
                "elements", "selectedElement");
    }
    
    public void testSetSelectedElementProperty2() {
        elements.get(0).setValue("0");
        elements.get(1).setValue("1");
        elements.get(2).setValue("2");
        Binding binding = new Binding(elements, null, cb, "elements");
        binding.putParameter(ParameterKeys.COMBOBOX_SELECTED_OBJECT_PROPERTY, "${value}");
        binding.addChildBinding("${value}", null);
        binding.bind();

        TestBean selectionBean = new TestBean();
        Binding selectedObjectBinding = new Binding(
                selectionBean, "${value}", cb, "selectedElementProperty");
        selectedObjectBinding.bind();
        selectionBean.setValue(elements.get(1).getValue());
        assertEquals(elements.get(1).getValue(), cb.getSelectedItem());
        assertEquals(1, cb.getSelectedIndex());

        cb.setSelectedIndex(2);
        assertEquals(elements.get(2).getValue(), selectionBean.getValue());
        assertEquals(elements.get(2).getValue(), cb.getSelectedItem());
        assertEquals(2, cb.getSelectedIndex());
        
        cb.setSelectedItem(elements.get(1).getValue());
        assertEquals(elements.get(1).getValue(), selectionBean.getValue());
        assertEquals(elements.get(1).getValue(), cb.getSelectedItem());
        assertEquals(1, cb.getSelectedIndex());
    }
    
    public void testSetSelectedElementProperty() {
        elements.get(0).setValue("0");
        elements.get(1).setValue("1");
        elements.get(2).setValue("2");
        Binding binding = new Binding(elements, null, cb, "elements");
        binding.putParameter(ParameterKeys.COMBOBOX_SELECTED_OBJECT_PROPERTY, "${value}");
        binding.bind();

        TestBean selectionBean = new TestBean();
        Binding selectedObjectBinding = new Binding(
                selectionBean, "${value}", cb, "selectedElementProperty");
        selectedObjectBinding.bind();
        selectionBean.setValue(elements.get(1).getValue());
        assertEquals(elements.get(1), cb.getSelectedItem());
        assertEquals(1, cb.getSelectedIndex());

        cb.setSelectedIndex(2);
        assertEquals(elements.get(2).getValue(), selectionBean.getValue());
        assertEquals(elements.get(2), cb.getSelectedItem());
        assertEquals(2, cb.getSelectedIndex());
        
        cb.setSelectedItem(elements.get(1));
        assertEquals(elements.get(1).getValue(), selectionBean.getValue());
        assertEquals(elements.get(1), cb.getSelectedItem());
        assertEquals(1, cb.getSelectedIndex());
    }
    
    public void testSelectedElement2() {
        elements.get(0).setValue("0");
        elements.get(1).setValue("1");
        elements.get(2).setValue("2");
        Binding binding = new Binding(elements, null, cb, "elements");
        binding.addChildBinding("${value}", null);
        binding.bind();
        
        TestBean selectionBean = new TestBean();
        Binding selectedObjectBinding = new Binding(
                selectionBean, "${value}", cb, "selectedElement");
        selectedObjectBinding.bind();
        selectionBean.setValue(elements.get(1));
        assertEquals(elements.get(1).getValue(), cb.getSelectedItem());
        assertEquals(1, cb.getSelectedIndex());
        
        cb.setSelectedIndex(2);
        assertEquals(elements.get(2).getValue(), cb.getSelectedItem());
        assertEquals(elements.get(2), selectionBean.getValue());
        
        cb.setSelectedItem(elements.get(0).getValue());
        assertEquals(elements.get(0).getValue(), cb.getSelectedItem());
        assertEquals(elements.get(0), selectionBean.getValue());
    }

    public void testSelectedElement() {
        Binding binding = new Binding(elements, null, cb, "elements");
        binding.bind();
        
        TestBean selectionBean = new TestBean();
        Binding selectedObjectBinding = new Binding(
                selectionBean, "${value}", cb, "selectedElement");
        selectedObjectBinding.bind();
        selectionBean.setValue(elements.get(1));
        assertEquals(elements.get(1), cb.getSelectedItem());
        assertEquals(1, cb.getSelectedIndex());
        
        cb.setSelectedIndex(2);
        assertEquals(elements.get(2), cb.getSelectedItem());
        assertEquals(elements.get(2), selectionBean.getValue());
        
        cb.setSelectedItem(elements.get(0));
        assertEquals(elements.get(0), cb.getSelectedItem());
        assertEquals(elements.get(0), selectionBean.getValue());
    }

    public void testElements() {
        Binding binding = new Binding(elements, null, cb, "elements");
        binding.bind();
        assertEquals(3, cb.getModel().getSize());
        for (int i = 0; i < elements.size(); i++) {
            assertEquals(elements.get(i), cb.getModel().getElementAt(i));
        }
    }

    public void testElementsAndValue() {
        elements.get(0).setValue("0");
        elements.get(1).setValue("1");
        elements.get(2).setValue("2");
        Binding binding = new Binding(elements, null, cb, "elements");
        binding.addChildBinding("${value}", null);
        binding.bind();
        for (int i = 0; i < elements.size(); i++) {
            assertEquals(elements.get(i).getValue(), cb.getModel().getElementAt(i));
        }
    }
}
