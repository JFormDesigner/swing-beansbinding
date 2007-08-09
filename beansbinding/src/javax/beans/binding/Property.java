/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * @author Shannon Hickey
 */
public interface Property<S, V> {

    Class<? extends V> getWriteType(S source);

    V getValue(S source);

    void setValue(S source, V value);

    boolean isReadable(S source);

    boolean isWriteable(S source);

    void addPropertyStateListener(S source, PropertyStateListener listener);

    void removePropertyStateListener(S source, PropertyStateListener listener);

    PropertyStateListener[] getPropertyStateListeners(S source);

}
