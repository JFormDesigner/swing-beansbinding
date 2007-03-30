/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 *
 * @author sky
 */
abstract class PropertyPath {
    private static final PropertyPath EMPTY_PROPERTY_PATH =
            new EmptyPropertyPath();
    
    public static PropertyPath createPropertyPath(String path) {
        int length;
        if (path == null || (length = path.length()) == 0) {
            return EMPTY_PROPERTY_PATH;
        }
        int dotIndex = path.indexOf('.');
        if (dotIndex == -1) {
            return new SinglePropertyPath(path);
        }
        // PENDING: optimize this, I suspect writing own split would
        // be more effecient
        return new MultiPropertyPath(path.split("\\."));
    }
    
    PropertyPath() {
    }

    public abstract int length();
    
    // throws ArrayIndexOutBoundsException if not valid
    public abstract String get(int index);
    
    public PropertyPath subPath(int start, int length) {
        if (start == 0 && length == length()) {
            return this;
        }
        if (start < 0 || start + length > length() || length < 0) {
            throw new IllegalArgumentException();
        }
        if (length == 0) {
            return EMPTY_PROPERTY_PATH;
        }
        if (length == 1) {
            return new SinglePropertyPath(get(start));
        }
        String[] path = new String[length];
        for (int i = 0; i < path.length; i++) {
            path[i] = get(i + start);
        }
        return new MultiPropertyPath(path);
    }
    
    public PropertyPath append(PropertyPath path) {
        if (this == EMPTY_PROPERTY_PATH) {
            return path;
        }
        if (path == EMPTY_PROPERTY_PATH) {
            return this;
        }
        // PENDING: optimize this
        return PropertyPath.createPropertyPath(toString() + "." +
                path.toString());
    }

    public abstract String toString();
    

    private static final class MultiPropertyPath extends PropertyPath {
        private final String[] path;
        
        MultiPropertyPath(String[] path) {
            this.path = path;
            for (int i = 0; i < path.length; i++) {
                path[i] = path[i].intern();
            }
            assert (path.length > 0);
        }

        public int length() {
            return path.length;
        }

        public String get(int index) {
            return path[index];
        }
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(path[0]);
            for (int i = 1; i < path.length; i++) {
                builder.append('.');
                builder.append(path[i]);
            }
            return builder.toString();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof MultiPropertyPath) {
                String[] oPath = ((MultiPropertyPath)o).path;
                if (oPath.length != path.length) {
                    return false;
                }
                for (int i = 0; i < oPath.length; i++) {
                    if (!path[i].equals(oPath[i])) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        public int hashCode() {
            int result = 17;
            for (int i = 0; i < path.length; i++) {
                result = 37 * result + path[i].hashCode();
            }
            return result;
        }
    }
    
    
    private static final class SinglePropertyPath extends PropertyPath {
        private final String path;
        
        SinglePropertyPath(String path) {
            this.path = path.intern();
        }

        public int length() {
            return 1;
        }

        public String get(int index) {
            if (index == 0) {
                return path;
            }
            throw new ArrayIndexOutOfBoundsException();
        }
        
        public String toString() {
            return path;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof SinglePropertyPath) {
                return path.equals(((SinglePropertyPath)o).path);
            }
            return false;
        }

        public int hashCode() {
            return 17 + 37 * path.hashCode();
        }
    }
    
    
    private static final class EmptyPropertyPath extends PropertyPath {
        public int length() {
            return 0;
        }

        public String get(int index) {
            throw new ArrayIndexOutOfBoundsException();
        }
        
        public String toString() {
            return "";
        }

        public boolean equals(Object o) {
            return (o == this);
        }

        public int hashCode() {
            return 17;
        }
    }
}
