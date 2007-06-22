/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

/**
 * An enumeration of the possible values for the text change strategy for
 * binding to Swing text components.
 *
 * @see ParameterKeys#TEXT_CHANGE_STRATEGY
 * @see javax.beans.binding.Binding#putParameter
 *
 * @author Scott Violet
 * @author Shannon Hickey
 */
public enum TextChangeStrategy {
    /**
     * Indicates the binding target should change as text is input.
     */
    ON_TYPE,

    /**
     * Indicates the binding target should change when enter is pressed,
     * or the text component loses focus.
     */
    ON_ACTION_OR_FOCUS_LOST,

    /**
     * Indicates the binding target should change when focus leaves the
     * text component.
     */
    ON_FOCUS_LOST
}
