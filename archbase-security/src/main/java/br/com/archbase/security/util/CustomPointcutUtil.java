package br.com.archbase.security.util;

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;

import java.lang.annotation.Annotation;

/**
 * Utilitário para criação de pointcuts personalizados
 */
public final class CustomPointcutUtil {

    private CustomPointcutUtil() {
        // Classe utilitária
    }

    /**
     * Cria um pointcut para anotações específicas, similar ao AuthorizationMethodPointcuts
     */
    @SafeVarargs
    public static Pointcut forAnnotations(Class<? extends Annotation>... annotations) {
        ComposablePointcut pointcut = null;
        
        for (Class<? extends Annotation> annotation : annotations) {
            if (pointcut == null) {
                pointcut = new ComposablePointcut(classOrMethod(annotation));
            } else {
                pointcut.union(classOrMethod(annotation));
            }
        }
        
        return pointcut;
    }

    /**
     * Cria um pointcut que intercepta métodos ou classes anotadas
     */
    private static Pointcut classOrMethod(Class<? extends Annotation> annotation) {
        AnnotationMatchingPointcut methodPointcut = new AnnotationMatchingPointcut(null, annotation, true);
        AnnotationMatchingPointcut classPointcut = new AnnotationMatchingPointcut(annotation, true);
        
        return Pointcuts.union(methodPointcut, classPointcut);
    }
}