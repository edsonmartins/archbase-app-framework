package br.com.archbase.security.integration;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ResourceEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ResourceJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Criação por endpoints que montam o DTO sem {@code id}: {@code POST /api/v1/simple-user} e a concessão nova em
 * {@code POST /api/v1/resource/permissions}. O id passa por DTO → domínio → entidade, e o construtor completo da
 * entidade grava o que recebe — inclusive nulo.
 * <p>
 * <b>Por que funciona.</b> A entidade chega ao {@code save} com id nulo e {@code version=1} (o {@code DomainEntityBase}
 * preenche a versão). Com versão, o Spring Data a trata como existente e chama {@code merge}; o Hibernate, sem id para
 * buscar, instancia uma entidade nova pelo construtor sem argumentos — que gera o UUID — e copia o estado. Funciona por
 * efeito colateral: trocar {@code save} por {@code persist}, zerar a versão no DTO ou mudar o construtor quebra a
 * criação. Estes testes existem para que isso apareça no build.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-criacao-sem-id-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Criação sem id informado")
class CriacaoSemIdTest {

    private static final String TENANT = "tenant-teste";
    private static final String SENHA = "senha-de-teste";
    private static final String ADMIN = "admin-sem-id@vendax.com.br";
    private static final String NOVO = "novo-sem-id@vendax.com.br";
    private static final String ALVO = "alvo-sem-id@vendax.com.br";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    PermissionJpaRepository permissionRepository;
    @Autowired
    ResourceJpaRepository resourceRepository;
    @Autowired
    ActionJpaRepository actionRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void preparar() {
        ArchbaseTenantContext.setTenantId(TENANT);
        criarUsuario(ADMIN, true);
    }

    @AfterEach
    void limpar() {
        ArchbaseTenantContext.clear();
    }

    @Test
    @DisplayName("POST /api/v1/simple-user cria o usuário com id gerado")
    void simpleUserGeraId() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/simple-user")
                        .header("Authorization", "Bearer " + entrar())
                        .header("X-TENANT-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", NOVO, "name", "Novo", "description", "Novo", "password", SENHA))))
                .andReturn();

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        String id = r.getResponse().getContentAsString();
        assertThat(id).isNotBlank();
        ArchbaseTenantContext.setTenantId(TENANT);
        UserEntity criado = userRepository.findByEmail(NOVO).orElseThrow();
        assertThat(criado.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("POST /api/v1/resource/permissions cria a concessão nova com id gerado")
    void concessaoNovaGeraId() throws Exception {
        UserEntity alvo = criarUsuario(ALVO, false);
        ResourceEntity recurso = resourceRepository.save(ResourceEntity.builder()
                .id("res-" + UUID.randomUUID()).name("rec.sem.id." + UUID.randomUUID()).description("Recurso")
                .active(true).type(TipoRecurso.API)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());
        ActionEntity acao = actionRepository.save(ActionEntity.builder()
                .id("act-" + UUID.randomUUID()).name("view").description("Ver")
                .resource(recurso).active(true)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build());

        MvcResult r = mockMvc.perform(post("/api/v1/resource/permissions")
                        .header("Authorization", "Bearer " + entrar())
                        .header("X-TENANT-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "securityId", alvo.getId(), "actionId", acao.getId(), "type", "USER"))))
                .andReturn();

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        ArchbaseTenantContext.setTenantId(TENANT);
        List<PermissionEntity> concedidas = permissionRepository.findAll().stream()
                .filter(p -> p.getAction() != null && acao.getId().equals(p.getAction().getId()))
                .toList();
        assertThat(concedidas).hasSize(1);
        assertThat(concedidas.get(0).getId()).isNotBlank();
    }

    private String entrar() throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", ADMIN, "password", SENHA))))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("access_token").asString();
    }

    private UserEntity criarUsuario(String email, boolean admin) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name(email).description(email).email(email).userName(email)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(admin)
                .accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true)
                .allowPasswordChange(true).allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .tenantId(TENANT)
                .build()));
    }
}
