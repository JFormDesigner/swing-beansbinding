/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import javax.swing.*;

/**
 * @author Shannon Hickey
 */
public class SwingBinding<S, T> extends Binding<S, T> {

    private JComponent comp;
    private boolean originalEnabled;
    private Handler handler = new Handler();

    public SwingBinding(Property<S> source, Property<T> target) {
        this(null, source, target);
    }

    public SwingBinding(String name, Property<S> source, Property<T> target) {
        super(name, source, target);
    }

    protected void bindImpl() {
        super.bindImpl();
        comp = getBackingComponent(getTarget());
        if (comp != null) {
            originalEnabled = comp.isEnabled();
            comp.setEnabled(getSource().isWriteable());
        }
        getSource().addPropertyStateListener(handler);
        getTarget().addPropertyStateListener(handler);
    }

    protected void unbindImpl() {
        super.unbindImpl();
        if (comp != null) {
            comp.setEnabled(originalEnabled);
            comp = null;
        }
        getSource().removePropertyStateListener(handler);
        getTarget().removePropertyStateListener(handler);
    }

    public static JComponent getBackingComponent(Property prop) {
        if (!(prop instanceof SourceableProperty)) {
            return null;
        }

        Object value = ((SourceableProperty)prop).getSource();

        if (value instanceof JComponent) {
            return (JComponent)value;
        }

        if (value instanceof Property) {
            return getBackingComponent((Property)value);
        }

        return null;
    }

    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (pse.getSource() == getSource()) {
                if (pse.getWriteableChanged()) {
                    if (comp != null) {
                        comp.setEnabled(pse.isWriteable());
                    }
                }
            } else {
                if (pse.getValueChanged()) {
                    if (comp != null) {
                        comp.setEnabled(originalEnabled);
                    }

                    comp = getBackingComponent(getTarget());
                    if (comp != null) {
                        originalEnabled = comp.isEnabled();
                        comp.setEnabled(getSource().isWriteable());
                    }
                }
            }
        }
    }
}
