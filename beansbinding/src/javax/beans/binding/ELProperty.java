/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import com.sun.el.ExpressionFactoryImpl;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.Expression;
import javax.el.Expression.ResolvedProperty;
import javax.el.Expression.Result;
import javax.el.ValueExpression;
import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;
import javax.beans.binding.ext.*;
import static javax.beans.binding.PropertyStateEvent.UNREADABLE;

/**
 * @author Shannon Hickey
 * @author Scott Violet
 */
public final class ELProperty<S, V> extends PropertyHelper<S, V> {

    private Property<S, ?> baseProperty;
    private final ValueExpression expression;
    private final ELContext context = new TempELContext();
    private IdentityHashMap<S, SourceEntry> map = new IdentityHashMap<S, SourceEntry>();
    private static final Object NOREAD = new Object();

    private final class SourceEntry implements PropertyChangeListener,
                                               ObservableMapListener,
                                               PropertyStateListener {

        private S source;
        private Object cachedBean;
        private Object cachedValue;
        private boolean cachedIsWriteable;
        private boolean ignoreChange;
        private Set<RegisteredListener> registeredListeners;
        private Set<RegisteredListener> lastRegisteredListeners;

        private SourceEntry(S source) {
            this.source = source;

            if (baseProperty != null) {
                baseProperty.addPropertyStateListener(source, this);
            }

            registeredListeners = new HashSet<RegisteredListener>(1);
            updateCachedBean();
            updateCache();
        }

        private void cleanup() {
            for (RegisteredListener rl : registeredListeners) {
                unregisterListener(rl, this);
            }

            if (baseProperty != null) {
                baseProperty.removePropertyStateListener(source, this);
            }

            cachedBean = null;
            registeredListeners = null;
            cachedValue = null;
        }

        private boolean cachedIsReadable() {
            return cachedValue != NOREAD;
        }

        private void updateCachedBean() {
            cachedBean = getBeanFromSource(source, true);
        }

        private void updateCache() {
            lastRegisteredListeners = registeredListeners;
            registeredListeners = new HashSet<RegisteredListener>(lastRegisteredListeners.size());
            List<ResolvedProperty> resolvedProperties = null;

            try {
                expression.setSource(getBeanFromSource(source, true));
                Expression.Result result = expression.getResult(context, true);
                
                if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                    log("updateCache()", "expression is unresolvable");
                    cachedValue = NOREAD;
                    cachedIsWriteable = false;
                } else {
                    cachedValue = result.getResult();
                    cachedIsWriteable = !expression.isReadOnly(context);
                }

                resolvedProperties = result.getResolvedProperties();
            } catch (ELException ele) {
                throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
            } finally {
                expression.setSource(null);
            }

            for (ResolvedProperty prop : resolvedProperties) {
                registerListener(prop, this);
            }

            // Uninstall all listeners that are no longer along the path.
            for (RegisteredListener listener : lastRegisteredListeners) {
                unregisterListener(listener, this);
            }

            lastRegisteredListeners = null;
        }

