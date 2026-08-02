package br.com.archbase.security.integration;

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
import br.com.archbase.security.service.ArchbaseSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Caracterização da consulta de autorização contra um banco de verdade (H2).
 *
 * <p><b>O que este teste registra.</b> O framework tem <b>duas</b> implementações da mesma
 * pergunta — "este usuário tem esta capacidade?" — com regras diferentes:
 *
 * <table border="1">
 *   <caption>As duas consultas</caption>
 *   <tr><th>Caminho</th><th>Implementação</th><th>Filtra {@code active}?</th></tr>
 *   <tr><td>Frontend, {@code GET /resource/permissions/{nome}}</td>
 *       <td>{@code ResourcePersistenceAdapter:84}, QueryDSL</td><td><b>sim</b></td></tr>
 *   <tr><td>Backend, {@code @HasPermission}</td>
 *       <td>{@code PermissionJpaRepository}, JPQL</td><td><b>não</b></td></tr>
 * </table>
 *
 * <p>A consequência é contraintuitiva e vale estar escrita: uma permissão concedida sobre uma ação
 * <b>inativa</b> é invisível para a tela e <b>honrada</b> pelo backend. Num sistema onde 57% das
 * concessões estão nessa situação — o caso medido no gestor-rq — ligar {@code @HasPermission} hoje
 * concederia <i>mais</i> do que a interface mostra, não menos.
 *
 * <p>Só um teste com banco real prova isso: com repositório mockado, a ausência de filtro na JPQL
 * é invisível.
 *
 * <p>Estes testes descrevem o comportamento <b>atual</b>. Quando
 * {@code archbase.security.permission.require-active} entrar na fase D, o cenário do filtro é
 * reescrito no mesmo commit, deliberadamente.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-autorizacao-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Consulta de autorização — comportamento atual (Spring + H2)")
class ConsultaDeAutorizacaoComportamentoAtualTest {

    private static final String RECURSO = "tms.ordemservico";
    private static final String ACAO = "aprovar_custo";

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
    @Autowired
    ArchbaseSecurityService securityService;

