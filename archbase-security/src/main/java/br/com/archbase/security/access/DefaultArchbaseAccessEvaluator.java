package br.com.archbase.security.access;

import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação de referência dos portões.
 *
 * <p><b>Esta classe não muda nenhuma decisão.</b> Ela reproduz, portão a portão, o que
 * {@code ArchbaseSecurityService.hasPermission} decidia antes — inclusive onde o comportamento
 * anterior surpreende. O que muda é que agora existe <i>um</i> lugar onde a regra está escrita, e
 * que a resposta carrega o motivo.
 *
 * <p>Dois comportamentos herdados que valem estar explícitos, porque não são o que se supõe:
 *
 * <ul>
 *   <li><b>Conta desativada não é barrada aqui.</b> {@code isEnabled()} só era consultado no atalho
 *       do administrador. Um usuário desativado com token válido segue para o catálogo e pode ser
 *       autorizado — quem o barra é o filtro de autenticação, antes. Mudar isso é decisão de
 *       produto, não de refactor.</li>
 *   <li><b>Administrador ignora o escopo.</b> A flag encerra a decisão antes de {@link Gate#SCOPE},
 *       então um administrador alcança qualquer tenant. Sujeitá-lo ao escopo está no plano do core,
 *       atrás de flag.</li>
 * </ul>
 *
 * <p>Os portões {@link Gate#RESTRICTION} e {@link Gate#LEVEL} ainda não existem — chegam nas fases
 * seguintes. Enquanto não existirem, não aparecem na cadeia.
 */
@Component
public class DefaultArchbaseAccessEvaluator implements ArchbaseAccessEvaluator {

    private static final Logger log = LoggerFactory.getLogger(DefaultArchbaseAccessEvaluator.class);

    private final PermissionJpaRepository permissionRepository;

    public DefaultArchbaseAccessEvaluator(PermissionJpaRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public AccessDecision decide(AccessSubject subject, AccessRequirement requirement) {
        List<GateOutcome> chain = new ArrayList<>();

        // ---------------------------------------------------------------- 1. IDENTITY

        if (subject == null) {
            // Antes: ClassCastException no cast do principal, capturada pelo AuthorizationManager e
            // transformada em negação com stack trace apontando para o lugar errado. Mesma decisão,
            // agora com motivo. Resolver o principal de um UserDetailsService próprio é
            // funcionalidade nova — alargaria acesso, então não entra num passo de refactor.
            chain.add(GateOutcome.denied(Gate.IDENTITY, AccessReasonCodes.PRINCIPAL_NOT_SUPPORTED,
                    "O principal autenticado não é um UserEntity do Archbase"));
            return AccessDecision.denied(Gate.IDENTITY, AccessReasonCodes.PRINCIPAL_NOT_SUPPORTED,
                    "Não foi possível resolver o usuário autenticado como usuário do Archbase. "
                            + "Aplicações com UserDetailsService próprio não são suportadas por "
                            + "@HasPermission nesta versão.", chain);
        }

        if (subject.administrator() == null) {
            // Antes: NullPointerException ao desempacotar getIsAdministrator(), capturada e
            // transformada em negação. A coluna aceita nulo, então o caso é alcançável.
            chain.add(GateOutcome.denied(Gate.IDENTITY, AccessReasonCodes.PRINCIPAL_INCOMPLETE,
                    "isAdministrator está nulo para " + subject.label()));
            return AccessDecision.denied(Gate.IDENTITY, AccessReasonCodes.PRINCIPAL_INCOMPLETE,
                    "O usuário " + subject.label() + " está sem a flag isAdministrator. "
                            + "Preencha-a com true ou false — nulo não é decidível.", chain);
        }

        chain.add(GateOutcome.passed(Gate.IDENTITY, "Usuário " + subject.label() + " resolvido"));

        // ---------------------------------------------------------------- 5. GRANT (administrador)

        if (subject.isAdministrator() && subject.enabled()) {
            chain.add(GateOutcome.passed(Gate.GRANT, AccessReasonCodes.GRANTED_ADMINISTRATOR,
                    "isAdministrator encerra a decisão sem consultar o catálogo"));
            return AccessDecision.granted(AccessReasonCodes.GRANTED_ADMINISTRATOR,
                    "Acesso concedido pela flag de administrador.", null, null, chain);
        }

        // ---------------------------------------------------------------- 5. GRANT (catálogo)

        List<PermissionEntity> permissoes = permissionRepository
                .findBySecurityIdsAndActionNameAndResourceName(
                        subject.securityIds(), requirement.action(), requirement.resource());

        if (permissoes == null || permissoes.isEmpty()) {
            chain.add(GateOutcome.denied(Gate.GRANT, AccessReasonCodes.NO_GRANT,
                    "Nenhuma permissão para " + requirement.capability()
                            + " entre as origens " + subject.securityIds()));
            return AccessDecision.denied(Gate.GRANT, AccessReasonCodes.NO_GRANT,
                    "Nenhuma permissão concedida para " + requirement.capability()
                            + " — nem direta, nem por grupo, nem por perfil.", chain);
        }

        // ---------------------------------------------------------------- 2. SCOPE

        PermissionEntity concedente = null;
        for (PermissionEntity permissao : permissoes) {
            if (permissao.allowAllTenantsAndCompaniesAndProjects() || alcancaEscopo(permissao, requirement)) {
                concedente = permissao;
                break;
            }
        }

        if (concedente == null) {
            chain.add(GateOutcome.denied(Gate.SCOPE, AccessReasonCodes.OUT_OF_SCOPE,
                    "Há " + permissoes.size() + " permissão(ões) para " + requirement.capability()
                            + ", nenhuma alcança tenant=" + requirement.tenantId()
                            + " company=" + requirement.companyId()
                            + " project=" + requirement.projectId()));
            return AccessDecision.denied(Gate.SCOPE, AccessReasonCodes.OUT_OF_SCOPE,
                    "A permissão existe, mas está restrita a outro escopo de tenant, empresa ou projeto.",
                    chain);
        }

        chain.add(GateOutcome.passed(Gate.SCOPE, "Escopo compatível"));

        String concedidoPor = concedente.getSecurity() == null ? null : concedente.getSecurity().getId();
        String concedidoPorNome = concedente.getSecurity() == null ? null : concedente.getSecurity().getName();

        chain.add(GateOutcome.passed(Gate.GRANT, AccessReasonCodes.GRANTED,
                "Concedido por " + (concedidoPorNome == null ? concedidoPor : concedidoPorNome)));

        if (log.isDebugEnabled()) {
            log.debug("Acesso a {} para {}: {}", requirement.capability(), subject.label(),
                    "PERMITE via " + concedidoPorNome);
        }

        return AccessDecision.granted(AccessReasonCodes.GRANTED,
                "Acesso concedido para " + requirement.capability() + ".",
                concedidoPor, concedidoPorNome, chain);
    }

    /**
     * Compatibilidade de escopo, exatamente como era avaliada antes: um lado nulo significa "não
     * restringe", e não "não bate".
     */
    private boolean alcancaEscopo(PermissionEntity permissao, AccessRequirement requirement) {
        return alcanca(requirement.tenantId(), permissao.getTenantId())
                && alcanca(requirement.companyId(), permissao.getCompanyId())
                && alcanca(requirement.projectId(), permissao.getProjectId());
    }

    private boolean alcanca(String pedido, String daPermissao) {
        return pedido == null || daPermissao == null || pedido.equals(daPermissao);
    }
}
