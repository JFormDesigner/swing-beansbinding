/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import com.sun.java.util.ObservableCollections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.beans.binding.ext.PropertyDelegateFactory;
import javax.beans.binding.ext.TestBeanPropertyDelegate;
import javax.beans.binding.ext.TestBeanPropertyDelegateProvider;
import javax.el.ELContext;
import javax.el.ELResolver;
import junit.framework.*;
import static javax.el.Expression.Result.Type.*;

/**
 *
 * @author sky
 */
public class ELPropertyResolverTest extends TestCase {
    private LoggingDelegate delegate;
    private ELPropertyResolver resolver;
    
    public ELPropertyResolverTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        delegate = new LoggingDelegate();
    }

    protected void tearDown() throws Exception {
    }
    
    private ELPropertyResolver createResolver(Object source, String expression) {
        BindingContext context = new BindingContext();
        ELPropertyResolver resolver = new ELPropertyResolver(
                context.getContext(), source, expression);
        return resolver;
    }

    // PENDING: make sure to have tests that cover empty path!
    
    public void testMulti() {
        TestBean bean = new TestBean();
        resolver = createResolver(bean, "${value.value}");
        resolver.setDelegate(delegate);
        resolver.bind();
        assertEquals(INCOMPLETE_PATH, 
                resolver.getEvaluationResultType());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, delegate.getAndClearChangeCount());
        verifyValueException();
        
        TestBean bean2 = new TestBean();
        bean.setValue(bean2);
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(1, bean2.getPropetyChangeListenerCount());
        assertEquals(SINGLE_VALUE, resolver.getEvaluationResultType());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean2.getValue(), resolver.getValueOfLastProperty());
        
        bean2.setValue("foo");
        assertEquals(SINGLE_VALUE, resolver.getEvaluationResultType());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(1, bean2.getPropetyChangeListenerCount());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean2.getValue(), resolver.getValueOfLastProperty());

        TestBean bean3 = new TestBean();
        bean.setValue(bean3);
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, bean2.getPropetyChangeListenerCount());
        assertEquals(1, bean3.getPropetyChangeListenerCount());
        assertEquals(SINGLE_VALUE, resolver.getEvaluationResultType());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean3.getValue(), resolver.getValueOfLastProperty());
        
        bean3.setValue("bar");
        assertEquals(SINGLE_VALUE, resolver.getEvaluationResultType());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, bean2.getPropetyChangeListenerCount());
        assertEquals(1, bean3.getPropetyChangeListenerCount());
        assertEquals(bean3.getValue(), resolver.getValueOfLastProperty());
        
        bean.setValue(null);
        assertEquals(INCOMPLETE_PATH, resolver.getEvaluationResultType());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(1, bean.getPropetyChangeListenerCount());
        assertEquals(0, bean2.getPropetyChangeListenerCount());
        assertEquals(0, bean3.getPropetyChangeListenerCount());
        verifyValueException();
    }
    
    public void testObservableMap() {
        Map<Object,Object> map = new HashMap<Object, Object>();
        map = ObservableCollections.observableMap(map);
        resolver = createResolver(map, "${source.value}");
        resolver.setDelegate(delegate);
        resolver.bind();
        assertEquals(INCOMPLETE_PATH, resolver.getEvaluationResultType());
        verifyValueException();

        TestBean bean = new TestBean();
        map.put("source", bean);
        assertEquals(SINGLE_VALUE, resolver.getEvaluationResultType());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());

        map.put("source", null);
        assertEquals(INCOMPLETE_PATH, resolver.getEvaluationResultType());
        assertEquals(1, delegate.getAndClearChangeCount());
        verifyValueException();
        
        map.put("source", bean);
        assertEquals(SINGLE_VALUE, resolver.getEvaluationResultType());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());
        
        bean.setValue("foo");
        assertEquals(SINGLE_VALUE, resolver.getEvaluationResultType());
        assertEquals(1, delegate.getAndClearChangeCount());
        assertEquals(bean.getValue(), resolver.getValueOfLastProperty());
    }
    
    public void testGetValueOfLastPropertyPD() throws Exception {
        TestBean bean = new TestBean();
        TestBeanPropertyDelegateProvider provider =
                TestBeanPropertyDelegateProvider.getLastTestBeanProvider();
        TestBeanPropertyDelegate pDelegate = (TestBeanPropertyDelegate) 
                PropertyDelegateFactory.getPropertyDelegate( bean, "foo");
        resolver = createResolver(bean, "${foo}");
        resolver.setDelegate(delegate);
        resolver.bind();
        assertEquals(null, resolver.getValueOfLastProperty());
        pDelegate.setFoo("blah");
        assertEquals("blah", resolver.getValueOfLastProperty());
        assertEquals(1, delegate.getAndClearChangeCount());
    }

    public void testGetValueOfLastProperty() throws Exception {
        List<TestBean> source = new ArrayList<TestBean>(2);
        source.add(new TestBean());
        source.add(new TestBean());
        resolver = createResolver(source, "${value}");
        resolver.bind();
        
        assertEquals(Arrays.asList(null, null), 
                resolver.getValueOfLastProperty());
        
        source.get(0).setValue("foo");

        assertEquals(Arrays.asList("foo", null), 
                resolver.getValueOfLastProperty());
    }
    
    public void testSetValueOfLastProperty2() {
        List<TestBean> source = new ArrayList<TestBean>(2);
        source.add(new TestBean());
        source.add(new TestBean());
        resolver = createResolver(source, "${value}");
        resolver.setDelegate(delegate);
        resolver.bind();
        resolver.setValueOfLastProperty("foo");

        assertEquals(0, delegate.getAndClearChangeCount());
        
        assertEquals("foo", source.get(0).getValue());
        assertEquals("foo", source.get(1).getValue());
        
        source.get(0).setValue("blah");
        assertEquals(1, delegate.getAndClearChangeCount());

        source.get(1).setValue("xxx");
        assertEquals(1, delegate.getAndClearChangeCount());
    }
    
    public void testSetValueOfLastProperty1() {
        TestBean bean = new TestBean();
        resolver = createResolver(bean, "${value}");
        resolver.bind();
        resolver.setValueOfLastProperty("foo");
        assertEquals("foo", bean.getValue());
    }
    
    public void testUnbind() {
        TestBean bean = new TestBean();
        resolver = createResolver(bean, "${value}");
        resolver.bind();
        assertEquals(1, bean.getPropetyChangeListenerCount());
        resolver.unbind();
        assertEquals(0, bean.getPropetyChangeListenerCount());
    }

    public void test() {
        TestBean2 bean2 = new TestBean2();
        resolver = createResolver(bean2, "${value}");
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

    private void verifyValueException() {
        try {
            resolver.getValueOfLastProperty();
            fail();
        } catch (IllegalStateException ile) {
        }
    }

    private static final class LoggingDelegate extends ELPropertyResolver.Delegate {
        private int changeCount;
        
        public void valueChanged(ELPropertyResolver resolver) {
            changeCount++;
        }
        
        public int getAndClearChangeCount() {
            int count = changeCount;
            changeCount = 0;
            return count;
        }
    }
}
