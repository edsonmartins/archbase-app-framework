package br.com.archbase.security.diagnostics;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.access.AccessSubject;
import br.com.archbase.security.access.ArchbaseAccessEvaluator;
import br.com.archbase.security.access.ArchbaseAccessSubjectLoader;
import br.com.archbase.security.access.ArchbaseCapabilityReader;
import br.com.archbase.security.access.EffectiveCapability;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.GroupJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ProfileJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Responde as perguntas que a tela de segurança não conseguia responder.
 *
 * <p>Todas as três — "o que esta pessoa pode?", "ela conseguiria fazer isto?", "como está o
 * conjunto?" — passam pelo <b>mesmo</b> {@link ArchbaseAccessEvaluator} que decide em produção. É
 * a razão de o core existir: se o diagnóstico tivesse motor próprio, ele divergiria da realidade
 * com o tempo, e um diagnóstico que mente é pior do que nenhum.
 */
@Service
public class ArchbaseAccessDiagnosticsService {

    private final ArchbaseAccessEvaluator evaluator;
    private final ArchbaseAccessSubjectLoader subjectLoader;
    private final ArchbaseCapabilityReader capabilityReader;
    private final PermissionJpaRepository permissionRepository;
    private final ActionJpaRepository actionRepository;
    private final ResourceJpaRepository resourceRepository;
    private final UserJpaRepository userRepository;
    private final GroupJpaRepository groupRepository;
    private final ProfileJpaRepository profileRepository;

    @Autowired(required = false)
    private List<ArchbaseRoleResolver> roleResolvers = List.of();

    @Value("${archbase.security.permission.require-active:false}")
    private boolean requireActive;

    @Value("${archbase.security.scan-packages:}")
    private String scanPackages;

    @Value("${archbase.security.admin-endpoints.policy:permit}")
    private String adminEndpointsPolicy;

    @Value("${archbase.security.require-role.no-resolver-policy:permit}")
    private String requireRoleNoResolverPolicy;

    public ArchbaseAccessDiagnosticsService(ArchbaseAccessEvaluator evaluator,
                                            ArchbaseAccessSubjectLoader subjectLoader,
                                            ArchbaseCapabilityReader capabilityReader,
                                            PermissionJpaRepository permissionRepository,
                                            ActionJpaRepository actionRepository,
                                            ResourceJpaRepository resourceRepository,
                                            UserJpaRepository userRepository,
                                            GroupJpaRepository groupRepository,
                                            ProfileJpaRepository profileRepository) {
        this.evaluator = evaluator;
        this.subjectLoader = subjectLoader;
        this.capabilityReader = capabilityReader;
        this.permissionRepository = permissionRepository;
        this.actionRepository = actionRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.profileRepository = profileRepository;
    }

    // ------------------------------------------------------------------ simulação

    /**
     * "Esta pessoa conseguiria fazer isto?" — avaliada, não executada.
     *
     * <p>Chama o avaliador de produção com o sujeito pedido. A resposta traz o portão em que parou
     * e o motivo, que é o que hoje obriga a abrir grupo por grupo.
     */
    @Transactional(readOnly = true)
    public Optional<AccessDecision> simulate(String userId, String email, AccessRequirement requirement) {
        return subjectLoader.resolve(userId, email)
                .map(subject -> evaluator.decide(subject, requirement));
    }

    // ------------------------------------------------------------------ efetivo

    /** "O que esta pessoa pode?" — a lista achatada, com origem e situação por linha. */
    @Transactional(readOnly = true)
    public Optional<EffectiveAccessReport> effective(String userId, String email) {
        return subjectLoader.resolve(userId, email).map(this::effectiveOf);
    }

    private EffectiveAccessReport effectiveOf(AccessSubject subject) {
        List<EffectiveCapability> capacidades = capabilityReader.grantedTo(subject);
        int inertes = (int) capacidades.stream()
                .filter(c -> c.situation() == EffectiveCapability.Situation.INERT).count();
        int negadas = (int) capacidades.stream()
                .filter(c -> c.situation() == EffectiveCapability.Situation.DENIED).count();

        // Nomes, não identificadores: este relatório existe para dispensar a ida ao admin.
        List<String> grupos = subject.groupNames().stream().sorted().toList();

        return new EffectiveAccessReport(
                subject.userId(),
                subject.label(),
                subject.profileName(),
                grupos,
                subject.isAdministrator(),
                subject.enabled(),
                capacidades.size(),
                capacidades.size() - inertes - negadas,
                inertes,
                negadas,
                capacidades);
    }


