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

    private Property<? extends JTextComponent> property;
    private JTextComponent component;

    public JTextComponentTextProperty() {
    }

    public JTextComponentTextProperty(JTextComponent component) {
        this.component = component;
    }

    public JTextComponentTextProperty(Property<? extends JTextComponent> property) {
        this.property = property;
    }

    public void setSource(JTextComponent component) {
        this.property = null;
        this.component = component;
    }

    public void setSource(Property<? extends JTextComponent> property) {
        this.component = null;
        this.property = property;
    }

    public Class<String> getWriteType() {
        JTextComponent comp = getComponent();
        if (comp == null || !comp.isEditable()) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return String.class;
    }

    public String getValue() {
        JTextComponent comp = getComponent();
        if (comp == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return comp.getText();
    }

    public void setValue(String value) {
        JTextComponent comp = getComponent();
        if (comp == null || !comp.isEditable()) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        comp.setText(value);
    }

    public boolean isReadable() {
        JTextComponent comp = getComponent();
        return comp != null;
    }

    public boolean isWriteable() {
        JTextComponent comp = getComponent();
        return comp != null && comp.isEditable();
    }

    private JTextComponent getComponent() {
        if (property == null) {
            return component;
        } else if (property.isReadable()) {
            return property.getValue();
        } else {
            return null;
        }
    }

    protected void listeningStarted() {
    }

    protected void listeningStopped() {
    }

    public String toString() {
        return "JTextComponent.text";
    }
}
