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

    public static <SS, TS, TV> AutoBinding<SS, SS, TS, TV> createAutoBinding(AutoBinding.UpdateStrategy strategy, SS source, TS targetObject, Property<TS, TV> targetProperty) {
        return new AutoBinding<SS, SS, TS, TV>(strategy, source, ObjectProperty.<SS>create(), targetObject, targetProperty, null);
    }

    public static <SS, SV, TS, TV> AutoBinding<SS, SV, TS, TV> createAutoBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty) {
        return new AutoBinding<SS, SV, TS, TV>(strategy, sourceObject, sourceProperty, targetObject, targetProperty, null);
    }

    public static <SS, TS, TV> AutoBinding<SS, SS, TS, TV> createAutoBinding(AutoBinding.UpdateStrategy strategy, SS source, TS targetObject, Property<TS, TV> targetProperty, String name) {
        return new AutoBinding<SS, SS, TS, TV>(strategy, source, ObjectProperty.<SS>create(), targetObject, targetProperty, name);
    }

    public static <SS, SV, TS, TV> AutoBinding<SS, SV, TS, TV> createAutoBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        return new AutoBinding<SS, SV, TS, TV>(strategy, sourceObject, sourceProperty, targetObject, targetProperty, name);
    }

}
