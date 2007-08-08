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

    private boolean disableTargetOnUnwriteableSource = true;
    private boolean originalEnabled;

    public SwingBinding(Property<S> source, Property<T> target) {
        super(source, target);
    }

    public SwingBinding(String name, Property<S> source, Property<T> target) {
        super(name, source, target);
    }

    public void setDisableTargetOnUnwriteableSource(boolean disableTargetOnUnwriteableSource) {
        throwIfBound();
        this.disableTargetOnUnwriteableSource = disableTargetOnUnwriteableSource;
    }

    public boolean getDisableTargetOnUnwriteableSource() {
        return disableTargetOnUnwriteableSource;
    }

    protected void bindImpl() {
        super.bindImpl();
        if (disableTargetOnUnwriteableSource && !getSource().isWriteable()) {
            JComponent comp = getComponentSource(getTarget());
            if (comp != null) {
                originalEnabled = comp.isEnabled();
                comp.setEnabled(false);
            }
        }
    }

    protected void unbindImpl() {
        super.unbindImpl();
        if (disableTargetOnUnwriteableSource) {
            JComponent comp = getComponentSource(getTarget());
            if (comp != null) {
                comp.setEnabled(originalEnabled);
            }
        }
    }

    public static JComponent getComponentSource(Property prop) {
        if (!(prop instanceof SourceableProperty)) {
            return null;
        }

        Object value = ((SourceableProperty)prop).getSource();

        if (value instanceof JComponent) {
            return (JComponent)value;
        }

        if (value instanceof Property) {
            return getComponentSource((Property)value);
        }

        return null;
    }
}
