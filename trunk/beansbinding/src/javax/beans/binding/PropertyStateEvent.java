/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.EventObject;

/**
 * @author Shannon Hickey
 */
public final class PropertyStateEvent extends EventObject {

    public static final Object UNREADABLE = new StringBuffer("UNREADABLE");

    private final boolean writeableChanged;
    private final boolean valueChanged;
    private final Object oldValue;
    private final Object newValue;

    public PropertyStateEvent(Property<?> source,
                              boolean writeableChanged,
                              boolean valueChanged,
                              Object oldValue,
                              Object newValue) {

        super(source);

        if (!writeableChanged && !valueChanged) {
            throw new IllegalArgumentException("Nothing has changed");
        }

        this.writeableChanged = writeableChanged;
        this.valueChanged = valueChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Property<?> getSource() {
        return (Property<?>)source;
    }

    public boolean getReadableChanged() {
        return valueChanged && oldValue != newValue && (oldValue == UNREADABLE || newValue == UNREADABLE);
    }

    public boolean getWriteableChanged() {
        return writeableChanged;
    }

    public boolean getValueChanged() {
        return valueChanged;
    }
    
    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

}
