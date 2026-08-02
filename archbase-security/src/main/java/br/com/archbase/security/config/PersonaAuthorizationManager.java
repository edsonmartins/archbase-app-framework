package br.com.archbase.security.config;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.access.Restriction;
import br.com.archbase.security.annotations.RequirePersona;
import br.com.archbase.security.service.ArchbaseSecurityService;
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
 * Adaptador de {@code @RequirePersona} para o core.
 *
 * <p>Não decide nada: lê a anotação, monta um {@link AccessRequirement} e delega. A regra — incluído
 * o mapeamento legado de perfil para persona — vive em {@code PersonaRestrictionEvaluator}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PersonaAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final ArchbaseSecurityService securityService;

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        RequirePersona requirePersona = AuthorizationAnnotationUtils.findAnnotation(methodInvocation, RequirePersona.class);

        if (requirePersona == null) {
            log.error("Interceptação de @RequirePersona sem anotação resolvível em {}#{} — acesso negado",
                    AuthorizationAdapters.declaringClass(methodInvocation),
                    methodInvocation.getMethod().getName());
            return new AuthorizationDecision(false);
        }

        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            log.debug("Usuário não autenticado - acesso negado");
            return new AuthorizationDecision(false);
        }

        Restriction restricao = Restriction.persona(
                Arrays.asList(requirePersona.value()),
                requirePersona.requireAll(),
                requirePersona.allowSystemAdmin(),
                requirePersona.requireActiveUser(),
                requirePersona.ownerOnly(),
                requirePersona.context(),
                requirePersona.message());

        String origem = AuthorizationAdapters.origin(methodInvocation);

        AccessRequirement requisito = requirePersona.resource().isEmpty()
                ? AccessRequirement.ofRestrictions(origem, restricao)
                : AccessRequirement.ofRestrictionsAndCapability(origem,
                        requirePersona.resource(),
                        requirePersona.action().isEmpty() ? "READ" : requirePersona.action(),
                        restricao);

        AccessDecision decisao = securityService.decide(auth, requisito);
        AuthorizationAdapters.log(log, decisao, origem);
        return new AuthorizationDecision(decisao.allowed());
    }
}
