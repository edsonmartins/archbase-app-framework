package br.com.archbase.query.rsql.parser.ast;

import java.util.Collection;

public interface Assert {

    public static void isTrue(boolean expression, String message, Object... args) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    public static void notNull(Object obj, String message, Object... args) {
        if (obj == null) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    public static void notEmpty(Collection<?> col, String message, Object... args) {
        if (col == null || col.isEmpty()) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    public static void notEmpty(Object[] ary, String message, Object... args) {
        if (ary == null || ary.length == 0) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    public static void notBlank(String str, String message, Object... args) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }
}
