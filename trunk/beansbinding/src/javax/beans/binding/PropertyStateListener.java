/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.EventListener;

/**
 * @author Shannon Hickey
 */
public interface PropertyStateListener<V> extends EventListener {

    public void propertyStateChanged(PropertyStateEvent<? extends V> pe);

}
