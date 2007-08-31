/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import java.util.*;
import javax.swing.*;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.ObjectProperty;
import org.jdesktop.beansbinding.Property;

/**
 * A factory class for creating instances of the custom Swing {@code Binding}
 * implementations provided by this package. See the
 * <a href="package-summary.html">package summary</a> for full details on
 * binding to Swing components.
 *
 * @author Shannon Hickey
 */
public class SwingBindings {
    
    private SwingBindings() {}

    public static <E> JListBinding<E, List<E>, JList> createListBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JList targetJList) {
        return new JListBinding<E, List<E>, JList>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetJList, ObjectProperty.<JList>create(), null);
    }

    public static <E> JListBinding<E, List<E>, JList> createListBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JList targetJList, String name) {
        return new JListBinding<E, List<E>, JList>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetJList, ObjectProperty.<JList>create(), name);
    }


    public static <E, SS> JListBinding<E, SS, JList> createListBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JList targetJList) {
        return new JListBinding<E, SS, JList>(strategy, sourceObject, sourceListProperty, targetJList, ObjectProperty.<JList>create(), null);
    }
    
    public static <E, SS> JListBinding<E, SS, JList> createListBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JList targetJList, String name) {
        return new JListBinding<E, SS, JList>(strategy, sourceObject, sourceListProperty, targetJList, ObjectProperty.<JList>create(), name);
    }


    public static <E, TS> JListBinding<E, List<E>, TS> createListBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JList> targetJListProperty) {
        return new JListBinding<E, List<E>, TS>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetObject, targetJListProperty, null);
    }
    
    public static <E, TS> JListBinding<E, List<E>, TS> createListBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JList> targetJListProperty, String name) {
        return new JListBinding<E, List<E>, TS>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetObject, targetJListProperty, name);
    }


    public static <E, SS, TS> JListBinding<E, SS, TS> createListBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JList> targetJListProperty) {
        return new JListBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJListProperty, null);
    }

    public static <E, SS, TS> JListBinding<E, SS, TS> createListBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JList> targetJListProperty, String name) {
        return new JListBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJListProperty, name);
    }

    

    public static <E> JTableBinding<E, List<E>, JTable> createTableBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JTable targetJTable) {
        return new JTableBinding<E, List<E>, JTable>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetJTable, ObjectProperty.<JTable>create(), null);
    }

    public static <E> JTableBinding<E, List<E>, JTable> createTableBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JTable targetJTable, String name) {
        return new JTableBinding<E, List<E>, JTable>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetJTable, ObjectProperty.<JTable>create(), name);
    }


    public static <E, SS> JTableBinding<E, SS, JTable> createTableBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JTable targetJTable) {
        return new JTableBinding<E, SS, JTable>(strategy, sourceObject, sourceListProperty, targetJTable, ObjectProperty.<JTable>create(), null);
    }

    public static <E, SS> JTableBinding<E, SS, JTable> createTableBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JTable targetJTable, String name) {
        return new JTableBinding<E, SS, JTable>(strategy, sourceObject, sourceListProperty, targetJTable, ObjectProperty.<JTable>create(), name);
    }

    
    public static <E, TS> JTableBinding<E, List<E>, TS> createTableBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty) {
        return new JTableBinding<E, List<E>, TS>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetObject, targetJTableProperty, null);
    }

    public static <E, TS> JTableBinding<E, List<E>, TS> createTableBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty, String name) {
        return new JTableBinding<E, List<E>, TS>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetObject, targetJTableProperty, name);
    }


    public static <E, SS, TS> JTableBinding<E, SS, TS> createTableBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty) {
        return new JTableBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJTableProperty, null);
    }
    
    public static <E, SS, TS> JTableBinding<E, SS, TS> createTableBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JTable> targetJTableProperty, String name) {
        return new JTableBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJTableProperty, name);
    }



    public static <E> JComboBoxBinding<E, List<E>, JComboBox> createComboBoxBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JComboBox targetJComboBox) {
        return new JComboBoxBinding<E, List<E>, JComboBox>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetJComboBox, ObjectProperty.<JComboBox>create(), null);
    }

    public static <E> JComboBoxBinding<E, List<E>, JComboBox> createComboBoxBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, JComboBox targetJComboBox, String name) {
        return new JComboBoxBinding<E, List<E>, JComboBox>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetJComboBox, ObjectProperty.<JComboBox>create(), name);
    }


    public static <E, SS> JComboBoxBinding<E, SS, JComboBox> createComboBoxBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JComboBox targetJComboBox) {
        return new JComboBoxBinding<E, SS, JComboBox>(strategy, sourceObject, sourceListProperty, targetJComboBox, ObjectProperty.<JComboBox>create(), null);
    }
    
    public static <E, SS> JComboBoxBinding<E, SS, JComboBox> createComboBoxBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, JComboBox targetJComboBox, String name) {
        return new JComboBoxBinding<E, SS, JComboBox>(strategy, sourceObject, sourceListProperty, targetJComboBox, ObjectProperty.<JComboBox>create(), name);
    }


    public static <E, TS> JComboBoxBinding<E, List<E>, TS> createComboBoxBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JComboBox> targetJComboBoxProperty) {
        return new JComboBoxBinding<E, List<E>, TS>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetObject, targetJComboBoxProperty, null);
    }
    
    public static <E, TS> JComboBoxBinding<E, List<E>, TS> createComboBoxBinding(AutoBinding.UpdateStrategy strategy, List<E> sourceList, TS targetObject, Property<TS, ? extends JComboBox> targetJComboBoxProperty, String name) {
        return new JComboBoxBinding<E, List<E>, TS>(strategy, sourceList, ObjectProperty.<List<E>>create(), targetObject, targetJComboBoxProperty, name);
    }


    public static <E, SS, TS> JComboBoxBinding<E, SS, TS> createComboBoxBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JComboBox> targetJComboBoxProperty) {
        return new JComboBoxBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJComboBoxProperty, null);
    }

    public static <E, SS, TS> JComboBoxBinding<E, SS, TS> createComboBoxBinding(AutoBinding.UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JComboBox> targetJComboBoxProperty, String name) {
        return new JComboBoxBinding<E, SS, TS>(strategy, sourceObject, sourceListProperty, targetObject, targetJComboBoxProperty, name);
    }

}
