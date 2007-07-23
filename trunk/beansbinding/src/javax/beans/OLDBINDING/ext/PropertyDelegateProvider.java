/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.OLDBINDING.ext;

/**
 * {@code PropertyDelegateProvider} is responsible for providing a property
 * delegate. A property delegate is an {@code Object} that supplies a
 * named property for another class. For example, {@code JList} does not
 * provide an {@code "elements"} property. The following illustrates
 * a property delegate provider for {@code JList's} {@code "elements"}
 * property:
 * <pre>
 *   // The property delegate must be public, and it must expose the
 *   // property it was obtained from.
 *   public class JListProvider {
 *       public void setElements(List&lt;?&gt; elements) { ... }
 *       public List&lt;?&gt; getElements() { ... }
 *   }
 *
 *   // The PropertyDelegateProvider must be public, and override the
 *   // necessary methods.
 *   public class MyPropertyDelegateProvider extends PropertyDelegateProvider {
 *       public Object getPropertyDelegate(Object source, String property) {
 *           return new JListProvider();
 *       }
 *       public Class<?> getPropertyDelegate(Object source, String property) {
 *           return JListProvider.class;
 *       }
 *       public boolean providesDelegate(Class<?> type, String property) {
 *           return (JList.class.isAssignableFrom(type) && 
 *                   "elements".equals(type));
 *       }
 *   };
 * </pre>
 * The set of {@code PropertyDelegateProvider}s are obtained using the 
 * {@code ServiceLoader} class, see it for details on how to register a
 * provider.
 *
 * @see PropertyDelegateFactory
 *
 * @author sky
 */
public abstract class PropertyDelegateProvider {
    /**
     * Used to identify if a particular {@code FeatureDescriptor} is a 
     * preferred property for binding. If a {@code FeatureDescriptor} has a value
     * for this ({@code featureDescriptor.getValue(PREFERRED_BINDING_PROPERTY)} 
     * is {@code non-null}), and the value is {@code true}, then the descriptor identifies
     * a preferred binding property. A class may, and often times does, have 
     * more than one preferred binding property.
     *
     * @see java.beans.Introspector
     * @see java.beans.FeatureDescriptor
     */
    public static final String PREFERRED_BINDING_PROPERTY = 
            "PreferredBindingProperty";

    /**
     * Returns whether this {@code PropertyDelegateProvider} provides a delegate
     * for the {@code Class} and property pair.
     *
     * @param type the {@code Class}
     * @param property the property
     */
    public abstract boolean providesDelegate(Class<?> type, String property);
    
    /**
     * Returns the property delegate for the specified object.
     * {@code PropertyDelegateFactory} only invokes this method once for a
     * particular provider and object pair. For example, consider the following:
     * <pre>
     *   PropertyDelegateProvider myProvider = createProvider();
     *   PropertyDelegateFactory.registerPropertyDelegateProvider(
     *       Foo.class, "foo", myProvider);
     *   PropertyDelegateFactory.registerPropertyDelegateProvider(
     *       Foo.class, "value", myProvider);
     * </pre>
     * Then {@code createPropertyDelegate} is only invoked once for each
     * unique instance of {@code Foo}.
     *
     * @param source the source to return the property delegate for
     * @param property the property
     * @return the property delegate, or {@code null} if the pair does not
     *         identify a legal property delegate
     *
     * @throws NullPointerException if {@code source} or {@code property} are
     *         {@code null}
     */
    public abstract Object createPropertyDelegate(Object source, String property);
    
    /**
     * Returns the class the property delegate creates for the specified
     * class.
     *
     * @param type the class to obtain the delegate class for
     * @return the class of the property delegate
     *
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public abstract Class<?> getPropertyDelegateClass(Class<?> type);
}
