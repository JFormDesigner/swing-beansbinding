/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import java.util.*;
import javax.swing.*;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyHelper;
import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.beansbinding.PropertyStateListener;

/**
 * @author Shannon Hickey
 */
class ElementsProperty<TS, T extends JComponent> extends PropertyHelper<TS, List> implements PropertyStateListener {

    private Property targetProperty;
    private List list;
    private Binding binding;

    public ElementsProperty(Property targetProperty) {
        super(true);

        if (targetProperty == null) {
            throw new IllegalArgumentException("can't have null target property");
        }

        this.targetProperty = targetProperty;
    }

    public T getComponent() {
        assert binding != null;
        return (T)targetProperty.getValue(binding.getTargetObject());
    }

    public Class<List> getWriteType(TS source) {
        if (!isWriteable(source)) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return (Class<List>)List.class;
    }

    public List getValue(TS source) {
        if (!isReadable(source)) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return list;
    }

    public void setValue(TS source, List list) {
        if (!isWriteable(source)) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        if (this.list == list) {
            return;
        }

        List old = this.list;
        this.list = list;

        PropertyStateEvent pse = new PropertyStateEvent(this, null, true, old, list, false, true);
        firePropertyStateChange(pse);
    }

    public boolean isReadable(TS source) {
        return binding != null &&
               targetProperty.isReadable(binding.getTargetObject()) &&
               targetProperty.getValue(binding.getTargetObject()) != null;
    }

    public boolean isWriteable(TS source) {
        return isReadable(source);
    }

    public String toString() {
        return "elements";
    }

    private static boolean isReadableSourceValue(Object value) {
        return value != null && value != PropertyStateEvent.UNREADABLE;
    }

    void installBinding(Binding binding) {
        if (this.binding != null) {
            throw new IllegalStateException();
        }

        this.binding = binding;
        targetProperty.addPropertyStateListener(binding.getTargetObject(), this);

        if (isReadable(null)) {
            PropertyStateEvent pse = new PropertyStateEvent(this, null, true, PropertyStateEvent.UNREADABLE, null, true, true);
            firePropertyStateChange(pse);
        }
    }

    void uninstallBinding() {
        if (this.binding == null) {
            throw new IllegalStateException();
        }

        boolean wasReadable = isReadable(null);
        List old = list;
        this.binding = null;
        this.list = null;
        targetProperty.removePropertyStateListener(binding.getTargetObject(), this);

        if (wasReadable) {
            PropertyStateEvent pse = new PropertyStateEvent(this, null, true, old, PropertyStateEvent.UNREADABLE, true, false);
            firePropertyStateChange(pse);
        }
    }

    public void propertyStateChanged(PropertyStateEvent pse) {
        if (!pse.getValueChanged()) {
            return;
        }

        boolean wasReadableSource = isReadableSourceValue(pse.getOldValue());
        boolean isReadableSource = isReadableSourceValue(pse.getNewValue());
        Object old = this.list;
        this.list = null;

        if (wasReadableSource && !isReadableSource) {
            PropertyStateEvent ps = new PropertyStateEvent(this, null, true, old, PropertyStateEvent.UNREADABLE, true, false);
            firePropertyStateChange(ps);
            return;
        }

        if (!wasReadableSource && isReadableSource) {
            PropertyStateEvent ps = new PropertyStateEvent(this, null, true, PropertyStateEvent.UNREADABLE, null, true, true);
            firePropertyStateChange(ps);
            return;
        }

        PropertyStateEvent ps = new PropertyStateEvent(this, null, true, old, null, false, true);
        firePropertyStateChange(ps);
    }

}
