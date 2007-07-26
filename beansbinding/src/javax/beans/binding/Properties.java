/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * @author Shannon Hickey
 */
public class Properties  {

    private Properties() {}

    public static <V> Property<V> unwriteableProperty(Property<? extends V> property) {
        return new UnwriteableProperty(property);
    }

    public static <S, V> SourceableProperty<S, V> unwriteableSourceableProperty(SourceableProperty<S, ? extends V> property) {
        return new UnwriteableSourceableProperty(property);
    }

    private static class UnwriteableSourceableProperty<S, V> extends UnwriteableProperty<V> implements SourceableProperty<S, V> {

        public UnwriteableSourceableProperty(SourceableProperty<S, ? extends V> property) {
            super(property);
        }

        public void setSource(S source) {
            ((SourceableProperty<S, ? extends V>)property).setSource(source);
        }

        public S getSource() {
            return ((SourceableProperty<S, ? extends V>)property).getSource();
        }
    }

    private static class UnwriteableProperty<V> implements Property<V> {
        protected Property<? extends V> property;

        public UnwriteableProperty(Property<? extends V> property) {
            this.property = property;
        }

        public Class<? extends V> getWriteType() {
            throw new UnsupportedOperationException("Unwriteable");
        }
        
        public V getValue() {
            return property.getValue();
        }
        
        public void setValue(V value) {
            throw new UnsupportedOperationException("Unwriteable");
        }
        
        public boolean isReadable() {
            return property.isReadable();
        }
        
        public boolean isWriteable() {
            return false;
        }
        
        public void addPropertyStateListener(PropertyStateListener listener) {
            property.addPropertyStateListener(listener);
        }
        
        public void removePropertyStateListener(PropertyStateListener listener) {
            property.removePropertyStateListener(listener);
        }
        
        public PropertyStateListener[] getPropertyStateListeners() {
            return property.getPropertyStateListeners();
        }
        
        public String toString() {
            return getClass().getName() + "[" + property.toString() + "]";
        }
    }

}
