/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;
import java.util.List;
import javax.beans.binding.Binding;
import javax.beans.binding.ext.PropertyDelegateProvider;
import javax.swing.JCheckBox;

/**
 *
 * @author sky
 */
class JCheckBoxBindingHelper extends AbstractBindingHelper {
    private final JCheckBox checkBox;
    private final PropertyDelegate delegate;
    private final ItemListener itemListener;
    private Binding.BindingController controller;

    public JCheckBoxBindingHelper(JCheckBox checkBox) {
        this.checkBox = checkBox;
        this.delegate = new PropertyDelegate();
        itemListener = new ItemHandler();
    }

    protected boolean shouldCreateBindingTarget(String property) {
        return (property == JCHECK_BOX_SELECTED_P);
    }

    public void bind(Binding.BindingController controller, String property) {
        throwIfNonNull(this.controller);
        assert (property == JCHECK_BOX_SELECTED_P);
        this.controller = controller;
        updateComponentEnabledFromBinding();
        checkBox.addItemListener(itemListener);
    }

    public void unbind(Binding.BindingController controller, String property) {
        checkBox.removeItemListener(itemListener);
        this.controller = null;
    }

    public Object getPropertyDelegate() {
        return delegate;
    }

    public void sourceValueStateChanged(Binding.BindingController controller, 
            String property) {
        updateComponentEnabledFromBinding();
    }

    protected Binding getBinding() {
        return controller.getBinding();
    }
    
    protected Component getComponent() {
        return checkBox;
    }
    
    
    public final class PropertyDelegate extends DelegateBase {
        public void setSelected(boolean selected) {
            checkBox.setSelected(selected);
        }
        
        public boolean isSelected() {
            return checkBox.isSelected();
        }
        
        private void fireSelectedChanged() {
            firePropertyChange(JCHECK_BOX_SELECTED_P, null, null);
        }
    }
    

    // PENDING: nuke this once BeanInfoAPT is working
    public static final class PropertyDelegateBeanInfo extends BindingBeanInfo {
        protected Class<?> getPropertyDelegateClass() {
            return PropertyDelegate.class;
        }

        protected Property[] getPreferredProperties() {
            return new Property[] {
                new Property("selected", "Whether the check box is selected")
            };
        }
    }
    
    
    private final class ItemHandler implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            delegate.fireSelectedChanged();
        }
    }
}
