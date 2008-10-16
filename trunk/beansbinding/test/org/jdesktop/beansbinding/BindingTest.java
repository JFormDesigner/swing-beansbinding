package org.jdesktop.beansbinding;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import junit.framework.TestCase;

/*
 * BindingTests.java
 *
 * Created on October 25, 2007, 2:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

//TOTEST:
// test: all failure modes (UNREADABLE, UNWRITABLE, etc)
// test: When/from where the unreadableSourceValue should be returned
// test: bogus property/binding

/**
 *
 * @author bchristi
 */
public class BindingTest extends TestCase {
    
    /** Creates a new instance of BindingTests */
    public BindingTest() {
    }
    
    /*
     * It would be nice if calling println() on an unbound Binding did not
     * throw an exception.
     */
    public void testPrintln() {
        Object source = new Object();
        Property classProp = BeanProperty.create("class");
        Object target = new Object();
        
        Binding binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_ONCE, source, classProp, target, classProp);
        System.out.println("Testing toString() (unbound):\n" + binding);
        binding.bind();
        System.out.println("Testing toString() (bound):\n" + binding);
    }
    
    private class SourceUnreadableListener implements PropertyChangeListener {
        boolean valueSetBecameTrue;
        boolean valueSetBecameFalse;
        Object oldValue;
        Object newValue;
        
        public SourceUnreadableListener() {
            init();
        }
        
        /*
         * Reset all internal state back to initial values
         */
        public void init() {
            valueSetBecameTrue = false;
            valueSetBecameFalse = false;
            oldValue = null;
            newValue = null;
        }
        
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals("sourceUnreadableValueSet")) {
                if (e.getOldValue().equals(Boolean.FALSE) &&
                        e.getNewValue().equals(Boolean.TRUE)) {
                    valueSetBecameTrue = true;
                } else if (e.getOldValue().equals(Boolean.TRUE) &&
                        e.getNewValue().equals(Boolean.FALSE)) {
                    valueSetBecameFalse = true;
                }
            } else if (e.getPropertyName().equals("sourceUnreadableValue")) {
                oldValue = e.getOldValue();
                newValue = e.getNewValue();
            }
        }
    }
    
    
    
    /*
     * Test Binding.isSourceUnreadableValueSet()
     */
    public void testIsSourceUnreadableValueSet() {
        Binding binding = createTestBinding();
        
        // By default, should return false
        assertFalse(binding.isSourceUnreadableValueSet());
        
        // If set, should return true
        binding.setSourceUnreadableValue(new Object());
        assertTrue(binding.isSourceUnreadableValueSet());
        
        // If unset, should return false
        binding.unsetSourceUnreadableValue();
        assertFalse(binding.isSourceUnreadableValueSet());
        
        // Should be callable on unbound, bound properties
        // (i.e. not throw an exception)
        binding.bind();
        boolean b = binding.isSourceUnreadableValueSet();
    }
    
    /*
     * Test Binding.unsetSourceUnreadableValue()
     */
    public void testUnsetSourceUnreadableValue() {
        Binding binding = createTestBinding();
        SourceUnreadableListener pcl = new SourceUnreadableListener();
        binding.addPropertyChangeListener(pcl);
        
        // By default, is unset.  Calling unset should still work fine,
        // but fire no props.
        binding.unsetSourceUnreadableValue();
        assertFalse(binding.isSourceUnreadableValueSet());
        assertFalse(pcl.valueSetBecameFalse);
        assertFalse(pcl.valueSetBecameTrue);
        
        binding.setSourceUnreadableValue(new Object());
        pcl.init();
        // If set, should return to being unset
        // and fire prop(s):
        //     unreadableSourceValueSet
        //     unreadableSourceValue -> null
        binding.unsetSourceUnreadableValue();
        assertFalse(binding.isSourceUnreadableValueSet());
        assertTrue(pcl.valueSetBecameFalse);
        assertTrue(pcl.newValue == null);
        
        // If called on bound binding, should throw exception, not unset
        binding.setSourceUnreadableValue(new Object());
        binding.bind();
        
        boolean caught = false;
        try {
            binding.unsetSourceUnreadableValue();
        } catch (IllegalStateException e) {
            caught = true;
        }
        assertTrue(caught);
    }
    
   /*
    * Test Binding.getSourceUnreadableValue()
    */
    public void testgetSourceUnreadableValue() {
        // If called with value unset, should throw exception
        Binding binding = createTestBinding();
        
        boolean caught = false;
        try {
            Object o = binding.getSourceUnreadableValue();
        } catch (UnsupportedOperationException e) {
            caught = true;
        }
        assertTrue(caught);
    }
    
   /*
    * Test Binding.setSourceUnreadableValue()
    */
    public void testSetSourceUnreadableValue() {
        // TEST: Set value.  isSourceUnreadableValueSet() should become true and
        // event(s) should be fired.
        Binding binding = createTestBinding();
        SourceUnreadableListener pcl = new SourceUnreadableListener();
        binding.addPropertyChangeListener(pcl);
        
        // Null should work
        binding.setSourceUnreadableValue(null);
        
        assertTrue(binding.isSourceUnreadableValueSet());
        assertTrue(binding.getSourceUnreadableValue() == null);
        assertTrue(pcl.valueSetBecameTrue);
        assertTrue(pcl.newValue == null);
        
        pcl.init();
        Object value = new Object();
        binding.setSourceUnreadableValue(value);
        assertTrue(binding.isSourceUnreadableValueSet());
        assertTrue(binding.getSourceUnreadableValue() == value);
        // This property wouldn't have changed
        assertFalse(pcl.valueSetBecameTrue);
        assertTrue(pcl.newValue == value);
        
        
        // if called on bound binding, throw exc, not set
        binding.unsetSourceUnreadableValue();
        binding.bind();
        boolean caught = false;
        try {
            binding.setSourceUnreadableValue(new Object());
        } catch (IllegalStateException e) {
            caught = true;
        }
        assertTrue(caught);
        assertFalse(binding.isSourceUnreadableValueSet());
    }
    
    /*
     * Variables for getSourceValueForTarget()/getTargetValueForSource() tests
     */
    
    private static final String PROP_NAME = "someProperty";
    private static final String OBJ_PROP_NAME = "objProperty";
    private static final String MYSTERY_PROP_NAME = "mystery";
    
    private static final String PROP_VALUE = "PropertyValue";
    private static final String PROP_NEW_VALUE = "NewPropertyValue";
    
    private static final Property stringProperty = BeanProperty.create(PROP_NAME);
    private static final Property bogusProperty = BeanProperty.create("bogus!");
    private static final Property objectProperty = BeanProperty.create(OBJ_PROP_NAME);
    private static final Property mysteryProperty = BeanProperty.create(MYSTERY_PROP_NAME);
    
    /*
     * How do we convert to this class?  It's a mystery!
     */
    private static class Mystery {}
    
    /*
     * Test Binding.getSourceValueForTarget()
     *
     * Note: It shouldn't matter whether the Binding is bound or not.  For
     * simplicity, this test case tests unbound Bindings.
     */
    public void testGetSourceValueForTarget() {
        TestBean source, target;
        Binding binding;
        Object retVal;
        Binding.ValueResult valRes;
        Object unreadableValue = "Unreadable!";
        
        // TEST: First check for SyncFailureType.TARGET_UNWRITEABLE
        source = new TestBean();
        target = new TestBean();
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, bogusProperty);
        retVal = binding.getSourceValueForTarget();
        valRes = (Binding.ValueResult)retVal;
        assertTrue(valRes.failed());
        assertTrue(valRes.getFailure().getType().equals(Binding.SyncFailureType.TARGET_UNWRITEABLE));
        
        // TEST: Then check isSourceUnreadableValueSet() is true, return
        //       the unreadableValue
