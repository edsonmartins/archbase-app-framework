package br.com.archbase.security.config;

import br.com.archbase.security.access.DefaultArchbaseAccessEvaluator;
import br.com.archbase.security.access.PersonaRestrictionEvaluator;
import br.com.archbase.security.access.ProfileRestrictionEvaluator;
import br.com.archbase.security.access.RestrictionEvaluator;
import br.com.archbase.security.access.RoleRestrictionEvaluator;
import br.com.archbase.security.annotation.HasPermission;
import br.com.archbase.security.annotations.RequirePersona;
import br.com.archbase.security.annotations.RequireProfile;
import br.com.archbase.security.annotations.RequireRole;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.service.ArchbaseSecurityService;
import br.com.archbase.security.spi.ArchbaseRoleResolver;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Caracterização do comportamento atual dos {@code AuthorizationManager}.
 *
 * <p><b>Para que serve.</b> O core único de autorização
 * ({@code MODELO_CORE_AUTORIZACAO.md}) troca o motor de decisão de um sistema em produção. Este
 * arquivo é a rede: ele descreve o que o framework decide <i>hoje</i>, defeitos inclusive, para que
 * qualquer mudança de comportamento durante o refactor apareça como teste vermelho e não como
 * incidente.
 *
 * <p><b>Como ler.</b> Um teste aqui não afirma que o comportamento é desejável — afirma que é o
 * atual. Onde o core vai mudar de propósito, o teste diz isso no comentário. Quando a mudança for
 * feita, o teste correspondente é reescrito no mesmo commit, de forma deliberada.
 *
 * <p><b>O que mudou na fase B, e o que não mudou.</b> Os colaboradores dos managers saíram do
 * lugar: a regra das trancas foi para os {@code RestrictionEvaluator} e a decisão para o
 * {@code ArchbaseAccessEvaluator}. A <b>montagem</b> destes testes acompanhou; as <b>asserções</b>
 * não mudaram nenhuma — é essa a linha que separa refactor de mudança de comportamento. Os cenários
 * passaram a exercitar o caminho real, com {@code PermissionJpaRepository} mockado no lugar do
 * serviço inteiro, o que os torna mais fiéis do que eram.
 *
 * <p>O comportamento de {@code hasPermission} também está coberto por
 * {@code ArchbaseSecurityServiceTest}; aqui ficam os quatro managers de anotação e as bordas que
 * ninguém testava.
 */
@DisplayName("Autorização — comportamento atual (caracterização)")
class AutorizacaoComportamentoAtualTest {

    private PermissionJpaRepository permissionRepository;

    @BeforeEach
    void setUp() {
        permissionRepository = mock(PermissionJpaRepository.class);
    }

