/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * @author Shannon Hickey
 */
public interface SourceableProperty<S, V> extends Property<V> {

    void setSource(S source);

    S getSource();

}
