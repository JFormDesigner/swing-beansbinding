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

    public static <S, V> Property<S, V> unwriteableProperty(Property<? super S, ? extends V> property) {
        return new UnwriteableProperty(property);
    }

    private static class UnwriteableProperty<S, V> implements Property<S, V> {
        protected Property<? super S, ? extends V> property;

        public UnwriteableProperty(Property<? super S, ? extends V> property) {
            this.property = property;
        }

        public Class<? extends V> getWriteType(S source) {
            throw new UnsupportedOperationException("Unwriteable");
        }
        
        public V getValue(S source) {
            return property.getValue(source);
        }
        
        public void setValue(S source, V value) {
            throw new UnsupportedOperationException("Unwriteable");
        }
        
        public boolean isReadable(S source) {
            return property.isReadable(source);
        }
        
        public boolean isWriteable(S source) {
            return false;
        }
        
        public void addPropertyStateListener(S source, PropertyStateListener listener) {
            property.addPropertyStateListener(source, listener);
        }
        
        public void removePropertyStateListener(S source, PropertyStateListener listener) {
            property.removePropertyStateListener(source, listener);
        }
        
        public PropertyStateListener[] getPropertyStateListeners(S source) {
            return property.getPropertyStateListeners(source);
        }
        
        public String toString() {
            return getClass().getName() + "[" + property.toString() + "]";
        }
    }

}
