/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;

/**
 *
 * @author sky
 */
public class EventListenerRecorder<T extends EventListener> {
    private final T listenerImpl;
    private List<InvocationRecord> records;
    
    public EventListenerRecorder(Class<T> listenerType) {
        listenerImpl = (T)Proxy.newProxyInstance(listenerType.getClassLoader(),
                new Class[] { listenerType }, new Handler());
        records = new ArrayList<InvocationRecord>(1);
    }
    
    public List<InvocationRecord> getAndClearRecords() {
        List<InvocationRecord> records = this.records;
        this.records = new ArrayList<InvocationRecord>(1);
        return records;
    }
    
    public T getEventListenerImpl() {
        return listenerImpl;
    }
    
    public String toString() {
        return "EventListenerRecorder [records=" + records + "]";
    }

    
    public final static class InvocationRecord {
        private final String methodName;
        private final List<Object> args;
        
        InvocationRecord(Method method, Object[] args) {
            this.methodName = method.getName();
            List<Object> argsList = Arrays.asList(args);
            argsList = Collections.unmodifiableList(argsList);
            this.args = argsList;
        }
        
        public String getMethodName() {
            return methodName;
        }
        
        public List<Object> getArgs() {
            return args;
        }
        
        public String toString() {
            return "InvocationRecord [methodName=" + methodName +
                    ", args=" + args + "]";
        }
    }
    
    
    private class Handler implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            records.add(new InvocationRecord(method, args));
            return null;
        }
    }
}
