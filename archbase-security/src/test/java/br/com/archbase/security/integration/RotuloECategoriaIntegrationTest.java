package br.com.archbase.security.integration;

import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.PermissionWithTypesDto;
import br.com.archbase.security.domain.dto.ResoucePermissionsWithTypeDto;
import br.com.archbase.security.domain.dto.ResourceRegisterDto;
import br.com.archbase.security.domain.dto.SimpleActionDto;
import br.com.archbase.security.domain.dto.SimpleResourceDto;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.adapter.ActionPersistenceAdapter;
import br.com.archbase.security.adapter.ResourcePersistenceAdapter;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.ActionDependencyJpaRepository;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ResourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rótulo e categoria: um campo deixa de fazer três trabalhos.
 *
 * <p>{@code DESCRICAO} identificava a linha, explicava a ação e — com o {@code ->} que o cliente
 * quebra na exibição — agrupava. Como o {@code useArchbaseCrudSecurity} do archbase-react gera as
 * descrições em massa ("Criar X", "Editar X", "Listar X"), o catálogo acaba com centenas de linhas
 * quase idênticas e nada que as distinga.
 *
 * <p>O que estes testes protegem não é o campo novo — é a <b>compatibilidade</b>. Rótulo nulo tem de
 * significar "use a descrição", cliente antigo não pode apagar o que não conhece, e capacidade que
 * já tem rótulo não pode oscilar entre o que o código diz e o que a tela registra.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-rotulo-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Rótulo e categoria da capacidade (Spring + H2)")
class RotuloECategoriaIntegrationTest {

    private static final String OS = "tms.ordemservico";
    private static final String COCKPIT = "Cockpit";

    @Autowired
    ResourcePersistenceAdapter adapter;
    @Autowired
    ActionPersistenceAdapter actionAdapter;
    @Autowired
    ResourceService resourceService;
    @Autowired
    ActionJpaRepository actionRepository;
    @Autowired
    ResourceJpaRepository resourceRepository;
    @Autowired
    PermissionJpaRepository permissionRepository;
    @Autowired
    ActionDependencyJpaRepository dependencyRepository;
    @Autowired
    UserJpaRepository userRepository;

    private final AtomicInteger sequencia = new AtomicInteger();
    private ResourceEntity os;

