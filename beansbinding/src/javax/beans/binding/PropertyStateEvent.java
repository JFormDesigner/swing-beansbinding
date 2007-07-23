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

    private final boolean valueChanged;
    private final Object oldValue;
    private final Object newValue;
    private final boolean writeableChanged;
    private boolean isWriteable;

    public PropertyStateEvent(Property<?> source,
                              boolean valueChanged,
                              Object oldValue,
                              Object newValue,
                              boolean writeableChanged,
                              boolean isWriteable) {

        super(source);

        if (!writeableChanged && !valueChanged) {
            throw new IllegalArgumentException("Nothing has changed");
        }

        this.valueChanged = valueChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.writeableChanged = writeableChanged;
        this.isWriteable = isWriteable;
    }

    public Property<?> getSource() {
        return (Property<?>)source;
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

    public boolean getReadableChanged() {
        return valueChanged && oldValue != newValue && (oldValue == UNREADABLE || newValue == UNREADABLE);
    }

    public boolean isReadable() {
        return newValue != UNREADABLE;
    }

    public boolean getWriteableChanged() {
        return writeableChanged;
    }

    public boolean isWriteable() {
        return isWriteable;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(getClass().getName());

        buffer.append(": Property ").append(getSource()).append(" changed:\n");
        
        if (getValueChanged()) {
            buffer.append("    value changed from ").append(getOldValue()).append(" to ").append(getNewValue()).append('\n');
        }
        
        if (getReadableChanged()) {
            buffer.append("    readable changed from ").append(!isReadable()).append(" to ").append(isReadable()).append('\n');
        }

        if (getWriteableChanged()) {
            buffer.append("    writeable changed from ").append(!isWriteable()).append(" to ").append(isWriteable()).append('\n');
        }

        buffer.deleteCharAt(buffer.length() - 1);

        return buffer.toString();
    }

}
