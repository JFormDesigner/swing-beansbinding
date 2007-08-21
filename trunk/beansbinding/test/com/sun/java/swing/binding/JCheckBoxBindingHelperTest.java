/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import java.beans.FeatureDescriptor;
import java.util.HashMap;
import java.util.Map;
import javax.beans.binding.TestBean;
import org.jdesktop.beans.binding.Binding;
import javax.beans.binding.BindingContext;
import javax.beans.binding.ext.PropertyDelegateProvider;
import junit.framework.*;
import javax.swing.JCheckBox;

/**
 *
 * @author sky
 */
public class JCheckBoxBindingHelperTest extends TestCase {
    private JCheckBox checkBox;
    private TestBean source;
    private BindingContext context;
    
    public JCheckBoxBindingHelperTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
        checkBox = new JCheckBox();
        context = new BindingContext();
        source = new TestBean();
    }
    
    public void testFeatureDescriptors() {
        TestUtils.verifyPreferredBindingProperty(this, checkBox, "selected");
    }
    
    public void testUpdatingEnabled() {
        Binding bd = new Binding(
                source, "${value.booleanProperty}", checkBox, "selected");
        bd.setValueForIncompleteSourcePath(Boolean.TRUE);
        bd.bind();
        assertFalse(checkBox.isEnabled());
        
        TestBean source2 = new TestBean();
        source.setValue(source2);
        source2.setBooleanProperty(true);
        assertTrue(checkBox.isEnabled());
        assertTrue(checkBox.isSelected());
        
        checkBox.setSelected(false);
        assertFalse(source2.getBooleanProperty());
    }

    public void testBind() {
        source.setBooleanProperty(true);
        Binding bd = new Binding(
                source, "${booleanProperty}", checkBox, "selected");
        bd.bind();
        assertTrue(checkBox.isSelected());
        
        checkBox.setSelected(false);
        assertFalse(source.getBooleanProperty());
        assertFalse(source.getBooleanProperty());
        
        checkBox.setSelected(true);
        assertTrue(source.getBooleanProperty());
        assertTrue(source.getBooleanProperty());
    }
}
