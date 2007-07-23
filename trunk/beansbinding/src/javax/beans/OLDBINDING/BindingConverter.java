/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.OLDBINDING;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * {@code BindingConverter} is used by {@code Binding} to convert values
 * between the source and the target. Use the {@code setConverter} method of
 * {@code Binding} to install a converter on a binding. The following example
 * illustrates a trivial converter that converts between an {@code integer}
 * source and a {@code String} target:
 * <pre>
 * BindingConverter intToStringConverter = new BindingConverter() {
 *     public Object sourceToTarget(Object value) {
 *         return Integer.toString((Integer)value);
 *     }
 *
 *     public Object targetToSource(Object value) {
 *         return Integer.parseInt((String)value);
 *     }
 *
 * };
 * binding.setConverter(stringToIntegerConverter);
 * </pre>
 * For one-way bindings, it is unnecessary to implement the target to source
 * conversion. As such, the {@code targetToSource} method is concrete. The
 * default implementation throws {@code IllegalArgumentException} indicating
 * that it cannot convert target values back to the source.
 *
 * @author sky
 * @author Shannon Hickey
 * @author Jan Stola
 */
public abstract class BindingConverter {

    static final BindingConverter BYTE_TO_STRING_CONVERTER = new ByteToStringConverter();
    static final BindingConverter STRING_TO_BYTE_CONVERTER = new ReversedConverter(BYTE_TO_STRING_CONVERTER);

    static final BindingConverter SHORT_TO_STRING_CONVERTER = new ShortToStringConverter();
    static final BindingConverter STRING_TO_SHORT_CONVERTER = new ReversedConverter(SHORT_TO_STRING_CONVERTER);

    static final BindingConverter INT_TO_STRING_CONVERTER = new IntToStringConverter();
    static final BindingConverter STRING_TO_INT_CONVERTER = new ReversedConverter(INT_TO_STRING_CONVERTER);

    static final BindingConverter LONG_TO_STRING_CONVERTER = new LongToStringConverter();
    static final BindingConverter STRING_TO_LONG_CONVERTER = new ReversedConverter(LONG_TO_STRING_CONVERTER);

    static final BindingConverter FLOAT_TO_STRING_CONVERTER = new FloatToStringConverter();
    static final BindingConverter STRING_TO_FLOAT_CONVERTER = new ReversedConverter(FLOAT_TO_STRING_CONVERTER);

    static final BindingConverter DOUBLE_TO_STRING_CONVERTER = new DoubleToStringConverter();
    static final BindingConverter STRING_TO_DOUBLE_CONVERTER = new ReversedConverter(DOUBLE_TO_STRING_CONVERTER);

    static final BindingConverter CHAR_TO_STRING_CONVERTER = new CharToStringConverter();
    static final BindingConverter STRING_TO_CHAR_CONVERTER = new ReversedConverter(CHAR_TO_STRING_CONVERTER);

    static final BindingConverter BOOLEAN_TO_STRING_CONVERTER = new BooleanToStringConverter();
    static final BindingConverter STRING_TO_BOOLEAN_CONVERTER = new ReversedConverter(BOOLEAN_TO_STRING_CONVERTER);

    static final BindingConverter INT_TO_BOOLEAN_CONVERTER = new IntToBooleanConverter();
    static final BindingConverter BOOLEAN_TO_INT_CONVERTER = new ReversedConverter(INT_TO_BOOLEAN_CONVERTER);

    static final BindingConverter BIGINTEGER_TO_STRING_CONVERTER = new BigIntegerToStringConverter();
    static final BindingConverter STRING_TO_BIGINTEGER_CONVERTER = new ReversedConverter(BIGINTEGER_TO_STRING_CONVERTER);

    static final BindingConverter BIGDECIMAL_TO_STRING_CONVERTER = new BigDecimalToStringConverter();
    static final BindingConverter STRING_TO_BIGDECIMAL_CONVERTER = new ReversedConverter(BIGDECIMAL_TO_STRING_CONVERTER);

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
     * <p>
     * The default implementation throws {@code IllegalArgumentException}
     * indicating that the converter cannot convert from target back to
     * the source.
     *
     * @param value the value to convert
     * @return the converted value
     *
     * @throws ClassCastException if {@code value} is not of a type appropriate
     *         for this converter
     * @throws IllegalArgumentException if {@code value} can not be converted
     */
    public Object targetToSource(Object value) {
        throw new IllegalArgumentException("One-way converter.");
    }

    private final static class ReversedConverter extends BindingConverter {
        private BindingConverter converter;

        ReversedConverter(BindingConverter converter) {
            if (converter == null) {
                throw new NullPointerException();
            }
            this.converter = converter;
        }

        public Object sourceToTarget(Object value) {
            return converter.targetToSource(value);
        }

        public Object targetToSource(Object value) {
            return converter.sourceToTarget(value);
        }
    }

