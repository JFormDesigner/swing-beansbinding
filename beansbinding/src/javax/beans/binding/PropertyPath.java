/*
 * Copyright (C) 2006-2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package javax.beans.binding;

/**
 *
 * @author sky
 * @author Shannon Hickey
 */
abstract class PropertyPath {

    private PropertyPath() {}

    public abstract int length();

    // throws ArrayIndexOutBoundsException if not valid
    public abstract String get(int index);

    public abstract String toString();

    public static PropertyPath createPropertyPath(String path) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("path must be non-empty and non-null");
        }

        int dotIndex = path.indexOf('.');

        if (dotIndex == -1) {
            return new SinglePropertyPath(path);
        }

        // PENDING: optimize this, I suspect writing own split would
        // be more effecient
        return new MultiPropertyPath(path.split("\\."));
    }


    private static final class MultiPropertyPath extends PropertyPath {
        private final String[] path;
        
        public MultiPropertyPath(String[] path) {
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
        
        public SinglePropertyPath(String path) {
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
}
