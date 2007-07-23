/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import javax.beans.OLDBINDING.ext.PropertyDelegateProvider;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;

/**
 *
 * @author sky
 */
public final class SwingPropertyDelegateProvider extends PropertyDelegateProvider {
    // PENDING: create defines for all possible properties

    public boolean providesDelegate(Class<?> type, String property) {
        property = property.intern();
        if (JCheckBox.class.isAssignableFrom(type)) {
            return (property == AbstractBindingHelper.JCHECK_BOX_SELECTED_P);
        }
        if (JComboBox.class.isAssignableFrom(type)) {
            return (property == AbstractBindingHelper.ELEMENTS_P ||
                    property == AbstractBindingHelper.SELECTED_ELEMENT_P ||
                    property == AbstractBindingHelper.JCOMBO_BOX_SELECTED_ELEMENT_P);
        }
        if (JList.class.isAssignableFrom(type)) {
            return (property == AbstractBindingHelper.ELEMENTS_P || 
                    property == AbstractBindingHelper.SELECTED_ELEMENTS_P ||
                    property == AbstractBindingHelper.SELECTED_ELEMENT_P);
        }
        if (JSlider.class.isAssignableFrom(type)) {
            return (property == AbstractBindingHelper.VALUE_P);
        }
        if (JTable.class.isAssignableFrom(type)) {
            return (property == AbstractBindingHelper.ELEMENTS_P || 
                    property == AbstractBindingHelper.SELECTED_ELEMENTS_P ||
                    property == AbstractBindingHelper.SELECTED_ELEMENT_P);
        }
        if (JTextComponent.class.isAssignableFrom(type)) {
            return property == AbstractBindingHelper.JTEXT_COMPONENT_TEXT_P;
        }
        if (JTree.class.isAssignableFrom(type)) {
            return (property == AbstractBindingHelper.JTREE_ROOT_P || 
                    property == AbstractBindingHelper.SELECTED_ELEMENTS_P ||
                    property == AbstractBindingHelper.SELECTED_ELEMENT_P);
        }
        return false;
    }
    
    public Object createPropertyDelegate(Object source, String property) {
        property = property.intern();
        AbstractBindingHelper helper = null;
        if (source instanceof JCheckBox) {
            if (property == AbstractBindingHelper.JCHECK_BOX_SELECTED_P) {
                helper = new JCheckBoxBindingHelper((JCheckBox)source);
            }
        } else if (source instanceof JComboBox) {
            if (property == AbstractBindingHelper.ELEMENTS_P ||
                    property == AbstractBindingHelper.SELECTED_ELEMENT_P ||
                    property == AbstractBindingHelper.JCOMBO_BOX_SELECTED_ELEMENT_P) {
                helper = new JComboBoxBindingHelper((JComboBox)source);
            }
        } else if (source instanceof JList) {
            if (property == AbstractBindingHelper.ELEMENTS_P || 
                    property == AbstractBindingHelper.SELECTED_ELEMENTS_P ||
                    property == AbstractBindingHelper.SELECTED_ELEMENT_P) {
                helper = new JListBindingHelper((JList)source);
            }
        } else if (source instanceof JSlider) {
            if (property == AbstractBindingHelper.VALUE_P) {
                helper = new JSliderBindingHelper((JSlider)source);
            }
        } else if (source instanceof JTable) {
            if (property == AbstractBindingHelper.ELEMENTS_P || 
                    property == AbstractBindingHelper.SELECTED_ELEMENTS_P ||
                    property == AbstractBindingHelper.SELECTED_ELEMENT_P) {
                helper = new JTableBindingHelper((JTable)source);
            }
        } else if (source instanceof JTextComponent) {
            if (property == AbstractBindingHelper.JTEXT_COMPONENT_TEXT_P) {
                helper = new JTextComponentBindingHelper((JTextComponent)source,
                        !(source instanceof JFormattedTextField));
            }
        } else if (source instanceof JTree) {
            if (property == AbstractBindingHelper.JTREE_ROOT_P || 
                    property == AbstractBindingHelper.SELECTED_ELEMENTS_P ||
                    property == AbstractBindingHelper.SELECTED_ELEMENT_P) {
                helper = new JTreeBindingHelper((JTree)source);
            }
        }
        if (helper != null) {
            return helper.getPropertyDelegate();
        }
        return null;
    }

    public Class<?> getPropertyDelegateClass(Class<?> type) {
        if (JCheckBox.class.isAssignableFrom(type)) {
            return JCheckBoxBindingHelper.PropertyDelegate.class;
        }
        if (JComboBox.class.isAssignableFrom(type)) {
            return JComboBoxBindingHelper.PropertyDelegate.class;
        }
        if (JList.class.isAssignableFrom(type)) {
            return JListBindingHelper.PropertyDelegate.class;
        }
        if (JSlider.class.isAssignableFrom(type)) {
            return JSliderBindingHelper.PropertyDelegate.class;
        }
        if (JTable.class.isAssignableFrom(type)) {
            return JTableBindingHelper.PropertyDelegate.class;
        }
        if (JTextComponent.class.isAssignableFrom(type)) {
            return JTextComponentBindingHelper.PropertyDelegate.class;
        }
        if (JTree.class.isAssignableFrom(type)) {
            return JTreeBindingHelper.PropertyDelegate.class;
        }
        return null;
    }
}
