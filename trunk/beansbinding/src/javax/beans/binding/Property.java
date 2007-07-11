/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 * @author Shannon Hickey
 */
public interface Property<S, V>  {

    Class<? extends V> getValueType();

    V getValue();

    void setValue(V value);

    boolean isReadable();

    boolean isWriteable();

    boolean isObservable();

    boolean isComplete();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    PropertyChangeListener[] getPropertyChangeListeners();

    String toString();

    // optional methods follow

    void setSource(S source);

    S getSource();
    
    void setValidator(Validator<V> validator);

    Validator<V> getValidator();

    <F> void putConverter(Class<F> otherType, Converter<F, V> converter);

    <F> Converter<F, V> getConverter(Class<F> otherType);

    Map<Class<?>, Converter<?, V>> getConverters();

}
