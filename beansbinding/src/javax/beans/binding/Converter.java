/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * {@code Converter} is responsible for converting a value before
 * being set on a {@code Property}.
 *
 * @author Shannon Hickey
 */
public abstract class Converter<F, T> {
    public abstract Object convertFrom(F value);
    public abstract Object convertTo(T value);
}
