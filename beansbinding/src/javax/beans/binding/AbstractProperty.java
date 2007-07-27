/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.*;

/**
 * @author Shannon Hickey
 */
public abstract class AbstractProperty<V> implements Property<V> {

    private List<PropertyStateListener> listeners;
    private boolean listening;

    public abstract Class<? extends V> getWriteType();

    public abstract V getValue();

    public abstract void setValue(V value);

    public abstract boolean isReadable();

    public abstract boolean isWriteable();

    protected abstract void listeningStarted();
    
    protected abstract void listeningStopped();
    
    public void addPropertyStateListener(PropertyStateListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<PropertyStateListener>(1);
        }

        listeners.add(listener);

        if (!listening && listeners.size() != 0) {
            listening = true;
            listeningStarted();
        }
    }

    public void removePropertyStateListener(PropertyStateListener listener) {
        if (listeners == null) {
            return;
        }

        listeners.remove(listener);

        if (listening && listeners.size() == 0) {
            listening = false;
            listeningStopped();
        }
    }

    public PropertyStateListener[] getPropertyStateListeners() {
        if (listeners == null) {
            return new PropertyStateListener[0];
        }

        PropertyStateListener[] ret = new PropertyStateListener[listeners.size()];
        ret = listeners.toArray(ret);
        return ret;
    }

    protected void firePropertyStateChange(PropertyStateEvent pse) {
        if (listeners == null) {
            return;
        }

        for (PropertyStateListener listener : listeners) {
            listener.propertyStateChanged(pse);
        }
    }

    public final boolean isListening() {
        return listening;
    }

    public abstract String toString();

}
