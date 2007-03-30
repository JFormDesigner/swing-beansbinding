/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding.ext;

import javax.beans.binding.TestBean;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author sky
 */
public final class TestBeanPropertyDelegate implements BindingTargetProvider {
    private final TestBean bean;
    private final PropertyChangeSupport changeSupport;
    private Object foo;
    private TestBeanBindingTarget bindingTarget;
    private Object bar;
    
    TestBeanPropertyDelegate(TestBean bean) {
        this.bean = bean;
        changeSupport = new PropertyChangeSupport(this);
    }
    
    public void setFoo(Object text) {
        this.foo = text;
        changeSupport.firePropertyChange("foo", null, null);
    }
    
    public Object getFoo() {
        return foo;
    }
    
    public void setBar(Object text) {
        this.bar = text;
        changeSupport.firePropertyChange("bar", null, null);
    }
    
    public Object getBar() {
        return bar;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        changeSupport.removePropertyChangeListener(l);
    }
    
    public void addPropertyChangeListener(String key,
            PropertyChangeListener l) {
        changeSupport.addPropertyChangeListener(key, l);
    }
    
    public void removePropertyChangeListener(String key,
            PropertyChangeListener l) {
        changeSupport.removePropertyChangeListener(key, l);
    }

    public BindingTarget createBindingTarget(String property) {
        if (bindingTarget == null) {
            bindingTarget = new TestBeanBindingTarget();
        }
        return bindingTarget;
    }
}
