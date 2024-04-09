package br.com.archbase.plugin.manager.util;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassUtils {

    private ClassUtils() {
    }

    public static List<String> getAllInterfacesNames(Class<?> aClass) {
        return toString(getAllInterfaces(aClass));
    }

    public static List<Class<?>> getAllInterfaces(Class<?> aClass) {
        List<Class<?>> list = new ArrayList<>();

        while (aClass != null) {
            Class<?>[] interfaces = aClass.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                if (!list.contains(anInterface)) {
                    list.add(anInterface);
                }

                List<Class<?>> superInterfaces = getAllInterfaces(anInterface);
                for (Class<?> superInterface : superInterfaces) {
                    if (!list.contains(superInterface)) {
                        list.add(superInterface);
                    }
                }
            }

            aClass = aClass.getSuperclass();
        }

        return list;
    }

    /**
     * Obtenha uma certa anotação de um {@link TypeElement}.
     * Consulte <a href="https://stackoverflow.com/a/10167558"> stackoverflow.com</a> para obter mais informações.
     *
     * @param typeElement     o elemento de tipo, que contém a anotação solicitada
     * @param annotationClass a classe da anotação solicitada
     * @return a anotação solicitada ou nulo, se nenhuma anotação da classe fornecida foi encontrada
     * @throws NullPointerException se <code> typeElement </code> ou <code> annotationClass </code> for nulo
     */
    public static AnnotationMirror getAnnotationMirror(TypeElement typeElement, Class<?> annotationClass) {
        String annotationClassName = annotationClass.getName();
        for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(annotationClassName)) {
                return m;
            }
        }

        return null;
    }

    /**
     * Obtenha um determinado parâmetro de um {@link AnnotationMirror}.
     * Consulte <a href="https://stackoverflow.com/a/10167558"> stackoverflow.com </a> para obter mais informações.
     *
     * @param annotationMirror    a anotação, que contém o parâmetro solicitado
     * @param annotationParameter o nome do parâmetro de anotação solicitado
     * @return o parâmetro solicitado ou nulo, se nenhum parâmetro do nome fornecido foi encontrado
     * @throws NullPointerException se <code> annotationMirror </code> for nulo
     */
    public static AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String annotationParameter) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(annotationParameter)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Obtenha um determinado parâmetro de anotação de um {@link TypeElement}.
     * Consulte <a href="https://stackoverflow.com/a/10167558"> stackoverflow.com </a> para obter mais informações.
     *
     * @param typeElement         o elemento de tipo, que contém a anotação solicitada
     * @param annotationClass     a classe da anotação solicitada
     * @param annotationParameter o nome do parâmetro de anotação solicitado
     * @return o parâmetro solicitado ou nulo, se nenhuma anotação para a classe fornecida foi encontrada ou nenhum parâmetro de anotação foi encontrado
     * @throws NullPointerException se <code> typeElement </code> ou <code> annotationClass </code> for nulo
     */
    public static AnnotationValue getAnnotationValue(TypeElement typeElement, Class<?> annotationClass, String annotationParameter) {
        AnnotationMirror annotationMirror = getAnnotationMirror(typeElement, annotationClass);
        return annotationMirror != null ? getAnnotationValue(annotationMirror, annotationParameter) : null;
    }

    /**
     * Uses {@link Class#getSimpleName()} to convert from {@link Class} to {@link String}.
     *
     * @param classes
     * @return
     */
    private static List<String> toString(List<Class<?>> classes) {
        List<String> list = new ArrayList<>();

        for (Class<?> aClass : classes) {
            list.add(aClass.getSimpleName());
        }

        return list;
    }

}
