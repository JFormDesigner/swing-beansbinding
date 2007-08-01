/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import java.beans.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ConcurrentModificationException;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class JToggleButtonSelectedProperty extends AbstractProperty<Boolean> implements Property<Boolean> {

    private Object source;
    private JToggleButton cachedComponent;
    private Object cachedValue;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;

    private static final Object NOREAD = new Object();

    public JToggleButtonSelectedProperty() {
    }

    public JToggleButtonSelectedProperty(JToggleButton component) {
        this.source = component;
    }

    public JToggleButtonSelectedProperty(Property<? extends JToggleButton> property) {
        this.source = property;
    }

    public void setSource(JToggleButton component) {
        setSource0(component);
    }

    public void setSource(Property<? extends JToggleButton> property) {
        setSource0(property);
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

    public Class<Boolean> getWriteType() {
        JToggleButton component;
        
        if (isListening()) {
            validateCache(-1);
            component = cachedComponent;
        } else {
            component = getJToggleButtonFromSource(true);
        }

        if (component == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return Boolean.class;
    }

    public Boolean getValue() {
        if (isListening()) {
            validateCache(-1);

            if (cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }

            return (Boolean)cachedValue;
        }

        JToggleButton comp = getJToggleButtonFromSource(true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return comp.isSelected();
    }

    public void setValue(Boolean value) {
        JToggleButton component;

        if (isListening()) {
            validateCache(-1);
            component = cachedComponent;
        } else {
            component = getJToggleButtonFromSource(true);
        }

        if (component == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        try {
            ignoreChange = true;
            component.setSelected(value);
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

        return (getJToggleButtonFromSource(true) != null);
    }

    public boolean isWriteable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsWriteable();
        }

        return (getJToggleButtonFromSource(true) != null);
    }

    private JToggleButton getJToggleButtonFromSource(boolean logErrors) {
        if (source == null) {
            if (logErrors) {
                System.err.println(hashCode() + ": LOG: getJToggleButtonFromSource(): source is null");
            }
            return null;
        }

        if (source instanceof Property) {
            Property<? extends JToggleButton> prop = (Property<? extends JToggleButton>)source;
            if (!prop.isReadable()) {
                if (logErrors) {
                    System.err.println(hashCode() + ": LOG: getJToggleButtonFromSource(): unreadable source property");
                }
                return null;
            }

            JToggleButton slider = prop.getValue();
            if (slider == null) {
                if (logErrors) {
                    System.err.println(hashCode() + ": LOG: getJToggleButtonFromSource(): source property returned null");
                }
                return null;
            }

            return slider;
        }

        return (JToggleButton)source;
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
                cachedComponent.removeItemListener(changeHandler);
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
        if (flag != 0 && getJToggleButtonFromSource(false) != cachedComponent) {
            System.err.println(hashCode() + ": LOG: validateCache(): concurrent modification");
        }

        if (flag != 1) {
            Object value = (cachedComponent == null ? NOREAD : cachedComponent.isSelected());
            if (cachedValue != value && (cachedValue == null || !cachedValue.equals(value))) {
                System.err.println(hashCode() + ": LOG: validateCache(): concurrent modification");
            }
        }
    }

    private void updateCachedComponent() {
        JToggleButton comp = getJToggleButtonFromSource(true);

        if (comp != cachedComponent) {
            if (cachedComponent != null) {
                cachedComponent.removeItemListener(changeHandler);
            }

            cachedComponent = comp;
            
            if (cachedComponent != null) {
                cachedComponent.addItemListener(getChangeHandler());
            }
        }
    }

    private void updateCachedValue() {
        cachedValue = (cachedComponent == null ? NOREAD : cachedComponent.isSelected());
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

    private void checkBoxValueChanged() {
        if (ignoreChange) {
            return;
        }

        validateCache(1);
        Object oldValue = cachedValue;
        updateCachedValue();
        notifyListeners(cachedIsWriteable(), oldValue);
    }

    public String toString() {
        return "JToggleButton.value";
    }

    private ChangeHandler getChangeHandler() {
        if (changeHandler ==  null) {
            changeHandler = new ChangeHandler();
        }
        return changeHandler;
    }
    
    private final class ChangeHandler implements PropertyStateListener, ItemListener {
        public void propertyStateChanged(PropertyStateEvent pe) {
            bindingPropertyChanged(pe);
        }

        public void itemStateChanged(ItemEvent ie) {
            checkBoxValueChanged();
        }
    }

}
