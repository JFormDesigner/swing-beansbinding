/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.beans.binding;

import java.util.List;

/**
 *
 * @author sky
 */
public final class BindingFunctions {
    private BindingFunctions() {
    }
    
    public static int listSize(List<?> elements) {
        return (elements == null) ? 0 : elements.size();
    }
}
