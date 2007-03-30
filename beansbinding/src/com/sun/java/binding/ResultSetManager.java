/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package com.sun.java.binding;

import com.sun.java.util.ObservableList;
import com.sun.java.util.ObservableListListener;
import com.sun.java.util.ObservableMap;
import com.sun.java.util.ObservableMapListener;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author sky
 */
public class ResultSetManager {
    private final ResultSet resultSet;
    private final List<Row> contents;
    private final ListImpl vendedList;
    private final Column[] columns;
    private final Map<Object,Column> columnMap;
    private final Set<Row> modified;
    private int size;
    
    public ResultSetManager(ResultSet resultSet) throws SQLException {
        this(resultSet, true);
    }
    
    public ResultSetManager(ResultSet resultSet,
            boolean useColumnNamesAsKeys) throws SQLException {
        this.resultSet = resultSet;
        if (resultSet == null) {
            throw new IllegalArgumentException("ResultSet must be non-null");
        }
        contents = new ArrayList<Row>(size);
        vendedList = new ListImpl();
        modified = new HashSet<Row>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        columnMap = new HashMap<Object,Column>(columnCount);
        columns = new Column[columnCount];
        for (int i = 0; i < metaData.getColumnCount(); i++) {
            String key = (useColumnNamesAsKeys) ? metaData.getColumnName(i + 1) :
                Integer.toString(i);
            if (columnMap.containsKey(key)) {
                int j = 2;
                while (columnMap.containsKey(key + j)) {
                    j++;
                }
                key = key + j;
            }
            columns[i] = new Column(i, key, metaData.getColumnType(i + 1));
            columnMap.put(key, columns[i]);
        }
        fetchAll();
    }
    
    public final ResultSet getResultSet() {
        return resultSet;
    }
    
    public final List<Map<Object,Object>> getContentsAsList() {
        return vendedList;
    }
    
