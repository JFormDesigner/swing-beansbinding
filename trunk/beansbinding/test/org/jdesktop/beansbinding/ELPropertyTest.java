/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import junit.framework.TestCase;

/**
 * @author Shannon Hickey
 */
public class ELPropertyTest extends TestCase {

    private Property valP = ELProperty.create("${value}");

    private class A {
        public int getValue() {
            return 10;
        }
        
        public void setValue(int value) {
        }
    }
    
    public class B {
        public int getValue() {
            return 10;
        }

        public void setValue(int value) {
        }
    }

    private class C extends B {
        public int getValue() {
            return 20;
        }
    }

    public class D {
        public int getValue() {
            return 30;
        }
    }
    
    private class E extends D {
    }

    private class F {
        public void setValue(int value) {
        }
    }
    
    private class G extends F {
    }
    
    protected void setUp() {
    }

    protected void tearDown() {
    }

    public void testPrivateClass() {
        A a = new A();
        assertFalse(valP.isReadable(a));
        assertFalse(valP.isWriteable(a));

        try {
            valP.getValue(a);
            fail();
        } catch (UnsupportedOperationException uoe) {
        } catch (Exception E) {
            fail();
        }

        try {
            valP.getWriteType(a);
            fail();
        } catch (UnsupportedOperationException uoe) {
        } catch (Exception E) {
            fail();
        }
        
        try {
            valP.setValue(a, 10);
            fail();
        } catch (UnsupportedOperationException uoe) {
        } catch (Exception E) {
            fail();
        }
    }

    public void testPrivateClassWithPublicSuperclass() {
        C c = new C();
        assertTrue(valP.isReadable(c));
        assertTrue(valP.isWriteable(c));
        assertEquals(20, valP.getValue(c));
        assertEquals(Integer.TYPE, valP.getWriteType(c));
        valP.setValue(c, 21);
    }

    public void testPrivateClassWithPublicSuperclassAndNoWriter() {
        E e = new E();
        assertTrue(valP.isReadable(e));
        assertFalse(valP.isWriteable(e));
        assertEquals(30, valP.getValue(e));
    }

    public void testPrivateClassWithPublicSuperclassAndNoReader() {
        G g = new G();
        assertFalse(valP.isReadable(g));
        assertTrue(valP.isWriteable(g));
        valP.setValue(g, 21);
    }

}
