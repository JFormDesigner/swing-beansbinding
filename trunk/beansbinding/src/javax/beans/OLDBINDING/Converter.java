/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.OLDBINDING;

/**
 * {@code Converter} is responsible for converting a value before
 * it is set on a {@code Property}.
 *
 * @author Shannon Hickey
 */
public abstract class Converter<F, T> {
    public abstract Object convertFrom(F value);
    public abstract Object convertTo(T value);
}
