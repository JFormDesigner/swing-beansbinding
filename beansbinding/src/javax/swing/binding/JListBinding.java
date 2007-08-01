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
public final class JListBinding<T> extends Binding<List<T>, List<T>> {

    public JListBinding(List<T> source, JList target) {
        super(new ObjectProperty(source), null);
    }

    public JListBinding(Property<List<T>> source, JList target) {
        super(source, null);
    }

    public JListBinding(List<T> source, Property<? extends JList> target) {
        super(new ObjectProperty(source), null);
    }

    public JListBinding(Property<List<T>> source, Property<? extends JList> target) {
        super(source, null);
    }

}
