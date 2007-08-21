/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beans.binding;

/**
 * @author Shannon Hickey
 */
public abstract class Property<S, V> {

    public abstract Class<? extends V> getWriteType(S source);

    public abstract V getValue(S source);

    public abstract void setValue(S source, V value);

    public abstract boolean isReadable(S source);

    public abstract boolean isWriteable(S source);

    public abstract void addPropertyStateListener(S source, PropertyStateListener listener);

    public abstract void removePropertyStateListener(S source, PropertyStateListener listener);

    public abstract PropertyStateListener[] getPropertyStateListeners(S source);

}
