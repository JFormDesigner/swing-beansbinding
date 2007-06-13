/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.swing.binding;

import javax.beans.binding.ext.PropertyDelegateFactory;
import javax.beans.binding.ext.PropertyDelegateProvider;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;
import javax.beans.binding.Binding.Parameter;
import javax.swing.JSlider;

/**
 * {@code SwingBindingSupport} provides constants and documentation for
 * binding to Swing components. The following describes how to bind to
 * Swing components.
 *
 * <h3>JCheckBox</h3>
 *
 * The primary property to bind is the "selected" property. The following
 * illustrates creating a binding targetting a {@code JCheckBox} and specifying 
 * it should not disable if the source contains an incomplete path:
 *
 * <pre>
 *   Binding binding = new Binding(source, sourceExpression, jCheckBox, "selected");
 *   binding.setValue(SwingBindingSupport.DisableOnIncompletePathParameter, false);
 * </pre>
 *
 * <h3><a name="JComboBoxBinding"></a>JComboBox</h3>
 *
 * Binding to a {@code JComboBox} is a two phase process. You first bind
 * to the "elements" property. The "elements" property specifies the
 * contents of the {@code JComboBox}:
 *
 * <pre>
 *   List<BugTypes> bugTypes;
 *   // Create the binding specifying the 
 *   Binding binding = new Binding(bugTypes, null, jComboBox, "elements");
 *   // Specify that each element is to be shown using the 'description'
 *   // property. If this is not specified, the elements in the list are
 *   // used used directly.
 *   binding.addBinding("${description}", null);
 * </pre>
 *
 * The next step is to bind to the selection. There are two ways to do this.
 * The first is to bind to an element of this list. This is useful if
 * the object controlling the selection property is of the same type as that
 * of the elements of the {@code JComboBox}:
 *
* <pre>
 *   Binding binding = new Binding(selection, "${bugType}", 
 *                                 jComboBox, "selectedElement");
 *   // Specify that each element is to be shown using the 'description'
 *   // property. If this is not specified, the elements in the list are
 *   // used used directly.
 *   binding.addBinding("${description}", null);
 * </pre>
 *
 * The second option is to bind the selection to a property of the elements
 * in the list. This involves binding to the "selectedElementProperty" and 
 * specifying the expression to apply to each element when binding to the elements.
 * The following example illustrates this:
 *
 * <pre>
 *   List<BugTypes> bugTypes;
 *   // Create the binding specifying the 
 *   Binding binding = new Binding(bugTypes, null, jComboBox, "elements");
 *   // Specify that the "selectedElementProperty" corresponds to
 *   // selectedObject.id.
 *   binding.setValue(SwingBindingSupport.ComboBoxSelectedObjectPropertyParameter, "${id}");
 *
 *   // Bind to the selection
 *   binding = new Binding(source, "${id}", jComboBox, "selectedElementProperty");
 * </pre>
 *
 * <h3>JList</h3>
 *
 * The primary property to bind is the "elements" property. Binding to
 * the "elements" property results in creating and setting a custom
 * {@code ListModel} on the target {@code JList}. The source 
 * property must resolve to a {@code List}. If the source property resolves
 * to an {@code ObservableList}, the {@code ListModel} tracks changes as 
 * appropriate. A specific property can be specified for each element using
 * a child {@code Binding}. The following illustrates creating a binding 
 * targetting a {@code JList}. The value for each element is obtained 
 * using the 'firstName' property of each {@code Customer}.
 *
 * <pre>
 *   ObservableList<Customer> customers;
 *   // Create the binding for the List.
 *   Binding binding = new Binding(customers, null, jList, "elements");
 *   // Specify getValueAt is to return the 'firstName' property of
 *   // each element.
 *   binding.addBinding("${firstName}", null);
 * </pre>
 *
 * The property delegate for {@code JList} also provides support for the
 * "selectedElement" and "selectedElements" properties. The "selectedElement"
 * property corresponds to the selected element (in terms of an element of
 * the {@code List} bound to the "elements" property). The "selectedElements"
 * property is a {@code List} of the selected elements. Both values change
 * as the selection of the {@code JList} is modified.
 *
 * <h3>JSlider</h3>
 *
 * The primary property to bind is the "value" property. The following
 * illustrates creating a binding targetting a {@code JSlider}. As disable
 * on incomplete path has not been specified, the slider disables if the
 * source path is incomplete.
 *
 * <pre>
 *   Binding binding = new Binding(source, sourceExpression, jSlider, "value");
 * </pre>
 *
 * <h3>JTable</h3>
 *
 * {@code JTable} provides similar properties to that of {@code JList}; the
 * "elements" properties, of type {@code List}, specifies the contents of
 * the {@code JTable} (or more correctly, the {@code TableModel}), and
 * the "selectedElement" and "selectedElements" properties may be used to
 * track changes in the selection. When binding to a {@code JTable}, you
 * must specify how the value for each column is obtained. This is done
 * using the binding property {@code TableColumnParameter}. The following
 * illustrates creating a binding targetting a {@code JTable}. Two columns
 * are created, the first using the property "firstName", and the second
 * "lastName".
 *
 * <pre>
 *   ObservableList<Customer> customers;
 *   // Create the binding for the List.
 *   Binding binding = new Binding(customers, null, jTable, "elements");
 *   // Specify the first column should use the "firstName" property
 *   binding.addBinding("firstName", null).setValue(
 *                      SwingBindingSupport.TableColumnParameter, 0);
 *   // Specify the second column should use the "lastName" property
 *   binding.addBinding("lastName", null).setValue(
 *                      SwingBindingSupport.TableColumnParameter, 1);
 * </pre>
 *
 * <h3>JTextComponent</h3>
 *
 * The primary property to bind to is the "text" property. By default,
 * the "text" property is updated when enter is pressed, or focus is lost. This
 * may be changed using the binding property {@code TextChangeStrategyParameter}.
 * The following illustrates creating a binding targetting a {@code JTextField}. 
 * The "text" property of the {@code JTextField} is updated anytime the
 * {@code Document} of the {@code JTextComponent} changes.
 *
 * <pre>
 *   Binding binding = new Binding(source, sourceExpression, jTextField, "text");
 *   binding.setValue(SwingBindingSupport.TextChangeStrategyParameter,
 *                    TextChangeStrategy.CHANGE_ON_TYPE);
 * </pre>
 *
 * <h3>JTree</h3>
 *
 * The primary property to bind to is the "root" property. The "root" 
 * specifies the root object of the tree (or more correctly, the 
 * {@code TreeModel}). Use child bindings to specify how children of the root,
 * and other objects in the graph, are obtained. The child bindings must
 * resolve to {@code List}s, and preferrably {@code ObservableList}s. The
 * following illustrates creating a binding targetting a {@code JTree}. 
 * For nodes in the tree of type {@code Manager}, the "reports" property is
 * used to locate children.
 *
 * <pre>
 *   Manager root;
 *   Binding binding = new Binding(root, null, jTree, "root");
 *   // For all nodes of type Manager, use the 'reports' property to find
 *   // their children.
 *   binding.addBinding("${reports}", null).setValue(
 *           SwingBindingSupport.TreeNodeClassParameter, Manager.class);
 * </pre>
 * @author sky
 */
