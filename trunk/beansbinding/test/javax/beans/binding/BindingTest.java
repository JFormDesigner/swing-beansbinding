/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.Collections;
import java.util.List;
import javax.beans.binding.ext.TestBeanBindingTarget;
import javax.beans.binding.ext.TestBeanPropertyDelegate;
import com.sun.java.util.BindingCollections;
import javax.beans.binding.ext.TestBeanPropertyDelegateProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.beans.binding.ext.PropertyDelegateFactory;
import junit.framework.*;
import static javax.beans.binding.Binding.ValueState.*;

/**
 *
 * @author sky
 */
public class BindingTest extends TestCase {
    private static final Binding.ParameterKey<Integer> IntKey = new Binding.ParameterKey<Integer>("");
    private static final Binding.ParameterKey<Boolean> BooleanKey = new Binding.ParameterKey<Boolean>("");

    private TestBean source;
    private TestBean target;
    private BindingContext context;
    private Map<Object,Object> map;
    
    public BindingTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(BindingTest.class);
        
        return suite;
    }
    
    protected void setUp() throws Exception {
        PropertyDelegateFactory.getPropertyDelegate(new TestBean(), "foo");
        TestBeanPropertyDelegateProvider.getLastTestBeanProvider().
                getAndClearCreateCount();
        context = new BindingContext();
        source = new TestBean();
        target = new TestBean();
        map = new HashMap<Object,Object>(1);
        map = BindingCollections.observableMap(map);
    }
    
    protected void tearDown() throws Exception {
        TestBeanPropertyDelegateProvider.getLastTestBeanProvider().
                getAndClearCreateCount();
        super.tearDown();
    }
    
    public void testListPathElement3() {
        TestBean source = new TestBean();
        List<TestBean> sources = Collections.emptyList();
        source.setValue(sources);
        Binding binding = new Binding(sources, "${value}", target, "value");
        assertEquals(null, target.getValue());
    }
    
    public void testListPathElement2() {
        List<TestBean> sources = Arrays.asList(new TestBean(), new TestBean());
        Binding binding = new Binding(sources, "${value}", target, "value");
        binding.setListCondenser(ListCondenser.concatenatingCondenser("\"", "\"", ", "));
        binding.bind();
        assertEquals("\"\", \"\"", target.getValue());
        
        sources.get(0).setValue("x");
        assertEquals("\"x\", \"\"", target.getValue());

        sources.get(1).setValue("!");
        assertEquals("\"x\", \"!\"", target.getValue());
        
        target.setValue("z");
        assertEquals("z", target.getValue());
        assertEquals("z", sources.get(0).getValue());
        assertEquals("z", sources.get(1).getValue());
        
        sources.get(1).setValue("!");
        assertEquals("\"z\", \"!\"", target.getValue());
    }
    
    public void testListPathElement() {
        List<TestBean> sources = Arrays.asList(new TestBean(), new TestBean());
        Binding binding = new Binding(sources, "${value}", target, "value");
        binding.bind();
        assertEquals(null, target.getValue());
        
        sources.get(0).setValue("y");
        assertEquals("y", target.getValue());

        sources.get(1).setValue("z");
        assertEquals("y", target.getValue());
        
        target.setValue("foo");
        assertEquals("foo", target.getValue());
        assertEquals("foo", sources.get(0).getValue());
        assertEquals("foo", sources.get(1).getValue());
    }
    
    public void testBindingListener2() {
        EventListenerRecorder<BindingListener> recorder =
                new EventListenerRecorder<BindingListener>(BindingListener.class);
        source.setValue("foo");
        Binding binding = new Binding(source, "${value}", target, "value");
        binding.setValidator(new BindingValidator() {
            public ValidationResult validate(Binding binding, Object value) {
                return new ValidationResult(ValidationResult.Action.DO_NOTHING);
            }
        });
        binding.addBindingListener(recorder.getEventListenerImpl());
        binding.bind();
        target.setValue("bar");
        List<EventListenerRecorder.InvocationRecord> records = recorder.getAndClearRecords();
        assertEquals(1, records.size());
        assertEquals("validationFailed", records.get(0).getMethodName());
    }
    
    public void testBindingListener() {
        EventListenerRecorder<BindingListener> recorder =
                new EventListenerRecorder<BindingListener>(BindingListener.class);
        source.setValue("foo");
        Binding binding = new Binding(source, "${value}", target, "value");
        binding.setConverter(new BindingConverter() {
            public Object sourceToTarget(Object value) {
                throw new ClassCastException();
            }
        });
        binding.addBindingListener(recorder.getEventListenerImpl());
        binding.bind();
        List<EventListenerRecorder.InvocationRecord> records = recorder.getAndClearRecords();
        assertEquals(1, records.size());
        assertEquals("converterFailed", records.get(0).getMethodName());
    }
    
    public void testStates17() {
        target.setValue(new TestBean());
        Binding binding = new Binding(source, "${value}", target, "value.value");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ_ONCE);
        binding.bind();
        target.setValue(null);
        assertEquals(VALID, binding.getSourceValueState());
        assertEquals(INCOMPLETE_PATH, binding.getTargetValueState());
    }

    public void testStates16() {
        Binding binding = new Binding(source, "${value}", target, "value");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ_ONCE);
        binding.bind();
        target.setValue("foo");
        assertEquals(VALID, binding.getSourceValueState());
        assertEquals(UNCOMMITTED, binding.getTargetValueState());
    }

    public void testStates15() {
        target.setValue(new TestBean());
        Binding binding = new Binding(source, "${value.value}", target, "value.value");
        binding.setValueForIncompleteTargetPath("foo");
        binding.bind();
        target.setValue(null);
        assertEquals(null, target.getValue());
        assertEquals(INCOMPLETE_PATH, binding.getSourceValueState());
        assertEquals(INCOMPLETE_PATH, binding.getTargetValueState());
    }

    public void testStates14() {
        target.setValue(new TestBean());
        Binding binding = new Binding(source, "${value.value}", target, "value");
        binding.setValueForIncompleteTargetPath("foo");
        binding.bind();
        target.setValue("foo");
        assertEquals("foo", target.getValue());
        assertEquals(INCOMPLETE_PATH, binding.getSourceValueState());
        assertEquals(UNCOMMITTED, binding.getTargetValueState());
    }

    public void testStates13() {
        target.setValue(new TestBean());
        Binding binding = new Binding(source, "${value}", target, "value.value");
        binding.bind();
        target.setValue(null);
        assertEquals(null, source.getValue());
        assertEquals(null, target.getValue());
        assertEquals(VALID, binding.getSourceValueState());
        assertEquals(INCOMPLETE_PATH, binding.getTargetValueState());
    }

    public void testStates12() {
        source.setValue("x");
        Binding binding = new Binding(source, "${value}", target, "value");
        ValidatorImpl validator = new ValidatorImpl(ValidationResult.Action.SET_TARGET_FROM_SOURCE);
        binding.setValidator(validator);
        binding.bind();
        target.setValue("foo");
        assertEquals("x", source.getValue());
        assertEquals("x", target.getValue());
        assertEquals(VALID, binding.getSourceValueState());
        assertEquals(VALID, binding.getTargetValueState());
    }

    public void testStates11() {
        source.setValue("x");
        Binding binding = new Binding(source, "${value}", target, "value");
        BindingConverterImpl converter = new BindingConverterImpl();
        binding.setConverter(converter);
        converter.setThrowSourceToTargetException(true);
        ValidatorImpl validator = new ValidatorImpl(ValidationResult.Action.SET_TARGET_FROM_SOURCE);
        binding.setValidator(validator);
        binding.bind();
        target.setValue("foo");
        assertEquals(INVALID, binding.getSourceValueState());
        assertEquals(INVALID, binding.getTargetValueState());
    }

    public void testStates10() {
        Binding binding = new Binding(source, "${value}", target, "value");
        BindingConverterImpl converter = new BindingConverterImpl();
        binding.setConverter(converter);
        ValidatorImpl validator = new ValidatorImpl(ValidationResult.Action.DO_NOTHING);
        binding.setValidator(validator);
        binding.bind();
        target.setValue("foo");
        assertEquals(VALID, binding.getSourceValueState());
        assertEquals(INVALID, binding.getTargetValueState());
    }

    public void testStates9() {
        Binding binding = new Binding(source, "${value}", target, "value");
        BindingConverterImpl converter = new BindingConverterImpl();
        binding.setConverter(converter);
        binding.bind();
        converter.setThrowTargetToSourceException(true);
        target.setValue("foo");
        assertEquals(VALID, binding.getSourceValueState());
        assertEquals(INVALID, binding.getTargetValueState());
    }

    public void testStates8() {
        Binding binding = new Binding(source, "${value.value}", target, "value");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ_ONCE);
        binding.bind();
        
        source.setValue(new TestBean());
        assertEquals(UNCOMMITTED, binding.getSourceValueState());
        assertEquals(VALID, binding.getTargetValueState());
        
        source.setValue(null);
        assertEquals(INCOMPLETE_PATH, binding.getSourceValueState());
        assertEquals(VALID, binding.getTargetValueState());
    }

    public void testStates7() {
        Binding binding = new Binding(source, "${value.value}", target, "value");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ_ONCE);
        binding.bind();
        
        source.setValue(new TestBean());
        assertEquals(UNCOMMITTED, binding.getSourceValueState());
        assertEquals(VALID, binding.getTargetValueState());
    }

    public void testStates6() {
        Binding binding = new Binding(source, "${value.value}", target, "value");
        binding.bind();
        assertEquals(INCOMPLETE_PATH, binding.getSourceValueState());
        assertEquals(VALID, binding.getTargetValueState());
    }

    public void testStates5() {
        source.setValue("foo");
        Binding binding = new Binding(source, "${value}", target, "value");
        BindingConverterImpl converter = new BindingConverterImpl();
        binding.setConverter(converter);
        converter.setThrowSourceToTargetException(true);
        binding.bind();
        assertEquals(INVALID, binding.getSourceValueState());
        assertEquals(VALID, binding.getTargetValueState());
    }

    public void testStates4() {
        Binding binding = new Binding(source, "${value}", target, "value");
        BindingConverterImpl converter = new BindingConverterImpl();
        binding.setConverter(converter);
        binding.bind();
        assertEquals(VALID, binding.getSourceValueState());
        assertEquals(VALID, binding.getTargetValueState());
    }

    public void testStates3() {
        Binding binding = new Binding(source, "${value}", target, "value");
        binding.bind();
        assertEquals(VALID, binding.getSourceValueState());
        assertEquals(VALID, binding.getTargetValueState());
    }

    public void testStates2() {
        Binding binding = new Binding(source, "${value}", target, "value.value");
        binding.bind();
        assertEquals(UNCOMMITTED, binding.getSourceValueState());
        assertEquals(INCOMPLETE_PATH, binding.getTargetValueState());
    }

    public void testStates1() {
        Binding binding = new Binding(source, "${value.value}", target, "value.value");
        binding.bind();
        assertEquals(INCOMPLETE_PATH, binding.getSourceValueState());
        assertEquals(INCOMPLETE_PATH, binding.getTargetValueState());
    }

    public void testValueForIncompleteTargetPath2() {
        source.setValue("foo");
        Binding binding = new Binding(source, "${value}", target, "value.value.value");
        binding.bind();
        assertEquals("foo", source.getValue());
        assertEquals(null, target.getValue());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getSourceValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getTargetValueState());
        
        TestBean v2 = new TestBean();
        target.setValue(v2);
        assertEquals("foo", source.getValue());
        assertEquals(v2, target.getValue());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getSourceValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getTargetValueState());
        
        TestBean v3 = new TestBean();
        v2.setValue(v3);
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals("foo", source.getValue());
        assertEquals("foo", v3.getValue());
    }

    public void testValueForIncompleteTargetPath() {
        Binding binding = new Binding(source, "${value}", target, "value.value.value");
        binding.setValueForIncompleteTargetPath("foo");
        binding.bind();
        assertEquals(null, source.getValue());
        assertEquals(null, target.getValue());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getSourceValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getTargetValueState());
        
        TestBean v2 = new TestBean();
        target.setValue(v2);
        assertEquals("foo", source.getValue());
        assertEquals(v2, target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getTargetValueState());
        
        TestBean v3 = new TestBean();
        v2.setValue(v3);
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals("foo", source.getValue());
        assertEquals("foo", v3.getValue());
    }
    
    public void testValueForIncompleteSourcePath2() {
        target.setValue("foo");
        Binding binding = new Binding(source, "${value.value}", target, "value");
        binding.bind();
        assertEquals("foo", target.getValue());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());

        source.setValue(new TestBean());
        assertEquals(null, target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
    }

    public void testValueForIncompleteSourcePath() {
        Binding binding = new Binding(source, "${value.value}", target, "value");
        binding.setValueForIncompleteSourcePath("foo");
        binding.bind();
        assertEquals("foo", target.getValue());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        
        binding.unbind();
        
        binding.setValueForIncompleteSourcePath(null);
        binding.bind();
        assertEquals("foo", target.getValue());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
    }
    
    // PENDING: need to test conversions errors from validation listener as
    // well.

    public void testValues() {
        Binding binding = new Binding();
        binding.putParameter(IntKey, 10);
        assertEquals((int)10, (int)binding.getParameter(IntKey, 0));
        
        binding.putParameter(BooleanKey, Boolean.FALSE);
        assertEquals(false, (boolean)binding.getParameter(BooleanKey, Boolean.TRUE));
        
        binding.putParameter(IntKey, null);
        assertEquals((int)0, (int)binding.getParameter(IntKey, 0));
    }

    public void testConvertExceptions2() {
        BindingConverterImpl converter = new BindingConverterImpl();
        converter.setThrowTargetToSourceException(true);
        source.setValue("bar");
        Binding binding = new Binding(source, "${value}", target, "value");
        binding.setConverter(converter);
        binding.bind();
        assertEquals("bar", target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        
        target.setValue("foo");
        assertEquals("foo", target.getValue());
        assertEquals("bar", source.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.INVALID, binding.getTargetValueState());
    }
    
    public void testConvertExceptions() {
        BindingConverterImpl converter = new BindingConverterImpl();
        converter.setThrowSourceToTargetException(true);
        source.setValue("bar");
        Binding binding = new Binding(source, "${value}", target, "value");
        binding.setConverter(converter);
        binding.bind();
        assertEquals(null, target.getValue());
        assertEquals(Binding.ValueState.INVALID, binding.getSourceValueState());
        
        converter.setThrowSourceToTargetException(false);
        source.setValue("foo");
        assertEquals("foo", target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
    }
    
    public void testConverter() {
        BindingConverterImpl converter = new BindingConverterImpl();
        source.setValue("bar");
        Binding binding = new Binding(source, "${value}", target, "value");
        binding.setConverter(converter);
        binding.bind();
        assertEquals(1, converter.getAndClearSourceToTargetCount());
        assertEquals(0, converter.getAndClearTargetToSourceCount());
        
        target.setValue("foo");
        assertEquals(0, converter.getAndClearSourceToTargetCount());
        assertEquals(1, converter.getAndClearTargetToSourceCount());

        source.setValue("x");
        assertEquals(1, converter.getAndClearSourceToTargetCount());
        assertEquals(0, converter.getAndClearTargetToSourceCount());
    }
    
    public void testAddBinding() {
        Binding binding = new Binding();
        Binding child = new Binding();
        binding.addChildBinding(child);
        try {
            binding.addChildBinding(child);
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException iae) {
        }
        
        assertEquals(Arrays.asList(child), binding.getChildBindings());
        
        binding.removeChildBinding(child);
        
        assertEquals(0, binding.getChildBindings().size());
        
        try {
            binding.removeChildBinding(child);
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException iae) {
        }
        
        binding.addChildBinding(child);
        
        Binding binding2 = new Binding();
        try {
            binding2.addChildBinding(child);
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testPrimitiveSwap() {
        Binding binding = new Binding(
                source, "${value}", target, "intProperty");
        binding.bind();

        binding = new Binding(
                source, "${value}", target, "booleanProperty");
        binding.bind();

        binding = new Binding(
                source, "${value}", target, "charProperty");
        binding.bind();

        binding = new Binding(
                source, "${value}", target, "floatProperty");
        binding.bind();
    }

    public void testIncompleteSource() {
        Binding binding = new Binding(
                source, "${value.value.value}", target, "value");
        binding.bind();
        
        assertEquals(0, target.getAndClearValueSetCount());
        
        TestBean sourceValue = new TestBean();
        source.setValue(sourceValue);
        assertEquals(0, target.getAndClearValueSetCount());

        TestBean sourceValueValue = new TestBean();
        sourceValue.setValue(sourceValueValue);
        assertEquals(1, target.getAndClearValueSetCount());
        
        sourceValueValue.setValue("x");
        assertEquals(1, target.getAndClearValueSetCount());
        assertEquals("x", target.getValue());
    }

    public void testBooleanToIntegerConverters() {
        Binding binding = new Binding(
                source, "${bigIntProperty}", target, "bigBooleanProperty");
        source.setBigIntProperty(0);
        binding.bind();
        assertFalse(target.getBigBooleanProperty());

        target.setBigBooleanProperty(true);
        assertEquals(new Integer(1), source.getBigIntProperty());

        source.setBigIntProperty(0);
        assertFalse(target.getBigBooleanProperty());
    }

    public void testBooleanToIntConverters() {
        Binding binding = new Binding(
                source, "${intProperty}", target, "booleanProperty");
        binding.bind();
        assertFalse(target.getBooleanProperty());

        target.setBooleanProperty(true);
        assertEquals(1, source.getIntProperty());

        source.setIntProperty(0);
        assertFalse(target.getBooleanProperty());
    }

    public void testStringToIntConverters() {
        source.setStringProperty("1");
        Binding binding = new Binding(
                source, "${stringProperty}", target, "intProperty");
        binding.bind();
        assertEquals(1, target.getIntProperty());
        
        target.setIntProperty(10);
        assertEquals("10", source.getStringProperty());
    }

    public void testListBindingRemoveOnCommit() {
        Binding binding = new Binding(source, "${value}", target, "foo");
        Binding subBinding = new Binding("${value}", "value");
        binding.addChildBinding(subBinding);
        binding.bind();
        TestBeanPropertyDelegate targetDelegate = (TestBeanPropertyDelegate)
                PropertyDelegateFactory.getPropertyDelegate(target, "foo");
        TestBeanBindingTarget bindingTarget = (TestBeanBindingTarget)
                targetDelegate.createBindingTarget("foo");
        
        TestBean subSource = new TestBean();
        TestBean subTarget = new TestBean();
        subBinding.setSource(subSource);
        subBinding.setTarget(subTarget);
        
        subBinding.bind();
        subTarget.setValue0("Z");
        bindingTarget.getController().unbindOnCommit(subBinding);
        subBinding.setTargetValueFromSourceValue();
        
        assertFalse(subBinding.isBound());
    }

    public void testListBinding() {
        Binding binding = new Binding(source, "${value}", target, "foo");
        Binding subBinding = new Binding("${value}", "value");
        binding.addChildBinding(subBinding);
        binding.bind();
        TestBeanPropertyDelegate targetDelegate = (TestBeanPropertyDelegate)
                PropertyDelegateFactory.getPropertyDelegate(target, "foo");
        TestBeanBindingTarget bindingTarget = (TestBeanBindingTarget)
                targetDelegate.createBindingTarget("foo");
        assertTrue(bindingTarget.isBound());
        assertEquals("foo", bindingTarget.getProperty());
        
        TestBean subSource = new TestBean();
        TestBean subTarget = new TestBean();
        Binding subDescription = new Binding(
                subSource, "${value}", subTarget, "value");
        
        subBinding.setSource(subSource);
        subBinding.setTarget(subTarget);
        subBinding.bind();
        assertEquals(1, subSource.getPropetyChangeListenerCount());
        assertEquals(1, subTarget.getPropetyChangeListenerCount());
        subTarget.setValue("xx");
        assertEquals("xx", subSource.getValue());
        assertEquals("xx", subTarget.getValue());

        subSource.setValue("yy");
        assertEquals("yy", subSource.getValue());
        assertEquals("yy", subTarget.getValue());
        
        subBinding.unbind();
        assertEquals(0, subSource.getPropetyChangeListenerCount());
        assertEquals(0, subTarget.getPropetyChangeListenerCount());
        subSource.setValue("z");
        assertEquals("z", subSource.getValue());
        assertEquals("yy", subTarget.getValue());
    }

    public void testSourcePropertyDelegate() {
        Binding binding = new Binding(
                source, "${foo}", target, "value");
        binding.bind();
        TestBeanPropertyDelegate sourceDelegate = (TestBeanPropertyDelegate)
                PropertyDelegateFactory.getPropertyDelegate(source, "foo");

        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());

        sourceDelegate.setFoo("foo");
        assertEquals("foo", sourceDelegate.getFoo());
        assertEquals("foo", target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        
        target.setValue("bar");
        assertEquals("bar", sourceDelegate.getFoo());
        assertEquals("bar", target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
    }

    public void testLazyProvideSource() {
        Binding binding = new Binding(
                map, "${source.value}", target, "value");
        binding.bind();

        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getSourceValueState());
        
        target.setValue("target");
        assertEquals("target", target.getValue());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getTargetValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getSourceValueState());
        
        map.put("source", source);
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals(null, target.getValue());
        assertEquals(null, source.getValue());
    }

    public void testLazyProvideTarget() {
        Binding binding = new Binding(
                source, "${value}", map, "target.value");
        binding.bind();

        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getSourceValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getTargetValueState());
        
        source.setValue("source");
        assertEquals("source", source.getValue());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getTargetValueState());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getSourceValueState());
        
        map.put("target", target);
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals("source", target.getValue());
        assertEquals("source", source.getValue());
    }

    public void testOne() {
        Binding binding = new Binding(
                source, "${value}", target, "value");
        binding.bind();

        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        source.setValue("x");
        assertEquals("x", target.getValue());
        target.setValue("y");
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        assertEquals("y", source.getValue());
    }

    public void testIncompleteSourcePath() {
        Binding binding = new Binding(
                source, "${value.value}", target, "value");
        binding.bind();
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getSourceValueState());
        
        TestBean childSourceBean = new TestBean();
        source.setValue(childSourceBean);
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());

        source.setValue(null);
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getSourceValueState());
    }

    public void testIncompleteTargetPath() {
        Binding binding = new Binding(
                source, "${value}", target, "value.value");
        binding.bind();
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getTargetValueState());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getSourceValueState());
        
        TestBean childTargetBean = new TestBean();
        target.setValue(childTargetBean);
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());

        target.setValue(null);
        assertEquals(Binding.ValueState.INCOMPLETE_PATH, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
    }

    public void testReadOnce() {
        Binding binding = new Binding(
                source, "${value}", target, "value");
        binding .setUpdateStrategy(Binding.UpdateStrategy.READ_ONCE);
        binding.bind();
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        
        source.setValue("x");
        assertEquals("x", source.getValue());
        assertEquals(null, target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getSourceValueState());
        
        target.setValue("y");
        assertEquals("x", source.getValue());
        assertEquals("y", target.getValue());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getTargetValueState());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getSourceValueState());
    }

    public void testReadOnly() {
        Binding binding = new Binding(
                source, "${value}", target, "value");
        binding.setUpdateStrategy(Binding.UpdateStrategy.READ);
        binding.bind();
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        
        source.setValue("x");
        assertEquals("x", source.getValue());
        assertEquals("x", target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        
        target.setValue("y");
        assertEquals("x", source.getValue());
        assertEquals("y", target.getValue());
        assertEquals(Binding.ValueState.UNCOMMITTED, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
    }

    public void testDoNothingValidation() {
        Binding binding = new Binding(
                source, "${value}", target, "value");
        ValidatorImpl validator = new ValidatorImpl(ValidationResult.Action.DO_NOTHING);
        binding.setValidator(validator);
        binding.bind();
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        
        target.setValue("x");
        assertEquals(null, source.getValue());
        assertEquals("x", target.getValue());
        assertEquals(Binding.ValueState.INVALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
    }

    public void testSetTargetFromSourceValidation() {
        Binding binding = new Binding(
                source, "${value}", target, "value");
        ValidatorImpl validator = new ValidatorImpl(
                ValidationResult.Action.SET_TARGET_FROM_SOURCE);
        binding.setValidator(validator);
        binding.bind();
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
        
        source.setValue("x");
        assertEquals("x", source.getValue());
        assertEquals("x", target.getValue());

        target.setValue("y");
        assertEquals("x", source.getValue());
        assertEquals("x", target.getValue());
        assertEquals(Binding.ValueState.VALID, binding.getTargetValueState());
        assertEquals(Binding.ValueState.VALID, binding.getSourceValueState());
    }

    public void testSetContext() {
        Binding bindingP = new Binding(source, "${value}", target, "value");
        Binding bindingC = new Binding(source, "${value}", target, "value");
        BindingContext context = new BindingContext();
        bindingP.addChildBinding(bindingC);
        context.addBinding(bindingP);
        try {
            context.addBinding(bindingC);
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException ise) {
        }
    }

    public void testSetParent() {
        Binding bindingP = new Binding(source, "${value}", target, "value");
        Binding bindingC = new Binding(source, "${value}", target, "value");
        BindingContext context = new BindingContext();
        context.addBinding(bindingP);
        context.addBinding(bindingC);
        try {
            bindingP.addChildBinding(bindingC);
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException ise) {
        }
    }

    public void testFetchByName1() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        Binding child = bindingP.addChildBinding("CHILD", "${value}", "value");
        Binding fetch = bindingP.getChildBinding("CHILD");
        assertEquals(child, fetch);
    }

    public void testFetchByName2() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        bindingP.addChildBinding("FOO", "${value}", "value");
        Binding fetch = bindingP.getChildBinding("CHILD");
        assertEquals(null, fetch);
    }

    public void testFetchByName3() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        bindingP.addChildBinding("${value}", "value");
        Binding fetch = bindingP.getChildBinding("CHILD");
        assertEquals(null, fetch);
    }

    public void testFetchByName4() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        Binding fetch = bindingP.getChildBinding("CHILD");
        assertEquals(null, fetch);
    }

    public void testFetchByName5() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        try {
            bindingP.getChildBinding(null);
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
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        Binding bindingC = new Binding("CHILD", source, "${value}", target, "value");
        bindingP.addChildBinding(bindingC);
        assertEquals(bindingC, bindingP.getChildBinding("CHILD"));
        bindingC.setName("CHILD2");
        assertEquals(null, bindingP.getChildBinding("CHILD"));
        assertEquals(bindingC, bindingP.getChildBinding("CHILD2"));
    }

    public void testSetName2() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        Binding bindingC = new Binding(source, "${value}", target, "value");
        bindingP.addChildBinding(bindingC);
        assertEquals(null, bindingP.getChildBinding("CHILD"));
        bindingC.setName("CHILD2");
        assertEquals(bindingC, bindingP.getChildBinding("CHILD2"));
    }

    public void testSetName3() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        Binding bindingC = new Binding("CHILD", source, "${value}", target, "value");
        bindingP.addChildBinding(bindingC);
        assertEquals(bindingC, bindingP.getChildBinding("CHILD"));
        bindingC.setName(null);
        assertEquals(null, bindingP.getChildBinding("CHILD"));
    }

    public void testSetName4() {
        Binding bindingP = new Binding("PARENT", source, "${value}", target, "value");
        Binding bindingC = new Binding("CHILD", source, "${value}", target, "value");
        Binding bindingC2 = new Binding("CHILD2", source, "${value}", target, "value");
        bindingP.addChildBinding(bindingC);
        bindingP.addChildBinding(bindingC2);
        try {
            bindingC2.setName("CHILD");
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testUnbindUnbound() {
        Binding binding = new Binding(source, "${value}", target, "value");
        try {
            binding.unbind();
            fail("ISE should have been thrown");
        } catch (IllegalStateException ise) {
        }
    }
    
    private static class TestBinding extends Binding{
        TestBinding() {
        }
    }

    private static class TestBinding2 extends Binding {
        TestBinding2() {
        }
    }
    
    
    private static final class BindingConverterImpl extends BindingConverter {
        private int targetToSourceCount;
        private int sourceToTargetCount;
        private boolean throwTargetToSource;
        private boolean throwSourceToTarget;

        public void setThrowTargetToSourceException(boolean shouldThrow) {
            throwTargetToSource = shouldThrow;
        }
        
        public void setThrowSourceToTargetException(boolean shouldThrow) {
            throwSourceToTarget = shouldThrow;
        }
        
        public Object targetToSource(Object value) {
            if (throwTargetToSource) {
                throw new ClassCastException();
            }
            targetToSourceCount++;
            return value;
        }

        public Object sourceToTarget(Object value) {
            if (throwSourceToTarget) {
                throw new ClassCastException();
            }
            sourceToTargetCount++;
            return value;
        }
        
        public int getAndClearTargetToSourceCount() {
            int count = targetToSourceCount;
            targetToSourceCount = 0;
            return count;
        }

        public int getAndClearSourceToTargetCount() {
            int count = sourceToTargetCount;
            sourceToTargetCount = 0;
            return count;
        }
    }
}
