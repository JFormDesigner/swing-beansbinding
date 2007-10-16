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
class ElementsProperty<TS> extends PropertyHelper<TS, List> {

    private boolean accessible;
    private List list;

    ElementsProperty() {
        super(true);
    }

    public Class<List> getWriteType(TS source) {
        if (!accessible) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return (Class<List>)List.class;
    }

    public List getValue(TS source) {
        if (!accessible) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return list;
    }

    public void setValue(TS source, List list) {
        if (!accessible) {
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
        return accessible;
    }

    public boolean isWriteable(TS source) {
        return accessible;
    }

    public String toString() {
        return "elements";
    }

    void setAccessible(boolean accessible) {
        if (this.accessible == accessible) {
            return;
        }

        this.accessible = accessible;

        PropertyStateEvent pse;

        if (accessible) {
            pse = new PropertyStateEvent(this, null, true, PropertyStateEvent.UNREADABLE, null, true, true);
        } else {
            Object old = list;
            list = null;
            pse = new PropertyStateEvent(this, null, true, old, PropertyStateEvent.UNREADABLE, true, false);
        }

        firePropertyStateChange(pse);
    }

    boolean isAccessible() {
        return accessible;
    }

}
