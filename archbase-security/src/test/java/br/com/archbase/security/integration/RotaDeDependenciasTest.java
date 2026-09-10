package br.com.archbase.security.integration;

import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.ActionDependencyJpaRepository;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseCapabilityDependencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O <b>roteamento</b> de {@code /api/v1/resource/permissions/**} — não o que cada handler devolve.
 *
 * <p><b>Por que existe.</b> As rotas de permissão dividem um prefixo e diferem só na forma:
 * {@code /permissions} (catálogo), {@code /permissions/{recurso}} (autoatendimento),
 * {@code /permissions/security/{id}} (por entidade) e agora
 * {@code /permissions/dependencies/{actionId}} (fecho de dependências). Uma delas engolir a outra é
 * exatamente o tipo de defeito que compila, passa em todo teste de serviço e só aparece como 404 —
 * ou, pior, como a resposta errada — no ambiente real. Foi assim que os controllers do analytics
 * deixaram de ser handler e devolveram 404 em todas as rotas, com o build verde.
 *
 * <p>Os testes de {@code DependenciasDeCapacidadeIntegrationTest} cobrem o cálculo do fecho. Estes
 * cobrem só uma coisa: que a requisição chega ao handler certo.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-rota-dependencias-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Roteamento das rotas de permissão")
class RotaDeDependenciasTest {

    private static final String SENHA = "senha-de-teste";
    private static final String ADMIN = "admin-rotas@vendax.com.br";
    private static final String OS = "tms.ordemservico";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ResourceJpaRepository resourceRepository;
    @Autowired
    ActionJpaRepository actionRepository;
    @Autowired
    ActionDependencyJpaRepository dependencyRepository;
    @Autowired
    ArchbaseCapabilityDependencyService dependencyService;

    private final ObjectMapper json = new ObjectMapper();

    private String aprovarId;

    @BeforeEach
    void preparar() {
        dependencyRepository.deleteAll();
        actionRepository.deleteAll();
        resourceRepository.deleteAll();

        criarAdministrador();

        ResourceEntity os = recurso("res-os", OS, TipoRecurso.API);
        ActionEntity aprovar = acao(os, "aprovar_custo");
        acao(os, "view");
        dependencyService.reconcileScan(Map.of(aprovar, Set.of(OS + ":view")), false);
        aprovarId = aprovar.getId();
    }

    @Test
    @DisplayName("a rota do fecho chega ao handler do fecho, e não ao de autoatendimento")
    void rotaDoFechoResolve() throws Exception {
        // O corpo prova qual handler respondeu: 'capability' e 'dependencies' só existem no fecho.
        // O irmão de autoatendimento devolveria {resourceName, permissions}.
        mockMvc.perform(get("/api/v1/resource/permissions/dependencies/" + aprovarId)
                        .header("Authorization", "Bearer " + entrar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capability").value(OS + ":aprovar_custo"))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.dependencies[0].capability").value(OS + ":view"))
                .andExpect(jsonPath("$.dependencies[0].depth").value(1));
    }

    @Test
    @DisplayName("capacidade inexistente responde 404, não uma árvore vazia")
    void inexistenteResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/resource/permissions/dependencies/nao-existe")
                        .header("Authorization", "Bearer " + entrar()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a rota de autoatendimento por recurso continua resolvendo")
    void autoatendimentoContinua() throws Exception {
        mockMvc.perform(get("/api/v1/resource/permissions/" + OS)
                        .header("Authorization", "Bearer " + entrar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceName").value(OS));
    }

    @Test
    @DisplayName("um recurso chamado 'dependencies' continua legível — um segmento, não dois")
    void recursoChamadoDependencies() throws Exception {
        // O caso que de fato colidiria, se colidisse. A rota nova tem DOIS segmentos depois de
        // /permissions; a de autoatendimento tem um. Nomear um recurso de 'dependencies' é
        // improvável e perfeitamente legal, e a tela não pode quebrar por causa disso.
        recurso("res-dep", "dependencies", TipoRecurso.VIEW);

        mockMvc.perform(get("/api/v1/resource/permissions/dependencies")
                        .header("Authorization", "Bearer " + entrar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceName").value("dependencies"));
    }

    @Test
    @DisplayName("o catálogo continua resolvendo, e traz as dependências diretas")
    void catalogoContinua() throws Exception {
        mockMvc.perform(get("/api/v1/resource/permissions")
                        .header("Authorization", "Bearer " + entrar()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.resourceName == '" + OS + "')]").exists());
    }

    @Test
    @DisplayName("sem autenticação nenhuma, não chega ao controller")
    void anonimoNaoAlcanca() throws Exception {
        mockMvc.perform(get("/api/v1/resource/permissions/dependencies/" + aprovarId))
                .andExpect(status().is4xxClientError());
    }

    // ---- fixtura ----

    private String entrar() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", ADMIN, "password", SENHA))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(resposta).get("access_token").asString();
    }

    private void criarAdministrador() {
        if (userRepository.findByEmail(ADMIN).isPresent()) {
            return;
        }
        userRepository.save(UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name(ADMIN).description(ADMIN).email(ADMIN).userName(ADMIN)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(true)
                .accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true)
                .allowPasswordChange(true).allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .tenantId("tenant-teste")
                .build());
    }

    private ResourceEntity recurso(String id, String nome, TipoRecurso tipo) {
        return resourceRepository.save(ResourceEntity.builder()
                .id(id).name(nome).description("Recurso " + nome).active(true).type(tipo)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }

    private ActionEntity acao(ResourceEntity recurso, String nome) {
        return actionRepository.save(ActionEntity.builder()
                .id("act-" + UUID.randomUUID()).name(nome).description("Ação " + nome)
                .resource(recurso).active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
    }
}
