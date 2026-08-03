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
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
