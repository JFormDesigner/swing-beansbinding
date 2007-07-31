/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import javax.swing.text.*;
import java.util.ConcurrentModificationException;

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

    private static final Object NOREAD = new Object();

    public JTextComponentTextProperty() {
    }

    public JTextComponentTextProperty(JTextComponent component) {
        this.source = component;
    }

    public JTextComponentTextProperty(Property<? extends JTextComponent> property) {
        this.source = property;
    }

    public void setSource(JTextComponent component) {
        this.source = component;
    }

    public void setSource(Property<? extends JTextComponent> property) {
        this.source = property;
    }

    public Class<String> getWriteType() {
        if (isListening()) {
            validateCache(-1);

            if (!cachedIsWriteable) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            return String.class;
        }

        JTextComponent comp = getJTextComponentFromSource();
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
                throw new UnsupportedOperationException("Unwriteable");
            }

            return (String)cachedValue;
        }

        JTextComponent comp = getJTextComponentFromSource();
        if (comp == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return comp.getText();
    }

    public void setValue(String value) {
        if (isListening()) {
            validateCache(-1);

            if (!cachedIsWriteable) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            cachedComponent.setText(value);
        }

        JTextComponent comp = getJTextComponentFromSource();
        if (comp == null) {
            throw new UnsupportedOperationException("Unwriteable");
        } else if (!comp.isEditable()) {
            System.err.println(hashCode() + ": LOG: setValue(): target JTextComponent is non-editable");
            throw new UnsupportedOperationException("Unwriteable");
        }

        comp.setText(value);
    }

    public boolean isReadable() {
        if (isListening()) {
            validateCache(-1);
            return cachedValue != NOREAD;
        }

        JTextComponent comp = getJTextComponentFromSource();
        return comp != null;
    }

    public boolean isWriteable() {
        if (isListening()) {
            validateCache(-1);
            return cachedIsWriteable;
        }

        JTextComponent comp = getJTextComponentFromSource();
        if (comp == null) {
            return false;
        } else if (!comp.isEditable()) {
            System.err.println(hashCode() + ": LOG: isWriteable(): target JTextComponent is non-editable");
            return false;
        }

        return true;
    }

    private JTextComponent getJTextComponentFromSource() {
        if (source == null) {
            System.err.println(hashCode() + ": LOG: getJTextComponentFromSource(): source is null");
            return null;
        }

        if (source instanceof Property) {
            Property<? extends JTextComponent> prop = (Property<? extends JTextComponent>)source;
            if (!prop.isReadable()) {
                System.err.println(hashCode() + ": LOG: getJTextComponentFromSource(): unreadable source property");
                return null;
            }

            JTextComponent tc = prop.getValue();
            if (tc == null) {
                System.err.println(hashCode() + ": LOG: getJTextComponentFromSource(): source property returned null");
                return null;
            }

            return tc;
        }

        return (JTextComponent)source;
    }

    protected final void listeningStarted() {
        if (isListening()) {
            return;
        }

        updateCachedComponent();
        updateCachedValue();
        updateCachedIsWriteable();
    }

    private void validateCache(int ignore) {
        // PENDING(shannonh) - enable this via a property
        //if (true) {
//            return;
//        }

        JTextComponent comp = getJTextComponentFromSource();

        if (comp != getJTextComponentFromSource()) {
            throw new ConcurrentModificationException();
        }

        Object value;
        boolean writeable;

        if (comp == null) {
            value = NOREAD;
            writeable = false;
        } else {
            value = comp.getText();
            writeable = comp.isEditable();
        }

        if (value != cachedValue) {
            throw new ConcurrentModificationException();
        }

        if (writeable != cachedIsWriteable) {
            throw new ConcurrentModificationException();
        }
    }
    
    private void updateCachedComponent() {
        JTextComponent comp;

        comp = getJTextComponentFromSource();

        if (comp != cachedComponent) {
            // unregister listener on old
            cachedComponent = comp;
            // register listener on new
        }
    }

    private void updateCachedValue() {
        cachedValue = (cachedComponent == null ? NOREAD : cachedComponent.getText());
    }

    private void updateCachedIsWriteable() {
        cachedIsWriteable = (cachedComponent == null ? true : cachedComponent.isEditable());
    }

    protected final void listeningStopped() {
        if (!isListening()) {
            return;
        }

        if (changeHandler != null) {
            // unregister listener on text component
            cachedComponent = null;
        }

        cachedValue = null;
        cachedIsWriteable = false;
        changeHandler = null;
    }

    public void bindingPropertyChanged(PropertyStateEvent pse) {
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
    
    private final class ChangeHandler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pe) {
            bindingPropertyChanged(pe);
        }
    }

}
