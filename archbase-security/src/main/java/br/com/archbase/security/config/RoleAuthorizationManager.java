package br.com.archbase.security.config;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.access.Restriction;
import br.com.archbase.security.annotations.RequireRole;
import br.com.archbase.security.service.ArchbaseSecurityService;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import br.com.archbase.security.util.AuthorizationAnnotationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Adaptador de {@code @RequireRole} para o core.
 *
 * <p>Não decide nada: lê a anotação, monta um {@link AccessRequirement} e delega. A regra — o SPI
 * {@link ArchbaseRoleResolver}, a política para a ausência dele e o tratamento de {@code ownerOnly}
 * — vive em {@code RoleRestrictionEvaluator}.
 *
 * <p>As roles de {@code @RequireRole} são do domínio da aplicação, não do Archbase. Sem um
 * {@link ArchbaseRoleResolver} registrado o framework não tem o que comparar, e
 * {@code archbase.security.require-role.no-resolver-policy} decide — com padrão {@code permit}, que
 * <b>não é controle de acesso</b>.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final ArchbaseSecurityService securityService;

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        RequireRole requireRole = AuthorizationAnnotationUtils.findAnnotation(methodInvocation, RequireRole.class);

        if (requireRole == null) {
            // Nega: o pointcut casou, então a anotação existe (possivelmente na classe) e a
            // resolução é que falhou. Liberar aqui é o defeito, não o comportamento.
            log.error("Interceptação de @RequireRole sem anotação resolvível em {}#{} — acesso negado",
                    AuthorizationAdapters.declaringClass(methodInvocation),
                    methodInvocation.getMethod().getName());
            return new AuthorizationDecision(false);
        }

        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            log.debug("Usuário não autenticado - acesso negado");
            return new AuthorizationDecision(false);
        }

        Restriction restricao = Restriction.role(
                Arrays.asList(requireRole.value()),
                requireRole.requireAll(),
                requireRole.allowSystemAdmin(),
                requireRole.requirePlatformAdmin(),
                requireRole.ownerOnly(),
                requireRole.context(),
                requireRole.message());

        String origem = AuthorizationAdapters.origin(methodInvocation);
        AccessDecision decisao = securityService.decide(auth, AccessRequirement.ofRestrictions(origem, restricao));
        AuthorizationAdapters.log(log, decisao, origem);
        return new AuthorizationDecision(decisao.allowed());
    }
}
