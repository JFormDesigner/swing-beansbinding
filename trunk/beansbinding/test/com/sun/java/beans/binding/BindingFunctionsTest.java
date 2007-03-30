/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.beans.binding;

import java.util.Arrays;
import javax.beans.binding.BindingContext;
import javax.beans.binding.TestBean;
import junit.framework.*;

/**
 *
 * @author sky
 */
public class BindingFunctionsTest extends TestCase {
    private BindingContext context;
    private TestBean source;
    private TestBean target;
    
    public BindingFunctionsTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        context = new BindingContext();
        source = new TestBean();
        target = new TestBean();
    }

    protected void tearDown() throws Exception {
    }

    public void testListSize() {
        context.addBinding(source, "${bb:listSize(value)}", target, "value");
        context.bind();
        assertEquals(0, target.getValue());
        
        source.setValue(Arrays.asList("one"));
        assertEquals(1, target.getValue());

        source.setValue(null);
        assertEquals(0, target.getValue());
    }
}
