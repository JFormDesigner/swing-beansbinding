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
public final class JComboBoxBinding<T> extends Binding<List<T>, List<T>> {

    public JComboBoxBinding(List<T> source, JComboBox target) {
        super(new ObjectProperty(source), null);
    }

    public JComboBoxBinding(Property<List<T>> source, JComboBox target) {
        super(source, null);
    }

    public JComboBoxBinding(List<T> source, Property<? extends JComboBox> target) {
        super(new ObjectProperty(source), null);
    }

    public JComboBoxBinding(Property<List<T>> source, Property<? extends JComboBox> target) {
        super(source, null);
    }

}
