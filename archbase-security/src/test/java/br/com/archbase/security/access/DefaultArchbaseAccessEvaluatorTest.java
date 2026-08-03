package br.com.archbase.security.access;

import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.SecurityEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.UserGroupEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O núcleo da decisão de acesso — os portões e, sobretudo, os motivos.
 *
 * <p>A decisão em si já está coberta por {@code ArchbaseSecurityServiceTest} e pela caracterização
 * em {@code AutorizacaoComportamentoAtualTest}, que continuam verdes com este motor: é essa
 * combinação que demonstra que a fase A não mudou comportamento. O que se testa aqui é o que
 * <b>não existia antes</b> — o código de motivo, o concedente e a cadeia de portões.
 */
@DisplayName("DefaultArchbaseAccessEvaluator")
class DefaultArchbaseAccessEvaluatorTest {

    private static final String RECURSO = "tms.ordemservico";
    private static final String ACAO = "aprovar_custo";

    private PermissionJpaRepository permissionRepository;
    private DefaultArchbaseAccessEvaluator evaluator;

    @BeforeEach
    void setUp() {
        permissionRepository = mock(PermissionJpaRepository.class);
        evaluator = new DefaultArchbaseAccessEvaluator(permissionRepository);
    }

    @Nested
    @DisplayName("portão IDENTITY")
    class Identidade {

        @Test
        @DisplayName("principal não resolvível nega com PRINCIPAL_NOT_SUPPORTED, sem consultar o catálogo")
        void principalNaoResolvivel() {
            AccessDecision decisao = evaluator.decide(null, AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isFalse();
            assertThat(decisao.deniedAt()).isEqualTo(Gate.IDENTITY);
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.PRINCIPAL_NOT_SUPPORTED);
            assertThat(decisao.message()).contains("UserDetailsService");
            verify(permissionRepository, never())
                    .findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString());
        }

        @Test
        @DisplayName("isAdministrator nulo não interrompe a decisão — segue para o catálogo")
        void administradorNuloSegueParaOCatalogo() {
            // BO_ADMINISTRADOR é nulável, então o caso acontece em dados reais. Nulo é tratado como
            // "não é administrador" — nunca como administrador, então não há como ganhar privilégio
            // por um campo em branco.
            //
            // Uma versão anterior deste core NEGAVA aqui, para reproduzir a NullPointerException do
            // código antigo. Era errado: o @RequireRole antigo usava Boolean.TRUE.equals e nunca
            // falhava, então negar tirava acesso de quem funcionava — e a negação reproduzia um
            // acidente, não uma decisão.
            UserEntity user = usuario("user-1", null);
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of(permissao(grupo("TIME-SAC"), null, null, null)));

