package br.com.archbase.security.integration;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessReasonCodes;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.access.ArchbaseAccessSubjectLoader;
import br.com.archbase.security.access.Gate;
import br.com.archbase.security.diagnostics.AccessOverview;
import br.com.archbase.security.diagnostics.ArchbaseAccessDiagnosticsService;
import br.com.archbase.security.diagnostics.EffectiveAccessReport;
import br.com.archbase.security.access.EffectiveCapability;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.SecurityEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.UserGroupEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.GroupJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ProfileJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import br.com.archbase.security.diagnostics.ReachEntry;
import br.com.archbase.security.diagnostics.TreeBranch;
import br.com.archbase.security.diagnostics.TreeNode;
import br.com.archbase.security.diagnostics.OverviewMetric;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Diagnóstico de autorização contra banco de verdade (H2).
 *
 * <p>O ponto que estes testes protegem: <b>a simulação usa o mesmo avaliador que decide em
 * produção</b>. Um diagnóstico com motor próprio diverge da realidade com o tempo, e um diagnóstico
 * que mente é pior do que nenhum. Aqui isso é exercitado de ponta a ponta — sujeito carregado por
 * id, fora de qualquer requisição autenticada, com grupos e perfil materializados por fetch join.
 *
 * <p>Cobre também o carregamento em si, que é onde mora a armadilha: montar o sujeito a partir de
 * associações lazy fora de transação é {@code LazyInitializationException} — o mesmo defeito que
 * apareceu no logout durante a auditoria.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-diagnostico-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste"
})
@DisplayName("Diagnóstico de acesso (Spring + H2)")
class DiagnosticoDeAcessoIntegrationTest {

    private static final String RECURSO = "tms.ordemservico";

    @Autowired
    ArchbaseAccessDiagnosticsService diagnostics;
    @Autowired
    ArchbaseAccessSubjectLoader subjectLoader;
    @Autowired
    PermissionJpaRepository permissionRepository;
    @Autowired
    ActionJpaRepository actionRepository;
    @Autowired
    ResourceJpaRepository resourceRepository;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    GroupJpaRepository groupRepository;
    @Autowired
    ProfileJpaRepository profileRepository;

    private ResourceEntity recurso;

    @BeforeEach
    void limpar() {
        permissionRepository.deleteAll();
        actionRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();
        profileRepository.deleteAll();

        recurso = resourceRepository.save(ResourceEntity.builder()
                .id("resource-1").name(RECURSO).description("Ordem de serviço").active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    @Test
    @DisplayName("o sujeito é carregado por id fora de transação, com grupos e perfil")
    void carregaSujeitoPorId() {
        // Sem o fetch join de findByIdWithGroupsAndProfile, tocar getGroups() aqui seria
        // LazyInitializationException — e a simulação inteira cairia.
        UserEntity user = usuario("user-1", false);
        configurar(user, grupo("GESTORES-FROTA"), perfil("SUPERVISOR"));

        var subject = subjectLoader.byId("user-1");

        assertThat(subject).isPresent();
        assertThat(subject.get().groupIds()).containsExactly("group-GESTORES-FROTA");
        assertThat(subject.get().profileName()).isEqualTo("SUPERVISOR");
        assertThat(subject.get().securityIds())
                .containsExactlyInAnyOrder("user-1", "group-GESTORES-FROTA", "profile-SUPERVISOR");
    }

    @Test
    @DisplayName("simula o acesso de outra pessoa e diz por qual grupo ela passa")
    void simulaEDizAOrigem() {
        UserEntity user = usuario("user-1", false);
        GroupEntity grupo = grupo("GESTORES-FROTA");
        vincular(user, grupo);
        concessao(grupo, "aprovar_custo", true);

        Optional<AccessDecision> decisao = diagnostics.simulate("user-1", null,
                AccessRequirement.of(RECURSO, "aprovar_custo"));

        assertThat(decisao).isPresent();
        assertThat(decisao.get().allowed()).isTrue();
        assertThat(decisao.get().grantedByName()).isEqualTo("GESTORES-FROTA");
        assertThat(decisao.get().chain()).isNotEmpty();
    }

    @Test
    @DisplayName("a simulação diz em que portão parou, não só que negou")
    void simulaEDizOndeParou() {
        usuario("user-1", false);

        Optional<AccessDecision> decisao = diagnostics.simulate("user-1", null,
                AccessRequirement.of(RECURSO, "aprovar_custo"));

        assertThat(decisao).isPresent();
        assertThat(decisao.get().allowed()).isFalse();
        assertThat(decisao.get().deniedAt()).isEqualTo(Gate.GRANT);
        assertThat(decisao.get().reasonCode()).isEqualTo(AccessReasonCodes.NO_GRANT);
    }

    @Test
    @DisplayName("simulação para usuário inexistente devolve vazio, não uma negação")
    void usuarioInexistente() {
        // Distinção que importa na tela: "não encontrei essa pessoa" não é "essa pessoa não pode".
        assertThat(diagnostics.simulate("nao-existe", null,
                AccessRequirement.of(RECURSO, "aprovar_custo"))).isEmpty();
    }

    @Test
    @DisplayName("o efetivo separa o que vale do que está inerte, com a origem de cada linha")
    void efetivoSeparaValidoDeInerte() {
        UserEntity user = usuario("user-1", false);
        GroupEntity grupo = grupo("GESTORES-FROTA");
        ProfileEntity perfil = perfil("SUPERVISOR");
        user = configurar(user, grupo, perfil);

        concessao(grupo, "aprovar_custo", true);
        concessao(perfil, "view", true);
        concessao(user, "cancelar", false);

        Optional<EffectiveAccessReport> relatorio = diagnostics.effective("user-1", null);

        assertThat(relatorio).isPresent();
        EffectiveAccessReport r = relatorio.get();
        assertThat(r.granted()).isEqualTo(3);
        assertThat(r.effective()).isEqualTo(2);
        assertThat(r.inert()).isEqualTo(1);

        assertThat(r.capabilities())
                .filteredOn(c -> c.situation() == EffectiveCapability.Situation.INERT)
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.action()).isEqualTo("cancelar");
                    assertThat(c.grantedByName()).isEqualTo("Usuário user-1");
                });

