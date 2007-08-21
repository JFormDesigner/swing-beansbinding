/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package sun.swing.binding;

import org.jdesktop.beans.binding.*;

/**
 * @author Shannon Hickey
 */
public class ColumnBinding extends Binding {

    private int column;
    private boolean calledInternally;

    public ColumnBinding(int column, Property columnSource, Property columnTarget, String name) {
        super(null, columnSource, null, columnTarget, name);
        this.column = column;
    }

    public final int getColumn() {
        return column;
    }

    protected final void setColumn(int column) {
        this.column = column;
    }

    public boolean isManaged() {
        return true;
    }

    public void bindInternal() {
        calledInternally = true;
        try {
            bind();
        } finally {
            calledInternally = false;
        }
    }

    public void unbindInternal() {
        calledInternally = true;
        try {
            unbind();
        } finally {
            calledInternally = false;
        }
    }
    
    public boolean bindImpl() {
        if (!calledInternally) {
            throw new IllegalStateException("this is a managed binding - don't call bind directly");
        }

        return true;
    }

    public boolean unbindImpl() {
        if (!calledInternally) {
            throw new IllegalStateException("this is a managed binding - don't call unbind directly");
        }
        
        return true;
    }

}
