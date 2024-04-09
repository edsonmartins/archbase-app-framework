package br.com.archbase.event.driven.bus.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class ReflectionUtils {
    private static final Log log = LogFactory.getLog(ReflectionUtils.class);

    private ReflectionUtils() {
    }

    public static List<Method> getAllDeclaredMethodsAnnotatedWith(
            Class<?> cls, Class<? extends Annotation> annotationClass) {
        // Recupere todas as classes reflexivas
        List<Class<?>> reflectingClasses = new ArrayList<>();
        reflectingClasses.add(cls);
        reflectingClasses.addAll((Collection<? extends Class<?>>) getAllSuperClasses(cls));

        // Recupere todos os métodos anotados com annotationClass
        List<Method> resultMethods = new ArrayList<>();
        for (Class<?> reflectingClass : reflectingClasses) {
            resultMethods.addAll(
                    getDeclaredMethodsAnnotatedWithForSingleClass(reflectingClass, annotationClass)
            );
        }

        return resultMethods;
    }

    public static List<Field> getAllDeclaredFieldsAnnotatedWith(
            Class<?> cls, Class<? extends Annotation> annotationClass) {
        // Recuperar classes refletivas
        List<Class<?>> reflectingClasses = new ArrayList<>();
        reflectingClasses.add(cls);
        reflectingClasses.addAll((Collection<? extends Class<?>>) getAllSuperClasses(cls));

        // Recupere todos os campos anotados com annotationClass
        List<Field> resultFields = new ArrayList<>();
        for (Class<?> reflectingClass : reflectingClasses) {
            resultFields.addAll(
                    getDeclaredFieldsAnnotatedWithForSingleClass(reflectingClass, annotationClass)
            );
        }

        return resultFields;
    }

    @SuppressWarnings("java:S3011")
    public static Object getDeclaredFieldValue(Object obj, String fieldName)
            throws NoSuchFieldException {
        List<Class<?>> reflectingClasses = new ArrayList<>();
        reflectingClasses.add(obj.getClass());
        reflectingClasses.addAll((Collection<? extends Class<?>>) getAllSuperClasses(obj.getClass()));

        for (Class<?> reflectingClass : reflectingClasses) {
            try {
                Field field = reflectingClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException ex) {
                //
            } catch (IllegalAccessException ex) {
                log.error(String.format("Erro ao obter o valor do campo %s em %s",
                        fieldName, obj.getClass().getName()));
            }
        }

        throw new NoSuchFieldException(String.format("%s não contém o campo %s",
                obj.getClass().getName(), fieldName));
    }

    private static List<Class<?>> getAllSuperClasses(Class<?> cls) {
        List<Class<?>> superClasses = new ArrayList<>();
        Class<?> currentSuperClass = cls.getSuperclass();
        while (currentSuperClass != null) {
            superClasses.add(currentSuperClass);
            currentSuperClass = currentSuperClass.getSuperclass();
        }
        return superClasses;
    }

    private static List<Method> getDeclaredMethodsAnnotatedWithForSingleClass(
            Class<?> cls, Class<? extends Annotation> annotationClass) {
        List<Method> annotatedMethods = new ArrayList<>();
        for (Method method : cls.getDeclaredMethods()) {
            if (method.getAnnotation(annotationClass) != null) {
                annotatedMethods.add(method);
            }
        }
        return annotatedMethods;
    }

    private static List<Field> getDeclaredFieldsAnnotatedWithForSingleClass(
            Class<?> cls, Class<? extends Annotation> annotationClass) {
        List<Field> annotatedFields = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            if (field.getAnnotation(annotationClass) != null) {
                annotatedFields.add(field);
            }
        }
        return annotatedFields;
    }
}
