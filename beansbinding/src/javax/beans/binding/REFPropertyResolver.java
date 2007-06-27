/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * @author Shannon Hickey
 */
public class REFPropertyResolver<S> extends PropertyResolver<S> {

    private S source;
    private String path;

    public REFPropertyResolver(S source, String path) {
        this.source = source;
        this.path = path;
    }

    @Override
    public void setSource(S source) {
        this.source = source;
    }

    @Override
    public S getSource() {
        return source;
    }

    @Override
    public Class<?> getValueType() {
        return Object.class;
    }

    @Override
    public Object getValue() {
        return "Foo";
    }

    @Override
    public void setValue(Object value) {
    }

    @Override
    public boolean isWriteable() {
        return true;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean isObservable() {
        return true;
    }
}