public final class SwingBindingSupport {
    private static boolean registered = false;

    /**
     * An enumeration of the possible values for the
     * {@code TextChangeStrategyParameter} of text components.
     */
    public enum TextChangeStrategy {
        /**
         * Indicates the binding target should change as text is input.
         */
        CHANGE_ON_TYPE,
        
        /**
         * Indicates the binding target should change when enter is pressed,
         * or the text component loses focus.
         */
        CHANGE_ON_ACTION_OR_FOCUS_LOST,
        
        /**
         * Indicates the binding target should change when focus leaves the
         * text component.
         */
        CHANGE_ON_FOCUS_LOST
    }

    /**
     * A {@code Binding.Parameter} used to specify when the "text" property of
     * text components should change. If not specified, the default value is
     *{@code CHANGE_ON_ACTION_OR_FOCUS_LOST}.
     *
     * @see javax.beans.binding.Binding#setValue
     */
    public static final Parameter<TextChangeStrategy> TextChangeStrategyParameter =
            new Parameter<TextChangeStrategy>(TextChangeStrategy.class, "TextChangeStrategy");
    
    /**
     * A {@code Binding.Parameter} used to specify whether the target component
     * should disable itself when the source path is incomplete. If not
     * specified, the default is {@code true}. This property is supported for
     * {@code JCheckBox}'s "selected" property, {@code JSlider}'s "value"
     * property, and {@code JTextComponent}'s "text" property.l
     *
     * @see javax.beans.binding.Binding#setValue
     */
    public static final Parameter<Boolean> DisableOnIncompletePathParameter =
            new Parameter<Boolean>(Boolean.class, "DisableOnIncompletePath");
    
    /**
     * A {@code Binding.Parameter} used to specify the column the binding applies
     * to. This is used on child bindings where the target is a {@code JTable}.
     * If not specified, an {@code IllegalArgumentException} is thrown when
     * bound.
     *
     * @see javax.beans.binding.Binding#setValue
     */
    public static final Parameter<Integer> TableColumnParameter =
            new Parameter<Integer>(Integer.class, "TableColumn");

    /**
     * A {@code Binding.Parameter} used to specify the class of the table column.
     * This is used on child bindings where the target is a {@code JTable}.
     * If not specified, the column class is treated as {@code Object.class}.
     *
     * @see javax.beans.binding.Binding#setValue
     */
    public static final Parameter<Class> TableColumnClassParameter =
            new Parameter<Class>(Class.class, "TableColumnClass");

    // PENDING:
//    public static final Key<Boolean> TableRendererKey =
//            new Key<Boolean>(Boolean.class, "TableRenderer");
//
//    public static final Key<Boolean> TableEditorKey =
//            new Key<Boolean>(Boolean.class, "TableEditor");

    /**
     * A {@code Binding.Parameter} used to specify whether a node with no children
     * is treated as a leaf. The default is {@code false}.
     *
     * @see javax.beans.binding.Binding#setValue
     */
    public static final Parameter<Boolean> EmptyNodeTreatedAsLeafParameter =
            new Parameter<Boolean>(Boolean.class, "EmptyNodeTreatedAsLeaf");

    /**
     * A {@code Binding.Parameter} used to specify the class a child binding
     * applies to. If not specified, an {@code IllegalArgumentException} is
     * thrown when bound.
     *
     * @see javax.beans.binding.Binding#setValue
     */
    public static final Parameter<Class> TreeNodeClassParameter =
            new Parameter<Class>(Class.class, "TreeNodeClass");
    
    /**
     * A {@code Binding.Parameter} used to specify whether the child binding
     * identifies the path for the "selectedObjectProperty" key of a
     * {@code JComboBox}. See the description of how to bind to
     * a <a href="#JComboBoxBinding">JComboBox</a> for more information.
     *
     * @see javax.beans.binding.Binding#setValue
     */
        public static final Parameter<String> ComboBoxSelectedObjectPropertyParameter =
            new Parameter<String>(String.class, "ComboBoxSelectedObjectProperty");

    private SwingBindingSupport() {}
}
