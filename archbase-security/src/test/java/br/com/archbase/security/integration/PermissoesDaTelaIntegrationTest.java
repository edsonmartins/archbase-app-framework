package br.com.archbase.security.integration;

import br.com.archbase.security.adapter.ResourcePersistenceAdapter;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A listagem de permissões que o <b>frontend</b> consome, em
 * {@code GET /api/v1/resource/permissions/{nome}}.
 *
 * <p>Este é o segundo caminho de autorização do framework, e por muito tempo o único com regra
 * própria: ele monta usuário ∪ grupos ∪ perfil em QueryDSL, separado da JPQL que o
 * {@code @HasPermission} usa — e <b>filtra {@code action.active}</b>, coisa que o outro caminho não
 * faz. Foi essa duplicação que produziu duas respostas diferentes para a mesma pergunta.
 *
 * <p>Estes testes caracterizaram o comportamento antes de a listagem passar a usar o core, e
 * continuam verdes depois — é o que demonstra que a unificação não mudou o que a tela recebe.
 *
 * <p><b>A classe não é {@code @Transactional}, e isso é a segunda coisa que ela prova.</b> Escrita
 * contra a implementação anterior, precisava ser: sem sessão aberta o caminho estourava
 * {@code LazyInitializationException}, porque {@code SecurityAdapter.getLoggedUser()} converte a
 * entidade para domínio tocando {@code groups}, que é lazy. Em produção passava só porque a
 * requisição web mantém a sessão aberta (Open Session In View) — dependência silenciosa que
 * quebraria a mesma chamada feita de uma task assíncrona ou de um job. A versão sobre o core carrega
 * o sujeito com fetch join e não depende de sessão nenhuma.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-permissoes-tela-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Permissões da tela (Spring + H2)")
class PermissoesDaTelaIntegrationTest {

    private static final String RECURSO = "tms.ordemservico";
    private static final String OUTRO_RECURSO = "tms.pneu";

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
    private ResourceEntity recurso;
    private ResourceEntity outroRecurso;

    @BeforeEach
    void limpar() {
        permissionRepository.deleteAll();
        actionRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();
        profileRepository.deleteAll();

        recurso = recursoSalvo("resource-1", RECURSO);
        outroRecurso = recursoSalvo("resource-2", OUTRO_RECURSO);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("soma as três origens: direto, grupo e perfil")
    void somaAsTresOrigens() {
        UserEntity user = usuario("user-1");
        GroupEntity grupo = grupo("GESTORES-FROTA");
        ProfileEntity perfil = perfil("SUPERVISOR");
        user = configurar(user, grupo, perfil);

        concessao(user, recurso, "cancelar", true);
        concessao(grupo, recurso, "aprovar_custo", true);
        concessao(perfil, recurso, "view", true);

        autenticar(user);

        ResourcePermissionsDto permissoes = adapter.findLoggedUserResourcePermissions(RECURSO);

        assertThat(permissoes.getResourceName()).isEqualTo(RECURSO);
        assertThat(permissoes.getPermissions())
                .containsExactlyInAnyOrder("cancelar", "aprovar_custo", "view");
    }

    @Test
    @DisplayName("ação inativa NÃO aparece — a tela filtra o que o @HasPermission ainda honra")
    void acaoInativaNaoAparece() {
        // A divergência central entre os dois caminhos. Aqui a concessão sobre ação inativa é
        // invisível; no caminho do backend, ela concede.
        UserEntity user = usuario("user-1");
        concessao(user, recurso, "ativa", true);
        concessao(user, recurso, "inativa", false);

        autenticar(user);

        assertThat(adapter.findLoggedUserResourcePermissions(RECURSO).getPermissions())
                .containsExactly("ativa");
    }

    @Test
    @DisplayName("permissão de outro recurso não vaza para a lista")
    void naoVazaDeOutroRecurso() {
        UserEntity user = usuario("user-1");
        concessao(user, recurso, "view", true);
        concessao(user, outroRecurso, "instalar", true);

        autenticar(user);

        assertThat(adapter.findLoggedUserResourcePermissions(RECURSO).getPermissions())
                .containsExactly("view");
    }

    @Test
    @DisplayName("permissão de outro usuário não vaza")
    void naoVazaDeOutroUsuario() {
        UserEntity dono = usuario("user-1");
        UserEntity outro = usuario("user-2");
        concessao(outro, recurso, "aprovar_custo", true);

        autenticar(dono);

        assertThat(adapter.findLoggedUserResourcePermissions(RECURSO).getPermissions()).isEmpty();
    }

    @Test
    @DisplayName("usuário sem nenhuma permissão recebe lista vazia, não erro")
    void semPermissaoListaVazia() {
        autenticar(usuario("user-1"));

        ResourcePermissionsDto permissoes = adapter.findLoggedUserResourcePermissions(RECURSO);

        assertThat(permissoes.getResourceName()).isEqualTo(RECURSO);
        assertThat(permissoes.getPermissions()).isEmpty();
    }

    @Test
    @DisplayName("administrador recebe a mesma lista de sempre — a flag não vale aqui")
    void administradorNaoTemAtalhoNaListagem() {
        // A listagem não é uma decisão de acesso: ela mostra o que foi concedido. A flag de
        // administrador encerra decisões, não preenche catálogo. Caracterizado para que a
        // unificação com o core não introduza um atalho aqui.
        UserEntity admin = usuario("admin-1");
        admin.setIsAdministrator(true);
        admin = userRepository.save(admin);

        autenticar(admin);

        assertThat(adapter.findLoggedUserResourcePermissions(RECURSO).getPermissions()).isEmpty();
    }

    // ------------------------------------------------------------------ apoio

    private void autenticar(UserEntity user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private ResourceEntity recursoSalvo(String id, String nome) {
        return resourceRepository.save(ResourceEntity.builder()
                .id(id).name(nome).description("Recurso " + nome).active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private void concessao(SecurityEntity destinatario, ResourceEntity emQualRecurso,
                           String nomeDaAcao, boolean acaoAtiva) {
        int n = sequencia.incrementAndGet();

        ActionEntity acao = actionRepository.save(ActionEntity.builder()
                .id("act-" + n).name(nomeDaAcao).description("Ação " + nomeDaAcao)
                .resource(emQualRecurso).active(acaoAtiva)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        permissionRepository.save(PermissionEntity.builder()
                .id("perm-" + n).security(destinatario).action(acao)
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

    private UserEntity configurar(UserEntity user, GroupEntity grupo, ProfileEntity perfil) {
        Set<UserGroupEntity> vinculos =
                user.getGroups() == null ? new HashSet<>() : new HashSet<>(user.getGroups());
        vinculos.add(UserGroupEntity.builder()
                .id("ug-" + sequencia.incrementAndGet()).user(user).group(grupo).build());
        user.setGroups(vinculos);
        user.setProfile(perfil);
        return userRepository.save(user);
    }
}
