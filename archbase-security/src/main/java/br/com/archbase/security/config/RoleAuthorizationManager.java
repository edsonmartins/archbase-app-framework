package br.com.archbase.security.config;

import br.com.archbase.security.annotations.RequireRole;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import br.com.archbase.security.util.AuthorizationAnnotationUtils;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * AuthorizationManager para processar a anotação {@link RequireRole}.
 *
 * <p>As roles de {@code @RequireRole} são do domínio da aplicação, não do Archbase; quem as conhece
 * é a implementação de {@link ArchbaseRoleResolver} registrada pelo projeto. Sem esse bean o
 * framework não tem o que comparar — veja
 * {@code archbase.security.require-role.no-resolver-policy} para escolher o que fazer nesse caso.
 */
@Component
@Slf4j
public class RoleAuthorizationManager implements AuthorizationManager<MethodInvocation> {

    /** Nega o acesso quando não há {@link ArchbaseRoleResolver} registrado. */
    private static final String POLICY_DENY = "deny";

    /**
     * Lista, e não bean único: uma aplicação modular pode registrar um resolver por módulo, e
     * injetar {@code ArchbaseRoleResolver} direto derrubaria a subida com
     * {@code NoUniqueBeanDefinitionException}. As roles de todos os resolvers são unidas.
     */
    @Autowired(required = false)
    private List<ArchbaseRoleResolver> roleResolvers = List.of();

    /**
     * O que fazer quando {@code @RequireRole} é avaliada sem nenhum {@link ArchbaseRoleResolver}
     * registrado: {@code permit} (padrão) ou {@code deny}.
     *
     * <p>O padrão é {@code permit} apenas por compatibilidade: até esta versão a validação de roles
     * não existia — o manager liberava todo usuário ativo, ignorando os valores da anotação — e
     * mudar isso de uma vez tiraria do ar aplicações que hoje passam por esses métodos. Registre um
     * resolver e mude para {@code deny}; enquanto estiver em {@code permit} sem resolver,
     * {@code @RequireRole} <b>não</b> é um controle de acesso, e o log avisa em cada método afetado.
     */
    @Value("${archbase.security.require-role.no-resolver-policy:permit}")
    private String noResolverPolicy;

    /** Métodos já avisados, para o alerta sair uma vez por método em vez de a cada requisição. */
    private final Set<String> warnedMethods = ConcurrentHashMap.newKeySet();

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication, MethodInvocation methodInvocation) {
        RequireRole requireRole = AuthorizationAnnotationUtils.findAnnotation(methodInvocation, RequireRole.class);

        if (requireRole == null) {
            // Nega: o pointcut casou, então a anotação existe (possivelmente na classe) e a
            // resolução é que falhou. Liberar aqui é o defeito, não o comportamento.
            log.error("Interceptação de @RequireRole sem anotação resolvível em {}#{} — acesso negado",
                    methodInvocation.getMethod().getDeclaringClass().getName(),
                    methodInvocation.getMethod().getName());
            return new AuthorizationDecision(false);
        }

        try {
            Authentication auth = authentication.get();

            if (auth == null || !auth.isAuthenticated()) {
                log.debug("Usuário não autenticado - acesso negado");
                return new AuthorizationDecision(false);
            }

            UserEntity user = (UserEntity) auth.getPrincipal();

            if (!user.isEnabled()) {
                log.debug("Usuário inativo ou bloqueado: {}", user.getEmail());
                return new AuthorizationDecision(false);
            }

            // Permite bypass para administradores do sistema
            if (requireRole.allowSystemAdmin() && Boolean.TRUE.equals(user.getIsAdministrator())) {
                log.debug("Acesso permitido para administrador do sistema: {}", user.getEmail());
                return new AuthorizationDecision(true);
            }

            // Verificação de admin da plataforma
            if (requireRole.requirePlatformAdmin() && !Boolean.TRUE.equals(user.getIsAdministrator())) {
                log.debug("Acesso negado - usuário não é admin da plataforma: {}", user.getEmail());
                return new AuthorizationDecision(false);
            }

            boolean hasAccess = validateRoleAccess(user, requireRole, methodInvocation);

            log.debug("Resultado da validação de role para usuário {}: {}", user.getEmail(), hasAccess);

            return new AuthorizationDecision(hasAccess);

        } catch (Exception e) {
            log.error("Erro ao validar acesso por role", e);
            return new AuthorizationDecision(false);
        }
    }

    /**
     * Compara as roles exigidas pela anotação com as roles que o {@link ArchbaseRoleResolver}
     * devolve para o usuário.
     */
    private boolean validateRoleAccess(UserEntity user, RequireRole requireRole, MethodInvocation invocation) {
        List<String> requiredRoles = Arrays.asList(requireRole.value());

        if (roleResolvers == null || roleResolvers.isEmpty()) {
            return handleMissingResolver(requiredRoles, invocation);
        }

        Set<String> userRoles = new HashSet<>();
        for (ArchbaseRoleResolver resolver : roleResolvers) {
            Set<String> resolved = resolver.resolveRoles(user);
            if (resolved != null) {
                userRoles.addAll(resolved);
            }
        }

        log.debug("Roles exigidas: {} | Roles do usuário {}: {}", requiredRoles, user.getEmail(), userRoles);

        boolean hasRole = requireRole.requireAll()
                ? userRoles.containsAll(requiredRoles)
                : requiredRoles.stream().anyMatch(userRoles::contains);

        if (!hasRole) {
            return false;
        }

        // Basta um resolver reconhecer o usuário como proprietário: cada um responde pelo seu
        // módulo, e exigir unanimidade negaria acesso legítimo por causa de quem não tem opinião.
        if (requireRole.ownerOnly() && roleResolvers.stream().noneMatch(resolver -> resolver.isOwner(user))) {
            log.debug("Acesso negado - método restrito a proprietários e {} não é", user.getEmail());
            return false;
        }

        return true;
    }

    private boolean handleMissingResolver(List<String> requiredRoles, MethodInvocation invocation) {
        boolean deny = POLICY_DENY.equalsIgnoreCase(noResolverPolicy);
        String signature = invocation.getMethod().getDeclaringClass().getName()
                + "#" + invocation.getMethod().getName();

        if (warnedMethods.add(signature)) {
            log.warn("@RequireRole({}) em {} não pode ser validada: nenhum bean ArchbaseRoleResolver "
                            + "registrado. Política atual: {}. Registre um ArchbaseRoleResolver e configure "
                            + "archbase.security.require-role.no-resolver-policy=deny.",
                    requiredRoles, signature, deny ? "negar" : "PERMITIR (sem controle de acesso efetivo)");
        }

        return !deny;
    }
}