    /** Serviço real, com o repositório mockado e as trancas indicadas registradas. */
    private ArchbaseSecurityService servicoCom(RestrictionEvaluator... trancas) {
        ArchbaseSecurityService service = new ArchbaseSecurityService();
        ReflectionTestUtils.setField(service, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(service, "accessEvaluator",
                new DefaultArchbaseAccessEvaluator(permissionRepository, List.of(trancas)));
        return service;
    }

    private void catalogoResponde(PermissionEntity... permissoes) {
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                .thenReturn(List.of(permissoes));
    }

    // ------------------------------------------------------------------ @RequireProfile

    @Nested
    @DisplayName("ProfileAuthorizationManager")
    class Perfil {

        static class Alvo {
            @RequireProfile("ADMIN")
            public void exigeAdmin() {
            }

            @RequireProfile(value = "ADMIN", allowSystemAdmin = false)
            public void exigeAdminSemIsencao() {
            }

            @RequireProfile(value = "ADMIN", resource = "PRODUTO", action = "VIEW")
            public void exigeAdminEPermissao() {
            }

            @RequireProfile(value = {"ADMIN", "GERENTE"}, requireAll = true)
            public void exigeAmbos() {
            }

            public void semAnotacao() {
            }
        }

        private ProfileAuthorizationManager manager() {
            return new ProfileAuthorizationManager(servicoCom(new ProfileRestrictionEvaluator()));
        }

        @Test
        @DisplayName("administrador passa sem ter o perfil exigido — allowSystemAdmin nasce true")
        void administradorIsentoPorPadrao() throws Exception {
            // O ponto mais sensível do refactor. Sob a regra "portões só negam", o administrador
            // seria barrado por um @RequireProfile que ele não satisfaz. O core continua honrando
            // allowSystemAdmin como isenção declarada DESTA restrição.
            UserEntity admin = usuario("admin-1", true);
            admin.setProfile(perfil("OPERADOR"));

            assertThat(permitido(manager(), admin, Alvo.class, "exigeAdmin")).isTrue();
        }

        @Test
        @DisplayName("com allowSystemAdmin=false o administrador é avaliado como qualquer um")
        void administradorSemIsencaoExplicita() throws Exception {
            // E este é o teste que obrigou o atalho de administrador a sair do topo do avaliador:
            // no lugar errado, ele transformaria a isenção declarada por anotação em desvio global.
            UserEntity admin = usuario("admin-1", true);
            admin.setProfile(perfil("OPERADOR"));

            assertThat(permitido(manager(), admin, Alvo.class, "exigeAdminSemIsencao")).isFalse();
        }

        @Test
        @DisplayName("usuário com o perfil exigido passa")
        void perfilCorrespondentePassa() throws Exception {
            UserEntity user = usuario("user-1", false);
            user.setProfile(perfil("ADMIN"));

            assertThat(permitido(manager(), user, Alvo.class, "exigeAdmin")).isTrue();
        }

        @Test
        @DisplayName("usuário sem perfil nenhum é negado")
        void semPerfilNega() throws Exception {
            assertThat(permitido(manager(), usuario("user-1", false), Alvo.class, "exigeAdmin")).isFalse();
        }

        @Test
        @DisplayName("requireAll com um perfil por usuário nunca passa com dois exigidos")
        void requireAllComUmUnicoPerfil() throws Exception {
            // O modelo permite um perfil por usuário, então requireAll=true com dois valores é
            // insatisfazível por construção. Caracterizado para que o core não o "conserte" por
            // acidente ao unificar a comparação.
            UserEntity user = usuario("user-1", false);
            user.setProfile(perfil("ADMIN"));

            assertThat(permitido(manager(), user, Alvo.class, "exigeAmbos")).isFalse();
        }

        @Test
        @DisplayName("com resource declarado, a restrição consulta o catálogo — portão 3 antes do 5")
        void comResourceDelegaAoCatalogo() throws Exception {
            // Restrição e concessão estavam misturadas dentro do manager. Agora são dois portões do
            // mesmo requisito, na mesma ordem de sempre, e o resultado observável é idêntico.
            catalogoResponde();

            UserEntity user = usuario("user-1", false);
            user.setProfile(perfil("ADMIN"));

            assertThat(permitido(manager(), user, Alvo.class, "exigeAdminEPermissao")).isFalse();
            verify(permissionRepository)
                    .findBySecurityIdsAndActionNameAndResourceName(anySet(), eq("VIEW"), eq("PRODUTO"));
        }

        @Test
        @DisplayName("com resource declarado e permissão concedida, passa")
        void comResourceEPermissaoPassa() throws Exception {
            catalogoResponde(permissao(grupo("TIME-SAC"), "PRODUTO", "VIEW", null));

            UserEntity user = usuario("user-1", false);
            user.setProfile(perfil("ADMIN"));

            assertThat(permitido(manager(), user, Alvo.class, "exigeAdminEPermissao")).isTrue();
        }

        @Test
        @DisplayName("sem o perfil, o catálogo nem é consultado — a restrição decide antes")
        void semPerfilNaoConsultaCatalogo() throws Exception {
            UserEntity user = usuario("user-1", false);
            user.setProfile(perfil("OPERADOR"));

            assertThat(permitido(manager(), user, Alvo.class, "exigeAdminEPermissao")).isFalse();
            verify(permissionRepository, never())
                    .findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString());
        }

        @Test
        @DisplayName("pointcut sem anotação resolvível nega")
        void semAnotacaoNega() throws Exception {
            assertThat(permitido(manager(), usuario("user-1", false), Alvo.class, "semAnotacao")).isFalse();
        }

        @Test
        @DisplayName("principal que não é UserEntity nega")
        void principalEstranhoNega() throws Exception {
            // Antes: ClassCastException engolida pelo catch. Agora: negação com
            // PRINCIPAL_NOT_SUPPORTED. Mesma decisão, motivo em vez de stack trace.
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getPrincipal()).thenReturn("um-principal-qualquer");

            AuthorizationDecision decisao =
                    manager().authorize(() -> auth, invocacao(Alvo.class, "exigeAdmin"));

            assertThat(decisao.isGranted()).isFalse();
        }

