/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import java.beans.*;
import javax.swing.text.*;
import java.util.ConcurrentModificationException;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class JTextComponentTextProperty extends AbstractProperty<String> implements Property<String> {

    private Object source;
    private JTextComponent cachedComponent;
    private Object cachedValue;
    private boolean cachedIsWriteable;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;
    private ChangeStrategy strategy = ChangeStrategy.ON_ACTION_OR_FOCUS_LOST;

    public enum ChangeStrategy {
        ON_TYPE,
        ON_ACTION_OR_FOCUS_LOST,
        ON_FOCUS_LOST
    };

    private static final Object NOREAD = new Object();

    public JTextComponentTextProperty() {
    }

    public JTextComponentTextProperty(JTextComponent component) {
        this.source = component;
    }

    public JTextComponentTextProperty(Property<? extends JTextComponent> property) {
        this.source = property;
    }

    public void setChangeStrategy(ChangeStrategy strategy) {
        if (isListening()) {
            throw new IllegalStateException("Can't change strategy when being observed");
        }
        this.strategy = strategy;
    }

    public ChangeStrategy getChangeStrategy() {
        return strategy;
    }

    public void setSource(JTextComponent component) {
        setSource0(component);
    }

    public void setSource(Property<? extends JTextComponent> property) {
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
            updateCachedIsWriteable();
        }
    }
    
    public Class<String> getWriteType() {
        if (isListening()) {
            validateCache(-1);

            if (!cachedIsWriteable) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            return String.class;
        }

        JTextComponent comp = getJTextComponentFromSource(true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unwriteable");
        } else if (!comp.isEditable()) {
            System.err.println(hashCode() + ": LOG: getWriteType(): target JTextComponent is non-editable");
            throw new UnsupportedOperationException("Unwriteable");
        }

        return String.class;
    }

    public String getValue() {
        if (isListening()) {
            validateCache(-1);

            if (cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }

            return (String)cachedValue;
        }

        JTextComponent comp = getJTextComponentFromSource(true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return comp.getText();
    }

    public void setValue(String value) {
        if (isListening()) {
            validateCache(-1);

            if (!cachedIsWriteable) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            try {
                ignoreChange = true;
                cachedComponent.setText(value);
                Object oldValue = cachedValue;
                updateCachedValue();
                notifyListeners(cachedIsWriteable, oldValue);
            } finally {
                ignoreChange = false;
            }
        }

        JTextComponent comp = getJTextComponentFromSource(true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unwriteable");
        } else if (!comp.isEditable()) {
            System.err.println(hashCode() + ": LOG: setValue(): target JTextComponent is non-editable");
            throw new UnsupportedOperationException("Unwriteable");
        }

        try {
            ignoreChange = true;
            comp.setText(value);
        } finally {
            ignoreChange = false;
        }
    }

    public boolean isReadable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsReadable();
        }

        JTextComponent comp = getJTextComponentFromSource(true);
        return comp != null;
    }

    public boolean isWriteable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsWriteable;
        }

        JTextComponent comp = getJTextComponentFromSource(true);
        if (comp == null) {
            return false;
        } else if (!comp.isEditable()) {
            System.err.println(hashCode() + ": LOG: isWriteable(): target JTextComponent is non-editable");
            return false;
        }

        return true;
    }

    private JTextComponent getJTextComponentFromSource(boolean logErrors) {
        if (source == null) {
            if (logErrors) {
                System.err.println(hashCode() + ": LOG: getJTextComponentFromSource(): source is null");
            }
            return null;
        }

        if (source instanceof Property) {
            Property<? extends JTextComponent> prop = (Property<? extends JTextComponent>)source;
            if (!prop.isReadable()) {
                if (logErrors) {
                    System.err.println(hashCode() + ": LOG: getJTextComponentFromSource(): unreadable source property");
                }
                return null;
            }

            JTextComponent tc = prop.getValue();
            if (tc == null) {
                if (logErrors) {
                    System.err.println(hashCode() + ": LOG: getJTextComponentFromSource(): source property returned null");
                }
                return null;
            }

            return tc;
        }

        return (JTextComponent)source;
    }

    protected final void listeningStarted() {
        if (source instanceof Property) {
            ((Property)source).addPropertyStateListener(getChangeHandler());
        }

        updateCachedComponent();
        updateCachedValue();
        updateCachedIsWriteable();
    }

    protected final void listeningStopped() {
        if (changeHandler != null) {

            // unregister listener on text component

            if (source instanceof Property) {
                ((Property)source).removePropertyStateListener(changeHandler);
            }

            if (cachedComponent != null) {
                cachedComponent.removePropertyChangeListener("editable", changeHandler);
            }
        }

        cachedComponent = null;
        cachedValue = null;
        cachedIsWriteable = false;
        changeHandler = null;
    }

    // flag -1 - validate all
    // level 0 - source property changed value or readability
    // level 1 - value changed
    // level 2 - editability of text field changed
    private void validateCache(int flag) {
        // PENDING(shannonh) - enable this via a property
        //if (true) {
        //    return;
        //}

        if (flag != 0 && getJTextComponentFromSource(false) != cachedComponent) {
            throw new ConcurrentModificationException();
        }

        Object value;
        boolean writeable;

        if (cachedComponent == null) {
            value = NOREAD;
            writeable = false;
        } else {
            value = cachedComponent.getText();
            writeable = cachedComponent.isEditable();
        }

        if (flag != 1 && cachedValue != value && (cachedValue == null || !cachedValue.equals(value))) {
            throw new ConcurrentModificationException();
        }

        if (flag != 2 && writeable != cachedIsWriteable) {
            throw new ConcurrentModificationException();
        }
    }

    private void updateCachedComponent() {
        JTextComponent comp = getJTextComponentFromSource(true);

        if (comp != cachedComponent) {
            if (cachedComponent != null) {
                cachedComponent.removePropertyChangeListener("editable", changeHandler);
            }

            // unregister listener on old

            cachedComponent = comp;
            
            if (cachedComponent != null) {
                cachedComponent.addPropertyChangeListener("editable", getChangeHandler());
            }

            // register listener on new
        }
    }

    private void updateCachedValue() {
        cachedValue = (cachedComponent == null ? NOREAD : cachedComponent.getText());
    }

    private void updateCachedIsWriteable() {
        cachedIsWriteable = (cachedComponent == null ? false : cachedComponent.isEditable());
    }

    private boolean cachedIsReadable() {
        return cachedValue != NOREAD;
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
        boolean writeableChanged = (wasWriteable != cachedIsWriteable);

        if (!valueChanged && !writeableChanged) {
            return;
        }
        
        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        cachedIsWriteable);

        this.firePropertyStateChange(pse);
    }

    private void bindingPropertyChanged(PropertyStateEvent pse) {
        boolean valueChanged = pse.getValueChanged() || pse.getReadableChanged();
        validateCache(0);
        Object oldValue = cachedValue;
        boolean wasWriteable = cachedIsWriteable;
        updateCachedComponent();
        updateCachedValue();
        updateCachedIsWriteable();
        notifyListeners(wasWriteable, oldValue);
    }

    private void textComponentEditabilityChanged() {
        validateCache(2);
        boolean wasWriteable = cachedIsWriteable;
        updateCachedIsWriteable();
        notifyListeners(wasWriteable, cachedValue);
    }
    
    public String toString() {
        return "JTextComponent.text";
    }

    private ChangeHandler getChangeHandler() {
        if (changeHandler ==  null) {
            changeHandler = new ChangeHandler();
        }
        return changeHandler;
    }
    
    private final class ChangeHandler implements PropertyStateListener, PropertyChangeListener {
        public void propertyStateChanged(PropertyStateEvent pe) {
            bindingPropertyChanged(pe);
        }

        public void propertyChange(PropertyChangeEvent pce) {
            if (pce.getPropertyName() == "editable") {
                textComponentEditabilityChanged();
            }
        }
    }

}
