/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.BindingContext;
import javax.beans.binding.TestBean;
import javax.beans.binding.ValidationResult;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.OLDBINDING.ParameterKeys;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import junit.framework.*;
import org.jdesktop.beans.binding.Binding;
import javax.swing.JTextField;
import javax.swing.binding.TextChangeStrategy;

/**
 *
 * @author sky
 */
public class JTextComponentBindingHelperTest extends TestCase {
    private BindingContext context;
    private JTextField textField;
    private Binding binding;

    private TestBean source;
    
    public JTextComponentBindingHelperTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        context = new BindingContext();
        textField = new JTextField();
        source = new TestBean();
        binding = new Binding(
                source, "${value}", textField, "text");
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(JTextComponentBindingHelperTest.class);
        
        return suite;
    }
    
    public void testFeatureDescriptors() {
        TestUtils.verifyPreferredBindingProperty(this, textField,
                "text");
    }

    // PENDING: test for disable on incomplete path
    
    public void testChangeOnType() throws Throwable {
        binding.putParameter(ParameterKeys.TEXT_CHANGE_STRATEGY,
                             TextChangeStrategy.ON_TYPE);
        context.addBinding(binding);
        context.bind();
        assertEquals("", textField.getText());
        assertEquals(null, source.getValue());
        
        source.setValue("xxx");
        assertEquals("xxx", textField.getText());
        assertEquals("xxx", source.getValue());
        
        textField.getDocument().insertString(0, "blah", null);
        assertEquals("blahxxx", textField.getText());
        assertEquals("blahxxx", source.getValue());
        
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
    }
    
    public void testChangeOnActionOrFocusLost() throws Throwable {
        binding.putParameter(ParameterKeys.TEXT_CHANGE_STRATEGY,
                             TextChangeStrategy.ON_ACTION_OR_FOCUS_LOST);
        context.addBinding(binding);
        context.bind();
        assertEquals("", textField.getText());
        assertEquals(null, source.getValue());
        
        source.setValue("xxx");
        assertEquals("xxx", textField.getText());
        assertEquals("xxx", source.getValue());
        
        textField.getDocument().insertString(0, "blah", null);
        assertEquals("blahxxx", textField.getText());
        assertEquals("xxx", source.getValue());

        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getTargetValueState());

        for (ActionListener listener : textField.getActionListeners()) {
            listener.actionPerformed(new ActionEvent(textField, 0, null));
        }
        assertEquals("blahxxx", textField.getText());
        assertEquals("blahxxx", source.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
    }
    
    public void testChangeOnFocusLost() throws Throwable {
        binding.putParameter(ParameterKeys.TEXT_CHANGE_STRATEGY,
                             TextChangeStrategy.ON_FOCUS_LOST);
        context.addBinding(binding);
        context.bind();
        assertEquals("", textField.getText());
        assertEquals(null, source.getValue());
        
        source.setValue("xxx");
        assertEquals("xxx", textField.getText());
        assertEquals("xxx", source.getValue());
        
        textField.getDocument().insertString(0, "blah", null);
        assertEquals("blahxxx", textField.getText());
        assertEquals("xxx", source.getValue());

        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getTargetValueState());

        for (FocusListener listener : textField.getFocusListeners()) {
            listener.focusLost(new FocusEvent(textField, FocusEvent.FOCUS_LOST));
        }
        assertEquals("blahxxx", textField.getText());
        assertEquals("blahxxx", source.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
    }
    
    public void testRevert() throws Exception {
        ConfigurableBindingValidator validator = new ConfigurableBindingValidator();
        binding.setValidator(validator);
        context.addBinding(binding);
        context.bind();
        
        validator.setType(ValidationResult.Action.SET_TARGET_FROM_SOURCE);
        textField.setText("foo");
        binding.setSourceValueFromTargetValue();
        assertEquals("", textField.getText());
    }
    
    public void testDocumentListener() throws Exception {
        DocumentFilter filter = new DocumentFilter() {};
        ((AbstractDocument)textField.getDocument()).setDocumentFilter(filter);
        
        context.addBinding(binding);
        context.bind();
        
        assertEquals(filter, ((AbstractDocument)textField.getDocument()).
                getDocumentFilter());
        
        textField.setText("blah");
        context.commitUncommittedValues();
        assertEquals("blah", source.getValue());
        assertEquals("blah", textField.getText());
        
        source.setValue("foo");
        assertEquals("foo", source.getValue());
        assertEquals("foo", textField.getText());
    }
    
    public void testIncompletePath() throws Exception {
        binding = context.addBinding(
                source, "${value.value}", textField, "text");
        
        context.bind();

        assertFalse(textField.isEnabled());
        textField.setText("blah");
        
        TestBean source2 = new TestBean();
        source.setValue(source2);
        assertTrue(textField.isEnabled());
        assertEquals("", textField.getText());
        
        source.setValue(null);
        assertFalse(textField.isEnabled());
    }
}
