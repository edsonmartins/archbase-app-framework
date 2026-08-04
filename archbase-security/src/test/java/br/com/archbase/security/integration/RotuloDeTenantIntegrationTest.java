package br.com.archbase.security.integration;

import br.com.archbase.security.auth.ArchbaseTenantInfoResolver;
import br.com.archbase.security.auth.TenantLoginOption;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quando a aplicação sabe o nome das organizações, ela informa — e o framework usa.
 *
 * <p>O {@code archbase-security} não tem cadastro de tenant, então deixou de inventar rótulo a
 * partir da linha de usuário (que era o nome da pessoa, não da empresa). Quem tem esse cadastro é a
 * aplicação: ela registra um {@link ArchbaseTenantInfoResolver} e passa a preencher os rótulos,
 * tanto na descoberta pré-login quanto na resposta do login.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-rotulo-tenant-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste",
        "teste.tenant-info-resolver.enabled=true"
})
@DisplayName("Rótulo de tenant fornecido pela aplicação (Spring + H2)")
class RotuloDeTenantIntegrationTest {

    private static final String EMAIL = "usuario@vendax.com.br";
    private static final String SENHA = "senha-de-teste";
    private static final String TENANT = "tenant-teste";

    // O resolver vive em ArchbaseSecurityTestApplication, ligado pela propriedade acima. Declará-lo
    // aqui como @TestConfiguration aninhada não funcionaria: o @ComponentScan daquela classe varre
    // o pacote de teste inteiro e o bean apareceria também nos cenários que precisam ficar sem ele.

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    AccessTokenJpaRepository tokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void preparar() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Fulano de Tal")
                .description("Descrição da pessoa")
                .email(EMAIL)
                .userName(EMAIL)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(true)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .build();
        user.setTenantId(TENANT);
        userRepository.save(user);
    }

    @Test
    @DisplayName("a descoberta usa o rótulo da aplicação, e o id continua vindo do banco")
    void descobertaUsaORotulo() throws Exception {
        var resposta = mockMvc.perform(get("/api/v1/auth/tenants").param("email", EMAIL))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode opcao = objectMapper.readTree(resposta.getResponse().getContentAsString()).get(0);
        assertThat(opcao.get("nome").asText()).isEqualTo("Frigocenter");
        assertThat(opcao.get("descricao").asText()).isEqualTo("Frigocenter Distribuidora Ltda");
        assertThat(opcao.get("tenantId").asText())
                .as("o id é o do banco mesmo que o resolver esqueça de preenchê-lo")
                .isEqualTo(TENANT);
    }

    @Test
    @DisplayName("o login devolve o tenant já rotulado")
    void loginDevolveORotulo() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tenant = objectMapper.readTree(resposta.getResponse().getContentAsString()).get("tenant");
        assertThat(tenant.get("tenantId").asText()).isEqualTo(TENANT);
        assertThat(tenant.get("nome").asText()).isEqualTo("Frigocenter");
    }
}
