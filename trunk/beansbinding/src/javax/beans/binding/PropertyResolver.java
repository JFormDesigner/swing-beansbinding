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
public abstract class PropertyResolver<S>  {

    public void setSource(S source) {
        throw new UnsupportedOperationException();
    }

    public S getSource() {
        throw new UnsupportedOperationException();
    }

    public abstract Class<?> getValueType();

    public abstract Object getValue();

    public void setValue(Object value) {
        throw new UnsupportedOperationException();
    }

    public boolean isWriteable() {
        return false;
    }

    public boolean isCompletePath() {
        return true;
    }

    public boolean isObservable() {
        return false;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {}

    public void removePropertyChangeListener(PropertyChangeListener listener) {}

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return new PropertyChangeListener[0];
    }

}
