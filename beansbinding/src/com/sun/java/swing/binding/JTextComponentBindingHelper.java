/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.swing.binding;

import java.awt.Component;
import javax.beans.binding.Binding;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.beans.binding.Binding.BindingController;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.binding.SwingBindingSupport;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import static javax.swing.binding.SwingBindingSupport.TextChangeStrategy;
import javax.swing.binding.SwingBindingSupport.TextChangeStrategy;

class JTextComponentBindingHelper extends AbstractBindingHelper {
    private final JTextComponent textComponent;
    private final PropertyDelegate delegate;
    private final Handler handler;
    private final boolean preferFilter;
    private boolean inDocumentListener;
    private Document document;
    private boolean installedFilter;
    private boolean changingText;
    private DocumentFilter.FilterBypass filterBypass;
    private BindingController controller;
    
    JTextComponentBindingHelper(JTextComponent text, boolean preferFilter) {
        this.textComponent = text;
        this.preferFilter = preferFilter;
        delegate = new PropertyDelegate();
        handler = new Handler();
        // PENDING: notice there is no way to know when the DocumentFilter
        // has changed, need to address that somehow.
    }
    
    public Object getPropertyDelegate() {
        return delegate;
    }
    
    protected boolean shouldCreateBindingTarget(String property) {
        return (property == JTEXT_COMPONENT_TEXT_P);
    }
    
    public void bind(BindingController controller,
            String property) {
        throwIfNonNull(this.controller);
        assert (property == JTEXT_COMPONENT_TEXT_P);
        this.controller = controller;
        textComponent.addPropertyChangeListener(handler);
        if (getChangeStrategy() != TextChangeStrategy.CHANGE_ON_TYPE) {
            textComponent.addFocusListener(handler);
        }
        if (getChangeStrategy() == TextChangeStrategy.CHANGE_ON_ACTION_OR_FOCUS_LOST &&
                (textComponent instanceof JTextField)) {
            ((JTextField)textComponent).addActionListener(handler);
        }
        document = textComponent.getDocument();
        installDocumentListener();
        updateComponentEnabledFromBinding();
    }
    
    public void unbind(BindingController controller,
            String property) {
        assert (controller == this.controller && 
                property == JTEXT_COMPONENT_TEXT_P);
        textComponent.removePropertyChangeListener(handler);
        if (getChangeStrategy() != TextChangeStrategy.CHANGE_ON_TYPE) {
            textComponent.removeFocusListener(handler);
        }
        if (getChangeStrategy() == TextChangeStrategy.CHANGE_ON_ACTION_OR_FOCUS_LOST &&
                (textComponent instanceof JTextField)) {
            ((JTextField)textComponent).removeActionListener(handler);
        }
        uninstallDocumentListener();
        this.controller = null;
        document = null;
    }
    
    public void sourceValueStateChanged(BindingController controller,
            String property) {
        updateComponentEnabledFromBinding();
    }
    
    protected Component getComponent() {
        return textComponent;
    }
    
    protected Binding getBinding() {
        return controller.getBinding();
    }
    
    protected void setComponentEnabled(boolean enable) {
        getComponent().setEnabled(enable);
        if (!enable) {
            setText("");
        }
    }
    
    private TextChangeStrategy getChangeStrategy() {
        return controller.getBinding().getParameterValue(
                SwingBindingSupport.TextChangeStrategyParameter.class,
                TextChangeStrategy.CHANGE_ON_ACTION_OR_FOCUS_LOST);
    }
    
    private void installDocumentListener() {
        if (preferFilter && (document instanceof AbstractDocument) &&
                ((AbstractDocument)document).getDocumentFilter() == null) {
            ((AbstractDocument)document).setDocumentFilter(handler);
            installedFilter = true;
        } else {
            document.addDocumentListener(handler);
            installedFilter = false;
        }
    }
    
