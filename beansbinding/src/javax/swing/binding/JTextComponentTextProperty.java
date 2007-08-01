/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.ConcurrentModificationException;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class JTextComponentTextProperty extends AbstractProperty<String> implements SourceableProperty<JTextComponent, String> {

    private Object source;
    private JTextComponent cachedComponent;
    private Document cachedDocument;
    private Object cachedValue;
    private Object liveValue;
    private boolean cachedIsWriteable;
    private ChangeHandler changeHandler;
    private boolean ignoreChange;
    private boolean inDocumentListener;
    private boolean installedFilter;
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

    public JTextComponent getSource() {
        if (isListening()) {
            validateCache(-1);
            return cachedComponent;
        }

        return getJTextComponentFromSource(false);
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

            updateEntireCache();
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
                if (inDocumentListener) {
                    throw new IllegalStateException("Not yet sure how to handle change from document listener");
                } else {
                    cachedComponent.setText(value);
                    Object oldValue = cachedValue;
                    updateLiveValue();
                    updateCachedValue();
                    notifyListeners(cachedIsWriteable, oldValue);
                }
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

        updateEntireCache();
    }

    protected final void listeningStopped() {
        if (changeHandler != null) {
            uninstallComponentListeners();

            if (source instanceof Property) {
                ((Property)source).removePropertyStateListener(changeHandler);
            }
        }

        cachedComponent = null;
        liveValue = null;
        cachedValue = null;
        cachedIsWriteable = false;
        changeHandler = null;
    }

    // flag -1 - validate all
    // flag  0 - source property changed value or readability
    // flag  1 - document changed
    // flag  2 - value changed
    // flag  3 - editability of text field changed
    // 
    private void validateCache(int flag) {
        if (flag != 0 && getJTextComponentFromSource(false) != cachedComponent) {
            System.err.println(hashCode() + ": LOG: validateCache(): concurrent modification");
        }

        Object value;
        boolean writeable;
        Document document;

        if (cachedComponent == null) {
            value = NOREAD;
            writeable = false;
            document = null;
        } else {
            value = cachedComponent.getText();
            writeable = cachedComponent.isEditable();
            document = cachedComponent.getDocument();
        }

        if (flag != 1 && cachedDocument != document && (cachedDocument == null || !cachedDocument.equals(document))) {
            System.err.println(hashCode() + ": LOG: validateCache(): concurrent modification");
        }

        if (flag != 2 && liveValue != value && (liveValue == null || !liveValue.equals(value))) {
            System.err.println(hashCode() + ": LOG: validateCache(): concurrent modification");
        }

        if (flag != 3 && writeable != cachedIsWriteable) {
            System.err.println(hashCode() + ": LOG: validateCache(): concurrent modification");
        }
    }

    private void updateCachedDocument() {
        Document d = (cachedComponent == null ? null : cachedComponent.getDocument());

        if (d != cachedDocument) {
            uninstallDocumentListener();
            cachedDocument = d;
            installDocumentListener();
        }
    }

    private void updateCachedComponent() {
        JTextComponent comp = getJTextComponentFromSource(true);

        if (comp != cachedComponent) {
            uninstallComponentListeners();
            cachedComponent = comp;
            installComponentListeners();
        }
    }

    private void installDocumentListener() {
        if (cachedDocument == null) {
            return;
        }

        boolean useDocumentFilter = !(cachedComponent instanceof JFormattedTextField);

        if (useDocumentFilter && (cachedDocument instanceof AbstractDocument) &&
                                 ((AbstractDocument)cachedDocument).getDocumentFilter() == null) {
            ((AbstractDocument)cachedDocument).setDocumentFilter(getChangeHandler());
            installedFilter = true;
        } else {
            cachedDocument.addDocumentListener(getChangeHandler());
            installedFilter = false;
        }
    }

    private void uninstallDocumentListener() {
        if (cachedDocument == null) {
            return;
        }

        if (installedFilter) {
            AbstractDocument ad = (AbstractDocument)cachedDocument;
            if (ad.getDocumentFilter() == changeHandler) {
                ad.setDocumentFilter(null);
            }
        } else {
            cachedDocument.removeDocumentListener(changeHandler);
        }
    }
    
    private void installComponentListeners() {
        if (cachedComponent == null) {
            return;
        }

        cachedComponent.addPropertyChangeListener(getChangeHandler());

        if (strategy != ChangeStrategy.ON_TYPE) {
            cachedComponent.addFocusListener(getChangeHandler());
        }

        if (strategy == ChangeStrategy.ON_ACTION_OR_FOCUS_LOST
                                   && (cachedComponent instanceof JTextField)) {

            ((JTextField)cachedComponent).addActionListener(getChangeHandler());
        }
    }

    private void uninstallComponentListeners() {
        if (cachedComponent == null) {
            return;
        }

        cachedComponent.removePropertyChangeListener(changeHandler);

        if (strategy != ChangeStrategy.ON_TYPE) {
            cachedComponent.removeFocusListener(changeHandler);
        }

        if (strategy == ChangeStrategy.ON_ACTION_OR_FOCUS_LOST
                                   && (cachedComponent instanceof JTextField)) {

            ((JTextField)cachedComponent).removeActionListener(changeHandler);
        }
    }

    private void updateEntireCache() {
        updateCachedComponent();
        updateCachedDocument();
        updateLiveValue();
        updateCachedValue();
        updateCachedIsWriteable();
    }
    
    private void updateLiveValue() {
        liveValue = (cachedComponent == null ? NOREAD : cachedComponent.getText());
    }

    private void updateCachedValue() {
        cachedValue = liveValue;
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
        updateCachedDocument();
        updateLiveValue();
        updateCachedValue();
        updateCachedIsWriteable();
        notifyListeners(wasWriteable, oldValue);
    }

    private void textComponentEditabilityChanged() {
        validateCache(3);
        boolean wasWriteable = cachedIsWriteable;
        updateCachedIsWriteable();
        notifyListeners(wasWriteable, cachedValue);
    }

    private void documentChanged() {
        validateCache(1);
        updateCachedDocument();
        updateLiveValue();
        Object oldValue = cachedValue;
        updateCachedValue();
        notifyListeners(cachedIsWriteable, oldValue);
    }

    private void actionWasPerformed() {
        validateCache(-1);
        Object oldValue = cachedValue;
        updateCachedValue();
        notifyListeners(cachedIsWriteable, oldValue);
    }
    
    private void focusWasLost() {
        validateCache(-1);
        Object oldValue = cachedValue;
        updateCachedValue();
        notifyListeners(cachedIsWriteable, oldValue);
    }

    private void documentTextChanged() {
        try {
            inDocumentListener = true;
            textChanged();
        } finally {
            inDocumentListener = false;
        }
    }

    private void textChanged() {
        if (ignoreChange) {
            return;
        }

        validateCache(2);
        updateLiveValue();
        if (strategy == ChangeStrategy.ON_TYPE) {
            Object oldValue = cachedValue;
            updateCachedValue();
            notifyListeners(cachedIsWriteable, oldValue);
        }
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
    
    private final class ChangeHandler extends DocumentFilter implements
                        ActionListener, DocumentListener, FocusListener,
                        PropertyChangeListener, PropertyStateListener {

        public void propertyStateChanged(PropertyStateEvent pe) {
            bindingPropertyChanged(pe);
        }

        public void propertyChange(PropertyChangeEvent pce) {
            String name = pce.getPropertyName();

            if (name == "editable") {
                textComponentEditabilityChanged();
            } else if (name == "document") {
                documentChanged();
            }
        }

        public void actionPerformed(ActionEvent e) {
            actionWasPerformed();
        }

        public void focusLost(FocusEvent e) {
            if (!e.isTemporary()) {
                focusWasLost();
            }
        }

        public void insertUpdate(DocumentEvent e) {
            documentTextChanged();
        }

        public void removeUpdate(DocumentEvent e) {
            documentTextChanged();
        }

        public void replace(DocumentFilter.FilterBypass fb, int offset,
                            int length, String text, AttributeSet attrs)
                            throws BadLocationException {

            super.replace(fb, offset, length, text, attrs);
            textChanged();
        }

        public void insertString(DocumentFilter.FilterBypass fb, int offset,
                                 String string, AttributeSet attr)
                                 throws BadLocationException {

            super.insertString(fb, offset, string, attr);
            textChanged();
        }

        public void remove(DocumentFilter.FilterBypass fb, int offset, int length)
                           throws BadLocationException {

            super.remove(fb, offset, length);
            textChanged();
        }

        public void focusGained(FocusEvent e) {}
        public void changedUpdate(DocumentEvent e) {}

    }

}
