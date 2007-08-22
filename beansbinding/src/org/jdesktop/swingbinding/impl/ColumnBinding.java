/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding.impl;

import org.jdesktop.beansbinding.*;

/**
 * @author Shannon Hickey
 */
public class ColumnBinding extends Binding {

    private int column;

    public ColumnBinding(int column, Property columnSource, Property columnTarget, String name) {
        super(null, columnSource, null, columnTarget, name);
        this.column = column;
        setManaged(true);
    }

    public final int getColumn() {
        return column;
    }

    protected final void setColumn(int column) {
        this.column = column;
    }

    public void bindImpl() {}

    public void unbindImpl() {}

}
