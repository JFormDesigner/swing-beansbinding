/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class JSliderValueProperty<S> extends AbstractProperty<S, Integer> {

    private Property<S, ? extends JSlider> sourceProperty;
    private IdentityHashMap<S, SourceEntry> map = new IdentityHashMap<S, SourceEntry>();
    private static final Object NOREAD = new Object();

    private final class SourceEntry implements PropertyStateListener, ChangeListener {
        private S source;
        private JSlider cachedComponent;
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
                cachedComponent.removeChangeListener(this);
            }

            sourceProperty.removePropertyStateListener(source, this);

            cachedComponent = null;
            cachedValue = null;
        }

        // flag -1 - validate all
        // flag  0 - source property changed value or readability
        // flag  1 - value changed
        private void validateCache(int flag) {
            if (flag != 0 && getJSliderFromSource(source, false) != cachedComponent) {
                log("validateCache()", "concurrent modification");
            }
            
            if (flag != 1) {
                Object value = (cachedComponent == null ? NOREAD : cachedComponent.getValue());
                if (cachedValue != value && (cachedValue == null || !cachedValue.equals(value))) {
                    log("validateCache()", "concurrent modification");
                }
            }
        }
        
        private void updateCachedComponent() {
            JSlider comp = getJSliderFromSource(source, true);

            if (comp != cachedComponent) {
                if (cachedComponent != null) {
                    cachedComponent.removeChangeListener(this);
                }

                cachedComponent = comp;

                if (cachedComponent != null) {
                    cachedComponent.addChangeListener(this);
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

        public void propertyStateChanged(PropertyStateEvent pe) {
            if (!pe.getValueChanged()) {
                return;
            }

            bindingPropertyChanged(pe);
        }

        public void stateChanged(ChangeEvent ce) {
            sliderValueChanged();
        }

        private void bindingPropertyChanged(PropertyStateEvent pse) {
            validateCache(0);
            Object oldValue = cachedValue;
            boolean wasWriteable = cachedIsWriteable();
            updateCachedComponent();
            updateCachedValue();
            notifyListeners(wasWriteable, oldValue, this);
        }

        private void sliderValueChanged() {
            if (ignoreChange) {
                return;
            }
            
            validateCache(1);
            Object oldValue = cachedValue;
            updateCachedValue();
            notifyListeners(cachedIsWriteable(), oldValue, this);
        }
    }

    private JSliderValueProperty(Property<S, ? extends JSlider> sourceProperty) {
        if (sourceProperty == null) {
            throw new IllegalArgumentException("can't have null source property");
        }

        this.sourceProperty = sourceProperty;
    }

    public static final JSliderValueProperty<JSlider> create() {
        return createForProperty(new ObjectProperty<JSlider>());
    }

    public static final <S> JSliderValueProperty<S> createForProperty(Property<S, ? extends JSlider> sourceProperty) {
        return new JSliderValueProperty<S>(sourceProperty);
    }

    public Class<Integer> getWriteType(S source) {
        JSlider component;

        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            component = entry.cachedComponent;
        } else {
            component = getJSliderFromSource(source, true);
        }

        if (component == null) {
            throw new UnsupportedOperationException("Unwriteable");
        }

        return Integer.class;
    }

    public Integer getValue(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (entry.cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }

            return (Integer)entry.cachedValue;
        }

        JSlider comp = getJSliderFromSource(source, true);
        if (comp == null) {
            throw new UnsupportedOperationException("Unreadable");
        }

        return comp.getValue();
    }
    
    public void setValue(S source, Integer value) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);

            if (entry.cachedComponent == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            try {
                entry.ignoreChange = true;
                entry.cachedComponent.setValue(value);
                Object oldValue = entry.cachedValue;
                entry.updateCachedValue();
                notifyListeners(entry.cachedIsWriteable(), oldValue, entry);
            } finally {
                entry.ignoreChange = false;
            }
        } else {
            JSlider component = getJSliderFromSource(source, true);
            if (component == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }

            component.setValue(value);
        }
    }
    
    public boolean isReadable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsReadable();
        }

        return (getJSliderFromSource(source, true) != null);
    }

    public boolean isWriteable(S source) {
        SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsWriteable();
        }

        return (getJSliderFromSource(source, true) != null);
    }

    private JSlider getJSliderFromSource(S source, boolean logErrors) {
        if (!sourceProperty.isReadable(source)) {
            if (logErrors) {
                log("getJSliderFromSource()", "unreadable source property");
            }
            return null;
        }

        JSlider slider = sourceProperty.getValue(source);
        if (slider == null) {
            if (logErrors) {
                log("getJSliderFromSource()", "source property returned null");
            }
            return null;
        }
        
        return slider;
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
        return "JSlider.value";
    }

    private static final boolean LOG = false;
    
    private static void log(String method, String message) {
        if (LOG) {
            System.err.println("LOG: " + method + ": " + message);
        }
    }
    
}
