/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import junit.framework.*;

/**
 *
 * @author Shannon Hickey
 */
public class REFPropertyResolverTest extends TestCase {

    private PropertyResolver resolver;

    public static Test suite() {
        TestSuite suite = new TestSuite(REFPropertyResolverTest.class);
        return suite;
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testFoo() {
        assertEquals("FOO", "FOO");
    }

    public void testBar() {
        assertEquals("BAR", "BAR");
    }

}