    private final static class ByteToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return Byte.toString((Byte)value);
        }

        public Object targetToSource(Object value) {
            return Byte.parseByte((String)value);
        }
    }

    private final static class ShortToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return Short.toString((Short)value);
        }

        public Object targetToSource(Object value) {
            return Short.parseShort((String)value);
        }
    }

    private final static class IntToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return Integer.toString((Integer)value);
        }

        public Object targetToSource(Object value) {
            return Integer.parseInt((String)value);
        }
    }

    private final static class LongToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return Long.toString((Long)value);
        }

        public Object targetToSource(Object value) {
            return Long.parseLong((String)value);
        }
    }

    private final static class FloatToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return Float.toString((Float)value);
        }

        public Object targetToSource(Object value) {
            return Float.parseFloat((String)value);
        }
    }

    private final static class DoubleToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return Double.toString((Double)value);
        }

        public Object targetToSource(Object value) {
            return Double.parseDouble((String)value);
        }
    }

    private final static class BigIntegerToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return ((BigInteger)value).toString();
        }

        public Object targetToSource(Object value) {
            return new BigInteger((String)value);
        }
    }

    private final static class BigDecimalToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return ((BigDecimal)value).toString();
        }

        public Object targetToSource(Object value) {
            return new BigDecimal((String)value);
        }
    }

    private final static class CharToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return ((Character)value).toString();
        }

        public Object targetToSource(Object value) {
            String s = ((String)value);
            // PENDING(shannonh) - don't know if I like this
            return (s.length() == 0) ? '?' : s.charAt(0);
        }
    }

    private final static class BooleanToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return ((Boolean)value).toString();
        }

        public Object targetToSource(Object value) {
            return new Boolean((String)value);
        }
    }
    
    private final static class IntToBooleanConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            if (((Integer)value).intValue() == 0) {
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }

        public Object targetToSource(Object value) {
            if (((Boolean)value).booleanValue()) {
                return 1;
            }
            return 0;
        }
    }

    private final static class ObjectToStringConverter extends BindingConverter {
        public Object sourceToTarget(Object value) {
            return value.toString();
        }
    }

    private static final boolean isByteClass(Class<?> type) {
        return (type == byte.class || type == Byte.class);
    }

    private static final boolean isShortClass(Class<?> type) {
        return (type == short.class || type == Short.class);
    }

    private static final boolean isIntClass(Class<?> type) {
        return (type == int.class || type == Integer.class);
    }

    private static final boolean isLongClass(Class<?> type) {
        return (type == long.class || type == Long.class);
    }

    private static final boolean isFloatClass(Class<?> type) {
        return (type == float.class || type == Float.class);
    }

    private static final boolean isDoubleClass(Class<?> type) {
        return (type == double.class || type == Double.class);
    }
    
    private static final boolean isBooleanClass(Class<?> type) {
        return (type == boolean.class || type == Boolean.class);
    }

    private static final boolean isCharClass(Class<?> type) {
        return (type == char.class || type == Character.class);
    }

    static BindingConverter getConverter(Class<?> sourceType, Class<?> targetType) {
        if (sourceType == targetType) {
            return null;
        }

        if (targetType == String.class) {
            if (isByteClass(sourceType)) {
                return BYTE_TO_STRING_CONVERTER;
            } else if (isShortClass(sourceType)) {
                return SHORT_TO_STRING_CONVERTER;
            } else if (isIntClass(sourceType)) {
                return INT_TO_STRING_CONVERTER;
            } else if (isLongClass(sourceType)) {
                return LONG_TO_STRING_CONVERTER;
            } else if (isFloatClass(sourceType)) {
                return FLOAT_TO_STRING_CONVERTER;
            } else if (isDoubleClass(sourceType)) {
                return DOUBLE_TO_STRING_CONVERTER;
            } else if (isBooleanClass(sourceType)) {
                return BOOLEAN_TO_STRING_CONVERTER;
            } else if (isCharClass(sourceType)) {
                return CHAR_TO_STRING_CONVERTER;
            } else if (sourceType == BigInteger.class) {
                return BIGINTEGER_TO_STRING_CONVERTER;
            } else if (sourceType == BigDecimal.class) {
                return BIGDECIMAL_TO_STRING_CONVERTER;
            }
        } else if (sourceType == String.class) {
            if (isByteClass(targetType)) {
                return STRING_TO_BYTE_CONVERTER;
            } else if (isShortClass(targetType)) {
                return STRING_TO_SHORT_CONVERTER;
            } else if (isIntClass(targetType)) {
                return STRING_TO_INT_CONVERTER;
            } else if (isLongClass(targetType)) {
                return STRING_TO_LONG_CONVERTER;
            } else if (isFloatClass(targetType)) {
                return STRING_TO_FLOAT_CONVERTER;
            } else if (isDoubleClass(targetType)) {
                return STRING_TO_DOUBLE_CONVERTER;
            } else if (isBooleanClass(targetType)) {
                return STRING_TO_BOOLEAN_CONVERTER;
            } else if (isCharClass(targetType)) {
                return STRING_TO_CHAR_CONVERTER;
            } else if (targetType == BigInteger.class) {
                return STRING_TO_BIGINTEGER_CONVERTER;
            } else if (targetType == BigDecimal.class) {
                return STRING_TO_BIGDECIMAL_CONVERTER;
            }
        } else if (isIntClass(sourceType) && isBooleanClass(targetType)) {
            return INT_TO_BOOLEAN_CONVERTER;
        } else if (isBooleanClass(sourceType) && isIntClass(targetType)) {
            return BOOLEAN_TO_INT_CONVERTER;
        }

        return null;
    }
}
