package br.com.archbase.shared.kernel.utils;

import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;

public interface ArchbaseAssert {


    public static void isTrue(boolean expression, Object message) {
        if (!expression) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static void isTrue(boolean expression) {
        isTrue(expression, "[Assertion failed] - this expression must be true");
    }

    public static void isNull(Object object, Object message) {
        if (object != null) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static void isNull(Object object) {
        isNull(object, "[Assertion failed] - the object argument must be null");
    }

    public static void notNull(Object object, Object message) {
        if (object == null) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static void notNull(Object object) {
        notNull(object, "[Assertion failed] - this argument is required; it must not be null");
    }

    public static void hasLength(String text, Object message) {
        if (!StringUtils.hasLength(text)) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static void hasLength(String text) {
        hasLength(text, "[Assertion failed] - this String argument must have length; it must not be null or empty");
    }

    public static void hasText(String text, Object message) {
        if (!StringUtils.hasText(text)) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static void hasText(String text) {
        hasText(text, "[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
    }

    public static void doesNotContain(String textToSearch, String substring, Object message) {
        if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring)
                && textToSearch.contains(substring)) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static void doesNotContain(String textToSearch, String substring) {
        doesNotContain(textToSearch, substring,
                "[Assertion failed] - this String argument must not contain the substring [" + substring + "]");
    }

    public static void notEmpty(Object[] array, Object message) {
        if (ObjectUtils.isEmpty(array)) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static void notEmpty(Object[] array) {
        notEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element");
    }

    public static void noNullElements(Object[] array, Object message) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    if (message instanceof RuntimeException){
                        throw (RuntimeException) message;
                    }
                    throw new IllegalArgumentException(message.toString());
                }
            }
        }
    }

    public static void noNullElements(Object[] array) {
        noNullElements(array, "[Assertion failed] - this array must not contain any null elements");
    }

    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(Collection<?> collection) {
        notEmpty(collection,
                "[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
    }

    public static void notEmpty(Map<?, ?> map, Object message) {
        if (map == null || map.isEmpty()) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public static void notEmpty(Map<?, ?> map) {
        notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
    }

    public static void isInstanceOf(Class<?> clazz, Object obj) {
        isInstanceOf(clazz, obj, "");
    }

    public static void isInstanceOf(Class<?> type, Object obj, Object message) {
        notNull(type, "Type to check against must not be null");
        if (!type.isInstance(obj)) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(
                    (StringUtils.hasLength(message.toString()) ? message + " " : "") + "Object of class ["
                            + (obj != null ? obj.getClass().getName() : "null") + "] must be an instance of " + type);
        }
    }

    public static void isAssignable(Class<?> superType, Class<?> subType) {
        isAssignable(superType, subType, "");
    }

    public static void isAssignable(Class<?> superType, Class<?> subType, Object message) {
        notNull(superType, "Type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString() + subType + " is not assignable to " + superType);
        }
    }

    public static void state(boolean expression, Object message) {
        if (!expression) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalStateException(message.toString());
        }
    }

    public static void state(boolean expression) {
        state(expression, "[Assertion failed] - this state invariant must be true");
    }

    public static void checkArgument(boolean expression, Object message) {
        if (!expression) {
            if (message instanceof RuntimeException){
                throw (RuntimeException) message;
            }
            throw new IllegalArgumentException(message.toString());
        }
    }


}
