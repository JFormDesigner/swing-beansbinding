/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.swing.CellRendererPane;
import javax.swing.JTextField;
import javax.swing.binding.SwingBindingSupport;
import junit.framework.*;
import javax.beans.binding.Binding;
import javax.beans.binding.BindingContext;
import javax.beans.binding.TestBean;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 *
 * @author sky
 */
public class JTableBindingHelperTest extends TestCase {
    private JTable table;
    private List<TestBean> values;
    private BindingContext context;
    
    public JTableBindingHelperTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        table = new JTable();
        values = new ArrayList<TestBean>();
        values.add(new TestBean());
        values.add(new TestBean());
        context = new BindingContext();
    }
    
    public void testFeatureDescriptors() {
        TestUtils.verifyPreferredBindingProperty(this, table,
                "elements", "selectedElement", "selectedElements");
    }

    private int getSelectionCount() {
        return table.getSelectedRowCount();
    }
    
    private int getSelectionCount(TestBean bean) {
        List<?> selection = (List<?>)bean.getValue();
        return (selection == null) ? 0 : selection.size();
    }
    
    public void testTableColumnClass() {
        Binding binding = new Binding(
                values, null, table, "elements");
        binding.addBinding("value", null)
               .setValue(SwingBindingSupport.TableColumnClassParameter, String.class)
               .setValue(SwingBindingSupport.TableColumnParameter, 0);
        binding.bind();
        assertEquals(String.class, table.getModel().getColumnClass(0));
    }
    
    public void testSelectedElements() {
        values.get(0).setValue("xxx");
        Binding binding = new Binding(
                values, null, table, "elements");
        binding.bind();

        TestBean selectionBean = new TestBean();
        Binding selectionBinding= new Binding(
                table, "${selectedElements}", selectionBean, "value");
        selectionBinding.bind();
        assertEquals(0, getSelectionCount());
        assertEquals(0, getSelectionCount(selectionBean));
        
        table.getSelectionModel().addSelectionInterval(0, 0);
        assertEquals(1, getSelectionCount());
        assertEquals(1, getSelectionCount(selectionBean));
        
        TestBean x = new TestBean();
        Binding binding2 = new Binding(
                table, "${selectedElements.value}", x, "value");
        binding2.bind();
        assertEquals("xxx", x.getValue());
        
        table.getSelectionModel().clearSelection();
    }
    
    public void testMultiedit() {
        Binding binding = new Binding(
                values, null, table, "elements");
        Binding column0Binding = new Binding("${value}", null);
        column0Binding.setValue(SwingBindingSupport.TableColumnParameter, 0);
        binding.addBinding(column0Binding);
        context.addBinding(binding);
        context.bind();
        
        table.setSize(100, 100);
        table.validate();
        table.editCellAt(0, 0);
        JTextField editor = (JTextField)getEditor();
        editor.setText("blah");
        context.commitUncommittedValues();
        assertEquals("blah", values.get(0).getValue());
        assertTrue(column0Binding.isBound());
        
        table.editCellAt(1, 0);
        editor = (JTextField)getEditor();
        editor.setText("foo");
        context.commitUncommittedValues();
        assertEquals("foo", values.get(1).getValue());
        assertTrue(column0Binding.isBound());
        
        table.getCellEditor().stopCellEditing();
        assertFalse(column0Binding.isBound());
    }
    
    public void testColumnTitle() {
        Binding binding = new Binding(
                values, null, table, "elements");
        binding.addBinding("${value}", null).setValue(
                SwingBindingSupport.TableColumnParameter, 0);
        binding.bind();
        assertEquals("Value", table.getModel().getColumnName(0));
    }
    
    public void testCancelEdit() {
        values.get(0).setValue("foo");
        Binding binding = new Binding(
                values, null, table, "elements");
        Binding column0Binding = new Binding("${value}", null);
        column0Binding.setValue(SwingBindingSupport.TableColumnParameter, 0);
        binding.addBinding(column0Binding);
        binding.addBinding("${value}", null).setValue(
                SwingBindingSupport.TableColumnParameter, 1);
        binding.bind();
        
        assertEquals(2, table.getModel().getColumnCount());
        
        table.setSize(100, 100);
        table.validate();
        table.editCellAt(0, 0);
        JTextField editor = (JTextField)getEditor();
        editor.setText("blah");

        table.getCellEditor().cancelCellEditing();
        
        assertEquals("foo", values.get(0).getValue());
        
        assertFalse(column0Binding.isBound());
    }

    public void testSelectedElement() {
        Binding binding = new Binding(
                values, null, table, "elements");
        binding.bind();

        TestBean selectionBean = new TestBean();
        Binding selectionBinding = new Binding(
                table, "${selectedElement}", selectionBean, "value");
        selectionBinding.bind();
        assertEquals(0, getSelectionCount());
        assertEquals(null, selectionBean.getValue());
        
        table.getSelectionModel().addSelectionInterval(0, 0);
        assertEquals(1, getSelectionCount());
        assertEquals(values.get(0), selectionBean.getValue());
        
        selectionBean.setValue(values.get(1));
        assertEquals(1, getSelectionCount());
        assertEquals(1, table.getSelectedRow());
        assertEquals(values.get(1), selectionBean.getValue());
    }
    
    public void testGenericEditor2() {
        Binding binding = new Binding(
                values, null, table, "elements");
        Binding zeroBinding = new Binding("${value}", null);
        zeroBinding.setValue(SwingBindingSupport.TableColumnParameter, 0);
        binding.addBinding(zeroBinding);
        binding.addBinding("${value}", null).setValue(
                SwingBindingSupport.TableColumnParameter, 1);
        binding.bind();
        
        assertEquals(2, table.getModel().getColumnCount());
        
        table.setSize(100, 100);
        table.validate();
        table.editCellAt(0, 0);
        JTextField editor = (JTextField)getEditor();
        editor.setText("blah");
        
        table.getCellEditor().stopCellEditing();
        
        assertEquals("blah", values.get(0).getValue());

        table.editCellAt(0, 0);
        editor = (JTextField)getEditor();
        editor.setText("foo");
        table.editCellAt(0, 1);
        assertEquals("foo", values.get(0).getValue());
        
        table.getCellEditor().cancelCellEditing();
        assertEquals(null, values.get(0).getValue2());
        
        assertFalse(zeroBinding.isBound());
    }
    
    public void testGenericEditor() {
        Binding binding = new Binding(
                values, null, table, "elements");
        Binding column0Binding = new Binding("${value}", null);
        column0Binding.setValue(SwingBindingSupport.TableColumnParameter, 0);
        binding.addBinding(column0Binding);
        context.addBinding(binding);
        context.bind();
        
        table.setSize(100, 100);
        table.validate();
        table.editCellAt(0, 0);
        JTextField editor = (JTextField)getEditor();
        editor.setText("blah");
        context.commitUncommittedValues();
        assertEquals("blah", values.get(0).getValue());
        assertTrue(column0Binding.isBound());
    }
    
    public void testElements() {
        Binding binding = new Binding(
                values, null, table, "elements");
        binding.bind();
        assertEquals(2, table.getModel().getRowCount());
    }

    public void testTwo() {
        Binding binding = new Binding(
                values, null, table, "elements");
        binding.addBinding("${value}", null).setValue(
                SwingBindingSupport.TableColumnParameter, 0);
        binding.bind();
        TableModel tableModel = table.getModel();
        assertEquals(2, tableModel.getRowCount());
        assertEquals(1, tableModel.getColumnCount());
        assertEquals(null, tableModel.getValueAt(0, 0));
        assertEquals(null, tableModel.getValueAt(1, 0));
 
        values.get(0).setValue("x");
        assertEquals("x", tableModel.getValueAt(0, 0));
        assertEquals(null, tableModel.getValueAt(1, 0));
        
        values.get(1).setValue("y");
        assertEquals("x", tableModel.getValueAt(0, 0));
        assertEquals("y", tableModel.getValueAt(1, 0));
    }

    public void testThree() {
        Binding binding= new Binding(
                values, null, table, "elements");
        Binding column0Binding = new Binding("${value}", null);
        column0Binding.setValue(SwingBindingSupport.TableColumnParameter, 0);
        binding.addBinding(column0Binding);
        binding.bind();
        
        assertEquals(2, table.getModel().getRowCount());
        assertEquals(1, table.getModel().getColumnCount());
        
        table.setSize(100, 100);
        table.validate();
        table.editCellAt(0, 0);
        assertTrue(table.isEditing());
        assertTrue(column0Binding.isBound());
    }

    private Component getEditor() {
        for (Component c : table.getComponents()) {
            if (!(c instanceof CellRendererPane)) {
                return c;
            }
        }
        return null;
    }
}
