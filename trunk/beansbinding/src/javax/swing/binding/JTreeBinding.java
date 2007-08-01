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
public final class JTreeBinding<T> extends Binding<T, T> {

    public JTreeBinding(T source, JTree target) {
        super(new ObjectProperty(source), null);
    }

    public JTreeBinding(Property<T> source, JTree target) {
        super(source, null);
    }

    public JTreeBinding(T source, Property<? extends JTree> target) {
        super(new ObjectProperty(source), null);
    }

    public JTreeBinding(Property<T> source, Property<? extends JTree> target) {
        super(source, null);
    }

}
