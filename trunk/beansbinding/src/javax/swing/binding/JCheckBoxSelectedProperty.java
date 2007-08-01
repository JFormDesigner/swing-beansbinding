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
public final class JCheckBoxSelectedProperty extends AbstractProperty<Boolean> implements Property<Boolean> {

    private Object source;
    private JCheckBox cachedComponent;
    private Object cachedValue;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;

    private static final Object NOREAD = new Object();

    public JCheckBoxSelectedProperty() {
    }

    public JCheckBoxSelectedProperty(JCheckBox component) {
        this.source = component;
    }

    public JCheckBoxSelectedProperty(Property<? extends JCheckBox> property) {
        this.source = property;
    }

    public void setSource(JCheckBox component) {
        setSource0(component);
    }

    public void setSource(Property<? extends JCheckBox> property) {
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
        JCheckBox component;
        
        if (isListening()) {
            validateCache(-1);
            component = cachedComponent;
        } else {
            component = getJCheckBoxFromSource(true);
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

        JCheckBox comp = getJCheckBoxFromSource(true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return comp.isSelected();
    }

    public void setValue(Boolean value) {
        JCheckBox component;

        if (isListening()) {
            validateCache(-1);
            component = cachedComponent;
        } else {
            component = getJCheckBoxFromSource(true);
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

        return (getJCheckBoxFromSource(true) != null);
    }

    public boolean isWriteable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsWriteable();
        }

        return (getJCheckBoxFromSource(true) != null);
    }

    private JCheckBox getJCheckBoxFromSource(boolean logErrors) {
        if (source == null) {
            if (logErrors) {
                System.err.println(hashCode() + ": LOG: getJCheckBoxFromSource(): source is null");
            }
            return null;
        }

        if (source instanceof Property) {
            Property<? extends JCheckBox> prop = (Property<? extends JCheckBox>)source;
            if (!prop.isReadable()) {
                if (logErrors) {
                    System.err.println(hashCode() + ": LOG: getJCheckBoxFromSource(): unreadable source property");
                }
                return null;
            }

            JCheckBox slider = prop.getValue();
            if (slider == null) {
                if (logErrors) {
                    System.err.println(hashCode() + ": LOG: getJCheckBoxFromSource(): source property returned null");
                }
                return null;
            }

            return slider;
        }

        return (JCheckBox)source;
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
    // level 0 - source property changed value or readability
    // level 1 - value changed
    private void validateCache(int flag) {
        // PENDING(shannonh) - enable this via a property
        //if (true) {
        //    return;
        //}

        if (flag != 0 && getJCheckBoxFromSource(false) != cachedComponent) {
            throw new ConcurrentModificationException();
        }

        if (flag != 1) {
            Object value = (cachedComponent == null ? NOREAD : cachedComponent.isSelected());
            if (cachedValue != value && (cachedValue == null || !cachedValue.equals(value))) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private void updateCachedComponent() {
        JCheckBox comp = getJCheckBoxFromSource(true);

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
        return "JCheckBox.value";
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
