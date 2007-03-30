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
public class TestBean {
    private final PropertyChangeSupport support;
    private Object value;
    private Object value2;
    private String stringProperty;
    private Integer bigIntProperty;
    private Boolean bigBooleanProperty;
    private char charProperty;
    private boolean booleanProperty;
    private int intProperty;
    private float floatProperty;
    
    private int valueSetCount;
    
    public TestBean() {
        support = new PropertyChangeSupport(this);
    }
    
    public void setFloatProperty(float value) {
        float oldValue = this.floatProperty;
        this.floatProperty = value;
        support.firePropertyChange("floatProperty", oldValue, value);
    }
    
    public float getFloatProperty() {
        return floatProperty;
    }
    
    public void setCharProperty(char value) {
        char oldValue = this.charProperty;
        this.charProperty = value;
        support.firePropertyChange("charProperty", oldValue, value);
    }
    
    public char getCharProperty() {
        return charProperty;
    }
    
    public void setStringProperty(String value) {
        String oldValue = this.stringProperty;
        this.stringProperty = value;
        support.firePropertyChange("stringProperty", oldValue, value);
    }
    
    public String getStringProperty() {
        return stringProperty;
    }
    
    public void setIntProperty(int value) {
        int oldValue = this.intProperty;
        this.intProperty = value;
        support.firePropertyChange("intProperty", oldValue, value);
    }
    
    public int getIntProperty() {
        return intProperty;
    }
    
    public void setBooleanProperty(boolean value) {
        boolean oldValue = this.booleanProperty;
        this.booleanProperty = value;
        support.firePropertyChange("booleanProperty", oldValue, value);
    }
    
    public boolean getBooleanProperty() {
        return booleanProperty;
    }
    
    public void setBigIntProperty(Integer value) {
        Integer oldValue = this.bigIntProperty;
        this.bigIntProperty = value;
        support.firePropertyChange("bigIntProperty", oldValue, value);
    }
    
    public Integer getBigIntProperty() {
        return bigIntProperty;
    }
    
    public void setBigBooleanProperty(Boolean value) {
        Boolean oldValue = this.bigBooleanProperty;
        this.bigBooleanProperty = value;
        support.firePropertyChange("bigBooleanProperty", oldValue, value);
    }
    
    public Boolean getBigBooleanProperty() {
        return bigBooleanProperty;
    }
    
    public int getAndClearValueSetCount() {
        int oldValue = valueSetCount;
        valueSetCount = 0;
        return oldValue;
    }

    public void setValue(Object value) {
        valueSetCount++;
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
    
    public void addPropertyChangeListener(String property,
            PropertyChangeListener listener) {
        support.addPropertyChangeListener(property, listener);
    }
    
    public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
        support.removePropertyChangeListener(property, listener);
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