        // flag -1 - validate all
        // flag  0 - source property changed value or readability
        // flag  1 - something else changed
        private void validateCache(int flag) {
            if (flag != 0 && getBeanFromSource(source, false) != cachedBean) {
                log("validateCache()", "concurrent modification");
            }

            if (flag != 1) {
                try {
                    expression.setSource(getBeanFromSource(source, true));
                    Expression.Result result = expression.getResult(context, false);

                    Object currValue;
                    boolean currIsWriteable;

                    if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                        currValue = NOREAD;
                        currIsWriteable = false;
                    } else {
                        currValue = result.getResult();
                        currIsWriteable = !expression.isReadOnly(context);
                    }

                    if (currValue != cachedValue || currIsWriteable != cachedIsWriteable) {
                        log("validateCache()", "concurrent modification");
                    }
                } catch (ELException ele) {
                    throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
                } finally {
                    expression.setSource(null);
                }
            }
        }

        public void propertyStateChanged(PropertyStateEvent pe) {
            if (!pe.getValueChanged()) {
                return;
            }

            validateCache(0);
            Object oldValue = cachedValue;
            boolean wasWriteable = cachedIsWriteable;
            updateCachedBean();
            updateCache();
            notifyListeners(wasWriteable, oldValue, this);
        }

        private void processSourceChanged() {
            validateCache(1);

            boolean wasWriteable = cachedIsWriteable;
            Object oldValue = cachedValue;

            updateCache();
            notifyListeners(wasWriteable, oldValue, this);
        }

        private void sourceChanged(Object source, String property) {
            if (ignoreChange) {
                return;
            }

            if (property != null) {
                property = property.intern();
            }

            for (RegisteredListener rl : registeredListeners) {
                if (rl.getSource() == source && (property == null || rl.getProperty() == property)) {
                    processSourceChanged();
                    break;
                }
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            sourceChanged(e.getSource(), e.getPropertyName());
        }

        public void mapKeyValueChanged(ObservableMap map, Object key, Object lastValue) {
            if (key instanceof String) {
                sourceChanged(map, (String)key);
            }
        }

        public void mapKeyAdded(ObservableMap map, Object key) {
            if (key instanceof String) {
                sourceChanged(map, (String)key);
            }
        }

        public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
            if (key instanceof String) {
                sourceChanged(map, (String)key);
            }
        }
    }

    public static final <S, V> ELProperty<S, V> create(String expression) {
        return new ELProperty<S, V>(expression);
    }

    public static final <S, V> ELProperty<S, V> create(Property<S, ?> baseProperty, String expression) {
        return new ELProperty<S, V>(baseProperty, expression);
    }

    /**
     * @throws IllegalArgumentException for empty or {@code null} path.
     */
    private ELProperty(String expression) {
        this(null, expression);
    }

    /**
     * @throws IllegalArgumentException for empty or {@code null} path.
     */
    public ELProperty(Property<S, ?> baseProperty, String expression) {
        if (expression == null || expression.length() == 0) {
            throw new IllegalArgumentException("expression must be non-null and non-empty");
        }

        try {
            this.expression = new ExpressionFactoryImpl().createValueExpression(context, expression, Object.class);
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error creating EL expression " + expression, ele);
        }

        this.baseProperty = baseProperty;
    }

    public Class<? extends V> getWriteType(S source) {
        /*SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
 
            if (entry.cachedWriter == null) {
                throw new UnsupportedOperationException("Unwriteable");
            }
 
            return (Class<? extends V>)getType(entry.cache[path.length() - 1], path.getLast());
        }

        return (Class<? extends V>)getType(getLastSource(source), path.getLast());*/

        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("getWriteType()", "expression is unresolvable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            if (expression.isReadOnly(context)) {
                log("getWriteType()", "property is unwriteable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            return (Class<? extends V>)expression.getType(context);
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }
    
    public V getValue(S source) {
        /*SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
 
            if (entry.cachedValue == NOREAD) {
                throw new UnsupportedOperationException("Unreadable");
            }
 
            return (V)entry.cachedValue;
        }
        
        Object src = getLastSource(source);
        if (src == null || src == NOREAD) {
            throw new UnsupportedOperationException("Unreadable");
        }
        
        src = getProperty(src, path.getLast());
        if (src == NOREAD) {
            System.err.println("LOG: getValue(): missing read method");
            throw new UnsupportedOperationException("Unreadable");
        }

        return (V)src;*/

        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("getValue()", "expression is unresolvable");
                throw new UnsupportedOperationException("Unreadable");
            }

            return (V)result.getResult();
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }
    
    public void setValue(S source, V value) {
        /*SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
 
            if (entry.cachedWriter == null) {
                throw new UnsupportedOperationException("Unwritable");
            }

            try {
                entry.ignoreChange = true;
                write(entry.cachedWriter, entry.cache[path.length() - 1], path.getLast(), value);
            } finally {
                entry.ignoreChange = false;
            }
 
            Object oldValue = entry.cachedValue;
            entry.updateCachedValue();
            notifyListeners(entry.cachedIsWriteable(), oldValue, entry);
        } else {
            setProperty(getLastSource(source), path.getLast(), value);
        }*/

        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("setValue()", "expression is unresolvable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            if (expression.isReadOnly(context)) {
                log("setValue()", "property is unwriteable");
                throw new UnsupportedOperationException("Unwriteable");
            }

            expression.setValue(context, value);
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }
    
    public boolean isReadable(S source) {
        /*SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsReadable();
        }
        
        Object src = getLastSource(source);
        if (src == null || src == NOREAD) {
            return false;
        }
        
        Object reader = getReader(src, path.getLast());
        if (reader == null) {
            System.err.println("LOG: isReadable(): missing read method");
            return false;
        }

        return true;*/

        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("isReadable()", "expression is unresolvable");
                return false;
            }
            
            return true;
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }

    public boolean isWriteable(S source) {
        /*SourceEntry entry = map.get(source);

        if (entry != null) {
            entry.validateCache(-1);
            return entry.cachedIsWriteable();
        }

        Object src = getLastSource(source);
        if (src == null || src == NOREAD) {
            return false;
        }

        Object writer = getWriter(src, path.getLast());
        if (writer == null) {
            System.err.println("LOG: isWritable(): missing write method");
            return false;
        }

        return true;*/
        
        try {
            expression.setSource(getBeanFromSource(source, true));
            Expression.Result result = expression.getResult(context, false);

            if (result.getType() == Expression.Result.Type.UNRESOLVABLE) {
                log("isWriteable()", "expression is unresolvable");
                return false;
            }

            if (expression.isReadOnly(context)) {
                log("isWriteable()", "property is unwriteable");
                return false;
            }

            return true;
        } catch (ELException ele) {
            throw new PropertyResolutionException("Error evaluating EL expression " + expression + " on " + source, ele);
        } finally {
            expression.setSource(null);
        }
    }

    private Object getBeanFromSource(S source, boolean logErrors) {
        if (baseProperty == null) {
            if (source == null) {
                if (logErrors) {
                    log("getBeanFromSource()", "source is null");
                }
            }

            return source;
        }

        if (!baseProperty.isReadable(source)) {
            if (logErrors) {
                log("getBeanFromSource()", "unreadable source property");
            }
            return NOREAD;
        }

        Object bean = baseProperty.getValue(source);
        if (bean == null) {
            if (logErrors) {
                log("getBeanFromSource()", "source property returned null");
            }
            return null;
        }
        
        return bean;
    }

    protected final void listeningStarted(S source) {
        SourceEntry entry = map.get(source);
        if (entry == null) {
            entry = new SourceEntry(source);
            map.put(source, entry);
        }
    }

    protected final void listeningStopped(S source) {
        SourceEntry entry = map.remove(source);
        if (entry != null) {
            entry.cleanup();
        }
    }

    private static boolean didValueChange(Object oldValue, Object newValue) {
        return oldValue == null || newValue == null || !oldValue.equals(newValue);
    }

    private void notifyListeners(boolean wasWriteable, Object oldValue, SourceEntry entry) {
        PropertyStateListener[] listeners = getPropertyStateListeners(entry.source);

        if (listeners == null || listeners.length == 0) {
            return;
        }

        oldValue = toUNREADABLE(oldValue);
        Object newValue = toUNREADABLE(entry.cachedValue);
        boolean valueChanged = didValueChange(oldValue, newValue);
        boolean writeableChanged = (wasWriteable != entry.cachedIsWriteable);

        if (!valueChanged && !writeableChanged) {
            return;
        }

        PropertyStateEvent pse = new PropertyStateEvent(this,
                                                        entry.source,
                                                        valueChanged,
                                                        oldValue,
                                                        newValue,
                                                        writeableChanged,
                                                        entry.cachedIsWriteable);

        this.firePropertyStateChange(pse);
    }

    public String toString() {
        return getClass().getName() + "[" + expression + "]";
    }

    /**
     * @throws PropertyResolutionException
     */
    private static BeanInfo getBeanInfo(Object object) {
        assert object != null;

        try {
            return Introspector.getBeanInfo(object.getClass(), Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException ie) {
            throw new PropertyResolutionException("Exception while introspecting " + object.getClass().getName(), ie);
        }
    }

    private static EventSetDescriptor getEventSetDescriptor(Object object) {
        assert object != null;
        
        EventSetDescriptor[] eds = getBeanInfo(object).getEventSetDescriptors();
        for (EventSetDescriptor ed : eds) {
            if (ed.getListenerType() == PropertyChangeListener.class) {
                return ed;
            }
        }

        return null;
    }

    /**
     * @throws PropertyResolutionException
     */
    private static Object invokeMethod(Method method, Object object, Object... args) {
        Exception reason = null;

        try {
            return method.invoke(object, args);
        } catch (IllegalArgumentException ex) {
            reason = ex;
        } catch (IllegalAccessException ex) {
            reason = ex;
        } catch (InvocationTargetException ex) {
            reason = ex;
        }

        throw new PropertyResolutionException("Exception invoking method " + method + " on " + object, reason);
    }

    private static Object toUNREADABLE(Object src) {
        return src == NOREAD ? UNREADABLE : src;
    }

    private void registerListener(ResolvedProperty resolved, SourceEntry entry) {
        Object source = resolved.getSource();
        Object property = resolved.getProperty();
        if (source != null && property instanceof String) {
            String sProp = (String)property;

            if (source instanceof ObservableMap) {
                RegisteredListener rl = new RegisteredListener(source, sProp);

                if (!entry.registeredListeners.contains(rl)) {
                    if (!entry.lastRegisteredListeners.remove(rl)) {
                        ((ObservableMap)source).addObservableMapListener(entry);
                    }
                    
                    entry.registeredListeners.add(rl);
                }
            } else if (!(source instanceof Map)) {
                source = getAdapter(source, sProp);

                RegisteredListener rl = new RegisteredListener(source, sProp);

                if (!entry.registeredListeners.contains(rl)) {
                    if (!entry.lastRegisteredListeners.remove(rl)) {
                        addPropertyChangeListener(source, entry);
                    }
                    
                    entry.registeredListeners.add(rl);
                }
            }
        }
    }

    private void unregisterListener(RegisteredListener rl, SourceEntry entry) {
        Object source = rl.getSource();
        if (source instanceof ObservableMap) {
            ((ObservableMap)source).removeObservableMapListener(entry);
        } else if (!(source instanceof Map)) {
            removePropertyChangeListener(source, entry);
        }
    }

    /**
     * @throws PropertyResolutionException
     */
    private static void addPropertyChangeListener(Object object, PropertyChangeListener listener) {
        EventSetDescriptor ed = getEventSetDescriptor(object);
        Method addPCMethod = null;

        if (ed == null || (addPCMethod = ed.getAddListenerMethod()) == null) {
            log("addPropertyChangeListener()", "can't add listener");
            return;
        }

        invokeMethod(addPCMethod, object, listener);
    }

    /**
     * @throws PropertyResolutionException
     */
    private static void removePropertyChangeListener(Object object, PropertyChangeListener listener) {
        EventSetDescriptor ed = getEventSetDescriptor(object);
        Method removePCMethod = null;

        if (ed == null || (removePCMethod = ed.getRemoveListenerMethod()) == null) {
            log("removePropertyChangeListener()", "can't remove listener from source");
            return;
        }
        
        invokeMethod(removePCMethod, object, listener);
    }

    private static boolean wrapsLiteral(Object o) {
        assert o != null;

        return o instanceof String ||
               o instanceof Byte ||
               o instanceof Character ||
               o instanceof Boolean ||
               o instanceof Short ||
               o instanceof Integer ||
               o instanceof Long ||
               o instanceof Float ||
               o instanceof Double;
    }

    // need special match method because when using reflection
    // to get a primitive value, the value is always wrapped in
    // a new object
    private static boolean match(Object a, Object b) {
        if (a == b) {
            return true;
        }

        if (a == null) {
            return false;
        }

        if (wrapsLiteral(a)) {
            return a.equals(b);
        }

        return false;
    }

    private Object getAdapter(Object o, String property) {
        Object adapter = null;
        adapter = BeanAdapterFactory.getAdapter(o, property);
        return adapter == null ? o : adapter;
    }
    
    private static final boolean LOG = false;

    private static void log(String method, String message) {
        if (LOG) {
            System.err.println("LOG: " + method + ": " + message);
        }
    }

    private static final class RegisteredListener {
        private final Object source;
        private final String property;
        
        RegisteredListener(Object source) {
            this(source, null);
        }
        
        RegisteredListener(Object source, String property) {
            this.source = source;
            if (property != null) {
                property = property.intern();
            }
            this.property = property;
        }
        
        public Object getSource() {
            return source;
        }
        
        public String getProperty() {
            return property;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof RegisteredListener) {
                RegisteredListener orl = (RegisteredListener) obj;
                return (orl.source == source && orl.property == property);
            }
            return false;
        }

        public int hashCode() {
            int result = 17;
            result = 37 * result + source.hashCode();
            if (property != null) {
                result = 37 * result + property.hashCode();
            }
            return result;
        }

        public String toString() {
            return "RegisteredListener [" +
                    " source=" + source +
                    " property=" + property + 
                    "]";
        }
    }

}
