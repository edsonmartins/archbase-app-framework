package br.com.archbase.security.access;

import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.SecurityEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Nível mínimo e negação explícita — o que a fase D acrescenta ao core.
 *
 * <p>As duas responsabilidades que este arquivo protege:
 *
 * <ul>
 *   <li><b>Nada muda com os defaults.</b> Coluna vazia e portão desligado precisam produzir
 *       exatamente a decisão de antes, senão a entrega quebra sistemas em produção no dia da
 *       atualização.</li>
 *   <li><b>Ter a capacidade atribuída não basta.</b> Com o portão ligado, uma concessão legítima
 *       sobre uma capacidade acima do nível da pessoa é negada — que é a resposta ao risco de
 *       alguém atribuir uma capacidade sensível a quem não deveria.</li>
 * </ul>
 */
@DisplayName("Nível mínimo e negação explícita")
class NivelENegacaoTest {

    private static final String RECURSO = "tms.ordemservico";
    private static final String ACAO = "aprovar_custo";

    private PermissionJpaRepository permissionRepository;

    @BeforeEach
    void setUp() {
        permissionRepository = mock(PermissionJpaRepository.class);
    }

    // ------------------------------------------------------------------ escala

    @Nested
    @DisplayName("AccessLevel")
    class Escala {

        @Test
        @DisplayName("a ordem é READER < OPERATOR < SUPERVISOR < TENANT_ADMIN")
        void ordem() {
            assertThat(AccessLevel.SUPERVISOR.reaches(AccessLevel.OPERATOR)).isTrue();
            assertThat(AccessLevel.SUPERVISOR.reaches(AccessLevel.SUPERVISOR)).isTrue();
            assertThat(AccessLevel.OPERATOR.reaches(AccessLevel.SUPERVISOR)).isFalse();
            assertThat(AccessLevel.TENANT_ADMIN.reaches(AccessLevel.READER)).isTrue();
        }

        @Test
        @DisplayName("mínimo nulo é alcançado por qualquer nível — capacidade sem piso")
        void semMinimo() {
            assertThat(AccessLevel.READER.reaches(null)).isTrue();
        }

        @Test
        @DisplayName("rótulo desconhecido vira nulo, não um degrau qualquer")
        void rotuloDesconhecido() {
            // Nem o mais alto, que abriria acesso, nem o mais baixo, que o fecharia em silêncio.
            assertThat(AccessLevel.parse("GERENTE_SENIOR")).isNull();
            assertThat(AccessLevel.parse("")).isNull();
            assertThat(AccessLevel.parse(null)).isNull();
            assertThat(AccessLevel.parse("  supervisor  ")).isEqualTo(AccessLevel.SUPERVISOR);
        }
    }

    // ------------------------------------------------------------------ efeito

    @Nested
    @DisplayName("PermissionEffect")
    class Efeito {

        @Test
        @DisplayName("nulo é GRANT — é o que toda concessão existente significa")
        void nuloEGrant() {
            assertThat(PermissionEffect.parse(null)).isEqualTo(PermissionEffect.GRANT);
            assertThat(PermissionEffect.parse("")).isEqualTo(PermissionEffect.GRANT);
        }

        @Test
        @DisplayName("valor desconhecido também é GRANT, e não DENY")
        void desconhecidoEGrant() {
            // Parece contraintuitivo num módulo de segurança, mas o contrário é pior: um erro de
            // digitação transformaria uma concessão legítima numa negação que ninguém pediu.
            assertThat(PermissionEffect.parse("NEGAR")).isEqualTo(PermissionEffect.GRANT);
            assertThat(PermissionEffect.parse("deny")).isEqualTo(PermissionEffect.DENY);
        }
    }

    // ------------------------------------------------------------------ compatibilidade

    @Nested
    @DisplayName("com os defaults, nada muda")
    class Compatibilidade {

        @Test
        @DisplayName("portão desligado ignora o nível, mesmo com mínimo declarado")
        void portaoDesligado() {
            UserEntity user = usuario("user-1");
            user.setProfile(perfil("OPERADOR", AccessLevel.OPERATOR));
            catalogoResponde(permissao(grupo("TIME-SAC"), AccessLevel.TENANT_ADMIN, null));

            AccessDecision decisao = avaliador(false, "READER")
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
            assertThat(decisao.chain()).extracting(GateOutcome::gate).doesNotContain(Gate.LEVEL);
        }

