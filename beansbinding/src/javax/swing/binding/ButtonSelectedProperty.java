/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import java.beans.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class ButtonSelectedProperty<S> extends AbstractProperty<S, Boolean> {

    private Property<S, ? extends AbstractButton> sourceProperty;
    private IdentityHashMap<S, SourceEntry> map = new IdentityHashMap<S, SourceEntry>();
    private static final Object NOREAD = new Object();
    
    private final class SourceEntry implements PropertyStateListener, ItemListener {
        private S source;
        private AbstractButton cachedComponent;
        private Object cachedValue;
        private boolean ignoreChange;
        
        private SourceEntry(S source) {
            this.source = source;
            sourceProperty.addPropertyStateListener(source, this);
            updateCachedComponent();
            updateCachedValue();
        }
        
        private void cleanup() {
            if (cachedComponent != null) {
                cachedComponent.removeItemListener(this);
            }

            sourceProperty.removePropertyStateListener(source, this);

            cachedComponent = null;
            cachedValue = null;
        }

        // flag -1 - validate all
        // flag  0 - source property changed value or readability
        // flag  1 - value changed
        private void validateCache(int flag) {
            if (flag != 0 && getButtonFromSource(source, false) != cachedComponent) {
                System.err.println("LOG: validateCache(): concurrent modification");
            }
            
            if (flag != 1) {
                Object value = (cachedComponent == null ? NOREAD : cachedComponent.isSelected());
                if (cachedValue != value && (cachedValue == null || !cachedValue.equals(value))) {
                    System.err.println("LOG: validateCache(): concurrent modification");
                }
            }
        }
        
        private void updateCachedComponent() {
            AbstractButton comp = getButtonFromSource(source, true);

            if (comp != cachedComponent) {
                if (cachedComponent != null) {
                    cachedComponent.removeItemListener(this);
                }

                cachedComponent = comp;

                if (cachedComponent != null) {
                    cachedComponent.addItemListener(this);
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

        public void propertyStateChanged(PropertyStateEvent pe) {
            bindingPropertyChanged(pe);
        }

        public void itemStateChanged(ItemEvent ie) {
            buttonSelectedChanged();
        }

        private void bindingPropertyChanged(PropertyStateEvent pse) {
            boolean valueChanged = pse.getValueChanged() || pse.getReadableChanged();
            validateCache(0);
            Object oldValue = cachedValue;
            boolean wasWriteable = cachedIsWriteable();
            updateCachedComponent();
            updateCachedValue();
            notifyListeners(wasWriteable, oldValue, this);
        }

        private void buttonSelectedChanged() {
            if (ignoreChange) {
                return;
            }

            validateCache(1);
            Object oldValue = cachedValue;
            updateCachedValue();
            notifyListeners(cachedIsWriteable(), oldValue, this);
        }
    }

    private ButtonSelectedProperty(Property<S, ? extends AbstractButton> sourceProperty) {
        if (sourceProperty == null) {
            throw new IllegalArgumentException("can't have null source property");
        }

        this.sourceProperty = sourceProperty;
    }

    public static final ButtonSelectedProperty<AbstractButton> create() {
        return createForProperty(new ObjectProperty<AbstractButton>());
    }

    public static final <S> ButtonSelectedProperty<S> createForProperty(Property<S, ? extends AbstractButton> sourceProperty) {
        return new ButtonSelectedProperty<S>(sourceProperty);
    }

    public Class<Boolean> getWriteType(S source) {
        AbstractButton component;

        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            component = entry.cachedComponent;
        } else {
            component = getButtonFromSource(source, true);
        }

        if (component == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return Boolean.class;
    }

    public Boolean getValue(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (entry.cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }

            return (Boolean)entry.cachedValue;
        }

        AbstractButton comp = getButtonFromSource(source, true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return comp.isSelected();
    }

    public void setValue(S source, Boolean value) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (entry.cachedComponent == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            try {
                entry.ignoreChange = true;
                entry.cachedComponent.setSelected(value);
                Object oldValue = entry.cachedValue;
                entry.updateCachedValue();
                notifyListeners(entry.cachedIsWriteable(), oldValue, entry);
            } finally {
                entry.ignoreChange = false;
            }
        } else {
            AbstractButton component = getButtonFromSource(source, true);
            if (component == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            component.setSelected(value);
        }
    }

    public boolean isReadable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsReadable();
        }

        return (getButtonFromSource(source, true) != null);
    }

    public boolean isWriteable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsWriteable();
        }

        return (getButtonFromSource(source, true) != null);
    }

    private AbstractButton getButtonFromSource(S source, boolean logErrors) {
        if (!sourceProperty.isReadable(source)) {
            if (logErrors) {
                System.err.println("LOG: getButtonFromSource(): unreadable source property");
            }
            return null;
        }

        AbstractButton button = sourceProperty.getValue(source);
        if (button == null) {
            if (logErrors) {
                System.err.println("LOG: getButtonFromSource(): source property returned null");
            }
            return null;
        }
        
        return button;
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
        boolean writeableChanged = (wasWriteable != entry.cachedIsWriteable());

        if (!valueChanged && !writeableChanged) {
            return;
        }
        
        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        entry.source,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        entry.cachedIsWriteable());

        this.firePropertyStateChange(pse);
    }

    public String toString() {
        return "Button.isSelected";
    }

}
