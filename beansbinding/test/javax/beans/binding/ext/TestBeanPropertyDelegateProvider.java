/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding.ext;

import javax.beans.binding.TestBean;

/**
 *
 * @author sky
 */
public final class TestBeanPropertyDelegateProvider extends PropertyDelegateProvider {
    private static TestBeanPropertyDelegateProvider lastProvider;
    
    private int createCount;

    public static TestBeanPropertyDelegateProvider getLastTestBeanProvider() {
        return lastProvider;
    }

    public TestBeanPropertyDelegateProvider() {
        if (lastProvider != null) {
            throw new IllegalStateException("Should only create one provider");
        }
        lastProvider = this;
    }
    
    
    public Object createPropertyDelegate(Object source, String property) {
        createCount++;
        return new TestBeanPropertyDelegate((TestBean)source);
    }
    
    public boolean providesDelegate(Class<?> type, String property) {
        return (TestBean.class.isAssignableFrom(type) &&
                ("foo".equals(property) || "bar".equals(property)));
    }

    public int getAndClearCreateCount() {
        int count = createCount;
        createCount = 0;
        return count;
    }

    public Class<?> getPropertyDelegateClass(Class<?> type) {
        if (TestBean.class.isAssignableFrom(type)) {
            return TestBeanPropertyDelegate.class;
        }
        return null;
    }
}
