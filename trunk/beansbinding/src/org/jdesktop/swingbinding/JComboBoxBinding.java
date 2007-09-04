/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.ObjectProperty;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.beansbinding.PropertyStateListener;
import org.jdesktop.swingbinding.impl.AbstractColumnBinding;
import org.jdesktop.swingbinding.impl.ListBindingManager;
import org.jdesktop.swingbinding.impl.*;

/**
 * Binds a {@code List} of objects to act as the items of a {@code JComboBox}.
 * Each object in the source {@code List} is an item in the {@code JComboBox}.
 * <p>
 * If the {@code List} is an instance of {@code ObservableList}, then changes
 * to the {@code List} are reflected in the {@code JComboBox}.
 * <p>
 * Instances of {@code JComboBoxBinding} are obtained by calling one of the
 * {@code createJComboBoxBinding} methods in the {@code SwingBindings} class. There
 * are methods for creating a {@code JComboBoxBinding} using direct references to a
 * {@code List} and/or {@code JComboBox} and methods for creating a {@code JComboBoxBinding} by
 * providing the {@code List} and/or {@code JComboBox} as {@code Property} instances
 * that derive the {@code List}/{@code JComboBox} from the binding's source/target objects.
 * <p>
 * This class is a subclass of {@code AutoBinding}. The update strategy
 * dictates how the binding responds to changes in the value of the source
 * {@code List} property itself. The strategy can be {@code READ_ONCE} or {@code READ}
 * ({@code READ_WRITE} is treated as {@code READ}).
 * <p>
 * {@code JComboBoxBinding} works by installing a custom model on the target {@code JComboBox},
 * at bind time if the {@code JComboBox} property is readable, or whenever it becomes
 * readable after binding. This model is uninstalled when the property becomes unreadable
 * or the binding is unbound. It is also uninstalled, and installed on the replacement,
 * when the value of the {@code JComboBox} property changes. When the model is uninstalled from a
 * {@code JComboBox}, the JComboBox's model is replaced with an empty {@code DefaultComboBoxModel}
 * so that it is left functional.
 * <p>
 * Keep in mind that with a {@code READ_ONCE} update strategy, the source {@code List} (if readable) is
 * automatically applied to the target {@code JComboBox} (if readable) only once, at bind time.
 * As a result, if the target {@code JComboBox} changes, it gets the model, but the items are
 * not automatically set on it.
 * <p>
 * Here is an example of creating a binding from a {@code List} of {@code Country}
 * objects to a {@code JComboBox}:
 * <p>
 * <pre><code>
 *    // create the country list
 *    List<Country> countries = createCountryList();
 *
 *    // create the binding from List to JList
 *    JComboBoxBinding cb = SwingBindings.createJComboBoxBinding(READ, countries, jComboBox);
 *
 *    // realize the binding
 *    cb.bind();
 * </code></pre>
 * <p>
 * In addition to binding the items of a {@code JComboBox}, it is possible to
 * bind to the selected item of a {@code JComboBox}.
 * See the list of <a href="package-summary.html#SWING_PROPS">
 * interesting swing properties</a> in the package summary for more details.
 *
 * @param <E> the type of elements in the source {@code List}
 * @param <SS> the type of source object (on which the source property resolves to {@code List})
 * @param <TS> the type of target object (on which the target property resolves to {@code JComboBox})
 *
 * @author Shannon Hickey
 */
public final class JComboBoxBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {
    
    private ElementsProperty<TS, JComboBox> ep;
    private Handler handler = new Handler();
    private BindingComboBoxModel model;
    private JComboBox combo;
    