        @Test
        @DisplayName("effect nulo concede, como sempre concedeu")
        void efeitoNuloConcede() {
            catalogoResponde(permissao(grupo("TIME-SAC"), null, null));

            AccessDecision decisao = avaliador(false, "READER")
                    .decide(AccessSubject.of(usuario("user-1")), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
        }

        @Test
        @DisplayName("portão ligado, mas capacidade sem mínimo: passa")
        void ligadoSemMinimo() {
            // O estado de toda ação existente no dia em que a coluna entra.
            catalogoResponde(permissao(grupo("TIME-SAC"), null, null));

            AccessDecision decisao = avaliador(true, "READER")
                    .decide(AccessSubject.of(usuario("user-1")), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
            assertThat(decisao.chain()).extracting(GateOutcome::gate).contains(Gate.LEVEL);
        }
    }

    // ------------------------------------------------------------------ portão LEVEL

    @Nested
    @DisplayName("portão LEVEL")
    class Nivel {

        @Test
        @DisplayName("concessão legítima é negada quando o nível não alcança")
        void nivelInsuficienteNega() {
            // A resposta ao risco central: ter a capacidade atribuída não basta.
            UserEntity user = usuario("user-1");
            user.setProfile(perfil("ATENDIMENTO", AccessLevel.OPERATOR));
            catalogoResponde(permissao(grupo("TIME-SAC"), AccessLevel.SUPERVISOR, null));

            AccessDecision decisao = avaliador(true, "READER")
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isFalse();
            assertThat(decisao.deniedAt()).isEqualTo(Gate.LEVEL);
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.LEVEL_TOO_LOW);
            assertThat(decisao.message()).contains("não basta");
        }

        @Test
        @DisplayName("nível suficiente passa")
        void nivelSuficientePassa() {
            UserEntity user = usuario("user-1");
            user.setProfile(perfil("SUPERVISOR", AccessLevel.SUPERVISOR));
            catalogoResponde(permissao(grupo("TIME-SAC"), AccessLevel.SUPERVISOR, null));

            AccessDecision decisao = avaliador(true, "READER")
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
        }

        @Test
        @DisplayName("usuário sem perfil cai no nível padrão configurado")
        void semPerfilCaiNoPadrao() {
            catalogoResponde(permissao(grupo("TIME-SAC"), AccessLevel.OPERATOR, null));

            assertThat(avaliador(true, "READER")
                    .decide(AccessSubject.of(usuario("user-1")), AccessRequirement.of(RECURSO, ACAO))
                    .allowed())
                    .as("READER não alcança OPERATOR")
                    .isFalse();

            assertThat(avaliador(true, "SUPERVISOR")
                    .decide(AccessSubject.of(usuario("user-2")), AccessRequirement.of(RECURSO, ACAO))
                    .allowed())
                    .as("com o padrão em SUPERVISOR, alcança")
                    .isTrue();
        }

        @Test
        @DisplayName("padrão inválido cai em READER, e não no degrau mais alto")
        void padraoInvalidoCaiEmReader() {
            // Um erro de digitação na configuração não pode virar liberação geral.
            catalogoResponde(permissao(grupo("TIME-SAC"), AccessLevel.OPERATOR, null));

            assertThat(avaliador(true, "CHEFAO")
                    .decide(AccessSubject.of(usuario("user-1")), AccessRequirement.of(RECURSO, ACAO))
                    .allowed()).isFalse();
        }

        @Test
        @DisplayName("o resolver da aplicação vence o perfil")
        void resolverVenceOPerfil() {
            UserEntity user = usuario("user-1");
            user.setProfile(perfil("ATENDIMENTO", AccessLevel.READER));
            catalogoResponde(permissao(grupo("TIME-SAC"), AccessLevel.SUPERVISOR, null));

            ArchbaseAccessLevelPolicy policy = policy(true, "READER");
            ReflectionTestUtils.setField(policy, "resolvers",
                    List.<ArchbaseAccessLevelResolver>of(s -> AccessLevel.SUPERVISOR));

            AccessDecision decisao = new DefaultArchbaseAccessEvaluator(
                    permissionRepository, List.of(), policy)
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
        }

        @Test
        @DisplayName("resolver que devolve nulo deixa a resolução seguir para o perfil")
        void resolverNuloNaoInterfere() {
            UserEntity user = usuario("user-1");
            user.setProfile(perfil("SUPERVISOR", AccessLevel.SUPERVISOR));
            catalogoResponde(permissao(grupo("TIME-SAC"), AccessLevel.SUPERVISOR, null));

            ArchbaseAccessLevelPolicy policy = policy(true, "READER");
            ReflectionTestUtils.setField(policy, "resolvers",
                    List.<ArchbaseAccessLevelResolver>of(s -> null));

            AccessDecision decisao = new DefaultArchbaseAccessEvaluator(
                    permissionRepository, List.of(), policy)
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
        }

        @Test
        @DisplayName("com pisos em conflito, vale o MAIS ALTO")
        void pisosEmConflitoValeOMaisAlto() {
            // Nada no catálogo impede duas ações de mesmo nome sob o mesmo recurso. As duas casam a
            // consulta, e escolher uma arbitrariamente tornaria o piso não-determinístico: a mesma
            // requisição negaria ou permitiria conforme a ordem que o banco devolvesse. Um catálogo
            // inconsistente não pode AFROUXAR a exigência.
            UserEntity user = usuario("user-1");
            user.setProfile(perfil("OPERACOES", AccessLevel.OPERATOR));

            catalogoResponde(
                    permissao(grupo("A"), AccessLevel.OPERATOR, null),
                    permissao(grupo("B"), AccessLevel.SUPERVISOR, null));

            assertThat(avaliador(true, "READER")
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO))
                    .reasonCode())
                    .isEqualTo(AccessReasonCodes.LEVEL_TOO_LOW);
        }

