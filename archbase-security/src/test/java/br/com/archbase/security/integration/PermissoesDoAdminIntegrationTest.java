package br.com.archbase.security.integration;

import br.com.archbase.security.adapter.ResourcePersistenceAdapter;
import br.com.archbase.security.domain.dto.PermissionWithTypesDto;
import br.com.archbase.security.domain.dto.ResoucePermissionsWithTypeDto;
import br.com.archbase.security.domain.dto.SecurityType;
import br.com.archbase.security.domain.entity.TipoRecurso;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A listagem que a <b>tela de concessão de permissões</b> consome, em
 * {@code GET /api/v1/resource/permissions/security/{id}?type=} e {@code GET /permissions}.
 *
 * <p><b>Não tinha teste nenhum.</b> É a quinta implementação de "usuário ∪ grupos ∪ perfil" do
 * módulo — três consultas QueryDSL separadas, independentes tanto da JPQL que o
 * {@code @HasPermission} usa quanto do {@code ArchbaseCapabilityReader} que as demais listagens
 * passaram a usar. Justamente a que ninguém verificava era a que o administrador olha para decidir
 * quem pode o quê.
 *
 * <p>Metade destes testes fixa o comportamento que já existia; a outra metade cobre os dois defeitos
 * que a ausência de teste escondia — a descrição transportada dentro de uma chave concatenada, e a
 * concessão herdada devolvendo o identificador de remoção de outra entidade.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-permissoes-admin-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Permissões da tela de administração (Spring + H2)")
class PermissoesDoAdminIntegrationTest {

    private static final String RECURSO_API = "tms.ordemservico";
    private static final String RECURSO_VIEW = "Cockpit";

    @Autowired
    ResourcePersistenceAdapter adapter;
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

    private final AtomicInteger sequencia = new AtomicInteger();
    private ResourceEntity recursoApi;

    @BeforeEach
    void limpar() {
        permissionRepository.deleteAll();
        actionRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();
        profileRepository.deleteAll();

        recursoApi = recurso("res-api", RECURSO_API, "Ordens de serviço", TipoRecurso.API);
    }

    @Test
    @DisplayName("soma as três origens e etiqueta cada uma")
    void somaAsTresOrigens() {
        UserEntity user = usuario("user-1");
        GroupEntity grupo = grupo("GESTORES");
        ProfileEntity perfil = perfil("SUPERVISOR");
        user = vincular(user, grupo, perfil);

        conceder(user, acao(recursoApi, "cancelar"));
        conceder(grupo, acao(recursoApi, "aprovar_custo"));
        conceder(perfil, acao(recursoApi, "view"));

        List<PermissionWithTypesDto> capacidades = capacidadesDe(
                adapter.findUserResourcesPermissions(user.getId()), RECURSO_API);

        assertThat(capacidades).extracting(PermissionWithTypesDto::getActionName)
                .containsExactlyInAnyOrder("cancelar", "aprovar_custo", "view");
        assertThat(porNome(capacidades, "cancelar").getTypes()).containsExactly(SecurityType.USER);
        assertThat(porNome(capacidades, "aprovar_custo").getTypes()).containsExactly(SecurityType.GROUP);
        assertThat(porNome(capacidades, "view").getTypes()).containsExactly(SecurityType.PROFILE);
    }

    @Test
    @DisplayName("a mesma capacidade por duas vias aparece uma vez, com as duas etiquetas")
    void mesmaCapacidadePorDuasVias() {
        UserEntity user = usuario("user-1");
        GroupEntity grupo = grupo("GESTORES");
        user = vincular(user, grupo, null);

        ActionEntity aprovar = acao(recursoApi, "aprovar_custo");
        conceder(user, aprovar);
        conceder(grupo, aprovar);

        List<PermissionWithTypesDto> capacidades = capacidadesDe(
                adapter.findUserResourcesPermissions(user.getId()), RECURSO_API);

        assertThat(capacidades).hasSize(1);
        assertThat(capacidades.get(0).getTypes())
                .containsExactlyInAnyOrder(SecurityType.USER, SecurityType.GROUP);
    }

