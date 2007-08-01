/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.binding.BindingContext;
import javax.beans.binding.TestBean;
import javax.beans.binding.Binding;
import javax.swing.OLDBINDING.ParameterKeys;
import junit.framework.*;
import javax.swing.JSlider;

/**
 *
 * @author sky
 */
public class JSliderBindingHelperTest extends TestCase {
    private JSlider slider;
    private BindingContext context;
    private TestBean source;
    
    public JSliderBindingHelperTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        slider = new JSlider();
        source = new TestBean();
        context = new BindingContext();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(JSliderBindingHelperTest.class);
        
        return suite;
    }
    
    public void testFeatureDescriptors() {
        TestUtils.verifyPreferredBindingProperty(this, slider, "value");
    }

    public void testToIntConversion() {
        Binding binding = new Binding(source, "${value}", slider, "value");
        binding.bind();
        assertEquals(0, slider.getValue());
    }
    
    public void testUpdatingEnabled() {
        Binding binding = new Binding(
                source, "${value.value}", slider, "value");
        binding.setValueForIncompleteSourcePath(1);
        binding.bind();
//        assertEquals(1, slider.getValue());
        assertFalse(slider.isEnabled());
        
        TestBean source2 = new TestBean();
        source2.setValue(10);
        source.setValue(source2);
        assertEquals(10, slider.getValue());
        assertTrue(slider.isEnabled());
        
        slider.setValue(50);
        assertEquals(50, source2.getValue());
    }

    public void testValueProperty() {
        source.setValue(1);
        context.addBinding(source, "${value}", slider, "value");
        context.bind();
        
        assertEquals(1, slider.getValue());
        
        source.setValue(25);
        assertEquals(25, source.getValue());
        assertEquals(25, slider.getValue());
        
        slider.setValue(50);
        assertEquals(50, source.getValue());
        assertEquals(50, slider.getValue());
    }
}
