/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.OLDBINDING;

import javax.beans.OLDBINDING.Binding.ParameterKey;

/**
 * This class contains constants for all keys used to put {@code Binding}
 * parameters when binding to Swing components.
 *
 * @see javax.beans.binding.Binding#putParameter
 * @author Shannon Hiceky
 */
public class ParameterKeys {

    private ParameterKeys() {}

    /**
     * Key used to specify whether or not the target component should be
     * disabled when the source path is incomplete. If not specified, the
     * default value is {@code true}. This property is supported for
     * {@code JCheckBox}'s "selected" property, {@code JSlider}'s "value"
     * property, and {@code JTextComponent}'s "text" property.
     */
    public static final ParameterKey<Boolean> DISABLE_ON_INCOMPLETE_PATH =
            new ParameterKey<Boolean>("DISABLE_ON_INCOMPLETE_PATH");

    /**
     * Key used to specify whether or not a node with no children
     * is treated as a leaf. This is used in bindings to {@code JTree}.
     * The default value is {@code false}.
     */
    public static final ParameterKey<Boolean> EMPTY_NODE_TREATED_AS_LEAF =
            new ParameterKey<Boolean>("EMPTY_NODE_TREATED_AS_LEAF");

    /**
     * Key used to specify the class that a child binding applies to.
     * This is used in child bindings where the target is a {@code JTree}.
     * If not specified, an {@code IllegalArgumentException} is
     * thrown when the {@code Binding} is bound.
     * See the description of how to bind to
     * a <a href="package-summary.html#JTreeBinding">JTree</a>
     * for more information.
     */
    public static final ParameterKey<Class> TREE_NODE_CLASS =
            new ParameterKey<Class>("TREE_NODE_CLASS");

    // PENDING(shannonh) - the following doesn't make much sense, and the
    // description in package.html isn't much better. Resolve this.

    /**
     * Key used to specify whether the child binding
     * identifies the path for the "selectedObjectProperty" key of a
     * {@code JComboBox}. See the description of how to bind to
     * a <a href="package-summary.html#JComboBoxBinding">JComboBox</a>
     * for more information.
     */
    public static final ParameterKey<String> COMBOBOX_SELECTED_OBJECT_PROPERTY =
            new ParameterKey<String>("COMBOBOX_SELECTED_OBJECT_PROPERTY");

}
