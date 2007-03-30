/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 * {@code BindingConverter} is used by {@code Binding} to convert
 * values between the source and target. Use the {@code setConverter} method of
 * {@code Binding} to install a converter on a binding. The following example
 * illustrates a trivial {@code String} to {@code Integer} converter:
 * <pre>
 * BindingConverter stringToIntegerConverter = new BindingConverter() {
 *     public Object sourceToTarget(Object value) {
 *         try {
 *           return Integer.parseInt((String)value);
 *         } catch (NumberFormatException nfe) {
 *             // String isn't a recognized integer, rethrow
 *             // IllegalArgumentException.
 *             throw new IllegalArgumentException(nfe);
 *         }
 *     }
 * };
 * binding.setConverter(stringToIntegerConverter);
 * </pre>
 * For one-way bindings, it is unnecessary to implement the target to source
 * conversion, as such, the {@code targetToSource} method is concrete.
 *
 * @author sky
 */
public abstract class BindingConverter {
    static final BindingConverter STRING_TO_INT_CONVERTER = new StringToIntConverter();
    static final BindingConverter INT_TO_STRING_CONVERTER = new IntToStringConverter();

    static final BindingConverter BOOLEAN_TO_INT_CONVERTER = new BooleanToIntConverter();
    static final BindingConverter INT_TO_BOOLEAN_CONVERTER = new IntToBooleanConverter();
    
    static final BindingConverter OBJECT_TO_INT_CONVERTER = new ObjectToIntConverter();
    
    /**
     * Converts a value from the source to the target.
     *
     * @param value the value to convert
     * @return the converted value
     *
     * @throws ClassCastException if {@code value} is not of a type appropriate
     *         for this converter
     * @throws IllegalArgumentException if {@code value} can not be converted
     */
    public abstract Object sourceToTarget(Object value);
    
    /**
     * Converts a value from the target to source. This implementation
     * returns the supplied value, without any conversion.
     *
     * @param value the value to convert
     * @return the converted value
     *
     * @throws ClassCastException if {@code value} is not of a type appropriate
     *         for this converter
     * @throws IllegalArgumentException if {@code value} can not be converted
     */
    public Object targetToSource(Object value) {
        return value;
    }
    
    
    private final static class StringToIntConverter extends BindingConverter {
        public Object targetToSource(Object value) {
            return Integer.toString((Integer)value);
        }

        public Object sourceToTarget(Object value) {
            return Integer.parseInt((String)value);
        }
    }
    

    private final static class IntToStringConverter extends BindingConverter {
        public Object targetToSource(Object value) {
            return Integer.parseInt((String)value);
        }

        public Object sourceToTarget(Object value) {
            return Integer.toString((Integer)value);
        }
    }

    
    private final static class BooleanToIntConverter extends BindingConverter {
        public Object targetToSource(Object value) {
            if (((Integer)value).intValue() == 0) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        public Object sourceToTarget(Object value) {
            if (((Boolean)value).booleanValue()) {
                return 1;
            }
            return 0;
        }
    }

    
    private final static class IntToBooleanConverter extends BindingConverter {
        public Object targetToSource(Object value) {
            if (((Boolean)value).booleanValue()) {
                return 1;
            }
            return 0;
        }

        public Object sourceToTarget(Object value) {
            if (((Integer)value).intValue() == 0) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }
    }

    
    private static final class ObjectToIntConverter extends BindingConverter {
        // PENDING: talk with Honza, this seems a bit hoaky.
        public Object targetToSource(Object value) {
            return value;
        }

        public Object sourceToTarget(Object value) {
            return Integer.parseInt(value.toString());
        }
    }
}
