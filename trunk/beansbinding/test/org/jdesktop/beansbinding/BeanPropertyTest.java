/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import junit.framework.TestCase;

/**
 * @author Shannon Hickey
 */
public class BeanPropertyTest extends TestCase {

    private B b;
    private Property valP = BeanProperty.create("value");
    
    public class A {
        public int getValue() {
            return 10;
        }

        public void setValue(int value) {
        }
    }

    private class B extends A {
        public int getValue() {
            return 20;
        }
    }

    protected void setUp() {
        b = new B();
    }

    protected void tearDown() {
        b = null;
    }

    public void testPrivateClass() {
        //assertFalse(valP.isReadable(b));
        //assertFalse(valP.isWriteable(b));
    }

}
