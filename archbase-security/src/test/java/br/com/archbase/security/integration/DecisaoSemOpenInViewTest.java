package br.com.archbase.security.integration;

import br.com.archbase.security.access.AccessDecision;
import br.com.archbase.security.access.AccessReasonCodes;
import br.com.archbase.security.access.AccessRequirement;
import br.com.archbase.security.access.ArchbaseAccessSubjectLoader;
import br.com.archbase.security.access.ArchbaseCapabilityReader;
import br.com.archbase.security.access.EffectiveCapability;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
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

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A decisão de acesso <b>sem</b> Open Session In View.
 *
 * <p><b>Por que esta classe existe separada.</b> O interceptador de autorização não abre transação:
 * a consulta de permissões roda na transação curta do próprio Spring Data e devolve entidades
 * <b>desanexadas</b>. Ler dali qualquer associação {@code LAZY} — e a decisão lê o nome de quem
 * concedeu ({@code p.security}) e o nível mínimo da capacidade ({@code p.action}) — dispara
 * {@code LazyInitializationException}, que o {@code CustomAuthorizationManager} converte em
 * negação.
 *
 * <p>O sintoma seria o pior possível: <b>403 para quem tem a permissão</b>, funcionando apenas para
 * administradores, e só em aplicações com {@code spring.jpa.open-in-view=false} — ou seja, invisível
 * no padrão do Spring Boot e presente justamente em quem seguiu a boa prática de desligá-lo.
 *
 * <p>Nenhum teste com repositório mockado alcança isso, e nenhum teste anotado com
 * {@code @Transactional} tampouco: os dois mantêm a sessão aberta. Daí a classe própria, com a
 * propriedade desligada e sem transação de teste.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-sem-osiv-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
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
@DisplayName("Decisão de acesso sem open-in-view (Spring + H2)")
class DecisaoSemOpenInViewTest {

    private static final String RECURSO = "tms.ordemservico";
    private static final String ACAO = "aprovar_custo";

    @Autowired
    ArchbaseSecurityService securityService;
    @Autowired
    ArchbaseAccessSubjectLoader subjectLoader;
    @Autowired
    ArchbaseCapabilityReader capabilityReader;
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

    @BeforeEach
    void preparar() {
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
    @DisplayName("permissão por grupo é concedida, e a decisão diz quem concedeu")
    void concedePorGrupoEDizAOrigem() {
        UserEntity user = usuario("user-1");
        GroupEntity grupo = grupo("GESTORES-FROTA");
        vincular(user, grupo);
        concessao(grupo, ACAO, true);

        AccessDecision decisao = securityService.decide(
                autenticacao(subjectLoader.byId("user-1").orElseThrow().principal()),
                ACAO, RECURSO, null, null, null);

        assertThat(decisao.allowed())
                .as("sem o JOIN FETCH, ler o nome do concedente aqui viraria negação")
                .isTrue();
        assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.GRANTED);
        assertThat(decisao.grantedByName()).isEqualTo("GESTORES-FROTA");
    }

    @Test
    @DisplayName("o principal como o filtro o entrega — relido do repositório, associações lazy")
    void principalComoOFiltroEntrega() {
        // ESTE é o caminho de produção, e os outros cenários deste arquivo NÃO o exercitavam: eles
        // montavam o principal a partir do ArchbaseAccessSubjectLoader, que carrega tudo com
        // @EntityGraph. O filtro JWT não faz isso — ele lê o usuário pelo repositório, a transação
        // curta do Spring Data fecha, e o que chega ao interceptador tem `groups` e `profile`
        // como proxies desanexados.
        //
        // Uma revisão apontou que o teste anterior era autoindulgente, e estava certa.
        UserEntity salvo = usuario("user-1");
        GroupEntity grupo = grupo("GESTORES-FROTA");
        vincular(salvo, grupo);
        concessao(grupo, ACAO, true);

        UserEntity comoOFiltroEntrega = userRepository.findByEmail("user-1@exemplo.test").orElseThrow();

        AccessDecision decisao = securityService.decide(
                autenticacao(comoOFiltroEntrega), ACAO, RECURSO, null, null, null);

        assertThat(decisao.allowed())
                .as("o sujeito precisa ser resolvido mesmo com o principal desanexado")
                .isTrue();
        assertThat(decisao.grantedByName()).isEqualTo("GESTORES-FROTA");
    }

