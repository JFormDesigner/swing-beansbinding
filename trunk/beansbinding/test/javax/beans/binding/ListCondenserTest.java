/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.Arrays;
import junit.framework.*;
import java.util.List;

/**
 *
 * @author sky
 */
public class ListCondenserTest extends TestCase {
    
    public ListCondenserTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testConcatenatingCondenser() {
        assertEquals("\"a\"", ListCondenser.concatenatingCondenser("\"", "\"", ", ").condense(Arrays.asList("a")));
        assertEquals("\"a\", \"b\"", ListCondenser.concatenatingCondenser("\"", "\"", ", ").condense(Arrays.asList("a", "b")));
    }
}
