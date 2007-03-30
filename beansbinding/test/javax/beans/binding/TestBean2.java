/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author sky
 */
public class TestBean2 {
    private final PropertyChangeSupport support;
    private Object value;

    private Object value2;
    
    public TestBean2() {
        support = new PropertyChangeSupport(this);
    }
    
    public void setValue(Object value) {
        Object oldValue = this.value;
        this.value = value;
        support.firePropertyChange("value", oldValue, value);
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setValue2(Object value) {
        Object oldValue = this.value2;
        this.value2 = value;
        support.firePropertyChange("value2", oldValue, value);
    }
    
    public Object getValue2() {
        return value2;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public int getPropetyChangeListenerCount() {
        return support.getPropertyChangeListeners().length;
    }
    
    public String toString() {
        return super.toString() + "[value=" + value + "]";
    }

    // PURELY for testing.
    void setValue0(String string) {
        this.value = string;
    }
}
