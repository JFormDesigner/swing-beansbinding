/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import java.util.*;
import javax.swing.*;

/**
 * @author Shannon Hickey
 */
class ElementsProperty<T extends JComponent> extends AbstractProperty<List> implements PropertyStateListener {

    private Property<? extends T> componentProperty;
    private List list;

    public ElementsProperty(Property<? extends T> componentProperty) {
        if (componentProperty == null) {
            throw new IllegalArgumentException("can't have null property");
        }

        this.componentProperty = componentProperty;
        componentProperty.addPropertyStateListener(this);
    }

    public T getComponent() {
        if (!isReadable()) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return (T)componentProperty.getValue();
    }

    public Class<List> getWriteType() {
        if (!isWriteable()) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return (Class<List>)List.class;
    }

    public List getValue() {
        if (!isReadable()) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return list;
    }

    public void setValue(List list) {
        if (!isWriteable()) {
            throw new UnsupportedOperationException("Unreadable");
        }

        if (this.list == list) {
            return;
        }

        List old = this.list;
        this.list = list;

        PropertyStateEvent pse = new PropertyStateEvent(this, true, old, list, false, true);
        firePropertyStateChange(pse);
    }

    public boolean isReadable() {
        return componentProperty.isReadable() && componentProperty.getValue() != null;
    }

    public boolean isWriteable() {
        return componentProperty.isReadable() && componentProperty.getValue() != null;
    }

    public String toString() {
        return "JTable.elements";
    }

    private boolean isReadableSourceValue(Object value) {
        return value != null && value != PropertyStateEvent.UNREADABLE;
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
            PropertyStateEvent ps = new PropertyStateEvent(this, true, old, PropertyStateEvent.UNREADABLE, true, false);
            firePropertyStateChange(ps);
            return;
        }

        if (!wasReadableSource && isReadableSource) {
            PropertyStateEvent ps = new PropertyStateEvent(this, true, PropertyStateEvent.UNREADABLE, null, true, true);
            firePropertyStateChange(ps);
            return;
        }

        PropertyStateEvent ps = new PropertyStateEvent(this, true, old, null, false, true);
        firePropertyStateChange(ps);
    }

}
