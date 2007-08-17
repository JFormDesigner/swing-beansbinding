/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding.ext;

/**
 * @author Shannon Hickey
 */
public abstract class PropertyDelegateProvider {

    public abstract boolean providesDelegate(Class<?> type, String property);

    public abstract Object createPropertyDelegate(Object source, String property);

}
