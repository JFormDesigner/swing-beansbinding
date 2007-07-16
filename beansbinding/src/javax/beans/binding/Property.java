/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.PropertyChangeListener;

/**
 * @author Shannon Hickey
 */
public interface Property<S, V>  {

    void setSource(S source);

    S getSource();

    Class<? extends V> getValueType();

    V getValue();

    void setValue(V value);

    boolean isReadable();

    boolean isWriteable();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

    PropertyChangeListener[] getPropertyChangeListeners();
    
    PropertyChangeListener[] getPropertyChangeListeners(String propertyName);

    String toString();

}