    // ------------------------------------------------------------------ panorama

    /** "Como está o conjunto?" — os números do tenant corrente. */
    @Transactional(readOnly = true)
    public AccessOverview overview() {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        for (Object[] linha : permissionRepository.countGroupedBySecurityType()) {
            Object tipo = linha[0];
            String nome = tipo instanceof Class<?> classe
                    ? classe.getSimpleName().replace("Entity", "")
                    : String.valueOf(tipo);
            porTipo.merge(nome, ((Number) linha[1]).longValue(), Long::sum);
        }

        long usuarios = userRepository.count();
        long administradores = userRepository.countAdministrators();

        return new AccessOverview(
                usuarios,
                administradores,
                groupRepository.count(),
                profileRepository.count(),
                resourceRepository.count(),
                resourceRepository.countByType(TipoRecurso.API),
                resourceRepository.countByTypeAndActive(TipoRecurso.API, false),
                resourceRepository.countWithoutAnyAction(),
                actionRepository.count(),
                actionRepository.countByActive(false),
                actionRepository.countWithoutAnyPermission(),
                permissionRepository.countAll(),
                permissionRepository.countPointingToInactive(),
                porTipo,
                new AccessOverview.Flags(
                        requireActive,
                        scanPackages != null && !scanPackages.isBlank(),
                        adminEndpointsPolicy,
                        requireRoleNoResolverPolicy,
                        roleResolvers != null && !roleResolvers.isEmpty()));
    }

    /**
     * Os itens por trás de um número do panorama.
     *
     * <p>Um número sozinho diz que há um problema; não diz qual. "29 ações inativas" não permite
     * agir — é preciso saber <b>quais</b>, para decidir se reativa, apaga ou ignora. Este método é o
     * que transforma o painel de leitura em ponto de partida de trabalho.
     *
     * <p>Cada ramo espelha a consulta de contagem correspondente. Se divergirem, o detalhe passa a
     * contradizer o card — por isso as consultas ficam lado a lado nos repositórios, com referência
     * cruzada no javadoc.
     *
     * @param metric  qual número detalhar
     * @param pageable página e tamanho; o catálogo de um tenant grande não cabe numa resposta só
     */
    @Transactional(readOnly = true)
    public Page<OverviewItem> listOverviewItems(OverviewMetric metric, Pageable pageable) {
        return switch (metric) {
            case PERMISSIONS_POINTING_TO_INACTIVE ->
                    permissionRepository.findPointingToInactive(pageable).map(this::toItem);
            case ADMINISTRATORS ->
                    userRepository.findAdministrators(pageable).map(this::toItem);
            case ACTIONS_INACTIVE ->
                    actionRepository.findByActive(false, pageable).map(a -> toItem(a, "Ação desativada"));
            case ACTIONS_WITHOUT_PERMISSION ->
                    actionRepository.findWithoutAnyPermission(pageable)
                            .map(a -> toItem(a, "Nenhuma concessão aponta para esta ação"));
            case API_RESOURCES_INACTIVE ->
                    resourceRepository.findByTypeAndActive(TipoRecurso.API, false, pageable)
                            .map(r -> toItem(r, "Recurso de API desativado"));
            case RESOURCES_WITHOUT_ACTION ->
                    resourceRepository.findWithoutAnyAction(pageable)
                            .map(r -> toItem(r, "Recurso sem nenhuma ação cadastrada"));
        };
    }

    private OverviewItem toItem(PermissionEntity p) {
        var acao = p.getAction();
        var recurso = acao != null ? acao.getResource() : null;
        // Distingue as duas origens: quem lê a lista precisa saber se reativa a ação ou o recurso.
        String motivo;
        if (acao != null && Boolean.FALSE.equals(acao.getActive()) && recurso != null && Boolean.FALSE.equals(recurso.getActive())) {
            motivo = "Ação e recurso inativos";
        } else if (acao != null && Boolean.FALSE.equals(acao.getActive())) {
            motivo = "Ação inativa";
        } else {
            motivo = "Recurso inativo";
        }
        String destinatario = p.getSecurity() != null ? p.getSecurity().getName() : "(sem destinatário)";
        String capacidade = (recurso != null ? recurso.getName() + " · " : "")
                + (acao != null ? acao.getName() : "(sem ação)");
        return new OverviewItem(p.getId(), capacidade, "Concedida a " + destinatario, motivo);
    }

