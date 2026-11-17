package br.com.archbase.controlplane.access;

import br.com.archbase.controlplane.ControlPlane;
import br.com.archbase.controlplane.session.TenantSession;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Aplica {@link RequiresAdmin} antes do método.
 *
 * @since 3.4.6
 */
public class ControlPlaneAccessInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneAccessInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RequiresAdmin requiresAdmin = find(invocation);
        if (requiresAdmin != null) {
            checkAdmin(requiresAdmin.value(), invocation);
        }
        return invocation.proceed();
    }

    private static void checkAdmin(AdminScope scope, MethodInvocation invocation) {
        Optional<TenantSession> session = ControlPlane.findCurrentSession();
        boolean allowed = session.isPresent()
                && !session.get().instance()
                && session.get().adminRole() != null
                && session.get().adminRole().atLeast(scope.minimum());
        if (!allowed) {
            log.info("controlplane.access.denied code={} scope={} sub={} metodo={}",
                    ControlPlaneAccessDeniedException.NOT_ADMIN, scope,
                    session.map(TenantSession::userId).orElse(null), invocation.getMethod().getName());
            throw new ControlPlaneAccessDeniedException(ControlPlaneAccessDeniedException.NOT_ADMIN,
                    "Exige papel administrativo: " + scope.minimum().claimValue());
        }
    }

    /** A anotação do método (inclusive na classe-alvo, quando há proxy) ou, na falta, a da classe. */
    private static RequiresAdmin find(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        Class<?> targetClass = invocation.getThis() != null
                ? AopUtils.getTargetClass(invocation.getThis()) : method.getDeclaringClass();
        Method specific = AopUtils.getMostSpecificMethod(method, targetClass);
        RequiresAdmin onMethod = AnnotatedElementUtils.findMergedAnnotation(specific, RequiresAdmin.class);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, RequiresAdmin.class);
    }
}
