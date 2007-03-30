/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import java.beans.FeatureDescriptor;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.beans.binding.BindingContext;
import javax.beans.binding.ext.PropertyDelegateProvider;
import junit.framework.TestCase;

/**
 *
 * @author sky
 */
public class TestUtils {
    private TestUtils() {
    }
    
    
    public static void verifyPreferredBindingProperty(TestCase test, 
            Object source, String...prefProperties) {
        BindingContext context = new BindingContext();
        Map<String,FeatureDescriptor> descs = new HashMap<String,FeatureDescriptor>();
        for (FeatureDescriptor desc : context.getFeatureDescriptors(source)) {
            test.assertNull(desc.getName(), descs.put(desc.getName(), desc));
        }
        for (String property : prefProperties) {
            test.assertNotNull("Couldn't find expected property " + property, descs.get(property));
            test.assertEquals("Property is not a preferred binding property " + property, Boolean.TRUE, descs.get(property).getValue(
                    PropertyDelegateProvider.PREFERRED_BINDING_PROPERTY));
        }
    }

}
