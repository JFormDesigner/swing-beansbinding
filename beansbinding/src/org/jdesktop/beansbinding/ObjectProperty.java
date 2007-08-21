/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

import java.util.List;
import java.util.ArrayList;
import static org.jdesktop.beansbinding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 */
public final class ObjectProperty<S> extends Property<S, S> {

    public static <S> ObjectProperty<S> create() {
        return new ObjectProperty<S>();
    }

    private ObjectProperty() {}

    public Class<? extends S> getWriteType(S source) {
        throw new IllegalStateException("Unwriteable");
    }

    public S getValue(S source) {
        return source;
    }

    public void setValue(S source, S value) {
        throw new IllegalStateException("Unwriteable");
    }

    public boolean isReadable(Object source) {
        return true;
    }

    public boolean isWriteable(Object source) {
        return false;
    }

    public String toString() {
        return getClass().getName();
    }

    public void addPropertyStateListener(S source, PropertyStateListener listener) {}
    public void removePropertyStateListener(S source, PropertyStateListener listener) {}
    public PropertyStateListener[] getPropertyStateListeners(S source) {
        return new PropertyStateListener[0];
    }

}