        @Test
        @DisplayName("o concedente exibido é estável entre execuções idênticas")
        void concedenteEstavel() {
            UserEntity user = usuario("user-1");
            catalogoResponde(
                    permissao(grupo("ZZZ"), null, null),
                    permissao(grupo("AAA"), null, null));

            String primeiro = avaliador(false, "READER")
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO)).grantedByName();
            String segundo = avaliador(false, "READER")
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO)).grantedByName();

            assertThat(primeiro).isNotNull().isEqualTo(segundo);
        }

        @Test
        @DisplayName("administrador nunca é barrado pelo nível — é o topo da escala")
        void administradorNuncaBarrado() {
            UserEntity admin = usuario("admin-1");
            admin.setIsAdministrator(true);

            AccessDecision decisao = avaliador(true, "READER")
                    .decide(AccessSubject.of(admin), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.GRANTED_ADMINISTRATOR);
        }
    }

    // ------------------------------------------------------------------ negação

    @Nested
    @DisplayName("negação explícita")
    class Negacao {

        @Test
        @DisplayName("DENY no usuário vence GRANT no grupo")
        void denyVenceGrant() {
            // O caso que hoje obriga a criar um grupo paralelo só para excluir uma pessoa.
            catalogoResponde(
                    permissao(grupo("GESTORES-FROTA"), null, null),
                    negacao(usuario("user-1"), null));

            AccessDecision decisao = avaliador(false, "READER")
                    .decide(AccessSubject.of(usuario("user-1")), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isFalse();
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.EXPLICIT_DENY);
            assertThat(decisao.message()).contains("Usuário user-1");
        }

        @Test
        @DisplayName("DENY vence mesmo vindo depois na lista")
        void denyVenceIndependenteDaOrdem() {
            catalogoResponde(
                    permissao(grupo("A"), null, null),
                    permissao(grupo("B"), null, null),
                    negacao(grupo("C"), null));

            assertThat(avaliador(false, "READER")
                    .decide(AccessSubject.of(usuario("user-1")), AccessRequirement.of(RECURSO, ACAO))
                    .allowed()).isFalse();
        }

        @Test
        @DisplayName("DENY de outro tenant não afeta o tenant pedido")
        void denyForaDeEscopoNaoAfeta() {
            // A negação vale dentro do escopo em que foi declarada — mesma semântica da concessão.
            catalogoResponde(
                    permissao(grupo("GESTORES-FROTA"), null, null),
                    negacao(usuario("user-1"), "tenant-b"));

            AccessDecision decisao = avaliador(false, "READER")
                    .decide(AccessSubject.of(usuario("user-1")),
                            AccessRequirement.of(RECURSO, ACAO, "tenant-a", null, null));

            assertThat(decisao.allowed()).isTrue();
        }

        @Test
        @DisplayName("DENY sem escopo vence em qualquer tenant")
        void denySemEscopoVenceEmTodos() {
            catalogoResponde(
                    permissao(grupo("GESTORES-FROTA"), null, null),
                    negacao(usuario("user-1"), null));

            assertThat(avaliador(false, "READER")
                    .decide(AccessSubject.of(usuario("user-1")),
                            AccessRequirement.of(RECURSO, ACAO, "tenant-a", null, null))
                    .allowed()).isFalse();
        }

        @Test
        @DisplayName("a negação alcança o ADMINISTRADOR — a flag não a contorna")
        void denyAlcancaAdministrador() {
            // Antes, o atalho de administrador encerrava a decisão sem olhar o catálogo: o admin
            // aceitava criar um DENY sobre um administrador, gravava a linha, e ela não fazia
            // efeito nenhum. Uma promessa quebrada na interface, igual à do campo `active`.
            UserEntity admin = usuario("admin-1");
            admin.setIsAdministrator(true);

            when(permissionRepository.findDenialsBySecurityIdsAndActionNameAndResourceName(
                    anySet(), anyString(), anyString()))
                    .thenReturn(List.of(negacao(usuario("admin-1"), null)));

            AccessDecision decisao = avaliador(false, "READER")
                    .decide(AccessSubject.of(admin), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isFalse();
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.EXPLICIT_DENY);
        }

        @Test
        @DisplayName("sem negação, o administrador continua passando sem consultar concessões")
        void administradorSemNegacaoNaoConsultaConcessoes() {
            UserEntity admin = usuario("admin-1");
            admin.setIsAdministrator(true);

            when(permissionRepository.findDenialsBySecurityIdsAndActionNameAndResourceName(
                    anySet(), anyString(), anyString()))
                    .thenReturn(List.of());

            AccessDecision decisao = avaliador(false, "READER")
                    .decide(AccessSubject.of(admin), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.allowed()).isTrue();
            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.GRANTED_ADMINISTRATOR);
            verify(permissionRepository, never())
                    .findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString());
        }

        @Test
        @DisplayName("negação de outro tenant não alcança o administrador")
        void denyForaDeEscopoNaoAlcancaAdministrador() {
            UserEntity admin = usuario("admin-1");
            admin.setIsAdministrator(true);

            when(permissionRepository.findDenialsBySecurityIdsAndActionNameAndResourceName(
                    anySet(), anyString(), anyString()))
                    .thenReturn(List.of(negacao(usuario("admin-1"), "tenant-b")));

            AccessDecision decisao = avaliador(false, "READER")
                    .decide(AccessSubject.of(admin),
                            AccessRequirement.of(RECURSO, ACAO, "tenant-a", null, null));

            assertThat(decisao.allowed()).isTrue();
        }

        @Test
        @DisplayName("a negação é avaliada antes do nível — o motivo mais forte prevalece")
        void negacaoAntesDoNivel() {
            UserEntity user = usuario("user-1");
            user.setProfile(perfil("ATENDIMENTO", AccessLevel.READER));
            catalogoResponde(negacao(usuario("user-1"), null));

            AccessDecision decisao = avaliador(true, "READER")
                    .decide(AccessSubject.of(user), AccessRequirement.of(RECURSO, ACAO));

            assertThat(decisao.reasonCode()).isEqualTo(AccessReasonCodes.EXPLICIT_DENY);
        }
    }

    // ------------------------------------------------------------------ apoio

    private DefaultArchbaseAccessEvaluator avaliador(boolean nivelLigado, String nivelPadrao) {
        return new DefaultArchbaseAccessEvaluator(
                permissionRepository, List.of(), policy(nivelLigado, nivelPadrao));
    }

    private ArchbaseAccessLevelPolicy policy(boolean ligado, String padrao) {
        ArchbaseAccessLevelPolicy policy = new ArchbaseAccessLevelPolicy();
        ReflectionTestUtils.setField(policy, "enabled", ligado);
        ReflectionTestUtils.setField(policy, "defaultLevel", padrao);
        ReflectionTestUtils.setField(policy, "resolvers", List.of());
        return policy;
    }

    private void catalogoResponde(PermissionEntity... permissoes) {
        when(permissionRepository.findBySecurityIdsAndActionNameAndResourceName(anySet(), anyString(), anyString()))
                .thenReturn(List.of(permissoes));
    }

    private static UserEntity usuario(String id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("Usuário " + id);
        user.setDescription("Usuário de teste");
        user.setEmail(id + "@exemplo.test");
        user.setIsAdministrator(false);
        user.setAccountDeactivated(false);
        user.setAccountLocked(false);
        return user;
    }

    private static ProfileEntity perfil(String nome, AccessLevel nivel) {
        return ProfileEntity.builder()
                .id("profile-" + nome).name(nome).description("Perfil " + nome)
                .accessLevel(nivel).build();
    }

    private static GroupEntity grupo(String nome) {
        return GroupEntity.builder().id("group-" + nome).name(nome).description("Grupo " + nome).build();
    }

    private static PermissionEntity permissao(SecurityEntity destinatario, AccessLevel minimo, String tenantId) {
        return linha(destinatario, minimo, tenantId, PermissionEffect.GRANT);
    }

    private static PermissionEntity negacao(SecurityEntity destinatario, String tenantId) {
        return linha(destinatario, null, tenantId, PermissionEffect.DENY);
    }

    private static PermissionEntity linha(SecurityEntity destinatario, AccessLevel minimo,
                                          String tenantId, PermissionEffect efeito) {
        ResourceEntity recurso = ResourceEntity.builder()
                .id("resource-1").name(RECURSO).description("Ordem de serviço").active(true).build();
        ActionEntity acao = ActionEntity.builder()
                .id("action-1").name(ACAO).description("Aprovar custo").resource(recurso).active(true)
                .minimumLevel(minimo).build();
        return PermissionEntity.builder()
                .id("permission-" + destinatario.getId() + "-" + efeito)
                .security(destinatario)
                .action(acao)
                .tenantId1(tenantId)
                .effect(efeito)
                .build();
    }
}