        assertThat(r.capabilities())
                .extracting(EffectiveCapability::grantedByName)
                .contains("GESTORES-FROTA", "SUPERVISOR");
    }

    @Test
    @DisplayName("para administrador o relatório marca que a lista é irrelevante")
    void administradorMarcado() {
        // A flag encerra a decisão antes do catálogo: nada configurado na tela se aplica a essa
        // conta, e o relatório precisa dizer isso em vez de exibir uma lista que não decide nada.
        usuario("admin-1", true);

        Optional<EffectiveAccessReport> relatorio = diagnostics.effective("admin-1", null);

        assertThat(relatorio).isPresent();
        assertThat(relatorio.get().administrator()).isTrue();
    }

    @Test
    @DisplayName("o efetivo também resolve por e-mail")
    void efetivoPorEmail() {
        usuario("user-1", false);

        assertThat(diagnostics.effective(null, "user-1@exemplo.test")).isPresent();
        assertThat(diagnostics.effective(null, "ninguem@exemplo.test")).isEmpty();
    }

    @Test
    @DisplayName("o panorama conta as concessões inertes e diz o estado das proteções")
    void panoramaContaInertes() {
        UserEntity user = usuario("user-1", false);
        usuario("admin-1", true);
        concessao(user, "aprovar_custo", true);
        concessao(user, "cancelar", false);

        AccessOverview overview = diagnostics.overview();

        assertThat(overview.users()).isEqualTo(2);
        assertThat(overview.administrators()).isEqualTo(1);
        assertThat(overview.permissions()).isEqualTo(2);
        assertThat(overview.permissionsPointingToInactive())
                .as("uma das duas aponta para ação inativa")
                .isEqualTo(1);
        assertThat(overview.actionsInactive()).isEqualTo(1);

        // O número só é interpretável junto do estado das proteções: concessão inerte é inofensiva
        // enquanto nada consulta o catálogo, e vira acesso indevido quando algo consultar.
        assertThat(overview.flags().requireActive()).isFalse();
        assertThat(overview.flags().adminEndpointsPolicy()).isEqualTo("permit");
        assertThat(overview.flags().roleResolverRegistered()).isFalse();
    }

    @Test
    @DisplayName("capacidade negada não é listada como efetiva — a lista concorda com a decisão")
    void negadaNaoContaComoEfetiva() {
        // O defeito que este teste fecha: o avaliador negava corretamente, e tanto a listagem que a
        // tela consome quanto o relatório de efetivo continuavam reportando a capacidade como
        // permitida. A tela mostrava o botão, o backend recusava o clique, e o diagnóstico — que
        // existe justamente para explicar o acesso — dizia o oposto da decisão.
        UserEntity user = usuario("user-1", false);
        GroupEntity grupo = grupo("GESTORES-FROTA");
        configurar(user, grupo, null);

        ActionEntity acao = actionRepository.save(ActionEntity.builder()
                .id("act-deny").name("excluir").description("Excluir")
                .resource(recurso).active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        // Concedida ao grupo, negada no usuário.
        permissionRepository.save(PermissionEntity.builder()
                .id("perm-grant").security(grupo).action(acao)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
        permissionRepository.save(PermissionEntity.builder()
                .id("perm-deny").security(user).action(acao)
                .effect(br.com.archbase.security.access.PermissionEffect.DENY)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        assertThat(diagnostics.simulate("user-1", null, AccessRequirement.of(RECURSO, "excluir")))
                .get()
                .satisfies(d -> assertThat(d.allowed()).as("a decisão nega").isFalse());

        EffectiveAccessReport relatorio = diagnostics.effective("user-1", null).orElseThrow();
        assertThat(relatorio.denied()).isEqualTo(2);
        assertThat(relatorio.effective()).isZero();
        assertThat(relatorio.capabilities())
                .allSatisfy(c -> assertThat(c.situation())
                        .isEqualTo(EffectiveCapability.Situation.DENIED));
    }

    @Test
    @DisplayName("negação COM ESCOPO não anula a concessão na listagem")
    void negacaoComEscopoNaoAnulaNaListagem() {
        // A listagem é cega a escopo — sempre foi, inclusive para concessões: ela responde "o que
        // posso neste recurso", sem tenant, empresa ou projeto na pergunta. Deixar uma negação
        // RESTRITA suprimir a linha faria a tela esconder um botão que o avaliador liberaria fora
        // daquele escopo. Seria trocar "mostra o que o backend recusa" por "esconde o que o backend
        // permite" — divergência na direção oposta, e igualmente errada.
        UserEntity user = usuario("user-1", false);
        GroupEntity grupo = grupo("GESTORES-FROTA");
        configurar(user, grupo, null);

        ActionEntity acao = actionRepository.save(ActionEntity.builder()
                .id("act-escopo").name("exportar").description("Exportar")
                .resource(recurso).active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        permissionRepository.save(PermissionEntity.builder()
                .id("perm-grant-global").security(grupo).action(acao)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
        permissionRepository.save(PermissionEntity.builder()
                .id("perm-deny-escopo").security(user).action(acao)
                .effect(br.com.archbase.security.access.PermissionEffect.DENY)
                // Escopo por empresa, e não por tenant: em PermissionEntity o campo tenantId de
                // ESCOPO tem o mesmo nome do discriminador de tenant herdado, e atribuir um valor
                // divergente ali é recusado pelo Hibernate. Modelagem confusa e pré-existente.
                .companyId("outra-empresa")
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        EffectiveAccessReport relatorio = diagnostics.effective("user-1", null).orElseThrow();

        assertThat(relatorio.capabilities())
                .filteredOn(c -> "exportar".equals(c.action())
                        && "GESTORES-FROTA".equals(c.grantedByName()))
                .singleElement()
                .satisfies(c -> assertThat(c.situation())
                        .as("a concessão global do grupo permanece efetiva")
                        .isEqualTo(EffectiveCapability.Situation.EFFECTIVE));
    }

    @Test
    @DisplayName("as chaves de permissionsBySecurityType são estáveis")
    void chavesDoAgrupamentoPorTipoSaoEstaveis() {
        // A consulta usa TYPE(p.security), e o que o Hibernate devolve ali — a classe da entidade
        // ou o valor do discriminador — não é garantido pela especificação. São dois vocabulários
        // possíveis para a MESMA resposta de API: User/Group/Profile de um lado,
        // USUARIO/SEGURANCA_GRUPO/SEGURANCA_PERFIL do outro. Sem este teste, uma troca de versão do
        // Hibernate mudaria o corpo do endpoint sem nada quebrar aqui — e quebraria a tela.
        UserEntity user = usuario("user-1", false);
        GroupEntity grupo = grupo("GESTORES-FROTA");
        ProfileEntity perfil = perfil("SUPERVISOR");
        configurar(user, grupo, perfil);

        concessao(user, "direta", true);
        concessao(grupo, "por_grupo", true);
        concessao(perfil, "por_perfil", true);

        assertThat(diagnostics.overview().permissionsBySecurityType())
                .containsOnlyKeys("User", "Group", "Profile")
                .containsEntry("User", 1L)
                .containsEntry("Group", 1L)
                .containsEntry("Profile", 1L);
    }

    @Test
    @DisplayName("o panorama conta recursos sem ação — registro preguiçoso, não defeito")
    void panoramaContaRecursosVazios() {
        resourceRepository.save(ResourceEntity.builder()
                .id("resource-2").name("tela.nunca.aberta").description("Tela").active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        AccessOverview overview = diagnostics.overview();

        assertThat(overview.resources()).isEqualTo(2);
        assertThat(overview.resourcesWithoutAction()).isEqualTo(2);
    }

    @Test
    @DisplayName("o detalhe do panorama devolve quais são os itens, não só quantos")
    void detalheDoPanoramaListaOsItens() {
        UserEntity user = usuario("pessoa@empresa.com.br", false);
        concessao(user, "acao_viva", true);
        concessao(user, "acao_morta", false);

        var inativas = diagnostics.listOverviewItems(
                OverviewMetric.ACTIONS_INACTIVE, PageRequest.of(0, 25));

        // O card diz "1 de 2 ações". O detalhe precisa dizer QUAL — é o que permite agir.
        assertThat(inativas.getTotalElements()).isEqualTo(1);
        assertThat(inativas.getContent()).singleElement()
                .satisfies(item -> {
                    assertThat(item.label()).isEqualTo("acao_morta");
                    assertThat(item.detail()).isEqualTo(RECURSO);
                    assertThat(item.reason()).isNotBlank();
                });
    }

    @Test
    @DisplayName("o detalhe das permissões inertes distingue ação inativa de recurso inativo")
    void detalheDistingueAOrigemDaInercia() {
        UserEntity user = usuario("outra@empresa.com.br", false);
        concessao(user, "acao_morta", false);

        var itens = diagnostics.listOverviewItems(
                OverviewMetric.PERMISSIONS_POINTING_TO_INACTIVE, PageRequest.of(0, 25));

        // Sem o motivo, quem lê a lista não sabe se reativa a ação ou o recurso — e o número volta
        // a ser um número.
        assertThat(itens.getContent()).singleElement()
                .satisfies(item -> assertThat(item.reason()).isEqualTo("Ação inativa"));
    }

    @Test
    @DisplayName("o detalhe do panorama bate com a contagem do card")
    void detalheBateComOCard() {
        UserEntity user = usuario("terceira@empresa.com.br", false);
        concessao(user, "sem_dono_1", true);
        concessao(user, "sem_dono_2", true);
        // Ação no catálogo que ninguém recebeu.
        actionRepository.save(ActionEntity.builder()
                .id("act-orfa").name("orfa").description("Órfã").resource(recurso).active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        AccessOverview overview = diagnostics.overview();
        var itens = diagnostics.listOverviewItems(
                OverviewMetric.ACTIONS_WITHOUT_PERMISSION, PageRequest.of(0, 25));

        // Se card e detalhe divergirem, o painel passa a se contradizer — e um diagnóstico que se
        // contradiz é pior do que nenhum.
        assertThat(itens.getTotalElements()).isEqualTo(overview.actionsWithoutPermission());
    }

    @Test
    @DisplayName("a árvore devolve um ramo por vez, paginado")
    void arvorePaginaCadaRamo() {
        for (int i = 1; i <= 7; i++) {
            usuario("pessoa-" + i, false);
        }

        var primeira = diagnostics.browse(TreeBranch.USERS, null, null, PageRequest.of(0, 3));

        // O ponto: o servidor não devolve o catálogo inteiro para exibir três linhas.
        assertThat(primeira.getContent()).hasSize(3);
        assertThat(primeira.getTotalElements()).isEqualTo(7);
        assertThat(primeira.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("o filtro da árvore roda no banco, não no cliente")
    void filtroDaArvoreRodaNoBanco() {
        usuario("ana", false);
        usuario("bruno", false);
        usuario("ana-maria", false);

        var filtrada = diagnostics.browse(TreeBranch.USERS, null, "ana", PageRequest.of(0, 50));

        // Se o filtro fosse no cliente, o total viria 3 e a filtragem aconteceria depois de já ter
        // carregado tudo — o que a paginação existe para evitar.
        assertThat(filtrada.getTotalElements()).isEqualTo(2);
        assertThat(filtrada.getContent()).extracting(TreeNode::label)
                .allSatisfy(nome -> assertThat(nome.toLowerCase()).contains("ana"));
    }

    @Test
    @DisplayName("o recurso marca no nó que tem ação desativada, sem precisar abrir o ramo")
    void recursoMarcaProblemaSemAbrir() {
        UserEntity user = usuario("alguem", false);
        concessao(user, "acao_viva", true);
        concessao(user, "acao_morta", false);

        var recursos = diagnostics.browse(TreeBranch.RESOURCES, null, null, PageRequest.of(0, 50));

        // O marcador é o que leva o olho até o problema; sem ele, achar exige abrir ramo por ramo.
        assertThat(recursos.getContent()).singleElement().satisfies(node -> {
            assertThat(node.severity()).isEqualTo("critical");
            assertThat(node.badge()).isEqualTo("2");
            assertThat(node.hasChildren()).isTrue();
        });
    }

    @Test
    @DisplayName("as ações de um recurso exigem o recurso pai")
    void acoesExigemPai() {
        // Sem o pai a consulta seria "todas as ações do tenant" — 621 numa instalação real, e não
        // o que a árvore pediu.
        assertThatThrownBy(() -> diagnostics.browse(
                TreeBranch.ACTIONS_OF_RESOURCE, null, null, PageRequest.of(0, 50)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("o grupo mostra os membros com o total que cada um acumula, não só o que ele concede")
    void grupoMostraOTotalDeCadaMembro() {
        UserEntity user = usuario("membro", false);
        GroupEntity grupo = grupo("TIME-SAC");
        ProfileEntity perfil = perfil("ATENDIMENTO");
        user = configurar(user, grupo, perfil);

        concessao(grupo, "mover_cartao", true);
        concessao(perfil, "editar_ticket", true);
        concessao(user, "direta", true);

        var relatorio = diagnostics.group("group-TIME-SAC");

        assertThat(relatorio).isPresent();
        // O grupo concede UMA capacidade...
        assertThat(relatorio.get().grants()).hasSize(1);
        // ...mas o membro acumula três, somando perfil e concessão direta. Mostrar só a do grupo
        // faria a tela responder a pergunta errada.
        assertThat(relatorio.get().members()).singleElement()
                .satisfies(m -> assertThat(m.total()).isEqualTo(3));
    }

    @Test
    @DisplayName("o perfil tem a mesma forma do grupo — uma ideia, uma tela")
    void perfilTemAMesmaForma() {
        UserEntity user = usuario("com-perfil", false);
        ProfileEntity perfil = perfil("SUPERVISOR");
        configurar(user, grupo("QUALQUER"), perfil);
        concessao(perfil, "aprovar", true);

        var relatorio = diagnostics.profile("profile-SUPERVISOR");

        assertThat(relatorio).isPresent();
        assertThat(relatorio.get().grants()).hasSize(1);
        assertThat(relatorio.get().members()).extracting(m -> m.userId()).contains("com-perfil");
    }

    @Test
    @DisplayName("a consulta reversa inclui o administrador, que alcança sem concessão nenhuma")
    void reversaIncluiAdministrador() {
        UserEntity comum = usuario("comum", false);
        GroupEntity grupo = grupo("TIME");
        vincular(comum, grupo);
        concessao(grupo, "aprovar_custo", true);
        usuario("chefe", true);

        String acaoId = actionRepository.findAll().stream()
                .filter(a -> a.getName().equals("aprovar_custo")).findFirst().orElseThrow().getId();

        var quem = diagnostics.whoCanReach(acaoId);

        // O administrador não tem concessão nenhuma e alcança tudo. Deixá-lo de fora daria a
        // resposta errada justamente para a conta que mais importa numa auditoria.
        assertThat(quem).extracting(ReachEntry::userId).contains("comum", "chefe");
        assertThat(quem).filteredOn(r -> r.userId().equals("chefe")).singleElement()
                .satisfies(r -> assertThat(r.kind()).isEqualTo(ReachEntry.KIND_ADMIN));
        assertThat(quem).filteredOn(r -> r.userId().equals("comum")).singleElement()
                .satisfies(r -> assertThat(r.via()).isEqualTo("grupo TIME"));
    }

    @Test
    @DisplayName("quem alcança por duas vias aparece uma vez, com a que vale")
    void reversaNaoDuplicaPessoa() {
        UserEntity user = usuario("dupla-via", false);
        GroupEntity grupo = grupo("G1");
        ProfileEntity perfil = perfil("P1");
        user = configurar(user, grupo, perfil);

        // A MESMA capacidade concedida por duas vias: uma inerte, outra efetiva.
        ActionEntity acao = actionRepository.save(ActionEntity.builder()
                .id("act-dupla").name("dupla").description("Dupla").resource(recurso).active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
        permissionRepository.save(PermissionEntity.builder()
                .id("perm-g").security(grupo).action(acao)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
        permissionRepository.save(PermissionEntity.builder()
                .id("perm-p").security(perfil).action(acao)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        var quem = diagnostics.whoCanReach("act-dupla");

        // Duplicar inflaria a contagem de "quem pode" — o número que uma auditoria usa.
        assertThat(quem).filteredOn(r -> r.userId().equals("dupla-via")).hasSize(1);
    }

    @Test
    @DisplayName("a capacidade carrega o nível exigido, para explicar o que está bloqueado")
    void capacidadeCarregaNivelExigido() {
        UserEntity user = usuario("alguem-nivel", false);
        concessao(user, "acao_com_nivel", true);

        var efetivo = diagnostics.effective("alguem-nivel", null);

        assertThat(efetivo).isPresent();
        // Sem este campo, "concedida e bloqueada por nível" fica indistinguível de "concedida e
        // valendo" — e a tela não consegue explicar a diferença.
        assertThat(efetivo.get().capabilities()).isNotEmpty();
        assertThat(efetivo.get().capabilities().get(0)).hasFieldOrProperty("minimumLevel");
    }

    // ------------------------------------------------------------------ apoio

    /** As colunas de id têm 40 caracteres; ids descritivos estouram esse limite. */
    private final java.util.concurrent.atomic.AtomicInteger sequencia =
            new java.util.concurrent.atomic.AtomicInteger();

    private void concessao(SecurityEntity destinatario, String nomeDaAcao, boolean acaoAtiva) {
        int n = sequencia.incrementAndGet();

        ActionEntity acao = actionRepository.save(ActionEntity.builder()
                .id("act-" + n)
                .name(nomeDaAcao)
                .description("Ação " + nomeDaAcao)
                .resource(recurso)
                .active(acaoAtiva)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("teste")
                .build());

        permissionRepository.save(PermissionEntity.builder()
                .id("perm-" + n)
                .security(destinatario)
                .action(acao)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("teste")
                .build());
    }

    private UserEntity usuario(String id, boolean administrador) {
        return userRepository.save(UserEntity.builder()
                .id(id)
                .name("Usuário " + id)
                .description("Usuário de teste")
                .userName(id)
                .email(id + "@exemplo.test")
                .password("irrelevante")
                .isAdministrator(administrador)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("teste")
                .build());
    }

    private GroupEntity grupo(String nome) {
        return groupRepository.save(GroupEntity.builder()
                .id("group-" + nome).name(nome).description("Grupo " + nome)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private ProfileEntity perfil(String nome) {
        return profileRepository.save(ProfileEntity.builder()
                .id("profile-" + nome).name(nome).description("Perfil " + nome)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    /**
     * Vincula grupo e perfil em <b>um único save</b>. Salvar a mesma instância duas vezes deixa a
     * segunda com versão defasada, e o resultado é {@code ObjectOptimisticLockingFailure}.
     */
    private UserEntity configurar(UserEntity user, GroupEntity grupo, ProfileEntity perfil) {
        if (grupo != null) {
            Set<UserGroupEntity> vinculos =
                    user.getGroups() == null ? new HashSet<>() : new HashSet<>(user.getGroups());
            vinculos.add(UserGroupEntity.builder()
                    .id("ug-" + sequencia.incrementAndGet()).user(user).group(grupo).build());
            user.setGroups(vinculos);
        }
        if (perfil != null) {
            user.setProfile(perfil);
        }
        return userRepository.save(user);
    }

    private UserEntity vincular(UserEntity user, GroupEntity grupo) {
        return configurar(user, grupo, null);
    }
}
