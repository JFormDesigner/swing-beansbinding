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
import java.util.*;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class JTextComponentTextProperty<S> extends AbstractProperty<S, String> {
    
    private Property<S, ? extends JTextComponent> sourceProperty;
    private IdentityHashMap<S, SourceEntry> map = new IdentityHashMap<S, SourceEntry>();
    private static final Object NOREAD = new Object();
    private ChangeStrategy strategy;
    
    public enum ChangeStrategy {
        ON_TYPE,
        ON_ACTION_OR_FOCUS_LOST,
        ON_FOCUS_LOST
    };
    
    private final class SourceEntry extends DocumentFilter
            implements ActionListener, DocumentListener,
            FocusListener, PropertyChangeListener,
            PropertyStateListener {
        
        private S source;
        private JTextComponent cachedComponent;
        private Document cachedDocument;
        private Object cachedValue;
        private Object liveValue;
        private boolean cachedIsWriteable;
        private boolean ignoreChange;
        private boolean inDocumentListener;
        private boolean installedFilter;
        
        private SourceEntry(S source) {
            this.source = source;
            sourceProperty.addPropertyStateListener(source, this);
            updateEntireCache();
        }
        
        private void cleanup() {
            uninstallComponentListeners();
            sourceProperty.removePropertyStateListener(source, this);
            cachedComponent = null;
            liveValue = null;
            cachedValue = null;
            cachedIsWriteable = false;
        }
        
        // flag -1 - validate all
        // flag  0 - source property changed value or readability
        // flag  1 - document changed
        // flag  2 - value changed
        // flag  3 - editability of text field changed
        private void validateCache(int flag) {
            if (flag != 0 && getJTextComponentFromSource(source, false) != cachedComponent) {
                log("validateCache()", "concurrent modification");
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
                log("validateCache()", "concurrent modification");
            }
            
            if (flag != 2 && liveValue != value && (liveValue == null || !liveValue.equals(value))) {
                log("validateCache()", "concurrent modification");
            }
            
            if (flag != 3 && writeable != cachedIsWriteable) {
                log("validateCache()", "concurrent modification");
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
            JTextComponent comp = getJTextComponentFromSource(source, true);
            
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
                ((AbstractDocument)cachedDocument).setDocumentFilter(this);
                installedFilter = true;
            } else {
                cachedDocument.addDocumentListener(this);
                installedFilter = false;
            }
        }
        
        private void uninstallDocumentListener() {
            if (cachedDocument == null) {
                return;
            }
            
            if (installedFilter) {
                AbstractDocument ad = (AbstractDocument)cachedDocument;
                if (ad.getDocumentFilter() == this) {
                    ad.setDocumentFilter(null);
                }
            } else {
                cachedDocument.removeDocumentListener(this);
            }
        }
        
        private void installComponentListeners() {
            if (cachedComponent == null) {
                return;
            }
            
            cachedComponent.addPropertyChangeListener(this);
            
            if (strategy != ChangeStrategy.ON_TYPE) {
                cachedComponent.addFocusListener(this);
            }
            
            if (strategy == ChangeStrategy.ON_ACTION_OR_FOCUS_LOST
                    && (cachedComponent instanceof JTextField)) {
                
                ((JTextField)cachedComponent).addActionListener(this);
            }
        }
        
        private void uninstallComponentListeners() {
            if (cachedComponent == null) {
                return;
            }
            
            cachedComponent.removePropertyChangeListener(this);
            
            if (strategy != ChangeStrategy.ON_TYPE) {
                cachedComponent.removeFocusListener(this);
            }
            
            if (strategy == ChangeStrategy.ON_ACTION_OR_FOCUS_LOST
                    && (cachedComponent instanceof JTextField)) {
                
                ((JTextField)cachedComponent).removeActionListener(this);
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
        
        private void bindingPropertyChanged(PropertyStateEvent pse) {
            validateCache(0);
            Object oldValue = cachedValue;
            boolean wasWriteable = cachedIsWriteable;
            updateCachedComponent();
            updateCachedDocument();
            updateLiveValue();
            updateCachedValue();
            updateCachedIsWriteable();
            notifyListeners(wasWriteable, oldValue, this);
        }
        
        private void textComponentEditabilityChanged() {
            validateCache(3);
            boolean wasWriteable = cachedIsWriteable;
            updateCachedIsWriteable();
            notifyListeners(wasWriteable, cachedValue, this);
        }
        
        private void documentChanged() {
            validateCache(1);
            updateCachedDocument();
            updateLiveValue();
            Object oldValue = cachedValue;
            updateCachedValue();
            notifyListeners(cachedIsWriteable, oldValue, this);
        }
        
        private void actionWasPerformed() {
            validateCache(-1);
            Object oldValue = cachedValue;
            updateCachedValue();
            notifyListeners(cachedIsWriteable, oldValue, this);
        }
        
        private void focusWasLost() {
            validateCache(-1);
            Object oldValue = cachedValue;
            updateCachedValue();
            notifyListeners(cachedIsWriteable, oldValue, this);
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
                notifyListeners(cachedIsWriteable, oldValue, this);
            }
        }

        public void propertyStateChanged(PropertyStateEvent pe) {
            if (!pe.getValueChanged()) {
                return;
            }

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

    private JTextComponentTextProperty(Property<S, ? extends JTextComponent> sourceProperty, ChangeStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("can't have null strategy");
        }

        if (sourceProperty == null) {
            throw new IllegalArgumentException("can't have null source property");
        }
        
        this.sourceProperty = sourceProperty;
        this.strategy = strategy;
    }

    public static final JTextComponentTextProperty<JTextComponent> create() {
        return createForProperty(new ObjectProperty<JTextComponent>());
    }

    public static final JTextComponentTextProperty<JTextComponent> create(ChangeStrategy strategy) {
        return createForProperty(new ObjectProperty<JTextComponent>(), strategy);
    }

    public static final <S> JTextComponentTextProperty<S> createForProperty(Property<S, ? extends JTextComponent> sourceProperty) {
        return createForProperty(sourceProperty, ChangeStrategy.ON_ACTION_OR_FOCUS_LOST);
    }

    public static final <S> JTextComponentTextProperty<S> createForProperty(Property<S, ? extends JTextComponent> sourceProperty, ChangeStrategy strategy) {
        return new JTextComponentTextProperty<S>(sourceProperty, strategy);
    }

    public ChangeStrategy getChangeStrategy() {
        return strategy;
    }

    public Class<String> getWriteType(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (!entry.cachedIsWriteable) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            return String.class;
        }

        JTextComponent comp = getJTextComponentFromSource(source, true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unwriteable");
        } else if (!comp.isEditable()) {
            log("getWriteType()", "target JTextComponent is non-editable");
            throw new UnsupportedOperationException("Unwriteable");
        }

        return String.class;
    }

    public String getValue(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (entry.cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }

            return (String)entry.cachedValue;
        }

        JTextComponent comp = getJTextComponentFromSource(source, true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return comp.getText();
    }

    public void setValue(S source, String value) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (!entry.cachedIsWriteable) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            try {
                entry.ignoreChange = true;
                if (entry.inDocumentListener) {
                    throw new IllegalStateException("Not yet sure how to handle change from document listener");
                } else {
                    entry.cachedComponent.setText(value);
                    Object oldValue = entry.cachedValue;
                    entry.updateLiveValue();
                    entry.updateCachedValue();
                    notifyListeners(entry.cachedIsWriteable, oldValue, entry);
                }
            } finally {
                entry.ignoreChange = false;
            }
        } else {
            JTextComponent comp = getJTextComponentFromSource(source, true);
            if (comp == null) {
                throw new UnsupportedOperationException("Unwriteable");
            } else if (!comp.isEditable()) {
                log("setValue()", "target JTextComponent is non-editable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            comp.setText(value);
        }
    }

    public boolean isReadable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsReadable();
        }

        return (getJTextComponentFromSource(source, true) != null);
    }

    public boolean isWriteable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsWriteable;
        }

        JTextComponent comp = getJTextComponentFromSource(source, true);
        if (comp == null) {
            return false;
        } else if (!comp.isEditable()) {
            log("isWriteable()", "target JTextComponent is non-editable");
            return false;
        }

        return true;
    }

    private JTextComponent getJTextComponentFromSource(S source, boolean logErrors) {
        if (!sourceProperty.isReadable(source)) {
            if (logErrors) {
                log("getJTextComponentFromSource()", "unreadable source property");
            }
            return null;
        }

        JTextComponent comp = sourceProperty.getValue(source);
        if (comp == null) {
            if (logErrors) {
                log("getJTextComponentFromSource()", "source property returned null");
            }
            return null;
        }
        
        return comp;
    }

    protected final void listeningStarted(S source) {
        SourceEntry entry = map.get(source);
        if (entry == null) {
            entry = new SourceEntry(source);
            map.put(source, entry);
        }
    }

    protected final void listeningStopped(S source) {
        SourceEntry entry = map.remove(source);
        if (entry != null) {
            entry.cleanup();
        }
    }

    private static boolean didValueChange(Object oldValue, Object newValue) {
        return oldValue == null || newValue == null || !oldValue.equals(newValue);
    }

    private static Object toUNREADABLE(Object src) {
        return src == NOREAD ? UNREADABLE : src;
    }

    private void notifyListeners(boolean wasWriteable, Object oldValue, SourceEntry entry) {
        PropertyStateListener[] listeners = getPropertyStateListeners(entry.source);

        if (listeners == null || listeners.length == 0) {
            return;
        }

        oldValue = toUNREADABLE(oldValue);
        Object newValue = toUNREADABLE(entry.cachedValue);
        boolean valueChanged = didValueChange(oldValue, newValue);
        boolean writeableChanged = (wasWriteable != entry.cachedIsWriteable);

        if (!valueChanged && !writeableChanged) {
            return;
        }
        
        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        entry.source,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        entry.cachedIsWriteable);

        this.firePropertyStateChange(pse);
    }

    public String toString() {
        return "JTextComponent.text";
    }

    private static final boolean LOG = false;
    
    private static void log(String method, String message) {
        if (LOG) {
            System.err.println("LOG: " + method + ": " + message);
        }
    }
    
}
