/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import javax.swing.text.*;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class JTextComponentTextProperty extends AbstractProperty<String> implements Property<String> {

    private Object source;

    public JTextComponentTextProperty() {
    }

    public JTextComponentTextProperty(JTextComponent component) {
        source = component;
    }

    public JTextComponentTextProperty(Property<? extends JTextComponent> property) {
        source = property;
    }

    public void setSource(JTextComponent component) {
        source = component;
    }

    public void setSource(Property<? extends JTextComponent> property) {
        source = property;
    }

    public Class<String> getWriteType() {
        // if not writeable - throw error
        return String.class;
    }

    public String getValue() {
        return null;
    }

    public void setValue(String value) {
        // set value if writeable
    }

    public boolean isReadable() {
        return false;
    }

    public boolean isWriteable() {
        return false;
    }

    protected void listeningStarted() {
    }

    protected void listeningStopped() {
    }

    public String toString() {
        return "JTextComponent.text";
    }
}
