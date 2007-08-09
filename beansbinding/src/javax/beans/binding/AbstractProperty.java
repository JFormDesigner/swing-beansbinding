/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.*;

/**
 * @author Shannon Hickey
 */
public abstract class AbstractProperty<S, V> implements Property<S, V> {

    private IdentityHashMap<S, List<PropertyStateListener>> map = new IdentityHashMap<S, List<PropertyStateListener>>();

    public abstract Class<? extends V> getWriteType(S source);

    public abstract V getValue(S source);

    public abstract void setValue(S source, V value);

    public abstract boolean isReadable(S source);

    public abstract boolean isWriteable(S source);

    protected void listeningStarted(S source) {
    }

    protected void listeningStopped(S source) {
    }

    public final void addPropertyStateListener(S source, PropertyStateListener listener) {
        if (listener == null) {
            return;
        }

        List<PropertyStateListener>listeners = map.get(source);
        boolean wasListening;

        if (listeners == null) {
            wasListening = false;
            listeners = new ArrayList<PropertyStateListener>();
            map.put(source, listeners);
        } else {
            wasListening = (listeners.size() != 0);
        }

        listeners.add(listener);

        if (!wasListening) {
            listeningStarted(source);
        }
    }

    public final void removePropertyStateListener(S source, PropertyStateListener listener) {
        if (listener == null) {
            return;
        }

        List<PropertyStateListener>listeners = map.get(source);

        if (listeners == null) {
            return;
        }

        boolean wasListening = (listeners.size() != 0);

        listeners.remove(listener);

        if (wasListening && listeners.size() == 0) {
            listeningStopped(source);
        }
    }

    public final PropertyStateListener[] getPropertyStateListeners(S source) {
        List<PropertyStateListener>listeners = map.get(source);

        if (listeners == null) {
            return new PropertyStateListener[0];
        }

        PropertyStateListener[] ret = new PropertyStateListener[listeners.size()];
        ret = listeners.toArray(ret);
        return ret;
    }

    protected final void firePropertyStateChange(PropertyStateEvent pse) {
        List<PropertyStateListener>listeners = map.get(pse.getSourceObject());

        if (listeners == null) {
            return;
        }

        for (PropertyStateListener listener : listeners) {
            listener.propertyStateChanged(pse);
        }
    }

    public final boolean isListening(S source) {
         List<PropertyStateListener>listeners = map.get(source);
         return listeners != null && listeners.size() != 0;
    }

}
