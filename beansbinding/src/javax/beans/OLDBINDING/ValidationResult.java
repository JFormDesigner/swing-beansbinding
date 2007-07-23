/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.OLDBINDING;

/**
 * A {@code ValidationResult} is returned from a {@code BindingValidator} to
 * indicate the value is invalid, and describe what action to take.
 *
 * @author sky
 */
public class ValidationResult {
    private final Action type;
    private final Object errorCode;
    private final String description;
  
    /**
     * An enumeration of the action the {@code Binding} should take.
     */
    public enum Action {
        /**
         * Indicates the {@code Binding} should do nothing in response
         * to this {@code ValidationResult}. That results in leaving
         * the target state {@code INVALID}.
         */
        DO_NOTHING,

        /**
         * Indicates the {@code Binding} should revert the target value
         * by setting it from the source value. Care must be taken when
         * specifying this action. It implies a level of reentrancy in setting
         * values that not all objects are designed to deal with.
         */
        SET_TARGET_FROM_SOURCE,
    }

    /**
     * Creates a {@code ValidationResult} of the specified type.
     *
     * @param type the action the {@code Binding} should take
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    public ValidationResult(Action type) {
        this(type, null, null);
    }
    
    /**
     * Creates a {@code ValidationResult}.
     *
     * @param type the action the {@code Binding} should take
     * @param description a textual description of this
     *        {@code ValidationResult}
     * @param errorCode an identifier for this {@code ValidationResult}
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    public ValidationResult(Action type, String description, Object errorCode) {
        if (type == null) {
            throw new IllegalArgumentException("Type must be non-null");
        }
        this.type = type;
        this.description = description;
        this.errorCode = errorCode;
    }

    /**
     * Returns the action the {@code Binding} should take.
     *
     * @return the action
     */
    public Action getType() {
        return type;
    }
    
    /**
     * Returns an identifier for the result.
     *
     * @return an identifier
     */
    public Object getErrorCode() {
        return errorCode;
    }
    
    /**
     * Returns a description of the validation result.
     *
     * @return a description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns a string representing this {@code ValidationResult}.  This
     * method is intended to be used only for debugging purposes, and
     * the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return a string representation of this binding
     */
    public String toString() {
        return "ValidationResult [" +
                ", type=" + type +
                ", errorCode=" + errorCode +
                ", description=" + description;
    }
}