        @Test
        @DisplayName("isAdministrator nulo é tratado como não-administrador, e o perfil decide")
        void administradorNuloNaoEAdministrador() throws Exception {
            // MUDANÇA DELIBERADA DE COMPORTAMENTO, e a única do core.
            //
            // Antes, `allowSystemAdmin() && user.getIsAdministrator()` desempacotava nulo e
            // lançava NullPointerException, capturada e convertida em negação. Este teste afirmava
            // essa negação.
            //
            // Mas a negação reproduzia um ACIDENTE, não uma decisão — e não era sequer uniforme:
            // o @RequireRole antigo usava Boolean.TRUE.equals e nunca falhava, então o mesmo
            // usuário passava lá e era barrado aqui. Quem tem o perfil exigido e a flag em branco
            // deve passar; nulo nunca significa "é administrador", então nenhum privilégio é
            // ganho.
            UserEntity user = usuario("user-1", false);
            user.setIsAdministrator(null);
            user.setProfile(perfil("ADMIN"));

            assertThat(permitido(manager(), user, Alvo.class, "exigeAdmin"))
                    .as("tem o perfil ADMIN exigido")
                    .isTrue();
        }

        @Test
        @DisplayName("isAdministrator nulo não isenta ninguém de tranca alguma")
        void administradorNuloNaoIsenta() throws Exception {
            UserEntity user = usuario("user-1", false);
            user.setIsAdministrator(null);
            user.setProfile(perfil("OPERADOR"));

            assertThat(permitido(manager(), user, Alvo.class, "exigeAdmin"))
                    .as("nulo não vira isenção de administrador")
                    .isFalse();
        }
    }

    // ------------------------------------------------------------------ @RequirePersona

    @Nested
    @DisplayName("PersonaAuthorizationManager")
    class Persona {

        static class Alvo {
            @RequirePersona("PLATFORM_ADMIN")
            public void exigePlatformAdmin() {
            }

            @RequirePersona(value = "PLATFORM_ADMIN", allowSystemAdmin = false)
            public void exigePlatformAdminSemIsencao() {
            }

            @RequirePersona(value = "DESPACHANTE", allowSystemAdmin = false)
            public void exigePersonaCustomizada() {
            }
        }

        private PersonaAuthorizationManager manager() {
            return new PersonaAuthorizationManager(servicoCom(new PersonaRestrictionEvaluator()));
        }

        @Test
        @DisplayName("administrador passa sem ter a persona — allowSystemAdmin nasce true")
        void administradorIsentoPorPadrao() throws Exception {
            UserEntity admin = usuario("admin-1", true);
            admin.setProfile(perfil("OPERADOR"));

            assertThat(permitido(manager(), admin, Alvo.class, "exigePlatformAdmin")).isTrue();
        }

        @Test
        @DisplayName("o mapeamento de personas é uma tabela fixa de e-commerce no código")
        void mapeamentoFixoDeOutroDominio() throws Exception {
            // PLATFORM_ADMIN/STORE_ADMIN/CUSTOMER/DRIVER estão embutidos no framework — vocabulário
            // de outro domínio. O perfil ADMIN casa com a persona PLATFORM_ADMIN por essa tabela,
            // não por configuração do cliente. Preservado tal e qual na migração para o core.
            UserEntity user = usuario("user-1", false);
            user.setProfile(perfil("ADMIN"));

            assertThat(permitido(manager(), user, Alvo.class, "exigePlatformAdminSemIsencao")).isTrue();
        }

        @Test
        @DisplayName("persona fora da tabela só casa quando o perfil tem o mesmo nome")
        void personaCustomizadaCasaPeloNomeDoPerfil() throws Exception {
            UserEntity casa = usuario("user-1", false);
            casa.setProfile(perfil("DESPACHANTE"));
            assertThat(permitido(manager(), casa, Alvo.class, "exigePersonaCustomizada")).isTrue();

            UserEntity naoCasa = usuario("user-2", false);
            naoCasa.setProfile(perfil("OPERADOR"));
            assertThat(permitido(manager(), naoCasa, Alvo.class, "exigePersonaCustomizada")).isFalse();
        }

        @Test
        @DisplayName("usuário sem perfil é negado")
        void semPerfilNega() throws Exception {
            assertThat(permitido(manager(), usuario("user-1", false), Alvo.class,
                    "exigePlatformAdminSemIsencao")).isFalse();
        }
    }

    // ------------------------------------------------------------------ @RequireRole

    @Nested
    @DisplayName("RoleAuthorizationManager")
    class Papel {

        static class Alvo {
            @RequireRole("GESTOR")
            public void exigeGestor() {
            }

            @RequireRole(value = "GESTOR", allowSystemAdmin = false)
            public void exigeGestorSemIsencao() {
            }

            @RequireRole(value = "GESTOR", requirePlatformAdmin = true, allowSystemAdmin = false)
            public void exigeAdminDaPlataforma() {
            }
        }

        @Test
        @DisplayName("sem resolver e política permit, a anotação libera ignorando seus valores")
        void semResolverComPermitLibera() throws Exception {
            // É o estado do gestor-rq: 15 @RequireRole e nenhum ArchbaseRoleResolver. A anotação
            // não é controle de acesso nenhum. O core não muda isso sem a política virar deny.
            RoleAuthorizationManager manager = roleManager(List.of(), "permit");

            assertThat(permitido(manager, usuario("user-1", false), Alvo.class, "exigeGestorSemIsencao")).isTrue();
        }

        @Test
        @DisplayName("sem resolver e política deny, nega")
        void semResolverComDenyNega() throws Exception {
            RoleAuthorizationManager manager = roleManager(List.of(), "deny");

            assertThat(permitido(manager, usuario("user-1", false), Alvo.class, "exigeGestorSemIsencao")).isFalse();
        }

        @Test
        @DisplayName("com resolver, compara as roles devolvidas")
        void comResolverCompara() throws Exception {
            RoleAuthorizationManager comPapel = roleManager(List.of(u -> Set.of("GESTOR")), "permit");
            assertThat(permitido(comPapel, usuario("user-1", false), Alvo.class, "exigeGestorSemIsencao")).isTrue();

            RoleAuthorizationManager semPapel = roleManager(List.of(u -> Set.of("OPERADOR")), "permit");
            assertThat(permitido(semPapel, usuario("user-2", false), Alvo.class, "exigeGestorSemIsencao")).isFalse();
        }

        @Test
        @DisplayName("as roles de vários resolvers são unidas")
        void variosResolversSaoUnidos() throws Exception {
            RoleAuthorizationManager manager = roleManager(
                    List.of(u -> Set.of("OPERADOR"), u -> Set.of("GESTOR")), "permit");

            assertThat(permitido(manager, usuario("user-1", false), Alvo.class, "exigeGestorSemIsencao")).isTrue();
        }

        @Test
        @DisplayName("administrador passa sem ter a role — allowSystemAdmin nasce true")
        void administradorIsentoPorPadrao() throws Exception {
            RoleAuthorizationManager manager = roleManager(List.of(u -> Set.of("OPERADOR")), "deny");

            assertThat(permitido(manager, usuario("admin-1", true), Alvo.class, "exigeGestor")).isTrue();
        }

        @Test
        @DisplayName("requirePlatformAdmin nega quem não é administrador, antes de olhar roles")
        void exigeAdministradorDaPlataforma() throws Exception {
            RoleAuthorizationManager manager = roleManager(List.of(u -> Set.of("GESTOR")), "permit");

            assertThat(permitido(manager, usuario("user-1", false), Alvo.class, "exigeAdminDaPlataforma")).isFalse();
            assertThat(permitido(manager, usuario("admin-1", true), Alvo.class, "exigeAdminDaPlataforma")).isTrue();
        }

        @Test
        @DisplayName("usuário desativado é negado antes de qualquer role")
        void usuarioDesativadoNega() throws Exception {
            // Em @RequireRole a conta desativada é verificada ANTES da isenção de administrador —
            // ordem diferente de @RequireProfile e @RequirePersona. A diferença é preservada.
            RoleAuthorizationManager manager = roleManager(List.of(u -> Set.of("GESTOR")), "permit");

            UserEntity user = usuario("user-1", false);
            user.setAccountDeactivated(true);

            assertThat(permitido(manager, user, Alvo.class, "exigeGestorSemIsencao")).isFalse();
        }

        private RoleAuthorizationManager roleManager(List<ArchbaseRoleResolver> resolvers, String politica) {
            RoleRestrictionEvaluator tranca = new RoleRestrictionEvaluator();
            ReflectionTestUtils.setField(tranca, "roleResolvers", resolvers);
            ReflectionTestUtils.setField(tranca, "noResolverPolicy", politica);
            return new RoleAuthorizationManager(servicoCom(tranca));
        }
    }

    // ------------------------------------------------------------------ @HasPermission

    @Nested
    @DisplayName("CustomAuthorizationManager")
    class Capacidade {

        @br.com.archbase.security.annotation.ArchbaseResource("PRODUTO")
        static class AlvoComRecursoNaClasse {

            @HasPermission(action = "VIEW", description = "Ver produto")
            public void verProduto() {
            }
        }

        @Test
        @DisplayName("o recurso declarado na classe vale na hora de decidir, não só ao catalogar")
        void recursoDaClasseValeNaDecisao() throws Exception {
            // O padrão recomendado na documentação: @ArchbaseResource na classe, @HasPermission
            // sem resource no método. Se a herança só acontecer na varredura, a capacidade é
            // catalogada e aparece no admin — e mesmo assim nega todo não-administrador, porque o
            // requisito chega sem recurso e não há o que consultar.
            catalogoResponde(permissao(grupo("TIME-SAC"), "PRODUTO", "VIEW", null));

            assertThat(permitido(manager(), usuario("user-1", false), AlvoComRecursoNaClasse.class, "verProduto"))
                    .isTrue();
            verify(permissionRepository)
                    .findBySecurityIdsAndActionNameAndResourceName(anySet(), eq("VIEW"), eq("PRODUTO"));
        }

        static class Alvo {
            @HasPermission(action = "VIEW", resource = "PRODUTO", description = "Ver produto")
            public void verProduto() {
            }

            @HasPermission(action = "EDIT", resource = "PRODUTO", description = "Editar",
                    tenantId = "tenant-fixo")
            public void editarProdutoDeTenantFixo() {
            }

            public void semAnotacao() {
            }
        }

        private CustomAuthorizationManager manager() {
            return new CustomAuthorizationManager(servicoCom());
        }

        @Test
        @DisplayName("delega ao catálogo e devolve o que ele disser")
        void delegaAoCatalogo() throws Exception {
            catalogoResponde(permissao(grupo("TIME-SAC"), "PRODUTO", "VIEW", null));

            assertThat(permitido(manager(), usuario("user-1", false), Alvo.class, "verProduto")).isTrue();
            verify(permissionRepository)
                    .findBySecurityIdsAndActionNameAndResourceName(anySet(), eq("VIEW"), eq("PRODUTO"));
        }

        @Test
        @DisplayName("sem permissão no catálogo, nega")
        void semPermissaoNega() throws Exception {
            catalogoResponde();

            assertThat(permitido(manager(), usuario("user-1", false), Alvo.class, "verProduto")).isFalse();
        }

        @Test
        @DisplayName("o tenant declarado na anotação vence o contexto")
        void tenantDaAnotacaoVence() throws Exception {
            // A permissão vale só em tenant-fixo, que é o que a anotação declara: passa.
            catalogoResponde(permissao(grupo("TIME-SAC"), "PRODUTO", "EDIT", "tenant-fixo"));
            assertThat(permitido(manager(), usuario("user-1", false), Alvo.class, "editarProdutoDeTenantFixo"))
                    .isTrue();

            // A mesma permissão em outro tenant: não alcança.
            catalogoResponde(permissao(grupo("TIME-SAC"), "PRODUTO", "EDIT", "outro-tenant"));
            assertThat(permitido(manager(), usuario("user-2", false), Alvo.class, "editarProdutoDeTenantFixo"))
                    .isFalse();
        }

        @Test
        @DisplayName("erro ao avaliar vira negação, não liberação")
        void erroNaAvaliacaoNega() throws Exception {
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenThrow(new IllegalStateException("falha de consulta"));

            assertThat(permitido(manager(), usuario("user-1", false), Alvo.class, "verProduto")).isFalse();
        }

        @Test
        @DisplayName("pointcut sem anotação resolvível nega")
        void semAnotacaoNega() throws Exception {
            assertThat(permitido(manager(), usuario("user-1", false), Alvo.class, "semAnotacao")).isFalse();
        }
    }

    // ------------------------------------------------------------------ apoio

    private static boolean permitido(AuthorizationManager<MethodInvocation> manager,
                                     UserEntity user, Class<?> alvo, String metodo) throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(user);

        org.springframework.security.authorization.AuthorizationResult resultado =
                manager.authorize(() -> auth, invocacao(alvo, metodo));
        return resultado != null && resultado.isGranted();
    }

    private static UserEntity usuario(String id, Boolean administrador) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("Usuário " + id);
        user.setDescription("Usuário de teste");
        user.setEmail(id + "@exemplo.test");
        user.setIsAdministrator(administrador);
        user.setAccountDeactivated(false);
        user.setAccountLocked(false);
        return user;
    }

    private static ProfileEntity perfil(String nome) {
        return ProfileEntity.builder().id("profile-" + nome).name(nome).description("Perfil " + nome).build();
    }

    private static GroupEntity grupo(String nome) {
        return GroupEntity.builder().id("group-" + nome).name(nome).description("Grupo " + nome).build();
    }

    private static PermissionEntity permissao(GroupEntity destinatario, String recurso, String acao,
                                              String tenantId) {
        ResourceEntity resourceEntity = ResourceEntity.builder()
                .id("resource-1").name(recurso).description("Recurso de teste").build();
        ActionEntity actionEntity = ActionEntity.builder()
                .id("action-1").name(acao).description("Ação de teste").resource(resourceEntity).build();
        return PermissionEntity.builder()
                .id("permission-1")
                .security(destinatario)
                .action(actionEntity)
                .tenantId1(tenantId)
                .build();
    }

    private static MethodInvocation invocacao(Class<?> tipo, String nomeDoMetodo) throws Exception {
        Object alvo = tipo.getDeclaredConstructor().newInstance();
        Method method = tipo.getMethod(nomeDoMetodo);
        return new MethodInvocation() {
            @Override
            public Method getMethod() {
                return method;
            }

            @Override
            public Object[] getArguments() {
                return new Object[0];
            }

            @Override
            public Object proceed() {
                return null;
            }

            @Override
            public Object getThis() {
                return alvo;
            }

            @Override
            public AccessibleObject getStaticPart() {
                return method;
            }
        };
    }
}
