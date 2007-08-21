/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beans.binding;

/**
 * {@code Validator} is responsible for validating the value from the target of
 * a {@code Binding}.
 * 
 * @author Shannon Hickey
 */ 
public abstract class Validator<S> {

    /**
     * {@code ValidationResult} is returned from {@code Validator} to
     * indicate an invalid value.
     */
    public class Result {
        private final Object errorCode;
        private final String description;

        /**
         * Creates a {@code ValidationResult}.
         *
         * @param errorCode an identifier for this {@code ValidationResult}
         * @param description a textual description of the {@code ValidationResult}
         */
        public Result(Object errorCode, String description) {
            this.description = description;
            this.errorCode = errorCode;
        }

        /**
         * Returns an identifier for the result, or {@code null}
         *
         * @return an identifier
         */
        public Object getErrorCode() {
            return errorCode;
        }

        /**
         * Returns a description of the validation result, or {@code null}
         *
         * @return a description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Returns a string representing the {@code ValidationResult}. This
         * method is intended to be used for debugging purposes only, and
         * the content and format of the returned string may vary between
         * implementations. The returned string may be empty but may not
         * be {@code null}.
         *
         * @return a string representation of this binding
         */
        public String toString() {
            return getClass().getName() +
                    " [" +
                    "errorCode=" + errorCode +
                    ", description=" + description +
                    "]";
        }
    }

    /**
     * Validates a value. An invalid value is identified by returning
     * a {@code non-null ValidationResult} from this method.
     *
     * @param value the value to validate, may be {@code null}
     * @throws IllegalArgumentException if {@code binding} is {@code null}
     */
    public abstract Result validate(S value);
}