    @Test
    @DisplayName("hasPermission continua devolvendo true pela porta pública")
    void hasPermissionPelaPortaPublica() {
        UserEntity user = usuario("user-1");
        concessao(user, ACAO, true);

        assertThat(securityService.hasPermission(
                autenticacao(subjectLoader.byId("user-1").orElseThrow().principal()),
                ACAO, RECURSO, null, null, null)).isTrue();
    }

    @Test
    @DisplayName("a simulação funciona com o sujeito carregado por id, fora de transação")
    void simulacaoForaDeTransacao() {
        UserEntity user = usuario("user-1");
        GroupEntity grupo = grupo("TIME-TRANSPORTE");
        vincular(user, grupo);
        concessao(grupo, ACAO, true);

        var subject = subjectLoader.byId("user-1");
        assertThat(subject).isPresent();
        assertThat(subject.get().groupNames())
                .as("nomes, e não identificadores — é o que o relatório de efetivo exibe")
                .containsExactly("TIME-TRANSPORTE");
    }

    @Test
    @DisplayName("a listagem da tela lê o nome do concedente sem sessão aberta")
    void listagemLeONomeDoConcedente() {
        UserEntity user = usuario("user-1");
        GroupEntity grupo = grupo("GESTORES-FROTA");
        vincular(user, grupo);
        concessao(grupo, ACAO, true);

        List<EffectiveCapability> capacidades =
                capabilityReader.grantedTo(subjectLoader.byId("user-1").orElseThrow(), RECURSO);

        assertThat(capacidades).singleElement().satisfies(c -> {
            assertThat(c.grantedByName()).isEqualTo("GESTORES-FROTA");
            assertThat(c.grantedByType())
                    .as("o discriminador precisa sobreviver ao proxy do Hibernate")
                    .isEqualTo("Group");
        });
    }

    @Test
    @DisplayName("o filtro por recurso acontece na consulta, não em memória")
    void filtroPorRecursoNaConsulta() {
        ResourceEntity outro = resourceRepository.save(ResourceEntity.builder()
                .id("resource-2").name("tms.pneu").description("Pneu").active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        UserEntity user = usuario("user-1");
        concessao(user, ACAO, true);
        concessaoEm(user, outro, "instalar", true);

        assertThat(capabilityReader.grantedTo(subjectLoader.byId("user-1").orElseThrow(), RECURSO))
                .extracting(EffectiveCapability::resource)
                .containsExactly(RECURSO);

        assertThat(capabilityReader.grantedTo(subjectLoader.byId("user-1").orElseThrow()))
                .as("sem filtro, devolve as duas")
                .hasSize(2);
    }

    // ------------------------------------------------------------------ apoio

    private void concessao(br.com.archbase.security.persistence.SecurityEntity destinatario,
                           String nomeDaAcao, boolean acaoAtiva) {
        concessaoEm(destinatario, recurso, nomeDaAcao, acaoAtiva);
    }

    private void concessaoEm(br.com.archbase.security.persistence.SecurityEntity destinatario,
                             ResourceEntity emQualRecurso, String nomeDaAcao, boolean acaoAtiva) {
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

    private void vincular(UserEntity user, GroupEntity grupo) {
        Set<UserGroupEntity> vinculos =
                user.getGroups() == null ? new HashSet<>() : new HashSet<>(user.getGroups());
        vinculos.add(UserGroupEntity.builder()
                .id("ug-" + sequencia.incrementAndGet()).user(user).group(grupo).build());
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
