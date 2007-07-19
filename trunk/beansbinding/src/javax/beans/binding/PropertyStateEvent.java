/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.EventObject;

/**
 * @author Shannon Hickey
 */
public final class PropertyStateEvent<V> extends EventObject {

    private final boolean readableChanged;
    private final boolean writeableChanged;
    private final boolean valueChanged;
    private final V oldValue;
    private final V newValue;

    public PropertyStateEvent(Property<? extends V> source,
                              boolean readableChanged,
                              boolean writeableChanged,
                              boolean valueChanged,
                              V oldValue,
                              V newValue) {

        super(source);

        if (!readableChanged && !writeableChanged && ! valueChanged) {
            throw new IllegalArgumentException("Nothing has changed");
        }

        this.readableChanged = readableChanged;
        this.writeableChanged = writeableChanged;
        this.valueChanged = valueChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Property<? extends V> getSource() {
        return (Property<? extends V>)source;
    }

    public boolean getReadableChanged() {
        return readableChanged;
    }

    public boolean getWriteableChanged() {
        return writeableChanged;
    }

    public boolean getValueChanged() {
        return valueChanged;
    }
    
    public V getOldValue() {
        return oldValue;
    }

    public V getNewValue() {
        return newValue;
    }

}
