package br.com.archbase.security.util;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Resolve a anotação de autorização que disparou o interceptador.
 *
 * <p><b>Por que existe.</b> {@code @RequireRole}, {@code @RequireProfile} e {@code @RequirePersona}
 * declaram {@code @Target({METHOD, TYPE})} e o pointcut
 * ({@link CustomPointcutUtil#forAnnotations}) intercepta os dois casos. Os managers, porém, liam
 * apenas {@code method.getAnnotation(...)}: anotação posta na <b>classe</b> do controller fazia o
 * interceptador rodar em todo método, não achar nada, e liberar — anotar a classe ficava pior do
 * que não anotar. O mesmo acontecia quando a anotação estava no método da implementação e o proxy
 * invocava o método da interface.
 *
 * <p>A busca segue a ordem natural de especificidade: método invocado → método da classe alvo →
 * classe alvo → classe declarante. Usa {@code AnnotatedElementUtils} para enxergar também
 * anotações meta-anotadas (composição).
 */
public final class AuthorizationAnnotationUtils {

    private AuthorizationAnnotationUtils() {
        // Classe utilitária
    }

    /**
     * @return a anotação aplicável à invocação, ou {@code null} se não houver nenhuma. Quem chama
     *         deve tratar {@code null} como <b>negação</b>: o interceptador só roda quando o
     *         pointcut casou, então "não encontrei" significa que a resolução falhou — e liberar
     *         nesse caso é exatamente o defeito que esta classe corrige.
     */
    public static <A extends Annotation> A findAnnotation(MethodInvocation invocation, Class<A> annotationType) {
        Method method = invocation.getMethod();

        A annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        if (annotation != null) {
            return annotation;
        }

        Class<?> targetClass = resolveTargetClass(invocation);
        if (targetClass != null) {
            Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
            annotation = AnnotatedElementUtils.findMergedAnnotation(targetMethod, annotationType);
            if (annotation != null) {
                return annotation;
            }
            annotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, annotationType);
            if (annotation != null) {
                return annotation;
            }
        }

        return AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), annotationType);
    }

    private static Class<?> resolveTargetClass(MethodInvocation invocation) {
        Object target = invocation.getThis();
        return target != null ? AopUtils.getTargetClass(target) : null;
    }
}
