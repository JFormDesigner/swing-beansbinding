/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import java.util.*;
import javax.swing.*;
import javax.beans.binding.*;

/**
 * @author Shannon Hickey
 */
public class SwingBindings {
    
    private SwingBindings() {}

    public static <E> JListBinding<E, List<E>, JList> createListBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JList targetJList) {
        return new JListBinding<E, List<E>, JList>(strategy, sourceList, new ObjectProperty<List<E>>(), targetJList, new ObjectProperty<JList>(), null);
    }

    public static <E> JListBinding<E, List<E>, JList> createListBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JList targetJList, String name) {
        return new JListBinding<E, List<E>, JList>(strategy, sourceList, new ObjectProperty<List<E>>(), targetJList, new ObjectProperty<JList>(), name);
    }


    public static <E, SS> JListBinding<E, SS, JList> createListBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JList targetJList) {
        return new JListBinding<E, SS, JList>(strategy, sourceObject, sourceListProperty, targetJList, new ObjectProperty<JList>(), null);
    }
    
    public static <E, SS> JListBinding<E, SS, JList> createListBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JList targetJList, String name) {
        return new JListBinding<E, SS, JList>(strategy, sourceObject, sourceListProperty, targetJList, new ObjectProperty<JList>(), name);
    }


    public static <E, TS> JListBinding<E, List<E>, TS> createListBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JList> targetJListProperty) {
        return new JListBinding<E, List<E>, TS>(strategy, sourceList, new ObjectProperty<List<E>>(), targetObject, targetJListProperty, null);
    }
    
    public static <E, TS> JListBinding<E, List<E>, TS> createListBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JList> targetJListProperty, String name) {
        return new JListBinding<E, List<E>, TS>(strategy, sourceList, new ObjectProperty<List<E>>(), targetObject, targetJListProperty, name);
    }


    public static <E, SS, TS> JListBinding<E, SS, TS> createListBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JList> targetJListProperty) {
        return new JListBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJListProperty, null);
    }

    public static <E, SS, TS> JListBinding<E, SS, TS> createListBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JList> targetJListProperty, String name) {
        return new JListBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJListProperty, name);
    }

    

    public static <E> JTableBinding<E, List<E>, JTable> createTableBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JTable targetJTable) {
        return new JTableBinding<E, List<E>, JTable>(strategy, sourceList, new ObjectProperty<List<E>>(), targetJTable, new ObjectProperty<JTable>(), null);
    }

    public static <E> JTableBinding<E, List<E>, JTable> createTableBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JTable targetJTable, String name) {
        return new JTableBinding<E, List<E>, JTable>(strategy, sourceList, new ObjectProperty<List<E>>(), targetJTable, new ObjectProperty<JTable>(), name);
    }


    public static <E, SS> JTableBinding<E, SS, JTable> createTableBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JTable targetJTable) {
        return new JTableBinding<E, SS, JTable>(strategy, sourceObject, sourceListProperty, targetJTable, new ObjectProperty<JTable>(), null);
    }

    public static <E, SS> JTableBinding<E, SS, JTable> createTableBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JTable targetJTable, String name) {
        return new JTableBinding<E, SS, JTable>(strategy, sourceObject, sourceListProperty, targetJTable, new ObjectProperty<JTable>(), name);
    }

    
    public static <E, TS> JTableBinding<E, List<E>, TS> createTableBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty) {
        return new JTableBinding<E, List<E>, TS>(strategy, sourceList, new ObjectProperty<List<E>>(), targetObject, targetJTableProperty, null);
    }

    public static <E, TS> JTableBinding<E, List<E>, TS> createTableBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty, String name) {
        return new JTableBinding<E, List<E>, TS>(strategy, sourceList, new ObjectProperty<List<E>>(), targetObject, targetJTableProperty, name);
    }


    public static <E, SS, TS> JTableBinding<E, SS, TS> createTableBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty) {
        return new JTableBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJTableProperty, null);
    }
    
    public static <E, SS, TS> JTableBinding<E, SS, TS> createTableBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty, String name) {
        return new JTableBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJTableProperty, name);
    }

}
