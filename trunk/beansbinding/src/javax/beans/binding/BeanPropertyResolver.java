/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;
import javax.beans.binding.ext.PropertyDelegateFactory;

public final class BeanPropertyResolver<S, V> implements PropertyResolver<S, V> {

    private Class<S> sourceClass;
    private S source;
    private final PropertyPath path;
    private PropertyChangeSupport support;
    private boolean isListening = false;


    //private final Object[] sources;
    //private final boolean emptySourcePath;
    //private boolean bound;
    //private ChangeHandler changeHandler;
    //private boolean ignoreChange;
    //private Delegate delegate;

    public BeanPropertyResolver(Class<S> sourceClass, String path, S source) {
        if (sourceClass == null) {
            throw new IllegalArgumentException("Must supply source class.");
        }

        this.sourceClass = sourceClass;
        this.path = PropertyPath.createPropertyPath(path);
        this.source = source;

        if (path.length() > 0) {
            sources = new Object[path.length()];
            sources[0] = source;
            emptySourcePath = false;
        } else {
            sources = new Object[] { source };
            emptySourcePath = true;
        }
    }

    public void setSource(S source) {
        boolean wasListening = isListening;
        V oldValue = null;

        if (wasListening) {
            stopListening();
            oldValue = getValue();
        }

        this.source = source;

        if (wasListening) {
            startListening();
            firePropertyChange(oldValue, getValue());
        }
    }

    public S getSource() {
        return source;
    }

    public Class<S> getSourceType() {
        return (Class<S>)sourceClass.getClass();
    }

    public Class<? extends V> getValueType() {
        return null;
    }

    public V getValue() {
        return null;
    }

    public void setValue(V value) {
    }

    public boolean isReadable() {
        return false;
    }

    public boolean isWriteable() {
        return false;
    }

    public boolean isObservable() {
        return false;
    }

    public boolean isCompletePath() {
        return false;
    }

    private void startListening() {
        isListening = true;
    }

    private void stopListening() {
        isListening = false;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (support == null) {
            support = new PropertyChangeSupport(this);
        }

        support.addPropertyChangeListener(listener);

        if (!isListening && getPropertyChangeListeners().length != 0) {
            startListening();
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (support == null) {
            return;
        }

        support.removePropertyChangeListener(listener);

        if (isListening && getPropertyChangeListeners().length == 0) {
            stopListening();
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (support == null) {
            return new PropertyChangeListener[0];
        }

        return support.getPropertyChangeListeners();
    }

    protected void firePropertyChange(Object oldValue, Object newValue) {
        if (support == null || support.getPropertyChangeListeners().length == 0) {
            return;
        }

        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }

        support.firePropertyChange("value", oldValue, newValue);
    }

    public String toString() {
        StringBuilder ret = new StringBuilder(sourceClass.getName());
        if (path != null) {
            ret.append('.').append(path);
        }
        return ret.toString();
    }

}
