package br.com.archbase.security.access;

import br.com.archbase.security.spi.ArchbaseRoleResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code @RequireRole} — papéis do domínio da aplicação.
 *
 * <p>Ordem preservada de {@code RoleAuthorizationManager}: a conta desativada é verificada
 * <b>antes</b> da isenção de administrador, ao contrário de {@link ProfileRestrictionEvaluator} e
 * {@link PersonaRestrictionEvaluator}. A diferença é comportamento existente.
 *
 * <p>As roles pertencem à aplicação, não ao Archbase; quem as conhece é o
 * {@link ArchbaseRoleResolver} que o projeto registra. Sem esse bean o framework não tem o que
 * comparar, e {@code archbase.security.require-role.no-resolver-policy} decide o que fazer — com
 * padrão {@code permit}, que <b>não é controle de acesso</b> e apenas preserva o comportamento
 * anterior à existência da validação.
 */
@Component
public class RoleRestrictionEvaluator implements RestrictionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RoleRestrictionEvaluator.class);

    private static final String POLICY_DENY = "deny";

    /**
     * Lista, e não bean único: uma aplicação modular pode registrar um resolver por módulo, e
     * injetar {@code ArchbaseRoleResolver} direto derrubaria a subida com
     * {@code NoUniqueBeanDefinitionException}. As roles de todos os resolvers são unidas.
     */
    @Autowired(required = false)
    private List<ArchbaseRoleResolver> roleResolvers = List.of();

    @Value("${archbase.security.require-role.no-resolver-policy:permit}")
    private String noResolverPolicy;

    /** Origens já avisadas, para o alerta sair uma vez por método e não a cada requisição. */
    private final Set<String> warnedOrigins = ConcurrentHashMap.newKeySet();

    @Override
    public RestrictionKind kind() {
        return RestrictionKind.ROLE;
    }

    @Override
    public RestrictionResult evaluate(AccessSubject subject, Restriction restriction, AccessRequirement requirement) {
        if (!subject.enabled()) {
            return RestrictionResult.denied(AccessReasonCodes.ACCOUNT_NOT_ACTIVE,
                    "conta desativada ou bloqueada");
        }

        if (restriction.allowSystemAdmin() && subject.isAdministrator()) {
            return RestrictionResult.waived("administrador isento por allowSystemAdmin");
        }

        if (restriction.requirePlatformAdmin() && !subject.isAdministrator()) {
            return RestrictionResult.denied(AccessReasonCodes.PLATFORM_ADMIN_REQUIRED,
                    "requirePlatformAdmin exige isAdministrator");
        }

        if (roleResolvers == null || roleResolvers.isEmpty()) {
            return semResolver(restriction, requirement);
        }

        Set<String> doUsuario = new HashSet<>();
        for (ArchbaseRoleResolver resolver : roleResolvers) {
            Set<String> resolvidas = resolver.resolveRoles(subject.principal());
            if (resolvidas != null) {
                doUsuario.addAll(resolvidas);
            }
        }

        boolean tem = restriction.requireAll()
                ? doUsuario.containsAll(restriction.required())
                : restriction.required().stream().anyMatch(doUsuario::contains);

        if (!tem) {
            return RestrictionResult.denied(AccessReasonCodes.ROLE_NOT_MATCHED,
                    "papéis do usuário " + doUsuario + "; exigido " + restriction.required()
                            + (restriction.requireAll() ? " (todos)" : ""));
        }

        if (restriction.ownerOnly() && !ehDono(subject)) {
            return RestrictionResult.denied(AccessReasonCodes.NOT_OWNER,
                    "ownerOnly não confirmado pelo ArchbaseRoleResolver");
        }

        return RestrictionResult.passed("papéis " + doUsuario);
    }

    /**
     * Responde {@code ownerOnly}.
     *
     * <p>Com <b>um</b> resolver a pergunta é direta. Com mais de um ela não tem resposta: o SPI
     * expõe {@code isOwner(user)} sem qualquer noção de domínio, então nada liga a propriedade que
     * um resolver afirma à role que outro forneceu. Aceitar qualquer "sim" deixaria um módulo
     * responder por outro. Diante de uma pergunta sem resposta, nega e diz por quê.
     */
    private boolean ehDono(AccessSubject subject) {
        if (roleResolvers.size() > 1) {
            log.error("@RequireRole(ownerOnly=true) com {} ArchbaseRoleResolver registrados: "
                            + "isOwner() não identifica o domínio, então não há como saber qual resolver "
                            + "responde por este método. Acesso negado. Consolide em um único resolver.",
                    roleResolvers.size());
            return false;
        }
        return roleResolvers.get(0).isOwner(subject.principal());
    }

    private RestrictionResult semResolver(Restriction restriction, AccessRequirement requirement) {
        boolean nega = POLICY_DENY.equalsIgnoreCase(noResolverPolicy);
        String origem = requirement.origin() == null ? requirement.label() : requirement.origin();

        if (warnedOrigins.add(origem)) {
            log.warn("@RequireRole({}) em {} não pode ser validada: nenhum bean ArchbaseRoleResolver "
                            + "registrado. Política atual: {}. Registre um ArchbaseRoleResolver e configure "
                            + "archbase.security.require-role.no-resolver-policy=deny.",
                    restriction.required(), origem,
                    nega ? "negar" : "PERMITIR (sem controle de acesso efetivo)");
        }

        if (nega) {
            return RestrictionResult.denied(AccessReasonCodes.ROLE_RESOLVER_MISSING,
                    "nenhum ArchbaseRoleResolver registrado e política = deny");
        }
        return RestrictionResult.passed(
                "nenhum ArchbaseRoleResolver registrado e política = permit — sem controle efetivo");
    }
}
