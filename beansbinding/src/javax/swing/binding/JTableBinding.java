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
public final class JTableBinding<T> extends Binding<List<T>, List<T>> {

    public JTableBinding(List<T> source, JTable target) {
        super(new ObjectProperty(source), null);
    }

    public JTableBinding(Property<List<T>> source, JTable target) {
        super(source, null);
    }

    public JTableBinding(List<T> source, Property<? extends JTable> target) {
        super(new ObjectProperty(source), null);
    }

    public JTableBinding(Property<List<T>> source, Property<? extends JTable> target) {
        super(source, null);
    }

}