    @Test
    @DisplayName("a concessão HERDADA não devolve permissionId — remover dali apagaria a do grupo")
    void herdadaNaoDevolvePermissionId() {
        UserEntity user = usuario("user-1");
        GroupEntity grupo = grupo("GESTORES");
        ProfileEntity perfil = perfil("SUPERVISOR");
        user = vincular(user, grupo, perfil);

        conceder(user, acao(recursoApi, "cancelar"));
        conceder(grupo, acao(recursoApi, "aprovar_custo"));
        conceder(perfil, acao(recursoApi, "view"));

        List<PermissionWithTypesDto> capacidades = capacidadesDe(
                adapter.findUserResourcesPermissions(user.getId()), RECURSO_API);

        // A via direta traz o que remover.
        assertThat(porNome(capacidades, "cancelar").getPermissionId()).isNotNull();
        // As herdadas, não: a concessão é do grupo e do perfil, e apagá-la a partir da tela de uma
        // pessoa tiraria o acesso de todo mundo que está no grupo.
        assertThat(porNome(capacidades, "aprovar_custo").getPermissionId()).isNull();
        assertThat(porNome(capacidades, "view").getPermissionId()).isNull();
    }

    @Test
    @DisplayName("concedida direto E pelo grupo devolve o permissionId DA PESSOA, não o do grupo")
    void permissionIdEODaViaDireta() {
        UserEntity user = usuario("user-1");
        GroupEntity grupo = grupo("GESTORES");
        user = vincular(user, grupo, null);

        ActionEntity aprovar = acao(recursoApi, "aprovar_custo");
        PermissionEntity doUsuario = conceder(user, aprovar);
        PermissionEntity doGrupo = conceder(grupo, aprovar);

        List<PermissionWithTypesDto> capacidades = capacidadesDe(
                adapter.findUserResourcesPermissions(user.getId()), RECURSO_API);

        assertThat(capacidades.get(0).getPermissionId())
                .isEqualTo(doUsuario.getId())
                .isNotEqualTo(doGrupo.getId());
    }

    @Test
    @DisplayName("o nome do recurso e o nome da ação chegam na resposta, não só a descrição")
    void nomesChegamNaResposta() {
        UserEntity user = usuario("user-1");
        conceder(user, acao(recursoApi, "aprovar_custo"));

        ResoucePermissionsWithTypeDto recurso =
                recursoDe(adapter.findUserResourcesPermissions(user.getId()), RECURSO_API);

        assertThat(recurso.getResourceName()).isEqualTo(RECURSO_API);
        assertThat(recurso.getResourceDescription()).isEqualTo("Ordens de serviço");
        assertThat(recurso.getPermissions().get(0).getActionName()).isEqualTo("aprovar_custo");
        assertThat(recurso.getPermissions().get(0).getActionDescription())
                .isEqualTo("Ação aprovar_custo");
    }

    @Test
    @DisplayName("o tipo do recurso chega, e distingue tela de endpoint")
    void tipoDistingueTelaDeEndpoint() {
        ResourceEntity recursoView = recurso("res-view", RECURSO_VIEW, "Cockpit do vendedor", TipoRecurso.VIEW);
        ResourceEntity semTipo = recurso("res-nulo", "legado.recurso", "Recurso legado", null);

        UserEntity user = usuario("user-1");
        conceder(user, acao(recursoApi, "aprovar_custo"));
        conceder(user, acao(recursoView, "abrir"));
        conceder(user, acao(semTipo, "usar"));

        List<ResoucePermissionsWithTypeDto> recursos = adapter.findUserResourcesPermissions(user.getId());

        assertThat(recursoDe(recursos, RECURSO_API).getResourceType()).isEqualTo(TipoRecurso.API);
        assertThat(recursoDe(recursos, RECURSO_VIEW).getResourceType()).isEqualTo(TipoRecurso.VIEW);
        // Recurso anterior à coluna: o nulo é uma terceira classificação, e a tela precisa vê-lo
        // como tal em vez de recebê-lo empurrado para um dos dois baldes.
        assertThat(recursoDe(recursos, "legado.recurso").getResourceType()).isNull();
    }

    @Test
    @DisplayName("descrição com ':' não volta truncada")
    void descricaoComDoisPontosNaoTrunca() {
        ResourceEntity comDoisPontos =
                recurso("res-dp", "tms.rota", "Rotas: planejamento e execução", TipoRecurso.API);

        UserEntity user = usuario("user-1");
        conceder(user, acao(comDoisPontos, "planejar"));

        assertThat(recursoDe(adapter.findUserResourcesPermissions(user.getId()), "tms.rota")
                .getResourceDescription())
                .isEqualTo("Rotas: planejamento e execução");
    }

