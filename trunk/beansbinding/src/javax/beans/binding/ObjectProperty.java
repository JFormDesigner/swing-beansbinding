/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.List;
import java.util.ArrayList;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 */
public final class ObjectProperty<S> implements SourceableProperty<S, S> {

    private S source;
    private List<PropertyStateListener> listeners;
    private boolean isListening = false;

    public ObjectProperty() {
        this(null);
    }

    public ObjectProperty(S source) {
        this.source = source;
    }

    public void setSource(S source) {
        S oldSource = source;

        this.source = source;

        if (isListening) {
            notifyListeners(oldSource);
        }
    };

    public S getSource() {
        return source;
    }

    public Class<? extends S> getWriteType() {
        throw new IllegalStateException("Unwriteable");
    }

    public S getValue() {
        return source;
    }

    public void setValue(S value) {
        throw new IllegalStateException("Unwriteable");
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isWriteable() {
        return false;
    }

    private void startListening() {
        isListening = true;
    }

    private void stopListening() {
        isListening = false;
    }

    public void addPropertyStateListener(PropertyStateListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<PropertyStateListener>(1);
        }

        listeners.add(listener);

        if (!isListening && listeners.size() != 0) {
            startListening();
        }
    }

    public void removePropertyStateListener(PropertyStateListener listener) {
        if (listeners == null) {
            return;
        }

        listeners.remove(listener);

        if (isListening && listeners.size() == 0) {
            stopListening();
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

    private boolean didValueChange(Object oldValue, Object newValue) {
        return oldValue == null || newValue == null || !oldValue.equals(newValue);
    }

    private void notifyListeners(Object oldValue) {
        if (listeners == null || listeners.size() == 0) {
            return;
        }

        boolean valueChanged = didValueChange(oldValue, source);

        if (!valueChanged) {
            return;
        }

        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        true,
                                                        oldValue,
                                                        source,
                                                        false,
                                                        false);

        for (PropertyStateListener listener : listeners) {
            listener.propertyStateChanged(pse);
        }
    }

    public String toString() {
        return source == null ? "null" : source.toString();
    }

}
