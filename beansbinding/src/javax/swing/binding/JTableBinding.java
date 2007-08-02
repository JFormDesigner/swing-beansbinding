/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.*;
import javax.swing.*;
import java.util.List;

/**
 * @author Shannon Hickey
 */
public final class JTableBinding<T> extends Binding<List<T>, List> {

    private ElementsProperty<JTable> ep;
    private Handler handler = new Handler();
    private JTable table;
    private List<T> elements;

    public JTableBinding(List<T> source, JTable target) {
        this(null, source, target);
    }

    public JTableBinding(Property<List<T>> source, JTable target) {
        this(null, source, target);
    }

    public JTableBinding(List<T> source, Property<? extends JTable> target) {
        this(null, source, target);
    }

    public JTableBinding(Property<List<T>> source, Property<? extends JTable> target) {
        this(null, source, target);
    }

    public JTableBinding(String name, List<T> source, JTable target) {
        super(name, new ObjectProperty(source), new ElementsProperty<JTable>(new ObjectProperty<JTable>(target)));
        setup();
    }

    public JTableBinding(String name, Property<List<T>> source, JTable target) {
        super(name, source, new ElementsProperty<JTable>(new ObjectProperty<JTable>(target)));
        setup();
    }

    public JTableBinding(String name, List<T> source, Property<? extends JTable> target) {
        super(name, new ObjectProperty(source), new ElementsProperty(target));
        setup();
    }

    public JTableBinding(String name, Property<List<T>> source, Property<? extends JTable> target) {
        super(name, source, new ElementsProperty(target));
        setup();
    }

    private void setup() {
        super.setAutoUpdateStrategy(AutoUpdateStrategy.READ);
        prepareElementsProperty();
    }
    
    private void prepareElementsProperty() {
        ep = (ElementsProperty)getTarget();
        ep.addPropertyStateListener(handler);
    }

    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (!pse.getValueChanged()) {
                return;
            }

            if (table != null) {
                System.out.println("uninstall table model");
                table = null;
                elements = null;
            }

            Object newValue = pse.getNewValue();

            if (newValue != null && newValue != PropertyStateEvent.UNREADABLE) {
                System.out.println("install table model");
                table = ep.getComponent();
                elements = (List<T>)newValue;
            }
        }
    }

}