    private OverviewItem toItem(UserEntity u) {
        return new OverviewItem(u.getId(), u.getName(), u.getEmail(),
                "Administrador — passa direto pelo portão GRANT");
    }

    private OverviewItem toItem(ActionEntity a, String motivo) {
        var recurso = a.getResource();
        return new OverviewItem(a.getId(), a.getName(),
                recurso != null ? recurso.getName() : "(sem recurso)", motivo);
    }

    // ------------------------------------------------------- grupo, perfil, reversa

    /**
     * Quem está no grupo e o que cada pessoa acaba tendo.
     *
     * <p><b>Os dois juntos porque separados enganam.</b> Ver o que o grupo concede não diz o que
     * seus membros podem: cada um acumula o perfil, os outros grupos e as concessões diretas. Um
     * grupo com três capacidades pode ter um membro que faz tudo, por outra via — e é o total que
     * decide o acesso.
     *
     * <p>O total de cada membro sai do <b>mesmo</b> leitor de capacidades que a tela de efetivo
     * usa. Contar aqui por conta própria produziria dois números para a mesma pergunta, que é como
     * um diagnóstico começa a mentir.
     */
    @Transactional(readOnly = true)
    public Optional<GroupReport> group(String groupId) {
        return groupRepository.findById(groupId).map(g -> {
            List<GroupReport.EffectiveCapabilityLine> concede =
                    permissionRepository.findAllBySecurityIds(Set.of(groupId)).stream()
                            .map(p -> new GroupReport.EffectiveCapabilityLine(
                                    p.getAction() != null && p.getAction().getResource() != null
                                            ? p.getAction().getResource().getName() : "(sem recurso)",
                                    p.getAction() != null ? p.getAction().getName() : "(sem ação)",
                                    situacaoDaConcessao(p)))
                            .sorted(Comparator.comparing(GroupReport.EffectiveCapabilityLine::resource)
                                    .thenComparing(GroupReport.EffectiveCapabilityLine::action))
                            .toList();

            List<GroupReport.Member> membros = userRepository.findMembersOfGroup(groupId).stream()
                    .map(this::resumoDe).toList();

            return new GroupReport(g.getId(), g.getName(), g.getDescription(), concede, membros);
        });
    }

    /**
     * Quem tem o perfil e o que cada pessoa acaba tendo.
     *
     * <p>Mesma forma do grupo, de propósito: para quem lê a tela, "o que esta via concede e quem
     * está nela" é a mesma pergunta — muda só a via. Formas diferentes obrigariam a aprender duas
     * telas para uma ideia só.
     *
     * <p>Perfil é a via mais ampla: vale para todo mundo que o tem, e ninguém tem dois. Por isso um
     * perfil mal concedido alcança mais gente de uma vez do que um grupo.
     */
    @Transactional(readOnly = true)
    public Optional<GroupReport> profile(String profileId) {
        return profileRepository.findById(profileId).map(p -> {
            List<GroupReport.EffectiveCapabilityLine> concede =
                    permissionRepository.findAllBySecurityIds(Set.of(profileId)).stream()
                            .map(perm -> new GroupReport.EffectiveCapabilityLine(
                                    perm.getAction() != null && perm.getAction().getResource() != null
                                            ? perm.getAction().getResource().getName() : "(sem recurso)",
                                    perm.getAction() != null ? perm.getAction().getName() : "(sem ação)",
                                    situacaoDaConcessao(perm)))
                            .sorted(Comparator.comparing(GroupReport.EffectiveCapabilityLine::resource)
                                    .thenComparing(GroupReport.EffectiveCapabilityLine::action))
                            .toList();

            List<GroupReport.Member> membros = userRepository.findMembersOfProfile(profileId).stream()
                    .map(this::resumoDe).toList();

            return new GroupReport(p.getId(), p.getName(), p.getDescription(), concede, membros);
        });
    }

