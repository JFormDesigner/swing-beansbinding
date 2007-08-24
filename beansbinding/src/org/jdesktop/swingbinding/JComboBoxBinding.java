/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.ObjectProperty;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.beansbinding.PropertyStateListener;
import org.jdesktop.swingbinding.impl.AbstractColumnBinding;
import org.jdesktop.swingbinding.impl.ListBindingManager;
import org.jdesktop.swingbinding.impl.*;

/**
 * @author Shannon Hickey
 */
public final class JComboBoxBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {

    private ElementsProperty<TS, JComboBox> ep;
    private Handler handler = new Handler();
    private BindingComboBoxModel model;
    private JComboBox combo;
    private DetailBinding detailBinding;
    private IDBinding idBinding;

    protected JComboBoxBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JComboBox> targetJComboBoxProperty, String name) {
        super(strategy, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JComboBox>(targetJComboBoxProperty), name);
        ep = (ElementsProperty<TS, JComboBox>)getTargetProperty();
        setDetailBinding(null);
    }

    protected void bindImpl() {
        model = new BindingComboBoxModel(detailBinding, idBinding);
        // order is important for the next two lines
        ep.addPropertyStateListener(null, handler);
        ep.installBinding(this);
        super.bindImpl();
    }

    protected void unbindImpl() {
        // order is important for the next two lines
        ep.uninstallBinding();
        ep.removePropertyStateListener(null, handler);
        model = null;
        super.unbindImpl();
    }

    public DetailBinding setDetailBinding(Property<E, ?> detailProperty) {
        return setDetailBinding(detailProperty, null);
    }

    public DetailBinding setDetailBinding(Property<E, ?> detailProperty, String name) {
        throwIfBound();

        if (name == null && JComboBoxBinding.this.getName() != null) {
            name = JComboBoxBinding.this.getName() + ".DETAIL_BINDING";
        }

        detailBinding = detailProperty == null ?
                        new DetailBinding(ObjectProperty.<E>create(), name) :
                        new DetailBinding(detailProperty, name);
        return detailBinding;
    }

    public IDBinding setIDBinding(Property<E, ?> IDProperty) {
        return setIDBinding(IDProperty, null);
    }

    public IDBinding setIDBinding(Property<E, ?> IDProperty, String name) {
        throwIfBound();

        if (name == null && JComboBoxBinding.this.getName() != null) {
            name = JComboBoxBinding.this.getName() + ".ID_BINDING";
        }

        idBinding = IDProperty == null ?
                    new IDBinding(ObjectProperty.<E>create(), name) :
                    new IDBinding(IDProperty, name);
        return idBinding;
    }
    
    public DetailBinding getDetailBinding() {
        return detailBinding;
    }

    public IDBinding getIDBinding() {
        return idBinding;
    }
    
    private final Property DETAIL_PROPERTY = new Property() {
        public Class<Object> getWriteType(Object source) {
            return Object.class;
        }

        public Object getValue(Object source) {
            throw new UnsupportedOperationException();
        }

        public void setValue(Object source, Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean isReadable(Object source) {
            throw new UnsupportedOperationException();
        }

        public boolean isWriteable(Object source) {
            return true;
        }

        public void addPropertyStateListener(Object source, PropertyStateListener listener) {
            throw new UnsupportedOperationException();
        }

        public void removePropertyStateListener(Object source, PropertyStateListener listener) {
            throw new UnsupportedOperationException();
        }

        public PropertyStateListener[] getPropertyStateListeners(Object source) {
            throw new UnsupportedOperationException();
        }
    };

    public final class IDBinding extends AbstractColumnBinding {
        public IDBinding(Property<E, ?> IDProperty, String name) {
            super(0, IDProperty, DETAIL_PROPERTY, name);
        }
    }
    
    public final class DetailBinding extends AbstractColumnBinding {
        public DetailBinding(Property<E, ?> detailProperty, String name) {
            super(0, detailProperty, DETAIL_PROPERTY, name);
        }
    }

    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (!pse.getValueChanged()) {
                return;
            }

            Object newValue = pse.getNewValue();

            if (newValue == PropertyStateEvent.UNREADABLE) {
                combo.setModel(new DefaultComboBoxModel());
                combo = null;
                model.setElements(null);
            } else {
                combo = ep.getComponent();
                model.setElements((List<E>)newValue);
                combo.setModel(model);
            }
        }
    }

}
