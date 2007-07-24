/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * {@code Converter} is responsible for converting a value from one type
 * to another.
 *
 * @author Shannon Hickey
 */
public abstract class Converter<S, T> {
    public abstract S convertForward(T value);
    public abstract T convertReverse(S value);
}
