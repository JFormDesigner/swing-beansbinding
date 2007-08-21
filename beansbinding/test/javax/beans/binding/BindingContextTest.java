/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.FeatureDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import junit.framework.*;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.observablecollections.ObservableMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingListener;

/**
 *
 * @author sky
 */
public class BindingContextTest extends TestCase {
    private BindingContext context;
    private TestBean source;
    private TestBean target;
    
    public BindingContextTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(BindingContextTest.class);
        
        return suite;
    }

    protected void setUp() throws Exception {
        super.setUp();
        context = new BindingContext();
        source = new TestBean();
        target = new TestBean();
    }
    
    public void testGetFeatureDescriptors() throws Throwable {
        verifyDescriptors(JCheckBox.class, "selected");
        verifyDescriptors(JComboBox.class, "elements", "selectedElement", 
                "selectedElementProperty");
        verifyDescriptors(JList.class, "elements", "selectedElement", 
                "selectedElements");
        verifyDescriptors(JSlider.class, "value");
        verifyDescriptors(JTable.class, "elements", "selectedElement", 
                "selectedElements");
        verifyDescriptors(JTextField.class, "text");
        verifyDescriptors(JTree.class, "root", "selectedElement", 
                "selectedElements");
    }
    
    public void testBind1() {
        context.addBinding(source, "${value}", target, "value");
        context.bind();
        source.setValue("x");
        assertEquals("x", source.getValue());
        assertEquals("x", target.getValue());

        target.setValue("y");
        assertEquals("y", source.getValue());
        assertEquals("y", target.getValue());
    }

    public void testBindAndUnbind() {
        context.addBinding(source, "${value}", target, "value");
        context.bind();
        context.unbind();
        source.setValue("source");
        assertEquals("source", source.getValue());
        assertEquals(null, target.getValue());
        context.bind();
        source.setValue("source2");
        assertEquals("source2", source.getValue());
        assertEquals("source2", target.getValue());
    }

    public void testHasUncommited() {
        EventListenerRecorder<PropertyChangeListener> recorder =
                new EventListenerRecorder<PropertyChangeListener>(PropertyChangeListener.class);
        context.addPropertyChangeListener(recorder.getEventListenerImpl());
        Binding binding = context.addBinding(source, "${value}", target, "value");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ);
        context.bind();
        recorder.getAndClearRecords();
        target.setValue("x");
        assertPropertyChanges(recorder.getAndClearRecords(),
                "hasUncommittedValues", Boolean.TRUE,
                "hasEditedTargetValues", Boolean.TRUE);
        assertTrue(context.getHasUncommittedValues());
        assertTrue(context.getHasEditedTargetValues());

        context.clearHasEditedTargetValues();
        assertFalse(context.getHasEditedTargetValues());
        context.commitUncommittedValues();
        assertFalse(context.getHasEditedTargetValues());
        assertPropertyChanges(recorder.getAndClearRecords(),
                "hasUncommittedValues", Boolean.FALSE,
                "hasEditedTargetValues", Boolean.FALSE);
        assertEquals("x", source.getValue());
        assertEquals("x", target.getValue());
        assertFalse(context.getHasUncommittedValues());
    }

    public void testHasInvalidValues() {
        EventListenerRecorder<BindingListener> validationRecorder =
                new EventListenerRecorder<BindingListener>(BindingListener.class);
        EventListenerRecorder<PropertyChangeListener> recorder =
                new EventListenerRecorder<PropertyChangeListener>(PropertyChangeListener.class);
        context.addPropertyChangeListener(recorder.getEventListenerImpl());
        context.addBindingListener(validationRecorder.getEventListenerImpl());
        List<EventListenerRecorder.InvocationRecord> records;
        PropertyChangeEvent e;
        ValidatorImpl validator = new ValidatorImpl(ValidationResult.Action.DO_NOTHING);
        Binding binding = context.addBinding(source, "${value}", target, "value");
        binding.setValidator(validator);
        context.bind();
        recorder.getAndClearRecords();
        validationRecorder.getAndClearRecords();
        target.setValue("x");
        assertPropertyChanges(recorder.getAndClearRecords(),
                "hasInvalidValues", Boolean.TRUE,
                "hasEditedTargetValues", Boolean.TRUE);
        assertEquals(1, validationRecorder.getAndClearRecords().size());
        assertFalse(context.getHasUncommittedValues());
        assertTrue(context.getHasInvalidValues());
    }

    public void testFetchByName1() {
        BindingContext context = new BindingContext();
        Binding binding = context.addBinding("BINDING", source, "${value}", target, "value");
        Binding fetch = context.getBinding("BINDING");
        assertEquals(binding, fetch);
    }

    public void testFetchByName2() {
        BindingContext context = new BindingContext();
        Binding binding = context.addBinding("FOO", source, "${value}", target, "value");
        Binding fetch = context.getBinding("BINDING");
        assertEquals(null, fetch);
    }

    public void testFetchByName3() {
        BindingContext context = new BindingContext();
        Binding binding = context.addBinding(source, "${value}", target, "value");
        Binding fetch = context.getBinding("BINDING");
        assertEquals(null, fetch);
    }

    public void testFetchByName4() {
        BindingContext context = new BindingContext();
        Binding fetch = context.getBinding("BINDING");
        assertEquals(null, fetch);
    }

    public void testFetchByName5() {
        BindingContext context = new BindingContext();
        try {
            context.getBinding(null);
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException ise) {
        }
    }

    public void testFetchByName6() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        Binding child = bindingP.addChildBinding("CHILD", "${value}", "value");
        bindingP.removeChildBinding(child);
        Binding fetch = bindingP.getChildBinding("CHILD");
        assertEquals(null, fetch);
    }

    public void testSetName() {
        BindingContext context = new BindingContext();
        Binding binding = new Binding("BINDING", source, "{$value}", target, "value");
        context.addBinding(binding);
        assertEquals(binding, context.getBinding("BINDING"));
        binding.setName("BINDING2");
        assertEquals(null, context.getBinding("BINDING"));
        assertEquals(binding, context.getBinding("BINDING2"));
    }

    public void testSetName2() {
        BindingContext context = new BindingContext();
        Binding binding = new Binding(source, "{$value}", target, "value");
        context.addBinding(binding);
        assertEquals(null, context.getBinding("BINDING"));
        binding.setName("BINDING2");
        assertEquals(binding, context.getBinding("BINDING2"));
    }

    public void testSetName3() {
        BindingContext context = new BindingContext();
        Binding binding = new Binding("BINDING", source, "{$value}", target, "value");
        context.addBinding(binding);
        assertEquals(binding, context.getBinding("BINDING"));
        binding.setName(null);
        assertEquals(null, context.getBinding("BINDING"));
    }

    public void testSetName4() {
        BindingContext context = new BindingContext();
        Binding binding = new Binding("BINDING", source, "{$value}", target, "value");
        Binding binding2 = new Binding("BINDING2", source, "{$value}", target, "value");
        context.addBinding(binding);
        context.addBinding(binding2);
        try {
            binding2.setName("BINDING");
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException iae) {
        }
    }

    public void testGetBindings() {
        BindingContext context = new BindingContext();
        List<Binding> bindings = context.getBindings();
        assertEquals(0, context.getBindings().size());
    }

    public void testGetBindings1() {
        BindingContext context = new BindingContext();
        context.addBinding("NAME", source, "${value}", target, "value");
        assertEquals(1, context.getBindings().size());
    }

    public void testGetBindings2() {
        BindingContext context = new BindingContext();
        context.addBinding("NAME", source, "${value}", target, "value");
        context.addBinding("NAME2", source, "${value}", target, "value");
        assertEquals(2, context.getBindings().size());
    }

    public void testGetBindings3() {
        BindingContext context = new BindingContext();
        Binding one = context.addBinding("NAME", source, "${value}", target, "value");
        Binding two = context.addBinding("NAME2", source, "${value}", target, "value");
        one.bind();
        assertEquals(2, context.getBindings().size());
    }

    public void testGetBindings4() {
        BindingContext context = new BindingContext();
        Binding one = context.addBinding("NAME", source, "${value}", target, "value");
        Binding two = context.addBinding("NAME2", source, "${value}", target, "value");
        one.bind();
        two.bind();
        one.unbind();
        assertEquals(2, context.getBindings().size());
    }

    public void testGetBindings5() {
        BindingContext context = new BindingContext();
        Binding one = context.addBinding("NAME", source, "${value}", target, "value");
        Binding two = context.addBinding("NAME2", source, "${value}", target, "value");
        one.bind();
        context.bind();
        assertEquals(2, context.getBindings().size());
        context.unbind();
        assertEquals(2, context.getBindings().size());
    }
    
    private void assertPropertyChanges(
            List<EventListenerRecorder.InvocationRecord> records, 
            Object...args) {
        assertEquals(0, args.length % 2);
        assertEquals(args.length / 2, records.size());
        Map<String,Object> expectedMap = new HashMap<String,Object>();
        for (int i = 0; i < args.length; i += 2) {
            expectedMap.put((String)args[i], args[i + 1]);
        }
        for (EventListenerRecorder.InvocationRecord record : records) {
            PropertyChangeEvent event = (PropertyChangeEvent)record.
                    getArgs().get(0);
            String propertyName = event.getPropertyName();
            assertTrue(expectedMap.containsKey(propertyName));
            assertEquals(expectedMap.get(propertyName), event.getNewValue());
            expectedMap.remove(propertyName);
        }
    }

    private void verifyDescriptors(Class type, String...properties) throws 
            Throwable {
        Set<String> propSet = new HashSet<String>();
        for (String property : properties) {
            propSet.add(property);
        }
        for (FeatureDescriptor d : context.getFeatureDescriptors(type.newInstance())) {
            propSet.remove(d.getName());
        }
        assertEquals("expecting=" + Arrays.asList(properties) +
                " remaining=" + propSet, 0, propSet.size());
    }
}
