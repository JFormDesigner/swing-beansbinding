/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * {@code PropertyResolverExceptions} are thrown at a various point in
 * the life cycle of a {@code PropertyResolver}. Any time {@code
 * PropertyResolver} encounters an exception in resolving a property a
 * {@code PropertyResolverException} is thrown. For example, if
 * resolving {@code "manager"} triggers an exception to be thrown, the
 * exception will be wrapped in a {@code PropertyResolverException}.
 *
 * @author sky
 */
public class PropertyResolverException extends RuntimeException {
    private final Object source;
    private final String path;
    
    public PropertyResolverException(String description, Object source,
            String path) {
        this(description, source, path, null);
    }

    public PropertyResolverException(String description, Object source,
            String path, Exception reason) {
        super(description, reason);
        this.source = source;
        this.path = path;
    }
    
    public String toString() {
        return "PropertyResolverException " +
                " [description=" + getMessage() + 
                ", source=" + source +
                ", path=" + path +
                "]";
    }
}