//        source = new TestBean();
//        target = new TestBean();
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, bogusProperty,
                target, stringProperty);
        binding.setSourceUnreadableValue(unreadableValue);
        assertTrue(binding.isSourceUnreadableValueSet());
        
        retVal = binding.getSourceValueForTarget();
        valRes = (Binding.ValueResult)retVal;
        Object valResVal = valRes.getValue();
        assertTrue(valResVal == unreadableValue);
        
        // TEST: Then check if isSourceUnreadableValueSet() is false, return
        //       SyncFailureType.SOURCE_UNREADABLE, as needed.
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, bogusProperty,
                target, stringProperty);
        assertFalse(binding.isSourceUnreadableValueSet());
        
        retVal = binding.getSourceValueForTarget();
        valRes = (Binding.ValueResult)retVal;
        assertTrue(valRes.failed());
        assertTrue(valRes.getFailure().getType().equals(Binding.SyncFailureType.SOURCE_UNREADABLE));
        
        // TEST: Then check if source value is null, return value from
        //       getSourceNullValue().
        source = new TestBean();
        source.setSomeProperty(null);
        target = new TestBean();
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, stringProperty);
        
        // By default, should return null
        retVal = binding.getSourceValueForTarget();
        valRes = (Binding.ValueResult)retVal;
        assertTrue(valRes.getValue() == null);
        
        // If nullValue is set, should return that value
        Object sourceNullValue = new Object();
        binding.setSourceNullValue(sourceNullValue);
        retVal = binding.getSourceValueForTarget();
        valRes = (Binding.ValueResult)retVal;
        assertTrue(valRes.getValue() == sourceNullValue);
        
        // TEST: If converter is set, must be used
        source = new TestBean();
        target = new TestBean();
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, stringProperty);
        ObjStringConverter converter = new ObjStringConverter();
        binding.setConverter(converter);
        
        // TEST: RuntimeExceptions from the Converter should be passed up
        boolean caught = false;
        converter.shouldThrow = true;
        try {
            retVal = binding.getSourceValueForTarget();
        } catch(RuntimeException e) {
            caught = true;
        }
        assertTrue(caught);
        
        // TEST: ClassCastExceptions from the Converter should be passed up
        caught = false;
        converter.init();
        converter.shouldThrowCCE = true;
        try {
            retVal = binding.getSourceValueForTarget();
        } catch(ClassCastException e) {
            caught = true;
        }
        assertTrue(caught);
        
        // TEST: Converter must have been called, and the returned value should
        //       be the one from the converter.
        converter.init();
        retVal = binding.getSourceValueForTarget();
        valRes = (Binding.ValueResult)retVal;
        assertFalse(valRes.failed());
        assertTrue(converter.forwardCalled);
        assertTrue(ObjStringConverter.wasConverted(valRes.getValue()));
        
        // TEST: If no Converter provided, default converter tries to convert.
        //       If successful, returned value will be of same type as the
        //       write-type for the target.
        source = new TestBean();
        // Default converter should be able to convert an Integer to a String
        source.setObjProperty(new Integer(42));
        target = new TestBean();
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, objectProperty,
                target, stringProperty);
        
        retVal = binding.getSourceValueForTarget();
        valRes = (Binding.ValueResult)retVal;
        assertFalse(valRes.failed());
        assertTrue(valRes.getValue().getClass().equals(stringProperty.getWriteType(target)));
        
        // TEST: If no Converter provided, default converter tries to convert.
        //       If unsuccessful, should throw ClassCastException.
        source = new TestBean();
        target = new TestBean();
        
        // Won't know how to convert a String to a Mystery - it's a Mystery!
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, mysteryProperty);
        
        caught = false;
        try {
            retVal = binding.getSourceValueForTarget();
        } catch(ClassCastException e) {
            caught = true;
        }
        assertTrue(caught);
    }
    
    /*
     * Test Binding.getTargetValueForSource()
     *
     * Note: It shouldn't matter whether the Binding is bound or not.  For
     * simplicity, this test case tests unbound Bindings.
     */
    public void testGetTargetValueForSource() {
        TestBean source, target;
        Binding binding;
        Object retVal;
        Binding.ValueResult valRes;
        
        // TEST: If source not writeable, return a ValueResult with failure type
        //       SyncFailureType.SOURCE_UNWRITEABLE
        source = new TestBean();
        target = new TestBean();
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, bogusProperty,
                target, stringProperty);
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertTrue(valRes.failed());
        assertTrue(valRes.getFailure().getType().equals(Binding.SyncFailureType.SOURCE_UNWRITEABLE));
        
        // TEST: If target not readable, return a ValueResult with failure type
        //       SyncFailureType.TARGET_UNREADABLE
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, bogusProperty);
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertTrue(valRes.failed());
        assertTrue(valRes.getFailure().getType().equals(Binding.SyncFailureType.TARGET_UNREADABLE));
        
        // TEST: Then check if target value is null, return value from
        //       getTargetNullValue().
        source = new TestBean();
        target = new TestBean();
        target.setSomeProperty(null);
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, stringProperty);
        
        // By default, getTargetNullValue() should return null
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertFalse(valRes.failed());
        assertTrue(valRes.getValue() == null);
        
        // If nullValue is set, should return that value
        Object targetNullValue = new Object();
        binding.setTargetNullValue(targetNullValue);
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertFalse(valRes.failed());
        assertTrue(valRes.getValue() == targetNullValue);
        
        // TEST: If value is non-null and converter is set, convertReverse()
        //       is called.  ValueResult is value from the Converter.
        source = new TestBean();
        target = new TestBean();
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, stringProperty);
        
        ObjStringConverter converter = new ObjStringConverter();
        binding.setConverter(converter);
        
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertFalse(valRes.failed());
        assertTrue(converter.reverseCalled);
        assertTrue(ObjStringConverter.wasConverted(valRes.getValue()));
        
        // TEST: RuntimeExceptions from the Converter should be converted to conversionFailure
        converter.init();
        converter.shouldThrow = true;
        
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertTrue(valRes.failed());
        assertTrue(valRes.getFailure().getType().equals(Binding.SyncFailureType.CONVERSION_FAILED));
        
        // TEST: ClassCastExceptions from the Converter should be thrown
        boolean caught = false;
        converter.init();
        converter.shouldThrowCCE = true;
        try {
            retVal = binding.getTargetValueForSource();
        } catch(ClassCastException e) {
            caught = true;
        }
        assertTrue(caught);
        
        // TEST: If no Converter provided, will use default converter to try
        //       to convert.
        //       If successful, returned value will be of same type as the
        //       write-type for the source.
        source = new TestBean();
        target = new TestBean();
        // Default converter should be able to convert an Integer to a String
        target.setObjProperty(new Integer(42));
        
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, objectProperty);
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertFalse(valRes.failed());
        assertTrue(valRes.getValue().getClass().equals(stringProperty.getWriteType(source)));
        
        // TEST: If no Converter provided, will use default converter to try
        //       to convert.
        //       If unsuccessful, should throw ClassCastException.
        source = new TestBean();
        target = new TestBean();
        
        // Won't know how to convert a String to a Mystery - it's a Mystery!
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, mysteryProperty,
                target, stringProperty);
        
        caught = false;
        try {
            retVal = binding.getTargetValueForSource();
        } catch(ClassCastException e) {
            caught = true;
        }
        assertTrue(caught);
        
        // TEST: Binding's Validator, if any, is called upon to validate the final value.
        source = new TestBean();
        target = new TestBean();
        
        binding = Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
                source, stringProperty,
                target, stringProperty);
        
        TestValidator validator = new TestValidator();
        binding.setValidator(validator);
        
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertFalse(valRes.failed());
        assertTrue(validator.calledValidate);
        
        // TEST: If the Validator returns non-null from its validate method,
        // return the validation result, with failure type VALIDATION_FAILURE.
        validator.init();
        validator.invalid = true;
        
        retVal = binding.getTargetValueForSource();
        valRes = (Binding.ValueResult)retVal;
        assertTrue(valRes.failed());
        assertTrue(validator.calledValidate);
        assertTrue(valRes.getFailure().getType().equals(Binding.SyncFailureType.VALIDATION_FAILED));
        assertTrue(valRes.getFailure().getValidationResult() == validator.valResult);
    }
    
    //
    // Test utility classes, etc
    //
    
    /*
     * Create a binding for some of the more basic testing.
     * Returned binding is unbound.
     */
    private static Binding createTestBinding() {
        return createTestBinding("bogus", "bogus");
    }
    
    private static Binding createTestBinding(String srcPropName, String trgPropName) {
        TestBean source = new TestBean();
        TestBean target = new TestBean();
        Property sourceProp = BeanProperty.create(srcPropName);
        Property targetProp = BeanProperty.create(trgPropName);
        
        return Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_ONCE,
                source, sourceProp,
                target, targetProp);
    }
    
    public static class TestBean {
        String value = PROP_VALUE;
        Object objVal = null;
        Mystery mystVal = null;
        
        PropertyChangeSupport pcs;
        
        public TestBean() {
            pcs = new PropertyChangeSupport(this);
        }
        public String getSomeProperty() {
            return value;
        }
        public void setSomeProperty(String newValue) {
            String oldVal = value;
            value = newValue;
            pcs.firePropertyChange(PROP_NAME, oldVal, newValue);
        }
        
        public Object getObjProperty() {
            return objVal;
        }
        public void setObjProperty(Object newValue) {
            Object oldVal = objVal;
            objVal = newValue;
            pcs.firePropertyChange(OBJ_PROP_NAME, oldVal, newValue);
        }
        
        public Mystery getMystery() {
            return mystVal;
        }
        public void setMystery(Mystery newValue) {
            Mystery oldVal = mystVal;
            mystVal = newValue;
            pcs.firePropertyChange(MYSTERY_PROP_NAME, oldVal, newValue);
        }
    }
    
    private static class ObjStringConverter extends Converter {
        final static String PREFIX = "CONVERTED:";
        boolean forwardCalled;
        boolean reverseCalled;
        boolean shouldThrow;
        boolean shouldThrowCCE;
        
        public ObjStringConverter() {
            init();
        }
        
        public void init() {
            forwardCalled = false;
            reverseCalled = false;
            shouldThrow = false;
            shouldThrowCCE = false;
        }
                
        public Object convertForward(Object value) {
            forwardCalled = true;
            if (shouldThrow) {
                throw new RuntimeException("Exception from convertForward()");
            }
            if (shouldThrowCCE) {
                throw new ClassCastException("Exception from convertForward()");
            }
            return PREFIX + " forward: " + value.toString();
        }
        
        public Object convertReverse(Object value) {
            reverseCalled = true;
            if (shouldThrow) {
                throw new RuntimeException("Exception from convertReverse()");
            }
            if (shouldThrowCCE) {
                throw new ClassCastException("Exception from convertReverse()");
            }
            return PREFIX + " reverse: " + value.toString();
        }
        
        /*
         * Was this value obtained from this Converter?
         */
        public static boolean wasConverted(Object value) {
            if (value instanceof String) {
                return ((String)value).startsWith(PREFIX);
            }
            return false;
        }
    }
    
    private static class TestValidator extends Validator {
        boolean throwRE;
        boolean throwCCE;
        boolean invalid;
        boolean calledValidate;
        Validator.Result valResult;
        
        public TestValidator() {
            init();
        }
        
        public void init() {
            throwRE = false;
            throwCCE = false;
            invalid = false;
            calledValidate = false;
            valResult = new Validator.Result("TestValidator Error Code",
                    "TestValidator Description");
        }
        
        
        public Validator.Result validate(Object value) {
            calledValidate = true;
            if (throwRE) {
                throw new RuntimeException("from validate()");
            }
            if (throwCCE) {
                throw new ClassCastException("from validate()");
            }
            if (invalid) {
                return valResult;
            }
            return null;
        }
    }
}
