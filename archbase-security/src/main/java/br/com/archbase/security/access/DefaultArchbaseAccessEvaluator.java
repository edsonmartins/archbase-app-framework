package br.com.archbase.security.access;

import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final Map<RestrictionKind, RestrictionEvaluator> restrictionEvaluators;

    /**
     * A política de nível. Opcional: {@code null} deixa o portão {@link Gate#LEVEL} fora da
     * avaliação, que é o que se quer nos testes de unidade que não exercitam nível.
     */
    private final ArchbaseAccessLevelPolicy levelPolicy;

    @Autowired
    public DefaultArchbaseAccessEvaluator(PermissionJpaRepository permissionRepository,
                                          List<RestrictionEvaluator> restrictionEvaluators,
                                          ArchbaseAccessLevelPolicy levelPolicy) {
        this.permissionRepository = permissionRepository;
        this.levelPolicy = levelPolicy;
        this.restrictionEvaluators = restrictionEvaluators == null
                ? Map.of()
                : restrictionEvaluators.stream()
                        .collect(Collectors.toMap(RestrictionEvaluator::kind, e -> e, (a, b) -> a,
                                () -> new EnumMap<>(RestrictionKind.class)));
    }

    /** Sem política de nível — o portão LEVEL não é avaliado. */
    public DefaultArchbaseAccessEvaluator(PermissionJpaRepository permissionRepository,
                                          List<RestrictionEvaluator> restrictionEvaluators) {
        this(permissionRepository, restrictionEvaluators, null);
    }

    /** Sem trancas — usado onde só há capacidade a avaliar. */
    public DefaultArchbaseAccessEvaluator(PermissionJpaRepository permissionRepository) {
        this(permissionRepository, List.of(), null);
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
            // BO_ADMINISTRADOR é nulável, então o caso acontece em dados reais.
            //
            // Nulo é tratado como "não é administrador" — nunca como administrador, então não há
            // como ganhar privilégio por um campo em branco. Uma versão anterior deste core NEGAVA
            // aqui, para reproduzir a NullPointerException que o código antigo lançava; era errado
            // por dois motivos. Primeiro, o antigo @RequireRole já usava Boolean.TRUE.equals e
            // NUNCA falhava, então negar aqui tirava acesso de quem funcionava. Segundo, a negação
            // reproduzia um ACIDENTE, não uma decisão: quem tem a permissão concedida e a flag em
            // branco recebia 403 sem motivo defensável.
            //
            // O que resta é o registro do defeito de dado, para que ele seja corrigido.
            log.warn("Usuário {} está com isAdministrator nulo em BO_ADMINISTRADOR. Tratado como "
                    + "não-administrador. Preencha a coluna para eliminar a ambiguidade.", subject.label());
        }

        chain.add(GateOutcome.passed(Gate.IDENTITY, "Usuário " + subject.label() + " resolvido"));

        // ---------------------------------------------------------------- 3. RESTRICTION

        for (Restriction restriction : requirement.restrictions()) {
            RestrictionEvaluator avaliador = restrictionEvaluators.get(restriction.kind());

            if (avaliador == null) {
                // Tranca declarada sem quem a avalie é falha de CONFIGURAÇÃO, não de permissão — e
                // a mensagem precisa dizer isso. Antes ela falava em "requisito vazio", que manda
                // quem investiga procurar o defeito na anotação; a causa quase sempre é o contexto
                // não ter os beans do módulo, tipicamente por usar apenas o archbase-starter-security
                // em vez do archbase-starter, ou por archbase.web.mvc.enabled=false.
                log.error("Nenhum RestrictionEvaluator registrado para {}. Os beans de "
                                + "br.com.archbase.security não estão no contexto: verifique se a "
                                + "aplicação usa archbase-starter (e não apenas "
                                + "archbase-starter-security) e se archbase.web.mvc.enabled não está "
                                + "false. Enquanto isso, TODA anotação de restrição nega.",
                        restriction.kind());
                chain.add(GateOutcome.denied(Gate.RESTRICTION, AccessReasonCodes.RESTRICTION_EVALUATOR_MISSING,
                        "Nenhum RestrictionEvaluator registrado para " + restriction.kind()));
                return AccessDecision.denied(Gate.RESTRICTION, AccessReasonCodes.RESTRICTION_EVALUATOR_MISSING,
                        "A restrição " + restriction.kind() + " não pôde ser avaliada: o avaliador "
                                + "correspondente não está registrado no contexto. É configuração da "
                                + "aplicação, não falta de permissão.", chain);
            }

            RestrictionEvaluator.RestrictionResult resultado =
                    avaliador.evaluate(subject, restriction, requirement);

            if (!resultado.passed()) {
                chain.add(GateOutcome.denied(Gate.RESTRICTION, resultado.reasonCode(),
                        restriction.kind() + ": " + resultado.detail()));
                String mensagem = restriction.message() == null || restriction.message().isBlank()
                        ? "Acesso negado pela restrição " + restriction.kind() + "."
                        : restriction.message();
                return AccessDecision.denied(Gate.RESTRICTION, resultado.reasonCode(), mensagem, chain);
            }

            chain.add(GateOutcome.passed(Gate.RESTRICTION, resultado.reasonCode(),
                    restriction.kind() + ": " + resultado.detail()));
        }

        // ---------------------------------------------------------------- 5. GRANT (administrador)

        // Depois das trancas, e não antes: @RequireProfile(allowSystemAdmin = false) nega o
        // administrador que não tem o perfil, e sempre negou. Um atalho no topo transformaria a
        // isenção declarada por anotação em desvio global.
        if (subject.isAdministrator() && subject.enabled()) {

            // A negação explícita alcança o administrador. Antes não alcançava: o atalho encerrava
            // a decisão sem olhar o catálogo, então o admin aceitava criar um DENY sobre um
            // administrador, gravava a linha, e ela não fazia efeito nenhum — a mesma promessa
            // quebrada na interface que o campo `active` já tinha.
            //
            // Consulta apenas as negações, e só quando há capacidade a avaliar: na esmagadora
            // maioria das decisões ela devolve vazio, então o atalho continua barato.
            if (requirement.hasCapability()) {
                PermissionEntity negacao = negacaoQueAlcanca(subject, requirement);
                if (negacao != null) {
                    return negado(chain, negacao, requirement);
                }
            }

            chain.add(GateOutcome.passed(Gate.GRANT, AccessReasonCodes.GRANTED_ADMINISTRATOR,
                    "isAdministrator concede sem consultar concessões"));
            return AccessDecision.granted(AccessReasonCodes.GRANTED_ADMINISTRATOR,
                    "Acesso concedido pela flag de administrador.", null, null, chain);
        }

        // ---------------------------------------------------------------- 5. GRANT (catálogo)

        if (!requirement.hasCapability()) {
            if (requirement.restrictions().isEmpty()) {
                // Nem capacidade nem tranca: nada a avaliar. É erro do adaptador, e liberar por
                // ausência de critério seria o pior desfecho possível.
                chain.add(GateOutcome.denied(Gate.GRANT, AccessReasonCodes.EMPTY_REQUIREMENT,
                        "Requisito sem capacidade e sem restrição"));
                return AccessDecision.denied(Gate.GRANT, AccessReasonCodes.EMPTY_REQUIREMENT,
                        "O requisito de acesso não declara capacidade nem restrição — nada a avaliar.",
                        chain);
            }

            // Tranca pura: passar nas restrições concede, porque não há catálogo a consultar. É a
            // razão pela qual uma anotação de restrição sozinha não substitui @HasPermission.
            chain.add(GateOutcome.passed(Gate.GRANT, AccessReasonCodes.GRANTED_RESTRICTIONS_ONLY,
                    "Sem capacidade declarada — as restrições respondem sozinhas"));
            return AccessDecision.granted(AccessReasonCodes.GRANTED_RESTRICTIONS_ONLY,
                    "Acesso concedido por satisfazer as restrições declaradas.", null, null, chain);
        }

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

        List<PermissionEntity> noEscopo = new ArrayList<>();
        for (PermissionEntity permissao : permissoes) {
            // Sem atalho de "não estreita": alcancaEscopo já trata cada campo nulo como "não
            // restringe". O atalho que existia aqui consultava um predicado que ignora o tenant —
            // e passava por cima da comparação de tenant, aceitando uma permissão restrita a outro.
            if (alcancaEscopo(permissao, requirement)) {
                noEscopo.add(permissao);
            }
        }

        if (noEscopo.isEmpty()) {
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

        // ---------------------------------------------------------------- negação explícita
        //
        // DENY vence, em qualquer nível — perfil, grupo ou direto — e só dentro do escopo em que
        // foi declarado, que é a mesma semântica que a concessão sempre teve. É o que permite tirar
        // uma pessoa de algo que o time inteiro tem, sem criar um grupo paralelo só para excluí-la.
        for (PermissionEntity permissao : noEscopo) {
            if (permissao.isDeny()) {
                return negado(chain, permissao, requirement);
            }
        }

        // Ordem estável. O catálogo não impede duas ações de mesmo nome sob o mesmo recurso, e sem
        // critério explícito a permissão escolhida — e portanto o concedente exibido no
        // diagnóstico — variaria entre execuções idênticas.
        PermissionEntity concedente = noEscopo.stream()
                .min(Comparator.comparing(PermissionEntity::getId, Comparator.nullsLast(String::compareTo)))
                .orElse(noEscopo.get(0));

        // ---------------------------------------------------------------- 4. LEVEL
        //
        // Avaliado aqui, e não antes do catálogo, por dois motivos. O mínimo é propriedade da ação,
        // que só se conhece depois de consultá-la — e, para quem investiga, "ninguém te concedeu" é
        // resposta mais útil do que "seu nível é baixo" quando as duas coisas são verdade.
        if (levelPolicy != null && levelPolicy.isEnabled()) {
            AccessLevel minimo = maiorMinimoEntre(noEscopo);
            AccessLevel doSujeito = levelPolicy.levelOf(subject);

            if (!doSujeito.reaches(minimo)) {
                chain.add(GateOutcome.denied(Gate.LEVEL, AccessReasonCodes.LEVEL_TOO_LOW,
                        "Nível " + doSujeito + "; " + requirement.capability() + " exige " + minimo));
                return AccessDecision.denied(Gate.LEVEL, AccessReasonCodes.LEVEL_TOO_LOW,
                        "A permissão está concedida, mas " + requirement.capability() + " exige nível "
                                + minimo + " e o usuário alcança " + doSujeito
                                + ". Ter a capacidade atribuída não basta.", chain);
            }

            chain.add(GateOutcome.passed(Gate.LEVEL,
                    "Nível " + doSujeito + (minimo == null ? " (capacidade sem mínimo)" : " ≥ " + minimo)));
        }

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

    /**
     * A negação, entre as que alcançam o escopo pedido — ou {@code null} se não houver.
     *
     * <p>Usada no caminho do administrador, onde só as negações são consultadas.
     */
    private PermissionEntity negacaoQueAlcanca(AccessSubject subject, AccessRequirement requirement) {
        List<PermissionEntity> negacoes = permissionRepository
                .findDenialsBySecurityIdsAndActionNameAndResourceName(
                        subject.securityIds(), requirement.action(), requirement.resource());

        if (negacoes == null || negacoes.isEmpty()) {
            return null;
        }
        return negacoes.stream()
                .filter(p -> alcancaEscopo(p, requirement))
                .findFirst()
                .orElse(null);
    }

    private AccessDecision negado(List<GateOutcome> chain, PermissionEntity negacao,
                                  AccessRequirement requirement) {
        String quem = nomeDe(negacao);
        chain.add(GateOutcome.denied(Gate.GRANT, AccessReasonCodes.EXPLICIT_DENY,
                "Negação explícita registrada em " + quem));
        return AccessDecision.denied(Gate.GRANT, AccessReasonCodes.EXPLICIT_DENY,
                "Acesso negado explicitamente para " + requirement.capability()
                        + " em " + quem + ". A negação vence qualquer concessão.", chain);
    }

    /**
     * O <b>maior</b> mínimo entre as permissões que alcançam o escopo.
     *
     * <p>Nada no catálogo impede duas ações de mesmo nome sob o mesmo recurso, com pisos
     * diferentes. Escolher uma delas arbitrariamente tornaria o piso aplicado não-determinístico —
     * a mesma requisição negaria ou permitiria conforme a ordem que o banco devolvesse. Diante de
     * pisos em conflito, vale o mais alto: um catálogo inconsistente não pode <i>afrouxar</i> a
     * exigência.
     */
    private AccessLevel maiorMinimoEntre(List<PermissionEntity> permissoes) {
        AccessLevel maior = null;
        for (PermissionEntity permissao : permissoes) {
            AccessLevel doItem = permissao.getAction() == null ? null : permissao.getAction().getMinimumLevel();
            if (AccessLevel.isUnset(doItem)) {
                continue;
            }
            if (maior == null || doItem.ordinal() > maior.ordinal()) {
                maior = doItem;
            }
        }
        return maior;
    }

    private String nomeDe(PermissionEntity permissao) {
        if (permissao.getSecurity() == null) {
            return "origem desconhecida";
        }
        String nome = permissao.getSecurity().getName();
        return nome == null ? permissao.getSecurity().getId() : nome;
    }
}