    /**
     * Quem alcança uma capacidade — a consulta <b>reversa</b>.
     *
     * <p>"Quem consegue aprovar custo?" hoje só se responde abrindo grupo por grupo e cruzando na
     * cabeça, e o resultado depende de quem cruzou. Aqui a resposta é a mesma sempre.
     *
     * <p><b>Administrador entra na lista</b>, com a via marcada como tal. Ele alcança sem concessão
     * nenhuma; omiti-lo daria uma resposta errada justamente para as contas que mais importam numa
     * auditoria.
     */
    @Transactional(readOnly = true)
    public List<ReachEntry> whoCanReach(String actionId) {
        Map<String, ReachEntry> porPessoa = new LinkedHashMap<>();

        for (PermissionEntity permissao : permissionRepository.findGrantsOfAction(actionId)) {
            var destinatario = permissao.getSecurity();
            if (destinatario == null) {
                continue;
            }
            String situacao = situacaoDaConcessao(permissao);

            if (destinatario instanceof UserEntity u) {
                registrar(porPessoa, u, "concessão direta", "USUARIO", situacao);
            } else if (destinatario instanceof br.com.archbase.security.persistence.GroupEntity g) {
                for (UserEntity u : userRepository.findMembersOfGroup(g.getId())) {
                    registrar(porPessoa, u, "grupo " + g.getName(), "GRUPO", situacao);
                }
            } else if (destinatario instanceof br.com.archbase.security.persistence.ProfileEntity pf) {
                for (UserEntity u : userRepository.findMembersOfProfile(pf.getId())) {
                    registrar(porPessoa, u, "perfil " + pf.getName(), "PERFIL", situacao);
                }
            }
        }

        for (UserEntity admin : userRepository.findAllAdministrators()) {
            registrar(porPessoa, admin, "administrador", ReachEntry.KIND_ADMIN, "EFFECTIVE");
        }

        return List.copyOf(porPessoa.values());
    }

    /**
     * Guarda a via de cada pessoa, sem duplicá-la.
     *
     * <p>Quem alcança por duas vias aparece uma vez só — e fica com a que <b>vale</b>. Listar a
     * mesma pessoa duas vezes inflaria a contagem de "quem pode", e mostrar a via inerte quando
     * existe uma efetiva diria que o acesso não funciona quando funciona.
     */
    private void registrar(Map<String, ReachEntry> acc, UserEntity u, String via, String kind, String situacao) {
        ReachEntry existente = acc.get(u.getId());
        if (existente != null && !"EFFECTIVE".equals(situacao)) {
            return;
        }
        if (existente != null && "EFFECTIVE".equals(existente.situation())) {
            return;
        }
        acc.put(u.getId(), new ReachEntry(u.getId(), u.getName(), u.getEmail(), via, kind, situacao));
    }

    private String situacaoDaConcessao(PermissionEntity p) {
        var acao = p.getAction();
        var recurso = acao != null ? acao.getResource() : null;
        boolean viva = acao != null && !Boolean.FALSE.equals(acao.getActive())
                && recurso != null && !Boolean.FALSE.equals(recurso.getActive());
        return viva ? "EFFECTIVE" : "INERT";
    }

    private GroupReport.Member resumoDe(UserEntity u) {
        AccessSubject sujeito = AccessSubject.of(u);
        List<EffectiveCapability> caps = capabilityReader.grantedTo(sujeito);
        int inertes = (int) caps.stream().filter(c -> c.situation() == EffectiveCapability.Situation.INERT).count();
        int negadas = (int) caps.stream().filter(c -> c.situation() == EffectiveCapability.Situation.DENIED).count();
        return new GroupReport.Member(
                u.getId(), u.getName(), u.getEmail(), sujeito.profileName(),
                sujeito.isAdministrator(), sujeito.enabled(),
                caps.size(), caps.size() - inertes - negadas, inertes, negadas);
    }

    // ------------------------------------------------------------------ árvore

    /**
     * Um ramo da árvore de objetos, paginado e filtrado.
     *
     * <p><b>Por que não devolve a árvore inteira.</b> Um tenant real tem 621 ações, 104 recursos e
     * 61 pessoas. Montar tudo numa resposta obriga o servidor a materializar o catálogo para exibir
     * cinco linhas, e trava o navegador ao renderizar. Cada ramo busca ao abrir.
     *
     * <p>O filtro é aplicado <b>no banco</b>, e não no cliente: filtrar em memória exigiria ter
     * carregado tudo antes — o problema que a paginação existe para evitar.
     *
     * <p>As contagens que aparecem à direita do nó (membros do grupo, ações do recurso) são
     * buscadas <b>em lote</b> para a página inteira. Uma consulta por nó transformaria uma página
     * de vinte itens em vinte e uma idas ao banco.
     *
     * @param parentId obrigatório para {@link TreeBranch#ACTIONS_OF_RESOURCE}; ignorado nos demais
     * @param filtro   texto de busca; {@code null} ou vazio devolve tudo
     */
    @Transactional(readOnly = true)
    public Page<TreeNode> browse(TreeBranch branch, String parentId, String filtro, Pageable pageable) {
        String f = (filtro == null || filtro.isBlank()) ? null : filtro.trim();
        return switch (branch) {
            case USERS -> userRepository.findForTree(f, pageable).map(this::toNode);
            case PROFILES -> profileRepository.findForTree(f, pageable)
                    .map(p -> new TreeNode(p.getId(), TreeNode.TreeNodeKind.PROFILE, p.getName(), null, false, null));
            case GROUPS -> comMembros(groupRepository.findForTree(f, pageable));
            case RESOURCES -> comAcoes(resourceRepository.findForTree(f, pageable));
            case ACTIONS_OF_RESOURCE -> {
                if (parentId == null || parentId.isBlank()) {
                    throw new IllegalArgumentException("ACTIONS_OF_RESOURCE exige o recurso pai");
                }
                yield comConcessoes(actionRepository.findForTree(parentId, f, pageable));
            }
        };
    }

