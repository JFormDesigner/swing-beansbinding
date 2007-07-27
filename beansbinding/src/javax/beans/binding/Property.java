/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * @author Shannon Hickey
 */
public interface Property<V> {

    Class<? extends V> getWriteType();

    V getValue();

    void setValue(V value);

    boolean isReadable();

    boolean isWriteable();

    void addPropertyStateListener(PropertyStateListener listener);

    void removePropertyStateListener(PropertyStateListener listener);

    PropertyStateListener[] getPropertyStateListeners();

    String toString();

}
