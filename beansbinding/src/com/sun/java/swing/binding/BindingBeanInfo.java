/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;
import java.util.List;
import javax.beans.binding.ext.PropertyDelegateProvider;

/**
 *
 * @author sky
 */
abstract class BindingBeanInfo extends SimpleBeanInfo {
    protected abstract Property[] getPreferredProperties();
    
    protected abstract Class<?> getPropertyDelegateClass();

    public PropertyDescriptor[] getPropertyDescriptors() {
        List<PropertyDescriptor> descriptors = new 
                ArrayList<PropertyDescriptor>();
        for (Property property : getPreferredProperties()) {
            try {
                PropertyDescriptor pd = new PropertyDescriptor(
                        property.getName(), getPropertyDelegateClass());
                pd.setShortDescription(property.getDescription());
                pd.setValue(
                        PropertyDelegateProvider.PREFERRED_BINDING_PROPERTY,
                        Boolean.TRUE);
                descriptors.add(pd);
            } catch (IntrospectionException ex) {
                assert false;
            }
        }
        return descriptors.toArray(new PropertyDescriptor[descriptors.size()]);
    }
    
    
    public static final class Property {
        private final String description;
        private final String name;
        
        public Property(String name, String description) {
            this.description = description;
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