    /**
     * Constructs an instance of {@code JComboBoxBinding}.
     *
     * @param strategy the update strategy
     * @param sourceObject the source object
     * @param sourceProperty a property on the source object that resolves to the {@code List} of elements
     * @param targetObject the target object
     * @param targetProperty a property on the target object that resolves to a {@code JComboBox}
     * @param name a name for the {@code JComboBoxBinding}
     * @throws IllegalArgumentException if the source property or target property is {@code null}
     */
    protected JComboBoxBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JComboBox> targetJComboBoxProperty, String name) {
        super(strategy, sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS, JComboBox>(targetJComboBoxProperty), name);
        ep = (ElementsProperty<TS, JComboBox>)getTargetProperty();
    }
    
    protected void bindImpl() {
        model = new BindingComboBoxModel();
        // order is important for the next two lines
        ep.addPropertyStateListener(null, handler);
        ep.installBinding(this);
        super.bindImpl();
    }
    
    protected void unbindImpl() {
        // order is important for the next two lines
        ep.uninstallBinding();
        ep.removePropertyStateListener(null, handler);
        model = null;
        super.unbindImpl();
    }
    
    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (!pse.getValueChanged()) {
                return;
            }
            
            Object newValue = pse.getNewValue();
            
            if (newValue == PropertyStateEvent.UNREADABLE) {
                combo.setModel(new DefaultComboBoxModel());
                combo = null;
                model.setElements(null);
            } else {
                combo = ep.getComponent();
                model.setElements((List<E>)newValue);
                combo.setModel(model);
            }
        }
    }

    private final class BindingComboBoxModel extends ListBindingManager implements ComboBoxModel  {
        private final List<ListDataListener> listeners;
        private Object selectedItem = null;
        private int selectedModelIndex = -1;

        public BindingComboBoxModel() {
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }

        public void setElements(List<?> elements) {
            super.setElements(elements);

            if (size() <= 0) {
                if (selectedModelIndex != -1) {
                    selectedModelIndex = -1;
                    selectedItem = null;
                }
            } else {
                if (selectedItem == null) {
                    selectedModelIndex = 0;
                    selectedItem = getElementAt(selectedModelIndex);
                }
            }
        }

        protected AbstractColumnBinding[] getColBindings() {
            return new AbstractColumnBinding[0];
        }

        public Object getSelectedItem() {
            return selectedItem;
        }

        public void setSelectedItem(Object item) {
            // This is what DefaultComboBoxModel does (yes, yuck!)
            if ((selectedItem != null && !selectedItem.equals(item)) || selectedItem == null && item != null) {
                selectedItem = item;
                contentsChanged(-1, -1);
                selectedModelIndex = -1;
                if (item != null) {
                    int size = size();
                    for (int i = 0; i < size; i++) {
                        if (item.equals(getElementAt(i))) {
                            selectedModelIndex = i;
                            break;
                        }
                    }
                }
            }
        }

        protected void allChanged() {
            contentsChanged(0, size());
        }

        protected void valueChanged(int row, int column) {
            // we're not expecting any value changes since we don't have any
            // detail bindings for JComboBox
        }

        protected void added(int index, int length) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index + length - 1);
            int size = listeners.size();
            for (int i = size - 1; i >= 0; i--) {
                listeners.get(i).intervalAdded(e);
            }

            if (size() == length && selectedItem == null) {
                setSelectedItem(getElementAt(0));
            }
        }

        protected void removed(int index, int length) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index + length - 1);
            int size = listeners.size();
            for (int i = size - 1; i >= 0; i--) {
                listeners.get(i).intervalRemoved(e);
            }
            
            if (selectedModelIndex >= index && selectedModelIndex < index + length) {
                if (size() == 0) {
                    setSelectedItem(null);
                } else {
                    setSelectedItem(getElementAt(Math.max(index - 1, 0)));
                }
            }
        }

        protected void changed(int row) {
            contentsChanged(row, row);
        }

        private void contentsChanged(int row0, int row1) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, row0, row1);
            int size = listeners.size();
            for (int i = size - 1; i >= 0; i--) {
                listeners.get(i).contentsChanged(e);
            }
        }
        
        public Object getElementAt(int index) {
            return getElement(index);
        }
        
        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }
        
        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }
        
        public int getSize() {
            return size();
        }
    }

}