    private void uninstallDocumentListener() {
        if (document != null) {
            if (installedFilter) {
                AbstractDocument ad = (AbstractDocument)document;
                if (ad.getDocumentFilter() == handler) {
                    ad.setDocumentFilter(null);
                }
            } else {
                document.removeDocumentListener(handler);
            }
        }
    }
    
    private void setText(String text) {
        changingText = true;
        textComponent.setText(text);
        textComponent.setCaretPosition(0);
        changingText = false;
    }
    
    private void documentChanged() {
        uninstallDocumentListener();
        document = textComponent.getDocument();
        installDocumentListener();
    }
    
    private void textChanged() {
        if (!changingText) {
            if (getChangeStrategy() == TextChangeStrategy.CHANGE_ON_TYPE) {
                delegate.fireTextChanged();
            } else {
                controller.valueEdited();
            }
        }
    }
    
    private boolean isUncommitted() {
        return (controller.getBinding().getTargetValueState() !=
                Binding.ValueState.VALID);
    }
    
    
    public class PropertyDelegate extends DelegateBase {
        public void setText(String text) {
            if (inDocumentListener) {
                SwingUtilities.invokeLater(new ChangeTextRunnable(text));
            } else {
                JTextComponentBindingHelper.this.setText(text);
            }
        }
        
        public String getText() {
            return textComponent.getText();
        }
        
        private void fireTextChanged() {
            firePropertyChange("text", null, null);
        }
    }
    
    
    // PENDING: nuke this once BeanInfoAPT is working
    public static final class PropertyDelegateBeanInfo extends BindingBeanInfo {
        protected Class<?> getPropertyDelegateClass() {
            return PropertyDelegate.class;
        }

        protected Property[] getPreferredProperties() {
            return new Property[] {
                new Property("text", "The context of the text component")
            };
        }
    }

    
    private final class ChangeTextRunnable implements Runnable {
        private final String text;
        
        ChangeTextRunnable(String text) {
            this.text = text;
        }
        
        public void run() {
            setText(text);
        }
    }
    
    
    private final class Handler extends DocumentFilter implements
            ActionListener, DocumentListener, FocusListener, 
            PropertyChangeListener {
        public void insertUpdate(DocumentEvent e) {
            documentListenerTextChanged();
        }

        public void removeUpdate(DocumentEvent e) {
            documentListenerTextChanged();
        }

        public void changedUpdate(DocumentEvent e) {
        }
        
        private void documentListenerTextChanged() {
            if (!changingText) {
                inDocumentListener = true;
                JTextComponentBindingHelper.this.textChanged();
                inDocumentListener = false;
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String property = e.getPropertyName();
            if (property == "document") {
                documentChanged();
            }
        }

        public void replace(DocumentFilter.FilterBypass fb, int offset,
                int length, String text, AttributeSet attrs) throws BadLocationException {
            filterBypass = fb;
            super.replace(fb, offset, length, text, attrs);
            textChanged();
            filterBypass = null;
        }

        public void insertString(DocumentFilter.FilterBypass fb, int offset,
                String string, AttributeSet attr) throws BadLocationException {
            filterBypass = fb;
            super.insertString(fb, offset, string, attr);
            textChanged();
            filterBypass = null;
        }

        public void remove(DocumentFilter.FilterBypass fb, int offset,
                int length) throws BadLocationException {
            filterBypass = fb;
            super.remove(fb, offset, length);
            textChanged();
            filterBypass = null;
        }

        public void actionPerformed(ActionEvent e) {
            if (getChangeStrategy() != TextChangeStrategy.CHANGE_ON_TYPE &&
                    isUncommitted()) {
                delegate.fireTextChanged();
            }
        }

        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
            if (!e.isTemporary() &&
                    getChangeStrategy() != TextChangeStrategy.CHANGE_ON_TYPE &&
                    isUncommitted()) {
                delegate.fireTextChanged();
            }
        }
    }
}
