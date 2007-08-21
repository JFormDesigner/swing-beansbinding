/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
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

    private Property tableTargetProperty;
    private List list;
    private Binding binding;

    public ElementsProperty(Property tableTargetProperty) {
        super(true);

        if (tableTargetProperty == null) {
            throw new IllegalArgumentException("can't have table target property");
        }

        this.tableTargetProperty = tableTargetProperty;
    }

    public T getComponent() {
        assert binding != null;
        return (T)tableTargetProperty.getValue(binding.getTargetObject());
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
               tableTargetProperty.isReadable(binding.getTargetObject()) &&
               tableTargetProperty.getValue(binding.getTargetObject()) != null;
    }

    public boolean isWriteable(TS source) {
        return binding != null &&
               tableTargetProperty.isReadable(binding.getTargetObject()) &&
               tableTargetProperty.getValue(binding.getTargetObject()) != null;
    }

    public String toString() {
        return "JTable.elements";
    }

    private static boolean isReadableSourceValue(Object value) {
        return value != null && value != PropertyStateEvent.UNREADABLE;
    }

    void installBinding(Binding binding) {
        if (this.binding != null) {
            throw new IllegalStateException();
        }

        this.binding = binding;

        if (this.isListening(null)) {
            // someone outside the binding has installed listeners
            // should be nice and notify them
            if (isReadable(null)) {
                PropertyStateEvent pse = new PropertyStateEvent(this, null, true, PropertyStateEvent.UNREADABLE, null, true, true);
            }
        }

        tableTargetProperty.addPropertyStateListener(binding.getTargetObject(), this);
    }

    void uninstallBinding() {
        if (this.binding == null) {
            throw new IllegalStateException();
        }

        boolean wasReadable = isReadable(null);
        List oldValue = getValue(null);

        this.binding = null;

        if (this.isListening(null)) {
            // someone outside the binding has installed listeners
            // should be nice and notify them
            if (wasReadable) {
                PropertyStateEvent pse = new PropertyStateEvent(this, null, true, oldValue, PropertyStateEvent.UNREADABLE, true, false);
            }
        }

        tableTargetProperty.removePropertyStateListener(binding.getTargetObject(), this);
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
