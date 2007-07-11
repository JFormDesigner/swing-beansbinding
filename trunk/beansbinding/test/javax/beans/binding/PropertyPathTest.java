/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import junit.framework.*;

/**
 *
 * @author sky
 */
public class PropertyPathTest extends TestCase {
    
    public PropertyPathTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(PropertyPathTest.class);
        
        return suite;
    }
    
    public void testEmptyPath() {
        try {
            PropertyPath path = PropertyPath.createPropertyPath(null);
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException ise) {
        }

        try {
            PropertyPath path2 = PropertyPath.createPropertyPath("");
            fail("IAE should have been thrown");
        } catch (IllegalArgumentException ise) {
        }
    }
    
    public void testSinglePath() {
        PropertyPath fooPath = PropertyPath.createPropertyPath("foo");
        assertEquals(1, fooPath.length());
        assertEquals("foo", fooPath.get(0));
        assertTrue(fooPath.equals(PropertyPath.createPropertyPath("foo")));
        assertFalse(fooPath.equals(PropertyPath.createPropertyPath("foo.bar")));
    }

    public void testMultiPath() {
        PropertyPath path = PropertyPath.createPropertyPath("foo.bar");
        assertEquals(2, path.length());
        assertEquals("foo", path.get(0));
        assertEquals("bar", path.get(1));

        assertTrue(path.equals(PropertyPath.createPropertyPath("foo.bar")));
        assertFalse(path.equals(PropertyPath.createPropertyPath("foo")));
        assertFalse(path.equals(PropertyPath.createPropertyPath("foo.bar.baz")));
    }

    public void testEqualsAndHashCode() {
        PropertyPath.SinglePropertyPath single = new PropertyPath.SinglePropertyPath("foo");
        PropertyPath.MultiPropertyPath multi1 = new PropertyPath.MultiPropertyPath(new String[] {"foo"});
        PropertyPath.MultiPropertyPath multi2 = new PropertyPath.MultiPropertyPath(new String[] {"foo.bar"});

        assertTrue(single.equals(multi1));
        assertFalse(single.equals(multi2));

        assertTrue(single.hashCode() == multi1.hashCode());
        assertFalse(single.hashCode() == multi2.hashCode());
    }
}
