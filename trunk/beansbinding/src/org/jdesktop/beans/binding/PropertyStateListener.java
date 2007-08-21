/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beans.binding;

import java.util.EventListener;

/**
 * @author Shannon Hickey
 */
public interface PropertyStateListener extends EventListener {

    public void propertyStateChanged(PropertyStateEvent pse);

}
