/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.util.*;
import java.beans.*;

/**
 * @author Shannon Hickey
 */
public class BindingGroup {
    private final List<Binding> unbound = new ArrayList<Binding>();
    private final List<Binding> bound = new ArrayList<Binding>();
    private List<BindingListener> listeners;
    private Handler handler;
    private Map<String, Binding> namedBindings;
    private Binding.AutoUpdateStrategy strategy;
    private Set<Binding> changedTargets;
    private PropertyChangeSupport changeSupport;

    public BindingGroup() {}

    public final Binding addBinding(Binding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("Binding must be non-null");
        }

        if (binding.getBindingGroup() != null) {
            throw new IllegalArgumentException("Binding already part of a BindingGroup");
        }

        String name = binding.getName();
        if (name != null) {
            if (getBinding(name) != null) {
                throw new IllegalArgumentException("Context already contains a binding with name \"" + name + "\"");
            } else {
                putNamed(name, binding);
            }
        }

        binding.setBindingGroup(this);
        binding.addBindingListener(getHandler());
        
        if (binding.isBound()) {
            bound.add(binding);
        } else {
            unbound.add(binding);
        }

        return binding;
    }

    public final void removeBinding(Binding binding) {
        if (binding.isBound()) {
            if (!bound.remove(binding)) {
                throw new IllegalArgumentException("Unknown Binding");
            }
        } else {
            if (!unbound.remove(binding)) {
                throw new IllegalArgumentException("Unknown Binding");
            }
        }

        String name = binding.getName();
        if (name != null) {
            assert namedBindings != null;
            namedBindings.remove(name);
        }

        binding.setBindingGroup(null);
    }

    void bindingBound(Binding binding) {
        unbound.remove(binding);
        bound.add(binding);
    }

    void bindingUnbound(Binding binding) {
        bound.remove(binding);
        unbound.add(binding);
    }

    private void putNamed(String name, Binding binding) {
        if (namedBindings == null) {
            namedBindings = new HashMap<String, Binding>();
        }

        namedBindings.put(name, binding);
    }

    public final Binding getBinding(String name) {
        if (name == null) {
            throw new IllegalArgumentException("cannot fetch unnamed bindings");
        }

        return namedBindings == null ? null : namedBindings.get(name);
    }

    public final List<Binding> getBindings() {
        ArrayList list = new ArrayList(bound);
        list.addAll(unbound);
        return Collections.unmodifiableList(list);
    }
    
    public final void setAutoUpdateStrategy(Binding.AutoUpdateStrategy strategy) {
        this.strategy = strategy;
    }

    public final Binding.AutoUpdateStrategy getAutoUpdateStrategy() {
        return strategy;
    }

    public void bind() {
        List<Binding> toBind = new ArrayList<Binding>(unbound);
        for (Binding binding : toBind) {
            binding.bind();
        }
    }

    public void unbind() {
        List<Binding> toUnbind = new ArrayList<Binding>(bound);
        for (Binding binding : toUnbind) {
            binding.unbind();
        }
    }

    public final boolean getHasChangedTargetBindings() {
        return changedTargets != null && changedTargets.size() != 0;
    }

    private void updateChangedTargets(Binding binding, boolean add) {
        if (add) {
            if (changedTargets == null) {
                changedTargets = new LinkedHashSet<Binding>();
            }

            if (changedTargets.add(binding) && changedTargets.size() == 1) {
                changeSupport.firePropertyChange("hasChangedTargetBindings", false, true);
            }
        } else {
            if (changedTargets == null) {
                return;
            } else if (changedTargets.remove(binding) && changedTargets.size() == 0) {
                changeSupport.firePropertyChange("hasChangedTargetBindings", true, false);
            }
        }
    }
    
    public final void addBindingListener(BindingListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<BindingListener>();
        }

        listeners.add(listener);
    }

    public final void removeBindingListener(BindingListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public final BindingListener[] getBindingListeners() {
        if (listeners == null) {
            return new BindingListener[0];
        }

        BindingListener[] ret = new BindingListener[listeners.size()];
        ret = listeners.toArray(ret);
        return ret;
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }

        changeSupport.addPropertyChangeListener(listener);
    }

    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }

        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }

        changeSupport.removePropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null) {
            return;
        }

        changeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public final PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        
        return changeSupport.getPropertyChangeListeners();
    }

    public final PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        
        return changeSupport.getPropertyChangeListeners(propertyName);
    }
    
    public final PropertyStateListener[] getPropertyStateListeners() {
        if (listeners == null) {
            return new PropertyStateListener[0];
        }

        PropertyStateListener[] ret = new PropertyStateListener[listeners.size()];
        ret = listeners.toArray(ret);
        return ret;
    }

    public Set<Binding> getChangedTargetBindings() {
        if (changedTargets == null) {
            return Collections.unmodifiableSet(new HashSet<Binding>());
        }

        return Collections.unmodifiableSet(changedTargets);
    }

    private final Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }

        return handler;
    }

    private class Handler implements BindingListener {
        public void syncFailed(Binding binding, Binding.SyncFailure... failures) {
            if (listeners == null) {
                return;
            }
            
            for (BindingListener listener : listeners) {
                listener.syncFailed(binding, failures);
            }
        }

        public void synced(Binding binding) {
            updateChangedTargets(binding, false);

            if (listeners == null) {
                return;
            }

            for (BindingListener listener : listeners) {
                listener.synced(binding);
            }
        }

        public void sourceChanged(Binding binding) {
            if (listeners == null) {
                return;
            }
            
            for (BindingListener listener : listeners) {
                listener.sourceChanged(binding);
            }
        }

        public void targetChanged(Binding binding) {
            updateChangedTargets(binding, true);

            if (listeners == null) {
                return;
            }

            for (BindingListener listener : listeners) {
                listener.targetChanged(binding);
            }
        }
    }
}
