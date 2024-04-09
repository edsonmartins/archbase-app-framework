package br.com.archbase.shared.kernel.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Annotations implements AnnotatedElement {

    private final Map<Class<? extends Annotation>, Annotation> annotationsCache = new HashMap<>();

    public Annotations(AnnotatedElement... elements) {
        for (AnnotatedElement element : elements) {
            for (Annotation annotation : element.getAnnotations()) {
                annotationsCache.put(annotation.annotationType(), annotation);
            }
        }
    }


    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return (T) annotationsCache.get(annotationClass);
    }


    public Annotation[] getAnnotations() {
        return annotationsCache.values().toArray(new Annotation[annotationsCache.values().size()]);
    }


    public Annotation[] getDeclaredAnnotations() {
        return getAnnotations();
    }


    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return annotationsCache.containsKey(annotationClass);
    }

    public void addAnnotation(Annotation annotation) {
        if (annotation != null) {
            annotationsCache.put(annotation.annotationType(), annotation);
        }

    }

}
