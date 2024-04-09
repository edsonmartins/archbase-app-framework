package br.com.archbase.shared.kernel.utils;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class ArrayUtils {
    public static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = new Character[0];
    public static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = new Boolean[0];
    public static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = new Float[0];
    public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];
    public static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = new Byte[0];
    public static final Short[] EMPTY_SHORT_OBJECT_ARRAY = new Short[0];
    public static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = new Integer[0];
    public static final Long[] EMPTY_LONG_OBJECT_ARRAY = new Long[0];
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final int INDEX_NOT_FOUND = -1;

    private ArrayUtils() {
    }

    public static Object[] add(Object[] array1, Object obj2) {
        Object[] array2 = {obj2};
        return add(array1, array2, null);
    }

    public static Object[] add(Object[] array1, Object[] array2) {
        return add(array1, array2, null);
    }

    public static Object[] add(Object[] array1, Object obj2, Object[] conv) {
        Object[] array2 = {obj2};
        return add(array1, array2, conv);
    }

    public static Object[] add(Object[] array1, Object[] array2, Object[] conv) {
        if ((array1 != null) && (array2 == null))
            return array1;
        if ((array1 == null) && (array2 != null)) {
            return array2;
        }
        List<Object> list = new LinkedList<>(Arrays.asList(array1));
        if (array2 != null) {
            for (int i = 0; i < array2.length; i++) {
                if ((!list.contains(array2[i])) && (array2[i] != null)) {
                    list.add(array2[i]);
                }
            }
        }
        if (conv == null)
            return list.toArray();
        return list.toArray(conv);
    }

    public static Object[] subtract(Object[] array1, Object obj2) {
        Object[] array2 = {obj2};
        return subtract(array1, array2, null);
    }

    public static Object[] subtract(Object[] array1, Object[] array2) {
        return subtract(array1, array2, null);
    }

    public static Object[] subtract(Object[] array1, Object obj2, Object[] conv) {
        Object[] array2 = {obj2};
        return subtract(array1, array2, conv);
    }

    public static Object[] subtract(Object[] array1, Object[] array2, Object[] conv) {
        if ((array1 == null) || (array1.length == 0) || (array2 == null) || (array2.length == 0)) {
            return array1;
        }
        LinkedList<Object> list = new LinkedList<>(Arrays.asList(array1));

        for (int i = 0; i < array2.length; i++) {
            if (list.contains(array2[i])) {
                list.remove(array2[i]);
            }
        }
        if (conv == null)
            return list.toArray();
        return list.toArray(conv);
    }

    public static Character[] toObject(char[] array) {
        if (array == null) {
            return new Character[]{};
        } else if (array.length == 0) {
            return EMPTY_CHARACTER_OBJECT_ARRAY;
        }
        final ArrayList<Character> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result.toArray(new Character[]{});
    }

    public static Long[] toObject(long[] array) {
        if (array == null) {
            return new Long[]{};
        } else if (array.length == 0) {
            return EMPTY_LONG_OBJECT_ARRAY;
        }
        final ArrayList<Long> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result.toArray(new Long[]{});
    }

    public static Integer[] toObject(int[] array) {
        if (array == null) {
            return new Integer[]{};
        } else if (array.length == 0) {
            return EMPTY_INTEGER_OBJECT_ARRAY;
        }
        final ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result.toArray(new Integer[]{});
    }

    public static Short[] toObject(short[] array) {
        if (array == null) {
            return new Short[]{};
        } else if (array.length == 0) {
            return EMPTY_SHORT_OBJECT_ARRAY;
        }
        final ArrayList<Short> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result.toArray(new Short[]{});
    }

    public static Byte[] toObject(byte[] array) {
        if (array == null) {
            return new Byte[]{};
        } else if (array.length == 0) {
            return EMPTY_BYTE_OBJECT_ARRAY;
        }
        final ArrayList<Byte> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result.toArray(new Byte[]{});
    }

    public static Double[] toObject(double[] array) {
        if (array == null) {
            return new Double[]{};
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        final ArrayList<Double> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result.toArray(new Double[]{});
    }

    public static Float[] toObject(float[] array) {
        if (array == null) {
            return new Float[]{};
        } else if (array.length == 0) {
            return EMPTY_FLOAT_OBJECT_ARRAY;
        }
        final ArrayList<Float> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result.toArray(new Float[]{});
    }

    public static Boolean[] toObject(boolean[] array) {
        if (array == null) {
            return new Boolean[]{};
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_OBJECT_ARRAY;
        }
        final ArrayList<Boolean> result = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            result.add(array[i]);
        }
        return result.toArray(new Boolean[]{});
    }

    public static char[] toPrimitive(Character[] array) {
        if (array == null) {
            return new char[]{};
        } else if (array.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        final char[] result = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].charValue();
        }
        return result;
    }

    public static char[] toPrimitive(Character[] array, char valueForNull) {
        if (array == null) {
            return new char[]{};
        } else if (array.length == 0) {
            return EMPTY_CHAR_ARRAY;
        }
        final char[] result = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            Character b = array[i];
            result[i] = (b == null ? valueForNull : b.charValue());
        }
        return result;
    }

    public static long[] toPrimitive(Long[] array) {
        if (array == null) {
            return new long[]{};
        } else if (array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].longValue();
        }
        return result;
    }

    public static long[] toPrimitive(Long[] array, long valueForNull) {
        if (array == null) {
            return new long[]{};
        } else if (array.length == 0) {
            return EMPTY_LONG_ARRAY;
        }
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            Long b = array[i];
            result[i] = (b == null ? valueForNull : b.longValue());
        }
        return result;
    }

    public static int[] toPrimitive(Integer[] array) {
        if (array == null) {
            return new int[]{};
        } else if (array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].intValue();
        }
        return result;
    }

    public static int[] toPrimitive(Integer[] array, int valueForNull) {
        if (array == null) {
            return new int[]{};
        } else if (array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            Integer b = array[i];
            result[i] = (b == null ? valueForNull : b.intValue());
        }
        return result;
    }

    public static short[] toPrimitive(Short[] array) {
        if (array == null) {
            return new short[]{};
        } else if (array.length == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        final short[] result = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].shortValue();
        }
        return result;
    }

    public static short[] toPrimitive(Short[] array, short valueForNull) {
        if (array == null) {
            return new short[]{};
        } else if (array.length == 0) {
            return EMPTY_SHORT_ARRAY;
        }
        final short[] result = new short[array.length];
        for (int i = 0; i < array.length; i++) {
            Short b = array[i];
            result[i] = (b == null ? valueForNull : b.shortValue());
        }
        return result;
    }

    public static byte[] toPrimitive(Byte[] array) {
        if (array == null) {
            return new byte[]{};
        } else if (array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].byteValue();
        }
        return result;
    }

    public static byte[] toPrimitive(Byte[] array, byte valueForNull) {
        if (array == null) {
            return new byte[]{};
        } else if (array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            Byte b = array[i];
            result[i] = (b == null ? valueForNull : b.byteValue());
        }
        return result;
    }

    public static double[] toPrimitive(Double[] array) {
        if (array == null) {
            return new double[]{};
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].doubleValue();
        }
        return result;
    }

    public static double[] toPrimitive(Double[] array, double valueForNull) {
        if (array == null) {
            return new double[]{};
        } else if (array.length == 0) {
            return EMPTY_DOUBLE_ARRAY;
        }
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            Double b = array[i];
            result[i] = (b == null ? valueForNull : b.doubleValue());
        }
        return result;
    }

    public static float[] toPrimitive(Float[] array) {
        if (array == null) {
            return new float[]{};
        } else if (array.length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].floatValue();
        }
        return result;
    }

    public static float[] toPrimitive(Float[] array, float valueForNull) {
        if (array == null) {
            return new float[]{};
        } else if (array.length == 0) {
            return EMPTY_FLOAT_ARRAY;
        }
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            Float b = array[i];
            result[i] = (b == null ? valueForNull : b.floatValue());
        }
        return result;
    }

    public static boolean[] toPrimitive(Boolean[] array) {
        if (array == null) {
            return new boolean[]{};
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }
        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].booleanValue();
        }
        return result;
    }

    public static boolean[] toPrimitive(Boolean[] array, boolean valueForNull) {
        if (array == null) {
            return new boolean[]{};
        } else if (array.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }
        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            Boolean b = array[i];
            result[i] = (b == null ? valueForNull : b.booleanValue());
        }
        return result;
    }

    public static Object[] subarray(Object[] array, int startIndexInclusive, int endIndexExclusive) {
        int newSize = endIndexExclusive - startIndexInclusive;
        Class<?> type = array.getClass().getComponentType();
        if (newSize <= 0) {
            return (Object[]) Array.newInstance(type, 0);
        }
        Object[] subarray = (Object[]) Array.newInstance(type, newSize);
        System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
        return subarray;
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(char[] array) {
        return array == null || array.length == 0;
    }

    public static <T> T[] clone(T[] array) {
        if (array == null) {
            ArrayList<T> list = new ArrayList<>();
            return (T[]) list.toArray();
        }
        return array.clone();
    }

    public static String toString(Object[] array) {
        return toString(array, "{}");
    }

    public static String toString(Object[] array, String stringIfNull) {
        if (array == null) {
            return stringIfNull;
        }
        return Arrays.toString(array);
    }

    public static boolean contains(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind) != INDEX_NOT_FOUND;
    }

    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }

    public static int indexOf(long[] array, long valueToFind) {
        return indexOf(array, valueToFind, 0);
    }

    public static int indexOf(long[] array, long valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else if (array.getClass().getComponentType().isInstance(objectToFind)) {
            for (int i = startIndex; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean isSameLength(Object[] array1, Object[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    /**
     * <p>
     * Checks whether two arrays are the same length, treating {@code null}
     * arrays as length {@code 0}.
     * </p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating {@code null}
     * as an empty array
     */
    public static boolean isSameLength(long[] array1, long[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    /**
     * <p>
     * Checks whether two arrays are the same length, treating {@code null}
     * arrays as length {@code 0}.
     * </p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating {@code null}
     * as an empty array
     */
    public static boolean isSameLength(int[] array1, int[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    /**
     * <p>
     * Checks whether two arrays are the same length, treating {@code null}
     * arrays as length {@code 0}.
     * </p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating {@code null}
     * as an empty array
     */
    public static boolean isSameLength(short[] array1, short[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    /**
     * <p>
     * Checks whether two arrays are the same length, treating {@code null}
     * arrays as length {@code 0}.
     * </p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating {@code null}
     * as an empty array
     */
    public static boolean isSameLength(char[] array1, char[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    /**
     * <p>
     * Checks whether two arrays are the same length, treating {@code null}
     * arrays as length {@code 0}.
     * </p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating {@code null}
     * as an empty array
     */
    public static boolean isSameLength(byte[] array1, byte[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    /**
     * <p>
     * Checks whether two arrays are the same length, treating {@code null}
     * arrays as length {@code 0}.
     * </p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating {@code null}
     * as an empty array
     */
    public static boolean isSameLength(double[] array1, double[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    /**
     * <p>
     * Checks whether two arrays are the same length, treating {@code null}
     * arrays as length {@code 0}.
     * </p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating {@code null}
     * as an empty array
     */
    public static boolean isSameLength(float[] array1, float[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    /**
     * <p>
     * Checks whether two arrays are the same length, treating {@code null}
     * arrays as length {@code 0}.
     * </p>
     *
     * @param array1 the first array, may be {@code null}
     * @param array2 the second array, may be {@code null}
     * @return {@code true} if length of arrays matches, treating {@code null}
     * as an empty array
     */
    public static boolean isSameLength(boolean[] array1, boolean[] array2) {
        return (!((array1 == null && array2 != null && array2.length > 0)
                || (array2 == null && array1 != null && array1.length > 0)
                || (array1 != null && array2 != null && array1.length != array2.length)));
    }

    public static <T> T[] remove(final T[] array, final int index) {
        return (T[]) remove((Object) array, index);
    }

    public static <T> T[] removeElement(final T[] array, final Object element) {
        final int index = indexOf(array, element);
        if (index == INDEX_NOT_FOUND) {
            return clone(array);
        }
        return remove(array, index);
    }

    public static boolean[] remove(final boolean[] array, final int index) {
        return (boolean[]) remove((Object) array, index);
    }

    public static byte[] remove(final byte[] array, final int index) {
        return (byte[]) remove((Object) array, index);
    }

    private static Object remove(final Object array, final int index) {
        final int length = getLength(array);
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }

        if (array != null) {
            final Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
            System.arraycopy(array, 0, result, 0, index);
            if (index < length - 1) {
                System.arraycopy(array, index + 1, result, index, length - index - 1);
            }
        }

        return null;
    }

    public static int getLength(final Object array) {
        if (array == null) {
            return 0;
        }
        return Array.getLength(array);
    }

    public static <T> List<T> asList(T[] array) {
        return Arrays.asList(array);
    }

    public static <T> List<T> asImmutableList(T[] array) {
        return Collections.unmodifiableList(asList(array));
    }

    public static <T> List<T> asList(T[] array, Class<? extends List> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        List<T> result = clazz.getConstructor().newInstance();
        Collections.addAll(result, array);
        return result;
    }

    public static <T> List<T> asImmutableList(T[] array, Class<? extends List> clazz) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return Collections.unmodifiableList(asList(array, clazz));
    }
}
