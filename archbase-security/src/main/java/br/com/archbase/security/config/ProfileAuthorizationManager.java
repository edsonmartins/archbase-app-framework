package br.com.archbase.security.config;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.access.Restriction;
import br.com.archbase.security.annotations.RequireProfile;
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
 * Adaptador de {@code @RequireProfile} para o core.
 *
 * <p>Não decide nada: lê a anotação, monta um {@link AccessRequirement} e delega. A regra vive em
 * {@code ProfileRestrictionEvaluator}, e a composição com os demais portões, no
 * {@code ArchbaseAccessEvaluator}.
 *
 * <p>Quando a anotação declara {@code resource}, o requisito carrega <b>tranca e capacidade</b> —
 * o que antes era o manager chamando {@code hasPermission} por dentro, misturando restrição com
 * concessão. Agora são dois portões distintos do mesmo requisito, na mesma ordem de sempre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    private final ArchbaseSecurityService securityService;

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        RequireProfile requireProfile = AuthorizationAnnotationUtils.findAnnotation(methodInvocation, RequireProfile.class);

        if (requireProfile == null) {
            // Nega: o pointcut casou, então a anotação existe (possivelmente na classe) e a
            // resolução é que falhou. Liberar aqui é o defeito, não o comportamento.
            log.error("Interceptação de @RequireProfile sem anotação resolvível em {}#{} — acesso negado",
                    AuthorizationAdapters.declaringClass(methodInvocation),
                    methodInvocation.getMethod().getName());
            return new AuthorizationDecision(false);
        }

        // Falha ao AVALIAR não é o mesmo que "não tem permissão", mas negar é a opção
        // segura. O que não pode é escapar: sem este catch, um ArchbaseRoleResolver da
        // aplicação que lance, ou uma associação lazy tocada fora de sessão, viram HTTP 500
        // em vez de 403 — e o 500 vaza stack trace onde deveria haver uma negação limpa.
        try {
            Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                log.debug("Usuário não autenticado - acesso negado");
                return new AuthorizationDecision(false);
            }

            Restriction restricao = Restriction.profile(
                    Arrays.asList(requireProfile.value()),
                    requireProfile.requireAll(),
                    requireProfile.allowSystemAdmin(),
                    requireProfile.requireActiveUser(),
                    requireProfile.message());

            String origem = AuthorizationAdapters.origin(methodInvocation);

            AccessRequirement requisito = requireProfile.resource().isEmpty()
                    ? AccessRequirement.ofRestrictions(origem, restricao)
                    : AccessRequirement.ofRestrictionsAndCapability(origem,
                            requireProfile.resource(),
                            requireProfile.action().isEmpty() ? "READ" : requireProfile.action(),
                            restricao);

            AccessDecision decisao = securityService.decide(auth, requisito);
            AuthorizationAdapters.log(log, decisao, origem);
            return new AuthorizationDecision(decisao.allowed());
        } catch (Exception e) {
            log.error("Erro ao avaliar @RequireProfile em {}", AuthorizationAdapters.origin(methodInvocation), e);
            return new AuthorizationDecision(false);
        }
    }
}