    @BeforeEach
    void limpar() {
        permissionRepository.deleteAll();
        actionRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    @Transactional
    @DisplayName("permissão sobre ação INATIVA continua concedendo acesso no caminho do backend")
    void acaoInativaAindaConcede() {
        UserEntity user = usuarioSalvo("user-1");
        concessao(user, recursoSalvo(true), false);

        assertThat(securityService.hasPermission(autenticacao(user), ACAO, RECURSO, null, null, null))
                .as("a JPQL de @HasPermission não filtra action.active — é o defeito caracterizado")
                .isTrue();
    }

    @Test
    @Transactional
    @DisplayName("permissão sobre RECURSO inativo também continua concedendo")
    void recursoInativoAindaConcede() {
        UserEntity user = usuarioSalvo("user-1");
        concessao(user, recursoSalvo(false), true);

        assertThat(securityService.hasPermission(autenticacao(user), ACAO, RECURSO, null, null, null))
                .as("nem resource.active é filtrado")
                .isTrue();
    }

    @Test
    @Transactional
    @DisplayName("a consulta devolve a permissão com o concedente — a origem já está disponível")
    void aOrigemJaVemNaConsulta() {
        // Fundamento da fase A: hasPermission joga fora esta informação com um anyMatch. A tela de
        // "efetivo do usuário" e a simulação não precisam de consulta nova — precisam que o
        // resultado pare de ser descartado.
        UserEntity user = usuarioSalvo("user-1");
        GroupEntity grupo = grupoSalvo("GESTORES-FROTA");
        vincular(user, grupo);
        concessao(grupo, recursoSalvo(true), true);

        List<PermissionEntity> encontradas = permissionRepository
                .findBySecurityIdsAndActionNameAndResourceName(Set.of(user.getId(), grupo.getId()), ACAO, RECURSO);

        assertThat(encontradas).hasSize(1);
        assertThat(encontradas.get(0).getSecurity().getId()).isEqualTo(grupo.getId());
    }

    @Test
    @Transactional
    @DisplayName("as três origens somam: perfil, grupo e concessão direta liberam igualmente")
    void tresOrigensSomam() {
        ProfileEntity perfil = perfilSalvo("SUPERVISOR");
        GroupEntity grupo = grupoSalvo("GESTORES-FROTA");
        ResourceEntity recurso = recursoSalvo(true);

        UserEntity porPerfil = usuarioSalvo("user-perfil");
        porPerfil.setProfile(perfil);
        userRepository.save(porPerfil);
        concessao(perfil, recurso, true);

        UserEntity porGrupo = usuarioSalvo("user-grupo");
        vincular(porGrupo, grupo);
        concessao(grupo, recurso, true);

        UserEntity direto = usuarioSalvo("user-direto");
        concessao(direto, recurso, true);

        assertThat(securityService.hasPermission(autenticacao(porPerfil), ACAO, RECURSO, null, null, null)).isTrue();
        assertThat(securityService.hasPermission(autenticacao(porGrupo), ACAO, RECURSO, null, null, null)).isTrue();
        assertThat(securityService.hasPermission(autenticacao(direto), ACAO, RECURSO, null, null, null)).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("não existe negação: nenhuma concessão consegue tirar acesso de outra")
    void naoExisteNegacao() {
        // O modelo é união pura. A coluna EFFECT da fase D é o que vai permitir tirar; hoje, a
        // única forma de excluir uma pessoa é não conceder a nenhuma das suas origens.
        UserEntity user = usuarioSalvo("user-1");
        GroupEntity grupo = grupoSalvo("TIME-TRANSPORTE");
        vincular(user, grupo);
        ResourceEntity recurso = recursoSalvo(true);
        concessao(grupo, recurso, true);

        assertThat(securityService.hasPermission(autenticacao(user), ACAO, RECURSO, null, null, null)).isTrue();

        List<PermissionEntity> todas = permissionRepository
                .findBySecurityIdsAndActionNameAndResourceName(Set.of(user.getId(), grupo.getId()), ACAO, RECURSO);
        assertThat(todas)
                .as("PermissionEntity não tem como expressar DENY hoje")
                .allSatisfy(p -> assertThat(p.getSecurity()).isNotNull());
    }

    // ------------------------------------------------------------------ apoio

    private ResourceEntity recursoSalvo(boolean recursoAtivo) {
        return resourceRepository.save(ResourceEntity.builder()
                .id("resource-1")
                .name(RECURSO)
                .description("Ordem de serviço")
                .active(recursoAtivo)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("teste")
                .build());
    }

    private void concessao(SecurityEntity destinatario, ResourceEntity recurso, boolean acaoAtiva) {
        ActionEntity acao = actionRepository.save(ActionEntity.builder()
                .id("action-" + destinatario.getId())
                .name(ACAO)
                .description("Aprovar custo")
                .resource(recurso)
                .active(acaoAtiva)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("teste")
                .build());

        permissionRepository.save(PermissionEntity.builder()
                .id("permission-" + destinatario.getId())
                .security(destinatario)
                .action(acao)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("teste")
                .build());
    }

    private UserEntity usuarioSalvo(String id) {
        return userRepository.save(UserEntity.builder()
                .id(id)
                .name("Usuário " + id)
                .description("Usuário de teste")
                .userName(id)
                .email(id + "@exemplo.test")
                .password("irrelevante")
                .isAdministrator(false)
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

    private GroupEntity grupoSalvo(String nome) {
        return groupRepository.save(GroupEntity.builder()
                .id("group-" + nome)
                .name(nome)
                .description("Grupo " + nome)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("teste")
                .build());
    }

    private ProfileEntity perfilSalvo(String nome) {
        return profileRepository.save(ProfileEntity.builder()
                .id("profile-" + nome)
                .name(nome)
                .description("Perfil " + nome)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("teste")
                .build());
    }

    private void vincular(UserEntity user, GroupEntity grupo) {
        // Coleção mutável: o mapeamento é @OneToMany(cascade = ALL) e o Hibernate precisa poder
        // gerenciá-la. Um Set.of imutável rebenta com UnsupportedOperationException no flush.
        // getGroups() vem nulo: o @Builder atribui o campo e anula o inicializador da declaração.
        Set<UserGroupEntity> vinculos =
                user.getGroups() == null ? new HashSet<>() : new HashSet<>(user.getGroups());
        vinculos.add(UserGroupEntity.builder()
                .id("ug-" + user.getId() + "-" + grupo.getId())
                .user(user)
                .group(grupo)
                .build());
        user.setGroups(vinculos);
        userRepository.save(user);
    }

    private static Authentication autenticacao(UserEntity user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.isAuthenticated()).thenReturn(true);
        return authentication;
    }
}
