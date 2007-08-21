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

    public static <SS, TS, TV> AutoBinding<SS, SS, TS, TV> createAutoBinding(SS source, TS targetObject, Property<TS, TV> targetProperty) {
        return new AutoBinding<SS, SS, TS, TV>(source, new ObjectProperty<SS>(), targetObject, targetProperty, null, null);
    }

    public static <SS, SV, TS, TV> AutoBinding<SS, SV, TS, TV> createAutoBinding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty) {
        return new AutoBinding<SS, SV, TS, TV>(sourceObject, sourceProperty, targetObject, targetProperty, null, null);
    }

    public static <SS, TS, TV> AutoBinding<SS, SS, TS, TV> createAutoBinding(SS source, TS targetObject, Property<TS, TV> targetProperty, String name) {
        return new AutoBinding<SS, SS, TS, TV>(source, new ObjectProperty<SS>(), targetObject, targetProperty, null, name);
    }

    public static <SS, SV, TS, TV> AutoBinding<SS, SV, TS, TV> createAutoBinding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        return new AutoBinding<SS, SV, TS, TV>(sourceObject, sourceProperty, targetObject, targetProperty, null, name);
    }

    public static <SS, TS, TV> AutoBinding<SS, SS, TS, TV> createAutoBinding(SS source, TS targetObject, Property<TS, TV> targetProperty, AutoBinding.UpdateStrategy strategy) {
        return new AutoBinding<SS, SS, TS, TV>(source, new ObjectProperty<SS>(), targetObject, targetProperty, strategy, null);
    }

    public static <SS, SV, TS, TV> AutoBinding<SS, SV, TS, TV> createAutoBinding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, AutoBinding.UpdateStrategy strategy) {
        return new AutoBinding<SS, SV, TS, TV>(sourceObject, sourceProperty, targetObject, targetProperty, strategy, null);
    }

    public static <SS, TS, TV> AutoBinding<SS, SS, TS, TV> createAutoBinding(SS source, TS targetObject, Property<TS, TV> targetProperty, AutoBinding.UpdateStrategy strategy, String name) {
        return new AutoBinding<SS, SS, TS, TV>(source, new ObjectProperty<SS>(), targetObject, targetProperty, strategy, name);
    }

    public static <SS, SV, TS, TV> AutoBinding<SS, SV, TS, TV> createAutoBinding(SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, AutoBinding.UpdateStrategy strategy, String name) {
        return new AutoBinding<SS, SV, TS, TV>(sourceObject, sourceProperty, targetObject, targetProperty, strategy, name);
    }

}
