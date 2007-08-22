/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.el.impl.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.jdesktop.el.ELException;
import org.jdesktop.el.MethodNotFoundException;
import org.jdesktop.el.PropertyNotFoundException;

import org.jdesktop.el.impl.lang.ELSupport;

/**
 * Utilities for Managing Serialization and Reflection
 * 
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public class ReflectionUtil {

    protected static final String[] EMPTY_STRING = new String[0];

    protected static final String[] PRIMITIVE_NAMES = new String[] { "boolean",
            "byte", "char", "double", "float", "int", "long", "short", "void" };

    protected static final Class[] PRIMITIVES = new Class[] { boolean.class,
            byte.class, char.class, double.class, float.class, int.class,
            long.class, short.class, Void.TYPE };

    /**
     * 
     */
    private ReflectionUtil() {
        super();
    }

    public static Class forName(String name) throws ClassNotFoundException {
        if (null == name || "".equals(name)) {
            return null;
        }
        Class c = forNamePrimitive(name);
        if (c == null) {
            if (name.endsWith("[]")) {
                String nc = name.substring(0, name.length() - 2);
                c = Class.forName(nc, true, Thread.currentThread().getContextClassLoader());
                c = Array.newInstance(c, 0).getClass();
            } else {
                c = Class.forName(name, true, Thread.currentThread().getContextClassLoader());
            }
        }
        return c;
    }

    protected static Class forNamePrimitive(String name) {
        if (name.length() <= 8) {
            int p = Arrays.binarySearch(PRIMITIVE_NAMES, name);
            if (p >= 0) {
                return PRIMITIVES[p];
            }
        }
        return null;
    }

    /**
     * Converts an array of Class names to Class types
     * @param s
     * @return
     * @throws ClassNotFoundException
     */
    public static Class[] toTypeArray(String[] s) throws ClassNotFoundException {
        if (s == null)
            return null;
        Class[] c = new Class[s.length];
        for (int i = 0; i < s.length; i++) {
            c[i] = forName(s[i]);
        }
        return c;
    }

    /**
     * Converts an array of Class types to Class names
     * @param c
     * @return
     */
    public static String[] toTypeNameArray(Class[] c) {
        if (c == null)
            return null;
        String[] s = new String[c.length];
        for (int i = 0; i < c.length; i++) {
            s[i] = c[i].getName();
        }
        return s;
    }

    /**
     * Returns a method based on the criteria
     * @param base the object that owns the method
     * @param property the name of the method
     * @param paramTypes the parameter types to use
     * @return the method specified
     * @throws MethodNotFoundException
     */
    public static Method getMethod(Object base, Object property,
            Class[] paramTypes) throws MethodNotFoundException {
        if (base == null || property == null) {
            throw new MethodNotFoundException(MessageFactory.get(
                    "error.method.notfound", base, property,
                    paramString(paramTypes)));
        }

        String methodName = property.toString();

        Method method = getMethod(base.getClass(), methodName, paramTypes);
        if (method == null) {
            throw new MethodNotFoundException(MessageFactory.get(
                    "error.method.notfound", base, property,
                    paramString(paramTypes)));
        }
        return method;
    }

    /*
     * Get a public method form a public class or interface of a given method.
     * Note that if the base is an instance of a non-public class that
     * implements a public interface,  calling Class.getMethod() with the base
     * will not find the method.  To correct this, a version of the
     * same method must be found in a superclass or interface.
     **/

    static private Method getMethod(Class cl, String methodName,
                                    Class[] paramTypes) {

        Method m = null;
        try {
            m = cl.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }

        Class dclass  = m.getDeclaringClass();
        if (Modifier.isPublic(dclass.getModifiers())) {
            return m;
        }

        for (Class c: dclass.getInterfaces()) {
            m = getMethod(c, methodName, paramTypes);
            if (m != null) {
                return m;
            }
        }
        Class c = dclass.getSuperclass();
        if (c != null) {
            m = getMethod(c, methodName, paramTypes);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    protected static final String paramString(Class[] types) {
        if (types != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < types.length; i++) {
                sb.append(types[i].getName()).append(", ");
            }
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * @param base
     * @param property
     * @return
     * @throws ELException
     * @throws PropertyNotFoundException
     */
    public static PropertyDescriptor getPropertyDescriptor(Object base,
            Object property) throws ELException, PropertyNotFoundException {
        String name = ELSupport.coerceToString(property);
        PropertyDescriptor p = null;
        try {
            PropertyDescriptor[] desc = Introspector.getBeanInfo(
                    base.getClass()).getPropertyDescriptors();
            for (int i = 0; i < desc.length; i++) {
                if (desc[i].getName().equals(name)) {
                    return desc[i];
                }
            }
        } catch (IntrospectionException ie) {
            throw new ELException(ie);
        }
        throw new PropertyNotFoundException(MessageFactory.get(
                "error.property.notfound", base, name));
    }
}
