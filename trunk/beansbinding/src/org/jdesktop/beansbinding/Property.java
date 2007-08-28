/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

/**
 * {@code Property} defines a uniform way to access the value of a property.
 * A typical {@code Property} implemention is an immutable representation
 * of a way to access some property derived from a source object. As such,
 * all methods of this class take a source object as an argument.
 * <p>
 * A {@code Property} implementation may, however, be designed such that the
 * {@code Property} itself stores the property value. In such a case, the
 * {@code Property} implementation may ignore the source object. {@code Property}
 * implementations should clearly document their behavior in this regard.
 *
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