    private TreeNode toNode(UserEntity u) {
        // Administrador é marcado na árvore: passa direto pelo portão final, e quem audita precisa
        // enxergar isso sem abrir a pessoa.
        boolean admin = Boolean.TRUE.equals(u.getIsAdministrator());
        return new TreeNode(u.getId(), TreeNode.TreeNodeKind.USER, u.getName(), null, false,
                admin ? "warning" : null);
    }

    private Page<TreeNode> comMembros(Page<br.com.archbase.security.persistence.GroupEntity> pagina) {
        Map<String, Long> membros = emLote(
                groupRepository.countMembersOf(pagina.getContent().stream().map(g -> g.getId()).toList()),
                pagina.isEmpty());
        return pagina.map(g -> new TreeNode(g.getId(), TreeNode.TreeNodeKind.GROUP, g.getName(),
                rotulo(membros.get(g.getId())), true, null));
    }

    private Page<TreeNode> comAcoes(Page<ResourceEntity> pagina) {
        List<Object[]> linhas = pagina.isEmpty() ? List.of()
                : resourceRepository.countActionsOf(pagina.getContent().stream().map(ResourceEntity::getId).toList());
        Map<String, long[]> porRecurso = new LinkedHashMap<>();
        for (Object[] l : linhas) {
            porRecurso.put((String) l[0], new long[]{((Number) l[1]).longValue(), ((Number) l[2]).longValue()});
        }
        return pagina.map(r -> {
            long[] c = porRecurso.getOrDefault(r.getId(), new long[]{0, 0});
            // Recurso com ação desativada é onde mora a concessão que não vale: marcar aqui poupa
            // abrir ramo por ramo procurando.
            String severidade = c[1] > 0 ? "critical" : (Boolean.FALSE.equals(r.getActive()) ? "critical" : null);
            return new TreeNode(r.getId(), TreeNode.TreeNodeKind.RESOURCE, r.getName(),
                    rotulo(c[0]), c[0] > 0, severidade);
        });
    }

    private Page<TreeNode> comConcessoes(Page<ActionEntity> pagina) {
        Map<String, Long> concessoes = emLote(
                pagina.isEmpty() ? List.of()
                        : actionRepository.countPermissionsOf(pagina.getContent().stream().map(ActionEntity::getId).toList()),
                pagina.isEmpty());
        return pagina.map(a -> new TreeNode(a.getId(), TreeNode.TreeNodeKind.ACTION, a.getName(),
                rotulo(concessoes.get(a.getId())), false,
                Boolean.FALSE.equals(a.getActive()) ? "critical" : null));
    }

    private Map<String, Long> emLote(List<Object[]> linhas, boolean vazia) {
        Map<String, Long> m = new LinkedHashMap<>();
        if (vazia) {
            return m;
        }
        for (Object[] l : linhas) {
            m.put((String) l[0], ((Number) l[1]).longValue());
        }
        return m;
    }

    /** {@code null} quando não há nada a mostrar: badge "0" é ruído, ausência já diz o mesmo. */
    private String rotulo(Long valor) {
        return valor == null || valor == 0 ? null : String.valueOf(valor);
    }

    private String rotulo(long valor) {
        return valor == 0 ? null : String.valueOf(valor);
    }

    private OverviewItem toItem(ResourceEntity r, String motivo) {
        return new OverviewItem(r.getId(), r.getName(),
                r.getDescription() != null ? r.getDescription() : "", motivo);
    }
}
