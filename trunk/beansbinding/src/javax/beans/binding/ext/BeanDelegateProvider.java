/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding.ext;

/**
 * @author sky
 * @author Shannon Hickey
 */
public interface BeanDelegateProvider {

    public abstract boolean providesDelegate(Class<?> type, String property);
    public abstract Object createPropertyDelegate(Object source, String property);
    public abstract Class<?> getPropertyDelegateClass(Class<?> type);

}