    @Test
    @DisplayName("descrição VAZIA não derruba a consulta")
    void descricaoVaziaNaoDerruba() {
        // O registro de tela grava a descrição que o cliente mandar, e o cliente pode mandar "".
        // A chave concatenada virava "id:", que split(":") reduz a UM elemento — e o get(1) que
        // vinha em seguida estourava IndexOutOfBounds, transformando a tela inteira num 500.
        ResourceEntity semDescricao = recurso("res-vazio", "tela.sem.descricao", "", TipoRecurso.VIEW);

        UserEntity user = usuario("user-1");
        conceder(user, acao(semDescricao, "abrir"));

        assertThatCode(() -> adapter.findUserResourcesPermissions(user.getId()))
                .doesNotThrowAnyException();
        assertThat(recursoDe(adapter.findUserResourcesPermissions(user.getId()), "tela.sem.descricao")
                .getResourceDescription()).isEmpty();
    }

    @Test
    @DisplayName("ação inativa não aparece para conceder")
    void acaoInativaNaoAparece() {
        UserEntity user = usuario("user-1");
        conceder(user, acaoInativa(recursoApi, "capacidade_aposentada"));
        conceder(user, acao(recursoApi, "aprovar_custo"));

        assertThat(capacidadesDe(adapter.findUserResourcesPermissions(user.getId()), RECURSO_API))
                .extracting(PermissionWithTypesDto::getActionName)
                .containsExactly("aprovar_custo");
    }

    @Test
    @DisplayName("a lista do grupo traz o permissionId — ali a concessão é dele")
    void grupoTrazOProprioPermissionId() {
        GroupEntity grupo = grupo("GESTORES");
        PermissionEntity concessao = conceder(grupo, acao(recursoApi, "aprovar_custo"));

        List<PermissionWithTypesDto> capacidades = capacidadesDe(
                adapter.findGroupResourcesPermissions(grupo.getId()), RECURSO_API);

        assertThat(capacidades).hasSize(1);
        assertThat(capacidades.get(0).getPermissionId()).isEqualTo(concessao.getId());
        assertThat(capacidades.get(0).getTypes()).containsExactly(SecurityType.GROUP);
    }

    @Test
    @DisplayName("o catálogo de disponíveis traz nome e tipo, e NÃO marca origem nenhuma")
    void catalogoNaoMarcaOrigem() {
        ResourceEntity recursoView = recurso("res-view", RECURSO_VIEW, "Cockpit do vendedor", TipoRecurso.VIEW);
        acao(recursoApi, "aprovar_custo");
        acao(recursoView, "abrir");
        acaoInativa(recursoApi, "capacidade_aposentada");

        List<ResoucePermissionsWithTypeDto> catalogo = adapter.findAllResourcesPermissions();

        assertThat(recursoDe(catalogo, RECURSO_API).getResourceType()).isEqualTo(TipoRecurso.API);
        assertThat(recursoDe(catalogo, RECURSO_VIEW).getResourceType()).isEqualTo(TipoRecurso.VIEW);

        List<PermissionWithTypesDto> daApi = capacidadesDe(catalogo, RECURSO_API);
        assertThat(daApi).extracting(PermissionWithTypesDto::getActionName)
                .containsExactly("aprovar_custo");
        // "Existe para ser concedido" não é "foi concedido a alguém": marcar origem aqui faria a
        // tela riscar o catálogo inteiro como já concedido.
        assertThat(daApi.get(0).getTypes()).isNull();
        assertThat(daApi.get(0).getPermissionId()).isNull();
    }

