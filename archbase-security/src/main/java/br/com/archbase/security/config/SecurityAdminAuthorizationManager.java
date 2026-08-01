package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.annotation.ArchbaseSecurityAdminEndpoint;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.service.ArchbaseSecurityService;
import br.com.archbase.security.util.AuthorizationAnnotationUtils;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Decide quem pode chamar os endpoints administrativos do módulo de segurança, marcados com
 * {@link ArchbaseSecurityAdminEndpoint}.
 *
 * <p>O comportamento vem de {@code archbase.security.admin-endpoints.policy}:
 *
 * <ul>
 *   <li>{@code permit} <b>(padrão)</b> — mantém o comportamento anterior: qualquer usuário
 *       autenticado passa. Só existe para que atualizar o framework não derrube aplicações em
 *       produção que ainda não cadastraram os {@code Resource}/{@code Action} correspondentes.
 *       Enquanto estiver assim, <b>a escalação de privilégio continua aberta</b> — o log avisa em
 *       cada endpoint atingido.</li>
 *   <li>{@code admin-only} <b>(recomendado)</b> — exige {@code isAdministrator}. É a opção que
 *       resolve sem depender de cadastro nenhum.</li>
 *   <li>{@code permission} — exige a permissão {@code action}/{@code resource} declarada na
 *       anotação, pelo mesmo motor de {@code @HasPermission}; administrador segue passando.</li>
 * </ul>
 */
@Component
@Slf4j
public class SecurityAdminAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private static final String POLICY_PERMIT = "permit";
    private static final String POLICY_ADMIN_ONLY = "admin-only";
    private static final String POLICY_PERMISSION = "permission";

    private final ArchbaseSecurityService securityService;

    @Value("${archbase.security.admin-endpoints.policy:permit}")
    private String policy;

    /** Endpoints já avisados, para o alerta sair uma vez por método e não a cada requisição. */
    private final Set<String> warnedMethods = ConcurrentHashMap.newKeySet();

    public SecurityAdminAuthorizationManager(ArchbaseSecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        ArchbaseSecurityAdminEndpoint marker =
                AuthorizationAnnotationUtils.findAnnotation(methodInvocation, ArchbaseSecurityAdminEndpoint.class);

        if (marker == null) {
            log.error("Interceptação administrativa sem anotação resolvível em {}#{} — acesso negado",
                    methodInvocation.getMethod().getDeclaringClass().getName(),
                    methodInvocation.getMethod().getName());
            return new AuthorizationDecision(false);
        }

        // Autoatendimento (trocar a própria senha, ler as próprias permissões) não é administração.
        if (marker.selfService()) {
            return new AuthorizationDecision(true);
        }

        try {
            Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }

            if (POLICY_PERMIT.equalsIgnoreCase(policy)) {
                warnOnce(methodInvocation);
                return new AuthorizationDecision(true);
            }

            if (!(auth.getPrincipal() instanceof UserEntity user)) {
                log.warn("Principal inesperado em endpoint administrativo — acesso negado");
                return new AuthorizationDecision(false);
            }

            if (!user.isEnabled()) {
                return new AuthorizationDecision(false);
            }

            if (Boolean.TRUE.equals(user.getIsAdministrator())) {
                return new AuthorizationDecision(true);
            }

            if (POLICY_ADMIN_ONLY.equalsIgnoreCase(policy)) {
                log.warn("Acesso negado a endpoint administrativo de segurança para não-administrador: {}",
                        user.getEmail());
                return new AuthorizationDecision(false);
            }

            if (POLICY_PERMISSION.equalsIgnoreCase(policy)) {
                boolean allowed = securityService.hasPermission(
                        auth,
                        marker.action(),
                        resolveResource(marker, methodInvocation),
                        ArchbaseTenantContext.getTenantId(),
                        ArchbaseTenantContext.getCompanyId(),
                        null);
                if (!allowed) {
                    log.warn("Acesso negado a endpoint administrativo de segurança para {}: falta {} em {}",
                            user.getEmail(), marker.action(), resolveResource(marker, methodInvocation));
                }
                return new AuthorizationDecision(allowed);
            }

            // Valor desconhecido em archbase.security.admin-endpoints.policy: nega, porque uma
            // configuração que não sabemos interpretar não pode virar liberação silenciosa.
            log.error("Valor inválido para archbase.security.admin-endpoints.policy: '{}'. "
                    + "Use permit, admin-only ou permission. Acesso negado.", policy);
            return new AuthorizationDecision(false);

        } catch (Exception e) {
            log.error("Erro ao avaliar acesso a endpoint administrativo de segurança", e);
            return new AuthorizationDecision(false);
        }
    }

    private String resolveResource(ArchbaseSecurityAdminEndpoint marker, MethodInvocation invocation) {
        if (!marker.resource().isEmpty()) {
            return marker.resource();
        }
        return invocation.getMethod().getDeclaringClass().getSimpleName();
    }

    private void warnOnce(MethodInvocation invocation) {
        String signature = invocation.getMethod().getDeclaringClass().getName()
                + "#" + invocation.getMethod().getName();
        if (warnedMethods.add(signature)) {
            log.warn("Endpoint administrativo de segurança {} liberado para qualquer usuário autenticado "
                            + "(archbase.security.admin-endpoints.policy=permit). Isto permite escalação de "
                            + "privilégio. Configure archbase.security.admin-endpoints.policy=admin-only.",
                    signature);
        }
    }
}