    private void fetchAll() throws SQLException {
        while (resultSet.next()) {
            size++;
            Object[] rowData = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                rowData[i] = resultSet.getObject(i + 1);
//                switch(columns[i].getType()) {
//                    case Types.CHAR:
//                    case Types.VARCHAR:
//                    case Types.LONGVARCHAR:
//                        data = resultSet.getString(i + 1);
//                        break;
//                    case Types.BINARY:
//                    case Types.VARBINARY:
//                    case Types.LONGVARBINARY:
//                        data = resultSet.getBytes(i + 1);
//                        break;
//                    case Types.BIT:
//                        data = resultSet.getBoolean(i + 1);
//                        break;
//                    case Types.TINYINT:
//                    case Types.SMALLINT:
//                        // NOTE: TINYINT could be byte too
//                        data = resultSet.getShort(i + 1);
//                        break;
//                    case Types.INTEGER:
//                        data = resultSet.getInt(i + 1);
//                        break;
//                    case Types.BIGINT:
//                        data = resultSet.getLong(i + 1);
//                        break;
//                    case Types.REAL:
//                        data = resultSet.getFloat(i + 1);
//                        break;
//                    case Types.DOUBLE:
//                    case Types.FLOAT:
//                        data = resultSet.getDouble(i + 1);
//                        break;
//                    case Types.DECIMAL:
//                    case Types.NUMERIC:
//                        data = resultSet.getBigDecimal(i + 1);
//                        break;
//                    case Types.DATE:
//                        data = resultSet.getDate(i + 1);
//                        break;
//                    case Types.TIME:
//                        data = resultSet.getTime(i + 1);
//                        break;
//                    case Types.TIMESTAMP:
//                        data = resultSet.getTimestamp(i + 1);
//                        break;
//                    case Types.BLOB:
//                        data = resultSet.getBlob(i + 1);
//                        break;
//                    case Types.CLOB:
//                        data = resultSet.getClob(i + 1);
//                        break;
//                    case Types.ARRAY:
//                        data = resultSet.getArray(i + 1);
//                        break;
//                    default:
//                        // unkonwn type.
//                        break;
//                }
            }
            contents.add(new Row(rowData));
        }
    }

    private Row getRow(int index) {
        return contents.get(index);
    }
    
    private void rowChanged(Row entry) {
        modified.add(entry);
        vendedList.fireEntryChanged(vendedList.indexOf(entry));
    }
    
    public Set<Map<Object,Object>> getModifiedEntries() {
        HashSet<Map<Object,Object>> modifiedCopy = 
                new HashSet<Map<Object,Object>>(modified);
        return modifiedCopy;
    }
    
    public void clearModifiedEntries() {
        modified.clear();
    }
    
    public Map<Object,Object> deleteRow(Map<Object,Object> row) {
        int index = contents.indexOf(row);
        if (index != -1) {
            contents.remove(index);
            size--;
            vendedList.fireEntryRemoved(index, row);
            return row;
        }
        return null;
    }
    
    public Map<Object,Object> addRow(int index) {
        Row newRow = new Row(new Object[columns.length]);
        contents.add(index, newRow);
        size++;
        vendedList.fireEntryAdded(index);
        return newRow;
    }
    
    
    private static final class Column {
        private final int index;
        private final String name;
        private final int type;
        
        Column(int index, String name, int type) {
            this.index = index;
            this.name = name;
            this.type = type;
        }

        private String getName() {
            return name;
        }

        private int getZeroBasedIndex() {
            return index;
        }

        private int getType() {
            return type;
        }
    }
    
    
    private abstract class AbstractIterator<T> implements Iterator<T> {
        protected int index;
        
        AbstractIterator() {
            if (columns.length > 0) {
                index = -1;
            } else {
                index = 0;
            }
        }
        
        public final boolean hasNext() {
            return index < columns.length;
        }

        public final T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return get(index++);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        protected abstract T get(int index);
    }
    
    
    private final class Row implements Map<Object,Object>, ObservableMap<Object,Object> {
        private final Object[] contents;
        private List<ObservableMapListener> listeners;
        
        Row(Object[] contents) {
            this.contents = contents;
        }
        
        public int size() {
            return columns.length;
        }

        public boolean isEmpty() {
            return false;
        }

        public boolean containsKey(Object key) {
            return columnMap.containsKey(key);
        }

        public boolean containsValue(Object value) {
            for (int i = 0; i < contents.length; i++) {
                if ((value == null && columns[i] == null) ||
                        (value != null && value.equals(columns[i]))) {
                    return true;
                }
            }
            return false;
        }

        public Object get(Object key) {
            Column column = columnMap.get(key);
            if (column == null) {
                return null;
            }
            return contents[column.getZeroBasedIndex()];
        }

        public Object put(Object key, Object value) {
            Column column = columnMap.get(key);
            if (column == null) {
                throw new IllegalArgumentException(
                        key + " is not a valid column identifier");
            }
            Object oldValue = contents[column.getZeroBasedIndex()];
            contents[column.getZeroBasedIndex()] = value;
            if (listeners != null) {
                for (ObservableMapListener l : listeners) {
                    l.mapKeyValueChanged(this, key, oldValue);
                }
            }
            rowChanged(this);
            return oldValue;
        }

        public Object remove(Object key) {
            return put(key, null);
        }

        public void putAll(Map<? extends Object, ? extends Object> m) {
            for (Object key : m.keySet()) {
                put(key, m.get(key));
            }
        }

        public void clear() {
            for (Object key : columnMap.keySet()) {
                put(key, null);
            }
        }

        public Set<Object> keySet() {
	    return new KeySetImpl();
        }

        public Collection<Object> values() {
            return new ValueCollection();
        }

        public Set<Map.Entry<Object, Object>> entrySet() {
            return new EntrySet();
        }

        public void addObservableMapListener(ObservableMapListener listener) {
            if (listeners == null) {
                listeners = new CopyOnWriteArrayList<ObservableMapListener>();
            }
            listeners.add(listener);
        }

        public void removeObservableMapListener(ObservableMapListener listener) {
            if (listeners != null) {
                listeners.remove(listener);
            }
        }
        
        public String toString() {
            StringBuilder builder = new StringBuilder("Row [");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(columns[i].getName());
                builder.append("=");
                builder.append(contents[i]);
            }
            builder.append("]");
            return builder.toString();
        }
        
        
        private final class EntrySet extends AbstractSet<Map.Entry<Object,Object>> {
            public Iterator<Map.Entry<Object, Object>> iterator() {
                return new EntrySetIterator();
            }

            public int size() {
                return columns.length;
            }
        }
        
        
        private final class EntrySetIterator extends AbstractIterator<Map.Entry<Object,Object>> {
            protected Map.Entry<Object,Object> get(int index) {
                return new MapEntry(index);
            }
        }
        
        
        private final class MapEntry implements Map.Entry<Object,Object> {
            private final int index;
            
            MapEntry(int index) {
                this.index = index;
            }
            
            public Object getKey() {
                return columns[index];
            }

            public Object getValue() {
                return contents[index];
            }

            public Object setValue(Object value) {
                return put(columns[index], value);
            }
        }
        
        
        private final class ValueCollection extends AbstractCollection<Object> {
            public int size() {
                return columns.length;
            }

            public Iterator<Object> iterator() {
                return new ValueCollectionIterator();
            }
        }
        
        
        private final class ValueCollectionIterator extends AbstractIterator<Object> {
            protected Object get(int index) {
                return contents[index];
            }
        }
    }
    
    
    private final class KeySetImpl extends AbstractSet<Object> {
        public int size() {
            return columns.length;
        }
        
        public boolean contains(Object k) {
            return columnMap.containsKey(k);
        }
        
        public Iterator<Object> iterator() {
            return new KeySetIteratorImpl();
        }
    }
        

    private final class KeySetIteratorImpl extends AbstractIterator<Object> {
        protected Object get(int index) {
            return columns[index];
        }
    }
    
    
    private final class ListImpl extends AbstractList<Map<Object,Object>> implements 
            ObservableList<Map<Object,Object>> {
        private final List<ObservableListListener> listeners;

        ListImpl() {
            listeners = new CopyOnWriteArrayList<ObservableListListener>();
        }
        
        public Map<Object,Object> get(int index) {
            return ResultSetManager.this.getRow(index);
        }

        public int size() {
            return size;
        }

        public void addObservableListListener(ObservableListListener listener) {
            listeners.add(listener);
        }

        public void removeObservableListListener(ObservableListListener listener) {
            listeners.remove(listener);
        }

        public boolean supportsElementPropertyChanged() {
            return true;
        }

        private void fireEntryChanged(int index) {
            for (ObservableListListener l : listeners) {
                l.listElementPropertyChanged(this, index);
            }
        }

        private void fireEntryRemoved(int index, Map<Object,Object> row) {
            ArrayList<Map<Object,Object>> removedList = new ArrayList<Map<Object,Object>>(1);
            removedList.add(row);
            for (ObservableListListener l : listeners) {
                l.listElementsRemoved(this, index, removedList);
            }
        }

        private void fireEntryAdded(int index) {
            for (ObservableListListener l : listeners) {
                l.listElementsAdded(this, index, 1);
            }
        }
    }
}