    @BeforeEach
    void limpar() {
        dependencyRepository.deleteAll();
        permissionRepository.deleteAll();
        actionRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
        os = recurso("res-os", OS, TipoRecurso.API);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("compatibilidade da leitura")
    class Leitura {

        @Test
        @DisplayName("capacidade sem rótulo não ganha o campo — a resposta de antes, intacta")
        void semRotuloOmiteOCampo() {
            acao(os, "aprovar_custo", null, null);

            PermissionWithTypesDto capacidade = doCatalogo("aprovar_custo");

            assertThat(capacidade.getActionLabel()).isNull();
            assertThat(capacidade.getActionCategory()).isNull();
            // O que a tela mostra hoje continua vindo, e no mesmo campo.
            assertThat(capacidade.getActionDescription()).isEqualTo("Ação aprovar_custo");
        }

        @Test
        @DisplayName("rótulo e categoria chegam na listagem quando existem")
        void rotuloECategoriaChegam() {
            acao(os, "aprovar_custo", "Aprovar custo", "Custos");

            PermissionWithTypesDto capacidade = doCatalogo("aprovar_custo");

            assertThat(capacidade.getActionLabel()).isEqualTo("Aprovar custo");
            assertThat(capacidade.getActionCategory()).isEqualTo("Custos");
            assertThat(capacidade.getActionDescription()).isEqualTo("Ação aprovar_custo");
        }
    }

    @Nested
    @DisplayName("edição pelo admin")
    class Edicao {

        @Test
        @DisplayName("cliente que não conhece os campos NÃO apaga o que já está gravado")
        void clienteAntigoNaoApaga() {
            // A mesma regra do nível mínimo, e pelo mesmo motivo: todo cliente anterior salva sem
            // esses campos, e copiar o nulo que chega apagaria o rótulo a cada edição de descrição.
            ActionEntity acao = acao(os, "aprovar_custo", "Aprovar custo", "Custos");

            ActionDto semOsCampos = ActionDto.builder()
                    .id(acao.getId()).name("aprovar_custo").description("Outra descrição")
                    .active(true).build();
            actionAdapter.updateAction(acao.getId(), semOsCampos);

            ActionEntity depois = actionRepository.findById(acao.getId()).orElseThrow();
            assertThat(depois.getLabel()).isEqualTo("Aprovar custo");
            assertThat(depois.getCategory()).isEqualTo("Custos");
            assertThat(depois.getDescription()).isEqualTo("Outra descrição");
        }

        @Test
        @DisplayName("string vazia limpa — é a forma de tirar de propósito")
        void vazioLimpa() {
            ActionEntity acao = acao(os, "aprovar_custo", "Aprovar custo", "Custos");

            actionAdapter.updateAction(acao.getId(), ActionDto.builder()
                    .id(acao.getId()).name("aprovar_custo").description("Ação aprovar_custo")
                    .label("").category("").active(true).build());

            ActionEntity depois = actionRepository.findById(acao.getId()).orElseThrow();
            assertThat(depois.getLabel()).isNull();
            assertThat(depois.getCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("registro de tela")
    class RegistroDeTela {

        @Test
        @DisplayName("a tela declara rótulo e categoria, e eles nascem com a capacidade")
        void telaDeclara() {
            autenticarAdministrador();

            registrar(SimpleActionDto.builder()
                    .actionName("aprovar").actionDescription("Aprovar a ordem no cockpit")
                    .actionLabel("Aprovar").actionCategory("Operação").build());

            ActionEntity criada = actionRepository
                    .findByActionNameAndResourceName("aprovar", COCKPIT).orElseThrow();
            assertThat(criada.getLabel()).isEqualTo("Aprovar");
            assertThat(criada.getCategory()).isEqualTo("Operação");
        }

        @Test
        @DisplayName("capacidade existente SEM rótulo recebe o que a tela declara")
        void semeiaOQueNuncaFoiSemeado() {
            // Campo novo: nulo numa capacidade existente é ausência do campo na versão em que a
            // linha nasceu, não decisão de quem administra.
            autenticarAdministrador();
            registrar(SimpleActionDto.builder()
                    .actionName("aprovar").actionDescription("Aprovar").build());

            registrar(SimpleActionDto.builder()
                    .actionName("aprovar").actionDescription("Aprovar")
                    .actionLabel("Aprovar").actionCategory("Operação").build());

            ActionEntity depois = actionRepository
                    .findByActionNameAndResourceName("aprovar", COCKPIT).orElseThrow();
            assertThat(depois.getLabel()).isEqualTo("Aprovar");
        }

        @Test
        @DisplayName("capacidade que JÁ tem rótulo não é sobrescrita por outra tela")
        void naoOscilaEntreTelas() {
            // Duas telas podem registrar a mesma capacidade. Sobrescrever faria o rótulo alternar a
            // cada abertura, e quem administra veria o texto mudar sozinho.
            autenticarAdministrador();
            registrar(SimpleActionDto.builder()
                    .actionName("aprovar").actionDescription("Aprovar")
                    .actionLabel("Aprovar").build());

            registrar(SimpleActionDto.builder()
                    .actionName("aprovar").actionDescription("Aprovar")
                    .actionLabel("Confirmar").build());

            assertThat(actionRepository.findByActionNameAndResourceName("aprovar", COCKPIT)
                    .orElseThrow().getLabel()).isEqualTo("Aprovar");
        }

        @Test
        @DisplayName("payload antigo, sem os campos, não mexe em nada")
        void payloadAntigoNaoMexe() {
            autenticarAdministrador();
            registrar(SimpleActionDto.builder()
                    .actionName("aprovar").actionDescription("Aprovar")
                    .actionLabel("Aprovar").actionCategory("Operação").build());

            registrar(SimpleActionDto.builder()
                    .actionName("aprovar").actionDescription("Aprovar").build());

            ActionEntity depois = actionRepository
                    .findByActionNameAndResourceName("aprovar", COCKPIT).orElseThrow();
            assertThat(depois.getLabel()).isEqualTo("Aprovar");
            assertThat(depois.getCategory()).isEqualTo("Operação");
        }
    }

    // ---- fixtura ----

    private void registrar(SimpleActionDto acao) {
        resourceService.registerResource(ResourceRegisterDto.builder()
                .resource(SimpleResourceDto.builder()
                        .resourceName(COCKPIT).resourceDescription("Cockpit do vendedor").build())
                .actions(List.of(acao))
                .build());
    }

    private PermissionWithTypesDto doCatalogo(String nomeDaAcao) {
        List<ResoucePermissionsWithTypeDto> catalogo = adapter.findAllResourcesPermissions();
        return catalogo.stream()
                .flatMap(r -> r.getPermissions().stream())
                .filter(c -> nomeDaAcao.equals(c.getActionName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Capacidade ausente: " + nomeDaAcao));
    }

    private ResourceEntity recurso(String id, String nome, TipoRecurso tipo) {
        return resourceRepository.save(ResourceEntity.builder()
                .id(id).name(nome).description("Recurso " + nome).active(true).type(tipo)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private ActionEntity acao(ResourceEntity recurso, String nome, String label, String category) {
        return actionRepository.save(ActionEntity.builder()
                .id("act-" + sequencia.incrementAndGet()).name(nome).description("Ação " + nome)
                .label(label).category(category)
                .resource(recurso).active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private void autenticarAdministrador() {
        UserEntity admin = userRepository.save(UserEntity.builder()
                .id("admin-1").name("Admin").description("Administrador de teste")
                .userName("admin").email("admin@exemplo.test").password("irrelevante")
                .isAdministrator(true).accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true)
                .allowPasswordChange(true).allowMultipleLogins(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }
}
