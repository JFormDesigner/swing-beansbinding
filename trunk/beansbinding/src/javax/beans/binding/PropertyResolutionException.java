/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * {@code PropertyResolutionExceptions} can be thrown at various points in
 * the life cycle of a {@code Property}. Any time a {@code Property}
 * encounters an exception in resolving a property, a
 * {@code PropertyResolutionException} can be thrown. For example, if a
 * {@code BeanProperty} encounters an exception while trying to resolve
 * the "foo" property of an object via reflection, the exception is
 * wrapped in a {@code PropertyResolutionException} and is re-thrown.
 *
 * @author Shannon Hickey
 * @author Scott Violet
 */
public class PropertyResolutionException extends RuntimeException {

    public PropertyResolutionException(String description) {
        super(description);
    }

    public PropertyResolutionException(String description, Exception reason) {
        super(description, reason);
    }

}
