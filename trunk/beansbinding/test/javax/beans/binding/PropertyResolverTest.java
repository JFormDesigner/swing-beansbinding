/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import com.sun.java.util.BindingCollections;
import java.util.HashMap;
import javax.beans.binding.ext.PropertyDelegateFactory;
import javax.beans.binding.ext.TestBeanPropertyDelegate;
import junit.framework.*;
import java.util.Map;

/**
 *
 * @author sky
 */
public class PropertyResolverTest extends TestCase {
    private LoggingDelegate delegate;
    private PropertyResolver resolver;
    
    public PropertyResolverTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        delegate = new LoggingDelegate();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(PropertyResolverTest.class);
        
        return suite;
    }
    
    private PropertyResolver createPropertyResolver(Object source, String path) {
        return PropertyResolver.createPropertyResolver(source, path);
    }
    
    private void verifyValueException() {
        try {
            resolver.getValueOfLastProperty();
            fail();
        } catch (IllegalStateException ile) {
        }
    }
    
    public void testSimplePCL() {
        TestBean2 bean2 = new TestBean2();
        resolver = createPropertyResolver(bean2, "value");
        resolver.bind();
        resolver.setDelegate(delegate);
        assertEquals(1, bean2.getPropetyChangeListenerCount());
        assertEquals(0, delegate.getAndClearChangeCount());

        bean2.setValue("xxx");
        assertEquals(1, delegate.getAndClearChangeCount());
        
        bean2.setValue2("zzz");
        assertEquals(0, delegate.getAndClearChangeCount());

        resolver.unbind();
        assertEquals(0, bean2.getPropetyChangeListenerCount());
    }
    
    public void testTypeOfLastProperty() {
        TestBean bean = new TestBean();
        resolver = createPropertyResolver(bean, "stringProperty");
        assertEquals(String.class, resolver.getTypeOfLastProperty());
        resolver.bind();
        assertEquals(String.class, resolver.getTypeOfLastProperty());

        resolver = createPropertyResolver(bean, "intProperty");
        assertEquals(int.class, resolver.getTypeOfLastProperty());
        resolver.bind();
        assertEquals(int.class, resolver.getTypeOfLastProperty());
    }
    
    public void testUnbind() {
        TestBean bean = new TestBean();
        resolver = createPropertyResolver(bean, "value");
        resolver.bind();
        assertEquals(1, bean.getPropetyChangeListenerCount());
        resolver.unbind();
        assertEquals(0, bean.getPropetyChangeListenerCount());
    }
    
    public void testSingle() {
        TestBean bean = new TestBean();
        resolver = createPropertyResolver(bean, "value");
        resolver.setDelegate(delegate);
        resolver.bind();
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());
        
        bean.setValue("foo");
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());
        
        bean.setValue(null);
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());
    }
    
    public void testMulti() {
        TestBean bean = new TestBean();
       resolver = createPropertyResolver(bean, "value.value");
        resolver.setDelegate(delegate);
        resolver.bind();
        assertFalse(resolver.hasAllPathValues());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, delegate.getAndClearChangeCount());
        verifyValueException();
        
        TestBean bean2 = new TestBean();
        bean.setValue(bean2);
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(1, bean2.getPropetyChangeListenerCount());
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean2.getValue(), resolver.getValueOfLastProperty());
        
        bean2.setValue("foo");
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(1, bean2.getPropetyChangeListenerCount());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean2.getValue(), resolver.getValueOfLastProperty());

        TestBean bean3 = new TestBean();
        bean.setValue(bean3);
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, bean2.getPropetyChangeListenerCount());
        assertEquals(1, bean3.getPropetyChangeListenerCount());
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean3.getValue(), resolver.getValueOfLastProperty());
        
        bean3.setValue("bar");
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, bean2.getPropetyChangeListenerCount());
        assertEquals(1, bean3.getPropetyChangeListenerCount());
        assertEquals(bean3.getValue(), resolver.getValueOfLastProperty());
        
        bean.setValue(null);
        assertFalse(resolver.hasAllPathValues());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, bean2.getPropetyChangeListenerCount());
        assertEquals(0, bean3.getPropetyChangeListenerCount());
        verifyValueException();
    }
    
    public void testObservableMap() {
        Map<Object,Object> map = new HashMap<Object, Object>();
        map = BindingCollections.observableMap(map);
        resolver = createPropertyResolver(map, "source.value");
        resolver.setDelegate(delegate);
        resolver.bind();
        assertFalse(resolver.hasAllPathValues());
        verifyValueException();

        TestBean bean = new TestBean();
        map.put("source", bean);
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());

        map.put("source", null);
        assertFalse(resolver.hasAllPathValues());
        assertEquals(1, delegate.getAndClearChangeCount());
        verifyValueException();
        
        map.put("source", bean);
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());
        
        bean.setValue("foo");
        assertTrue(resolver.hasAllPathValues());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());
    }
    
    public void testOne() {
        TestBean bean = new TestBean();
        PropertyResolver resolver = createPropertyResolver(
                bean, "value.value");
        assertEquals(false, resolver.hasAllPathValues());
        
        bean.setValue(new TestBean());
        assertEquals(true, resolver.hasAllPathValues());
        
        resolver.setValueOfLastProperty("xxx");
        assertEquals("xxx", ((TestBean)bean.getValue()).getValue());
    }
    
    public void testPCLOnPropertyDelegate() {
        TestBean bean = new TestBean();
        PropertyResolver resolver = createPropertyResolver(
                bean, "foo");
        PropertyResolverDelegate delegate = new PropertyResolverDelegate();
        resolver.setDelegate(delegate);
        resolver.bind();
        assertEquals(true, resolver.hasAllPathValues());
        assertEquals(0, delegate.getChangeCountAndClear());

        TestBeanPropertyDelegate beanDelegate = (TestBeanPropertyDelegate)
            PropertyDelegateFactory.getPropertyDelegate(bean, "foo");
        beanDelegate.setFoo("y");
        assertEquals(1, delegate.getChangeCountAndClear());
        
        bean.setValue("xxx");
        assertEquals(0, delegate.getChangeCountAndClear());
    }
    
    public void testNestedPCLs() {
        TestBean bean = new TestBean();
        PropertyResolver resolver = createPropertyResolver(
                bean, "foo.foo");
        PropertyResolverDelegate delegate = new PropertyResolverDelegate();
        resolver.setDelegate(delegate);
        resolver.bind();
        assertEquals(false, resolver.hasAllPathValues());
        assertEquals(0, delegate.getChangeCountAndClear());

        TestBeanPropertyDelegate beanDelegate = (TestBeanPropertyDelegate)
            PropertyDelegateFactory.getPropertyDelegate(bean, "foo");
        beanDelegate.setFoo("y");
        assertEquals(1, delegate.getChangeCountAndClear());
        
        TestBean subBean = new TestBean();
        beanDelegate.setFoo(subBean);
        assertEquals(1, delegate.getChangeCountAndClear());

        TestBeanPropertyDelegate subBeanDelegate = (TestBeanPropertyDelegate)
            PropertyDelegateFactory.getPropertyDelegate(subBean, "foo");
        subBeanDelegate.setFoo("y");
        assertEquals(1, delegate.getChangeCountAndClear());

        beanDelegate.setFoo("zzz");
        assertEquals(1, delegate.getChangeCountAndClear());
    }

    
    private static final class PropertyResolverDelegate extends PropertyResolver.Delegate {
        private int changeCount;
        
        public void valueChanged(PropertyResolver resolver) {
            changeCount++;
        }
        
        public int getChangeCountAndClear() {
            int changeCount = this.changeCount;
            this.changeCount = 0;
            return changeCount;
        }
    }
    
    private static final class LoggingDelegate extends PropertyResolver.Delegate {
        private int changeCount;
        
        public void valueChanged(PropertyResolver resolver) {
            changeCount++;
        }
        
        public int getAndClearChangeCount() {
            int count = changeCount;
            changeCount = 0;
            return count;
        }
    }
}
