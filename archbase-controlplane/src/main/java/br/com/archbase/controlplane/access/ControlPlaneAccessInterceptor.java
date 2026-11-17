package br.com.archbase.controlplane.access;

import br.com.archbase.controlplane.ControlPlane;
import br.com.archbase.controlplane.api.Entitlements;
import br.com.archbase.controlplane.session.TenantSession;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.tenant.TenantSnapshot;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Aplica {@link RequiresAdmin}, {@link RequiresSeat} e {@link RequiresEntitlement} antes do método, nessa ordem.
 *
 * @since 3.4.6
 */
public class ControlPlaneAccessInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneAccessInterceptor.class);

    private final ControlPlaneState state;

    public ControlPlaneAccessInterceptor(ControlPlaneState state) {
        this.state = state;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RequiresAdmin requiresAdmin = find(invocation, RequiresAdmin.class);
        if (requiresAdmin != null) {
            checkAdmin(requiresAdmin.value(), invocation);
        }
        if (find(invocation, RequiresSeat.class) != null) {
            checkSeat(invocation);
        }
        RequiresEntitlement entitlement = find(invocation, RequiresEntitlement.class);
        if (entitlement != null) {
            checkEntitlement(entitlement.value(), invocation);
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
            deny(ControlPlaneAccessDeniedException.NOT_ADMIN, "Exige papel administrativo: " + scope.minimum().claimValue(),
                    session, invocation);
        }
    }

    private void checkSeat(MethodInvocation invocation) {
        Optional<TenantSession> session = ControlPlane.findCurrentSession();
        boolean allowed = session.isPresent() && !session.get().instance() && session.get().hasSeat();
        if (allowed) {
            Optional<TenantSnapshot> snapshot = state.tenant(session.get().tenantId());
            if (snapshot.isPresent() && snapshot.get().seatsKnown()
                    && !snapshot.get().seatHolders().containsKey(session.get().userId())) {
                allowed = false;
            }
        }
        if (!allowed) {
            deny(ControlPlaneAccessDeniedException.NO_SEAT, "Usuário sem assento neste produto", session, invocation);
        }
    }

    private void checkEntitlement(String key, MethodInvocation invocation) {
        if (!new Entitlements(state, ArchbaseTenantContext.getTenantId()).can(key)) {
            deny(ControlPlaneAccessDeniedException.NOT_ENTITLED, "Recurso não contratado: " + key,
                    ControlPlane.findCurrentSession(), invocation);
        }
    }

    private static void deny(String code, String message, Optional<TenantSession> session,
                             MethodInvocation invocation) {
        log.info("controlplane.access.denied code={} tnt={} sub={} metodo={}", code,
                ArchbaseTenantContext.getTenantId(), session.map(TenantSession::userId).orElse(null),
                invocation.getMethod().getName());
        throw new ControlPlaneAccessDeniedException(code, message);
    }

    /** A anotação do método (inclusive na classe-alvo, quando há proxy) ou, na falta, a da classe. */
    private static <A extends Annotation> A find(MethodInvocation invocation, Class<A> type) {
        Method method = invocation.getMethod();
        Class<?> targetClass = invocation.getThis() != null
                ? AopUtils.getTargetClass(invocation.getThis()) : method.getDeclaringClass();
        Method specific = AopUtils.getMostSpecificMethod(method, targetClass);
        A onMethod = AnnotatedElementUtils.findMergedAnnotation(specific, type);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, type);
    }
}
