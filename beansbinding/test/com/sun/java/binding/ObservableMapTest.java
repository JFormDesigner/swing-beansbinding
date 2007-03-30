/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.binding;

import com.sun.java.util.BindingCollections;
import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import junit.framework.*;
import java.util.Map;

/**
 *
 * @author sky
 */
public class ObservableMapTest extends TestCase {
    private Map realMap;
    private ObservableMap map;
    private ObservableMapHandler handler;
    
    enum ChangeType {
        KEY_CHANGED,
        KEY_ADDED,
        KEY_REMOVED
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(ObservableMapTest.class);
        return suite;
    }

    public ObservableMapTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        realMap = new HashMap();
        map = BindingCollections.observableMap(realMap);
        handler = new ObservableMapHandler();
        map.addObservableMapListener(handler);
    }

    protected void tearDown() throws Exception {
    }

    public void test() {
        map.put("foo", "bar");
        List<Change> changes = handler.getAndClearChanges();
        assertEquals(1, changes.size());
        assertEquals(new Change(ChangeType.KEY_ADDED, "foo", "bar"),
                changes.get(0));
        compareMaps("foo", "bar");

        map.put("foo", "baz");
        changes = handler.getAndClearChanges();
        assertEquals(1, changes.size());
        assertEquals(new Change(ChangeType.KEY_CHANGED, "foo", "bar", "baz"),
                changes.get(0));
        compareMaps("foo", "baz");

        map.remove("foo");
        changes = handler.getAndClearChanges();
        assertEquals(1, changes.size());
        assertEquals(new Change(ChangeType.KEY_REMOVED, "foo", "bar"),
                changes.get(0));
        compareMaps();
    }
    
    public void testKeyIteratorRemove() {
        map.put("foo", "bar");
        List<Change> changes = handler.getAndClearChanges();
        
        Iterator iterator = map.keySet().iterator();
        iterator.next();
        iterator.remove();
        changes = handler.getAndClearChanges();
        assertEquals(1, changes.size());
        assertEquals(new Change(ChangeType.KEY_REMOVED, "foo", "bar"),
                changes.get(0));
    }
    
    public void testValueIteratorRemove() {
        map.put("foo", "bar");
        List<Change> changes = handler.getAndClearChanges();
        
        Iterator iterator = map.values().iterator();
        iterator.next();
        iterator.remove();
        changes = handler.getAndClearChanges();
        assertEquals(1, changes.size());
        assertEquals(new Change(ChangeType.KEY_REMOVED, "foo", "bar"),
                changes.get(0));
    }
    
    public void testEntrySetIteratorRemove() {
        map.put("foo", "bar");
        List<Change> changes = handler.getAndClearChanges();
        
        Iterator iterator = map.entrySet().iterator();
        iterator.next();
        iterator.remove();
        changes = handler.getAndClearChanges();
        assertEquals(1, changes.size());
        assertEquals(new Change(ChangeType.KEY_REMOVED, "foo", "bar"),
                changes.get(0));
    }
    
    private void compareMaps(Object...values) {
        assertEquals(values.length / 2, map.size());
        for (int i = 0; i < values.length; i += 2) {
            Object key = values[i++];
            Object value = values[i];
            assertEquals(realMap.get(key), map.get(key));
        }
    }
    
    
    private static final class ObservableMapHandler implements 
            ObservableMapListener {
        private List<Change> changes;
        
        public ObservableMapHandler() {
            changes = new LinkedList<Change>();
        }
        
        public void mapKeyValueChanged(ObservableMap map, Object key,
                Object lastValue) {
            changes.add(new Change(ChangeType.KEY_CHANGED, key, lastValue, map.get(key)));
        }

        public void mapKeyAdded(ObservableMap map, Object key) {
            changes.add(new Change(ChangeType.KEY_ADDED, key, map.get(key)));
        }

        public void mapKeyRemoved(ObservableMap map, Object key, Object value) {
            changes.add(new Change(ChangeType.KEY_REMOVED, key, value));
        }
        
        public List<Change> getAndClearChanges() {
            List<Change> changes = this.changes;
            this.changes = new LinkedList<Change>();
            return changes;
        }
    }
    
    
    private static final class Change {
        private final ChangeType type;
        private final Object[] args;
        
        public Change(ChangeType type, Object... args) {
            this.type = type;
            this.args = args;
        }
        
        public ChangeType getType() {
            return type;
        }
        
        public Object[] getArgs() {
            return args;
        }
        
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof Change) {
                Change oc = (Change)o;
                if (oc.type == type && oc.args.length == args.length) {
                    for (int i = 0; i < args.length; i++) {
                        if ((args[i] == null && oc.args[i] != null) ||
                                (args[i] != null && oc.args[i] == null)) {
                            return false;
                        }
                        if (args[i] != null && !args[i].equals(oc.args[i])) {
                            return false;
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
