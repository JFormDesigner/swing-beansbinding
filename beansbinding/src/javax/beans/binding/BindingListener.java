/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.EventListener;

/**
 * @author Shannon Hickey
 */
public interface BindingListener extends EventListener {
    public void syncFailed(Binding binding, Binding.SyncFailure... failures);
    public void synced(Binding binding);
    public void sourceChanged(Binding binding);
    public void targetChanged(Binding binding);
}
