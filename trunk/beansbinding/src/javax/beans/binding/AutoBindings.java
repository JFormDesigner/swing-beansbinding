/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.*;
import javax.swing.*;
import javax.beans.binding.*;

/**
 * @author Shannon Hickey
 */
public class Bindings {
    
    private Bindings() {}

    public static <SS, TS, TV> Binding<SS, SS, TS, TV> createBinding(SS source, TS targetObject, Property<TS, TV> targetProperty) {
        return new Binding<SS, SS, TS, TV>(source, new ObjectProperty<SS>(), targetObject, targetProperty, null);
    }

    public static <SS, SV, TS, TV> Binding<SS, SV, TS, TV> createBinding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty) {
        return new Binding<SS, SV, TS, TV>(sourceObject, sourceProperty, targetObject, targetProperty, null);
    }

    public static <SS, TS, TV> Binding<SS, SS, TS, TV> createBinding(SS source, TS targetObject, Property<TS, TV> targetProperty, String name) {
        return new Binding<SS, SS, TS, TV>(source, new ObjectProperty<SS>(), targetObject, targetProperty, name);
    }

    public static <SS, SV, TS, TV> Binding<SS, SV, TS, TV> createBinding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        return new Binding<SS, SV, TS, TV>(sourceObject, sourceProperty, targetObject, targetProperty, name);
    }

}
