/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.*;

/**
 * @author Shannon Hickey
 */
public abstract class PropertyHelper<S, V> extends Property<S, V> {

    private final boolean ignoresSource;
    private Object listeners;

    public PropertyHelper() {
        this(false);
    }

    public PropertyHelper(boolean ignoresSource) {
        this.ignoresSource = ignoresSource;
    }

    private List<PropertyStateListener> getListeners(S source, boolean create) {
        if (ignoresSource) {
            List<PropertyStateListener> list = (List<PropertyStateListener>)listeners;

            if (list == null && create) {
                list = new ArrayList<PropertyStateListener>();
                listeners = list;
            }

            return list;
        }

        IdentityHashMap<S, List<PropertyStateListener>> map = (IdentityHashMap<S, List<PropertyStateListener>>)listeners;

        if (map == null) {
            if (create) {
                map = new IdentityHashMap<S, List<PropertyStateListener>>();
                listeners = map;
            } else {
                return null;
            }
        }

        List<PropertyStateListener> list = map.get(source);
        if (list == null && create) {
            list = new ArrayList<PropertyStateListener>();
            map.put(source, list);
        }

        return list;
    }

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

        List<PropertyStateListener> listeners = getListeners(source, true);
        boolean wasListening = (listeners.size() != 0);
        listeners.add(listener);

        if (!wasListening) {
            listeningStarted(ignoresSource ? null : source);
        }
    }

    public final void removePropertyStateListener(S source, PropertyStateListener listener) {
        if (listener == null) {
            return;
        }

        List<PropertyStateListener> listeners = getListeners(source, false);

        if (listeners == null) {
            return;
        }

        boolean wasListening = (listeners.size() != 0);

        listeners.remove(listener);

        if (wasListening && listeners.size() == 0) {
            listeningStopped(ignoresSource ? null : source);
        }
    }

    public final PropertyStateListener[] getPropertyStateListeners(S source) {
         List<PropertyStateListener> listeners = getListeners(source, false);

        if (listeners == null) {
            return new PropertyStateListener[0];
        }

        PropertyStateListener[] ret = new PropertyStateListener[listeners.size()];
        ret = listeners.toArray(ret);
        return ret;
    }

    protected final void firePropertyStateChange(PropertyStateEvent pse) {
        List<PropertyStateListener> listeners = getListeners((S)pse.getSourceObject(), false);

        if (listeners == null) {
            return;
        }

        for (PropertyStateListener listener : listeners) {
            listener.propertyStateChanged(pse);
        }
    }

    public final boolean isListening(S source) {
         List<PropertyStateListener> listeners = getListeners(source, false);
         return listeners != null && listeners.size() != 0;
    }

}
