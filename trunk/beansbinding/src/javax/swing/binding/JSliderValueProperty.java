/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.ConcurrentModificationException;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class JSliderValueProperty extends AbstractProperty<Integer> implements SourceableProperty<JSlider, Integer> {

    private Object source;
    private JSlider cachedComponent;
    private Object cachedValue;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;

    private static final Object NOREAD = new Object();

    public JSliderValueProperty() {
    }

    public JSliderValueProperty(JSlider component) {
        this.source = component;
    }

    public JSliderValueProperty(Property<? extends JSlider> property) {
        this.source = property;
    }

    public void setSource(JSlider component) {
        setSource0(component);
    }

    public void setSource(Property<? extends JSlider> property) {
        setSource0(property);
    }

    public JSlider getSource() {
        if (isListening()) {
            validateCache(-1);
            return cachedComponent;
        }

        return getJSliderFromSource(false);
    }

    private void setSource0(Object object) {
        if (isListening()) {
            validateCache(-1);

            if (source instanceof Property) {
                ((Property)source).removePropertyStateListener(changeHandler);
            }
        }

        this.source = source;

        if (isListening()) {
            if (source instanceof Property) {
                ((Property)source).addPropertyStateListener(getChangeHandler());
            }

            updateCachedComponent();
            updateCachedValue();
        }
    }
    
    public Class<Integer> getWriteType() {
        JSlider component;
        
        if (isListening()) {
            validateCache(-1);
            component = cachedComponent;
        } else {
            component = getJSliderFromSource(true);
        }

        if (component == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return Integer.class;
    }

    public Integer getValue() {
        if (isListening()) {
            validateCache(-1);

            if (cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }

            return (Integer)cachedValue;
        }

        JSlider comp = getJSliderFromSource(true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return comp.getValue();
    }

    public void setValue(Integer value) {
        JSlider component;

        if (isListening()) {
            validateCache(-1);
            component = cachedComponent;
        } else {
            component = getJSliderFromSource(true);
        }

        if (component == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        try {
            ignoreChange = true;
            component.setValue(value);
            if (isListening()) {
                Object oldValue = cachedValue;
                updateCachedValue();
                notifyListeners(cachedIsWriteable(), oldValue);
            }
        } finally {
            ignoreChange = false;
        }
    }

    public boolean isReadable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsReadable();
        }

        return (getJSliderFromSource(true) != null);
    }

    public boolean isWriteable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsWriteable();
        }

        return (getJSliderFromSource(true) != null);
    }

    private JSlider getJSliderFromSource(boolean logErrors) {
        if (source == null) {
            if (logErrors) {
                System.err.println(hashCode() + ": LOG: getJSliderFromSource(): source is null");
            }
            return null;
        }

        if (source instanceof Property) {
            Property<? extends JSlider> prop = (Property<? extends JSlider>)source;
            if (!prop.isReadable()) {
                if (logErrors) {
                    System.err.println(hashCode() + ": LOG: getJSliderFromSource(): unreadable source property");
                }
                return null;
            }

            JSlider slider = prop.getValue();
            if (slider == null) {
                if (logErrors) {
                    System.err.println(hashCode() + ": LOG: getJSliderFromSource(): source property returned null");
                }
                return null;
            }

            return slider;
        }

        return (JSlider)source;
    }

    protected final void listeningStarted() {
        if (source instanceof Property) {
            ((Property)source).addPropertyStateListener(getChangeHandler());
        }

        updateCachedComponent();
        updateCachedValue();
    }

    protected final void listeningStopped() {
        if (changeHandler != null) {

            if (cachedComponent != null) {
                cachedComponent.removeChangeListener(changeHandler);
            }

            if (source instanceof Property) {
                ((Property)source).removePropertyStateListener(changeHandler);
            }
        }

        cachedComponent = null;
        cachedValue = null;
        changeHandler = null;
    }

    // flag -1 - validate all
    // flag  0 - source property changed value or readability
    // flag  1 - value changed
    private void validateCache(int flag) {
        if (flag != 0 && getJSliderFromSource(false) != cachedComponent) {
            System.err.println(hashCode() + ": LOG: validateCache(): concurrent modification");
        }

        if (flag != 1) {
            Object value = (cachedComponent == null ? NOREAD : cachedComponent.getValue());
            if (cachedValue != value && (cachedValue == null || !cachedValue.equals(value))) {
                System.err.println(hashCode() + ": LOG: validateCache(): concurrent modification");
            }
        }
    }

    private void updateCachedComponent() {
        JSlider comp = getJSliderFromSource(true);

        if (comp != cachedComponent) {
            if (cachedComponent != null) {
                cachedComponent.removeChangeListener(changeHandler);
            }

            cachedComponent = comp;
            
            if (cachedComponent != null) {
                cachedComponent.addChangeListener(getChangeHandler());
            }
        }
    }

    private void updateCachedValue() {
        cachedValue = (cachedComponent == null ? NOREAD : cachedComponent.getValue());
    }

    private boolean cachedIsReadable() {
        return cachedValue != NOREAD;
    }

    private boolean cachedIsWriteable() {
        return cachedComponent != null;
    }

    private boolean didValueChange(Object oldValue, Object newValue) {
        return oldValue == null || newValue == null || !oldValue.equals(newValue);
    }

    private Object toUNREADABLE(Object src) {
        return src == NOREAD ? UNREADABLE : src;
    }

    private void notifyListeners(boolean wasWriteable, Object oldValue) {
        PropertyStateListener[] listeners = getPropertyStateListeners();

        if (listeners == null || listeners.length == 0) {
            return;
        }

        oldValue = toUNREADABLE(oldValue);
        Object newValue = toUNREADABLE(cachedValue);
        boolean valueChanged = didValueChange(oldValue, newValue);
        boolean writeableChanged = (wasWriteable != cachedIsWriteable());

        if (!valueChanged && !writeableChanged) {
            return;
        }
        
        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        cachedIsWriteable());

        this.firePropertyStateChange(pse);
    }

    private void bindingPropertyChanged(PropertyStateEvent pse) {
        boolean valueChanged = pse.getValueChanged() || pse.getReadableChanged();
        validateCache(0);
        Object oldValue = cachedValue;
        boolean wasWriteable = cachedIsWriteable();
        updateCachedComponent();
        updateCachedValue();
        notifyListeners(wasWriteable, oldValue);
    }

    private void sliderValueChanged() {
        if (ignoreChange) {
            return;
        }

        validateCache(1);
        Object oldValue = cachedValue;
        updateCachedValue();
        notifyListeners(cachedIsWriteable(), oldValue);
    }

    public String toString() {
        return "JSlider.value";
    }

    private ChangeHandler getChangeHandler() {
        if (changeHandler ==  null) {
            changeHandler = new ChangeHandler();
        }
        return changeHandler;
    }
    
    private final class ChangeHandler implements PropertyStateListener, ChangeListener {
        public void propertyStateChanged(PropertyStateEvent pe) {
            bindingPropertyChanged(pe);
        }

        public void stateChanged(ChangeEvent ce) {
            sliderValueChanged();
        }
    }

}
