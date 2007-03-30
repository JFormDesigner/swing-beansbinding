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
        PropertyPath path = PropertyPath.createPropertyPath(null);
        assertEquals(0, path.length());
        assertEquals("", path.toString());
        
        PropertyPath path2 = PropertyPath.createPropertyPath("");
        assertEquals(0, path2.length());
        assertEquals("", path2.toString());
    }
    
    public void testSinglePath() {
        PropertyPath fooPath = PropertyPath.createPropertyPath("foo");
        assertEquals(1, fooPath.length());
        assertEquals("foo", fooPath.get(0));

        assertTrue(fooPath.equals(PropertyPath.createPropertyPath("foo")));
        
        assertFalse(fooPath.equals(PropertyPath.createPropertyPath("")));
        
        assertFalse(fooPath.equals(PropertyPath.createPropertyPath("foo.bar")));
    }
    
    public void testMultiPath() {
        PropertyPath path = PropertyPath.createPropertyPath("foo.bar");
        assertEquals(2, path.length());
        assertEquals("foo", path.get(0));
        assertEquals("bar", path.get(1));

        assertTrue(path.equals(PropertyPath.createPropertyPath("foo.bar")));
        
        assertFalse(path.equals(PropertyPath.createPropertyPath("")));
        
        assertFalse(path.equals(PropertyPath.createPropertyPath("foo")));

        assertFalse(path.equals(PropertyPath.createPropertyPath("foo.bar.baz")));
    }
    
    public void testSubPath() {
        PropertyPath path = PropertyPath.createPropertyPath(null);
        assertEquals(path, path.subPath(0, 0));
        
        path = PropertyPath.createPropertyPath("foo");
        assertEquals(path, path.subPath(0, 1));

        assertEquals(PropertyPath.createPropertyPath(null),
                path.subPath(0, 0));
        
        path = PropertyPath.createPropertyPath("foo.bar.baz");
        assertEquals(PropertyPath.createPropertyPath("foo"), path.subPath(0, 1));
        assertEquals(PropertyPath.createPropertyPath("foo.bar"), path.subPath(0, 2));
        assertEquals(PropertyPath.createPropertyPath("bar"), path.subPath(1, 1));
        assertEquals(PropertyPath.createPropertyPath("baz"), path.subPath(2, 1));
    }
}
