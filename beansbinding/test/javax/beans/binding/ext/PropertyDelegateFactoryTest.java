/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding.ext;

import java.util.List;
import javax.beans.binding.TestBean;
import java.lang.ref.WeakReference;
import junit.framework.*;

/**
 *
 * @author sky
 */
public class PropertyDelegateFactoryTest extends TestCase {
    
    public PropertyDelegateFactoryTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        PropertyDelegateFactory.getPropertyDelegate(new TestBean(), "foo");
        TestBeanPropertyDelegateProvider.getLastTestBeanProvider().
                getAndClearCreateCount();
    }

    protected void tearDown() throws Exception {
        TestBeanPropertyDelegateProvider.getLastTestBeanProvider().
                getAndClearCreateCount();
    }
    
    public void testGetPropertyDelegate() {
        TestBean bean = new TestBean();
        TestBeanPropertyDelegateProvider provider =
                TestBeanPropertyDelegateProvider.getLastTestBeanProvider();
        Object fooDelegate = PropertyDelegateFactory.getPropertyDelegate(
                bean, "foo");
        assertNotNull(fooDelegate);
        assertEquals(1, provider.getAndClearCreateCount());
        Object barDelegate = PropertyDelegateFactory.getPropertyDelegate(
                bean, "bar");
        assertEquals(0, provider.getAndClearCreateCount());
        assertEquals(fooDelegate, barDelegate);
        
        WeakReference<Object> weakFooDelegate = new WeakReference<Object>(fooDelegate);
        fooDelegate = null;
        barDelegate = null;
        System.gc();
        assertNull(weakFooDelegate.get());
        
        fooDelegate = PropertyDelegateFactory.getPropertyDelegate(
                bean, "foo");
        assertEquals(1, provider.getAndClearCreateCount());
        assertNotNull(fooDelegate);
        barDelegate = PropertyDelegateFactory.getPropertyDelegate(
                bean, "bar");
        assertEquals(0, provider.getAndClearCreateCount());
        assertEquals(fooDelegate, barDelegate);
    }
    
    public void testGetPropertyDelegateClass() {
        assertEquals(0, PropertyDelegateFactory.getPropertyDelegateClass(
                Object.class).size());
        List<Class<?>> types = PropertyDelegateFactory.
                getPropertyDelegateClass(TestBean.class);
        assertNotNull(types);
        assertEquals(1, types.size());
        assertEquals(TestBeanPropertyDelegate.class, types.get(0));
    }
}
