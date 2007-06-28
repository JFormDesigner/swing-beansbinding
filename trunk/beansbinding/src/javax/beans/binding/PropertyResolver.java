/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * @author Shannon Hickey
 */
public interface PropertyResolver<S, V>  {

    void setSource(S source);

    S getSource();

    Class<S> getSourceType();

    Class<? extends V> getValueType();

    V getValue();

    void setValue(V value);

    boolean isReadable();

    boolean isWriteable();

    boolean isObservable();

    boolean isCompletePath();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    PropertyChangeListener[] getPropertyChangeListeners();

    String toString();

}
