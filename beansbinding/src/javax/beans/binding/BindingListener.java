/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.EventListener;

/**
 * @author Shannon Hickey
 */
public interface BindingListener extends EventListener {
    public void targetUnwriteable(Binding binding);
    public void bindingInSync(Binding binding);
}
