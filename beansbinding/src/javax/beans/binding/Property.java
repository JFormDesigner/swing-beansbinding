/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;

/**
 * @author Shannon Hickey
 */
public interface Property<S, V>  {

    void setSource(S source);

    S getSource();

    Class<? extends V> getValueType();

    V getValue();

    void setValue(V value);

    boolean isReadable();

    boolean isWriteable();

    boolean isObservable();

    boolean isCompletePath();

    void setValidator(Validator<V> validator);

    Validator<V> getValidator();

    <F> void putConverter(Class<F> otherType, Converter<F, V> converter);

    Converter getConverter(Class<?> otherType);

    Map<Class<?>, Converter<?, V>> getConverters();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    PropertyChangeListener[] getPropertyChangeListeners();

    String toString();

}
