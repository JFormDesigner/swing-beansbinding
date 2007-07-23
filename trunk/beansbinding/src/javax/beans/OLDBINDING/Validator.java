/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.OLDBINDING;

/**
 * {@code Validator} is responsible for validating a value before
 * it is set on a {@code Property}.
 * 
 * @author Shannon Hickey
 */
public abstract class Validator<T> {
    public abstract ValidationResult validate(T value);
}