            AccessDecision decisao = evaluator.decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed())
                    .as("tem a permissão concedida; a flag em branco não é motivo para negar")
                    .isTrue();
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.GRANTED);
        }

        @Test
        @DisplayName("isAdministrator nulo não concede o atalho de administrador")
        void administradorNuloNaoTemAtalho() {
            UserEntity user = usuario("user-1", null);
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of());

            AccessDecision decisao = evaluator.decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isFalse();
            assertThat(decisao.reasonCode())
                    .as("caiu no catálogo como qualquer não-administrador")
                    .isEqualTo(AccessReasonCodes.NO_GRANT);
        }
    }

    @Nested
    @DisplayName("portão GRANT")
    class Concessao {

        @Test
        @DisplayName("administrador concede sem consultar o catálogo")
        void administradorConcede() {
            AccessDecision decisao = evaluator.decide(
                    AccessSubject.of(usuario("admin-1", true)), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.GRANTED_ADMINISTRATOR);
            assertThat(decisao.grantedBy()).isNull();
            verify(permissionRepository, never())
                    .findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString());
        }

        @Test
        @DisplayName("administrador desativado cai no catálogo como qualquer um")
        void administradorDesativadoNaoTemAtalho() {
            UserEntity admin = usuario("admin-1", true);
            admin.setAccountDeactivated(true);
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of());

            AccessDecision decisao = evaluator.decide(AccessSubject.of(admin), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isFalse();
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.NO_GRANT);
        }

        @Test
        @DisplayName("usuário DESATIVADO não-administrador continua sendo avaliado pelo catálogo")
        void usuarioDesativadoNaoEBarradoAqui() {
            // Comportamento herdado que surpreende e que a fase A preserva de propósito:
            // isEnabled() só era consultado no atalho do administrador. Quem barra a conta
            // desativada é o filtro de autenticação, antes de chegar aqui. Mudar isso é decisão de
            // produto, não de refactor — e quebraria quem depende do fluxo atual.
            UserEntity user = usuario("user-1", false);
            user.setAccountDeactivated(true);
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of(permissao(grupo("TIME-SAC"), null, null, null)));

            AccessDecision decisao = evaluator.decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
        }

        @Test
        @DisplayName("sem permissão nenhuma nega com NO_GRANT")
        void semPermissaoNega() {
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of());

            AccessDecision decisao = evaluator.decide(
                    AccessSubject.of(usuario("user-1", false)), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isFalse();
            assertThat(decisao.deniedAt()).isEqualTo(Gate.GRANT);
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.NO_GRANT);
        }

        @Test
        @DisplayName("a decisão diz qual grupo concedeu — a informação que o anyMatch descartava")
        void aDecisaoDizQuemConcedeu() {
            GroupEntity grupo = grupo("GESTORES-FROTA");
            UserEntity user = usuario("user-1", false);
            vincular(user, grupo);

            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of(permissao(grupo, null, null, null)));

            AccessDecision decisao = evaluator.decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.GRANTED);
            assertThat(decisao.grantedBy()).isEqualTo(grupo.getId());
            assertThat(decisao.grantedByName()).isEqualTo("GESTORES-FROTA");
        }
    }

    @Nested
    @DisplayName("portão SCOPE")
    class Escopo {

        @Test
        @DisplayName("permissão de outro tenant nega com OUT_OF_SCOPE, distinguindo de NO_GRANT")
        void tenantDiferenteNega() {
            // A distinção importa no diagnóstico: "não te concederam" e "concederam, mas para outro
            // tenant" levam a correções completamente diferentes.
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of(permissao(grupo("TIME-SAC"), "tenant-a", null, null)));

            AccessDecision decisao = evaluator.decide(
                    AccessSubject.of(usuario("user-1", false)),
                    AccessRequirement.of(RECURSO, ACAO, "tenant-b", null, null));

            assertThat(decisao.allowed()).isFalse();
            assertThat(decisao.deniedAt()).isEqualTo(Gate.SCOPE);
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.OUT_OF_SCOPE);
        }

        @Test
        @DisplayName("permissão sem escopo alcança qualquer tenant")
        void permissaoSemEscopoAlcancaTudo() {
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of(permissao(grupo("TIME-SAC"), null, null, null)));

            AccessDecision decisao = evaluator.decide(
                    AccessSubject.of(usuario("user-1", false)),
                    AccessRequirement.of(RECURSO, ACAO, "tenant-x", "company-x", "project-x"));

            assertThat(decisao.allowed()).isTrue();
        }

        @Test
        @DisplayName("entre várias permissões, a que alcança o escopo é a escolhida")
        void escolheAQueAlcanca() {
            GroupEntity foraDeEscopo = grupo("TIME-SAC");
            GroupEntity noEscopo = grupo("GESTORES-FROTA");
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of(
                            permissao(foraDeEscopo, "tenant-a", null, null),
                            permissao(noEscopo, "tenant-b", null, null)));

            AccessDecision decisao = evaluator.decide(
                    AccessSubject.of(usuario("user-1", false)),
                    AccessRequirement.of(RECURSO, ACAO, "tenant-b", null, null));

            assertThat(decisao.allowed()).isTrue();
            assertThat(decisao.grantedByName()).isEqualTo("GESTORES-FROTA");
        }
    }

    @Nested
    @DisplayName("cadeia de motivos")
    class Cadeia {

        @Test
        @DisplayName("uma concessão registra IDENTITY, SCOPE e GRANT, nessa ordem")
        void cadeiaDeUmaConcessao() {
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of(permissao(grupo("TIME-SAC"), null, null, null)));

            AccessDecision decisao = evaluator.decide(
                    AccessSubject.of(usuario("user-1", false)), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.chain()).extracting(GateOutcome::gate)
                    .containsExactly(Gate.IDENTITY, Gate.SCOPE, Gate.GRANT);
            assertThat(decisao.chain()).allMatch(GateOutcome::passed);
        }

        @Test
        @DisplayName("a cadeia para no portão que negou")
        void cadeiaParaOndeNegou() {
            when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                    .thenReturn(List.of());

            AccessDecision decisao = evaluator.decide(
                    AccessSubject.of(usuario("user-1", false)), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.chain()).extracting(GateOutcome::gate)
                    .containsExactly(Gate.IDENTITY, Gate.GRANT);
            assertThat(decisao.chain().get(1).passed()).isFalse();
        }

        @Test
        @DisplayName("a cadeia é imutável — quem recebe a decisão não a altera")
        void cadeiaImutavel() {
            AccessDecision decisao = evaluator.decide(null, AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.chain()).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("AccessSubject")
    class Sujeito {

        @Test
        @DisplayName("as origens são a união de usuário, grupos e perfil — sem precedência")
        void origensSaoUniao() {
            UserEntity user = usuario("user-1", false);
            user.setProfile(br.com.archbase.security.persistence.ProfileEntity.builder()
                    .id("profile-1").name("SUPERVISOR").description("Perfil").build());
            vincular(user, grupo("GESTORES-FROTA"));

            AccessSubject subject = AccessSubject.of(user);

            assertThat(subject.securityIds())
                    .containsExactlyInAnyOrder("user-1", "group-GESTORES-FROTA", "profile-1");
            assertThat(subject.profileName()).isEqualTo("SUPERVISOR");
            assertThat(subject.groupIds()).containsExactly("group-GESTORES-FROTA");
        }

        @Test
        @DisplayName("isAdministrator nulo não é administrador, e é distinguível de false")
        void nuloNaoEAdministrador() {
            AccessSubject nulo = AccessSubject.of(usuario("user-1", null));

            assertThat(nulo.administrator()).isNull();
            assertThat(nulo.isAdministrator()).isFalse();
            assertThat(AccessSubject.of(usuario("user-2", false)).administrator()).isFalse();
        }
    }

    // ------------------------------------------------------------------ apoio

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

    private static GroupEntity grupo(String nome) {
        return GroupEntity.builder().id("group-" + nome).name(nome).description("Grupo " + nome).build();
    }

    private static void vincular(UserEntity user, GroupEntity grupo) {
        Set<UserGroupEntity> vinculos =
                user.getGroups() == null ? new HashSet<>() : new HashSet<>(user.getGroups());
        vinculos.add(UserGroupEntity.builder()
                .id("ug-" + grupo.getId()).user(user).group(grupo).build());
        user.setGroups(vinculos);
    }

    private static PermissionEntity permissao(SecurityEntity destinatario,
                                              String tenantId, String companyId, String projectId) {
        ResourceEntity recurso = ResourceEntity.builder()
                .id("resource-1").name(RECURSO).description("Ordem de serviço").build();
        ActionEntity acao = ActionEntity.builder()
                .id("action-1").name(ACAO).description("Aprovar custo").resource(recurso).build();
        return PermissionEntity.builder()
                .id("permission-" + destinatario.getId())
                .security(destinatario)
                .action(acao)
                .tenantId1(tenantId)
                .companyId(companyId)
                .projectId(projectId)
                .build();
    }
}