    @Test
    @DisplayName("recurso DESATIVADO com ação ativa continua listado, e vem marcado")
    void recursoDesativadoVemMarcado() {
        // Esconder tiraria do admin a chance de arrumar antes de ligar require-active. E escondê-lo
        // da tela sem tirá-lo da decisão seria a interface discordando do avaliador, que é a
        // divergência que o core existe para eliminar.
        ResourceEntity desativado = resourceRepository.save(ResourceEntity.builder()
                .id("res-off").name("tms.legado").description("Recurso legado")
                .active(false).type(TipoRecurso.API)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        UserEntity user = usuario("user-1");
        conceder(user, acao(desativado, "usar"));
        conceder(user, acao(recursoApi, "aprovar_custo"));

        List<ResoucePermissionsWithTypeDto> recursos = adapter.findUserResourcesPermissions(user.getId());

        assertThat(recursoDe(recursos, "tms.legado").getResourceActive()).isFalse();
        assertThat(recursoDe(recursos, RECURSO_API).getResourceActive()).isTrue();
        // E continua concedível: a listagem não filtra por isto, e não deve passar a filtrar.
        assertThat(capacidadesDe(recursos, "tms.legado"))
                .extracting(PermissionWithTypesDto::getActionName)
                .containsExactly("usar");
    }

    @Test
    @DisplayName("o catálogo de disponíveis também traz o estado do recurso")
    void catalogoTrazOEstado() {
        resourceRepository.save(ResourceEntity.builder()
                .id("res-off").name("tms.legado").description("Recurso legado")
                .active(false).type(TipoRecurso.API)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
        acao(resourceRepository.findById("res-off").orElseThrow(), "usar");
        acao(recursoApi, "aprovar_custo");

        List<ResoucePermissionsWithTypeDto> catalogo = adapter.findAllResourcesPermissions();

        assertThat(recursoDe(catalogo, "tms.legado").getResourceActive()).isFalse();
        assertThat(recursoDe(catalogo, RECURSO_API).getResourceActive()).isTrue();
    }

    // ---- fixtura ----

    private ResourceEntity recurso(String id, String nome, String descricao, TipoRecurso tipo) {
        return resourceRepository.save(ResourceEntity.builder()
                .id(id).name(nome).description(descricao).active(true).type(tipo)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private ActionEntity acao(ResourceEntity recurso, String nome) {
        return salvarAcao(recurso, nome, true);
    }

    private ActionEntity acaoInativa(ResourceEntity recurso, String nome) {
        return salvarAcao(recurso, nome, false);
    }

    private ActionEntity salvarAcao(ResourceEntity recurso, String nome, boolean ativa) {
        return actionRepository.save(ActionEntity.builder()
                .id("act-" + sequencia.incrementAndGet()).name(nome).description("Ação " + nome)
                .resource(recurso).active(ativa)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private PermissionEntity conceder(SecurityEntity destinatario, ActionEntity acao) {
        return permissionRepository.save(PermissionEntity.builder()
                .id("perm-" + sequencia.incrementAndGet()).security(destinatario).action(acao)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private UserEntity usuario(String id) {
        return userRepository.save(UserEntity.builder()
                .id(id).name("Usuário " + id).description("Usuário de teste")
                .userName(id).email(id + "@exemplo.test").password("irrelevante")
                .isAdministrator(false).accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true)
                .allowPasswordChange(true).allowMultipleLogins(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
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

    private UserEntity vincular(UserEntity user, GroupEntity grupo, ProfileEntity perfil) {
        if (grupo != null) {
            Set<UserGroupEntity> vinculos =
                    user.getGroups() == null ? new HashSet<>() : new HashSet<>(user.getGroups());
            vinculos.add(UserGroupEntity.builder()
                    .id("ug-" + sequencia.incrementAndGet()).user(user).group(grupo).build());
            user.setGroups(vinculos);
        }
        user.setProfile(perfil);
        return userRepository.save(user);
    }

    private ResoucePermissionsWithTypeDto recursoDe(List<ResoucePermissionsWithTypeDto> recursos,
                                                    String nomeDoRecurso) {
        return recursos.stream()
                .filter(r -> nomeDoRecurso.equals(r.getResourceName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Recurso ausente na resposta: " + nomeDoRecurso));
    }

    private List<PermissionWithTypesDto> capacidadesDe(List<ResoucePermissionsWithTypeDto> recursos,
                                                       String nomeDoRecurso) {
        return recursoDe(recursos, nomeDoRecurso).getPermissions();
    }

    private PermissionWithTypesDto porNome(List<PermissionWithTypesDto> capacidades, String nome) {
        return capacidades.stream()
                .filter(c -> nome.equals(c.getActionName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Capacidade ausente na resposta: " + nome));
    }
}
