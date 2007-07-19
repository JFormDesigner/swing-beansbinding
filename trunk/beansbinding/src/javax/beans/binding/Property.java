/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * @author Shannon Hickey
 */
public interface Property<V>  {

    Class<? extends V> getValueType();

    V getValue();

    void setValue(V value);

    boolean isReadable();

    boolean isWriteable();

    void addPropertyStateListener(PropertyStateListener<? super V> listener);

    void removePropertyStateListener(PropertyStateListener<? super V> listener);

    PropertyStateListener<? super V>[] getPropertyStateListeners();

    String toString();

}
