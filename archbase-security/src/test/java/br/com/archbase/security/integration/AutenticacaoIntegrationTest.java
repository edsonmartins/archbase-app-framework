package br.com.archbase.security.integration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseJwtService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita a autenticação de ponta a ponta com Spring + H2: login, uso do token, refresh e logout,
 * passando pela cadeia de filtros de verdade.
 *
 * <p><b>Por que este teste existe.</b> Toda a auditoria de segurança passou por build verde e
 * testes unitários verdes — e ainda assim três rodadas de revisão encontraram defeitos que só
 * aparecem em runtime: {@code @Value} que não é injetado porque o objeto foi criado com
 * {@code new}, associação LAZY tocada fora de sessão, {@code @Transactional} que não se aplica,
 * handler que nunca chegou a ser registrado na cadeia, JPQL que o banco recusa. Nenhum teste
 * unitário com mock alcança isso: é preciso subir o contexto e mandar requisição.
 *
 * <p>Cada cenário abaixo corresponde a um defeito real que escapou de alguma rodada.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-security-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Autenticação de ponta a ponta (Spring + H2)")
class AutenticacaoIntegrationTest {

    private static final String EMAIL = "usuario@vendax.com.br";
    private static final String SENHA = "senha-de-teste";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    AccessTokenJpaRepository tokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ArchbaseJwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void criarUsuario() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Usuário de Teste")
                .description("Usuário de Teste")
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
        user.setTenantId("tenant-teste");
        userRepository.save(user);
    }

    // ─────────────────────────── cenários ───────────────────────────

    @Test
    @DisplayName("login devolve access e refresh, e o access autentica uma requisição")
    void loginEUsoDoToken() throws Exception {
        JsonNode login = login();

        String accessToken = login.get("access_token").asText();
        String refreshToken = login.get("refresh_token").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        mockMvc.perform(get("/api/v1/user/findAll?page=0&size=10")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("refresh token renova o par de credenciais")
    void refreshRenova() throws Exception {
        String refreshToken = login().get("refresh_token").asText();

        var resposta = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(resposta.getResponse().getContentAsString());
        assertThat(json.get("access_token").asText()).isNotBlank();
        assertThat(json.get("refresh_token").asText()).isNotBlank();
    }

    @Test
    @DisplayName("access token não é aceito como refresh — confusão de tipo de credencial")
    void accessTokenNaoServeComoRefresh() throws Exception {
        String accessToken = login().get("access_token").asText();

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", accessToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("desafio de MFA não vira credencial pelo /refresh-token — o bypass original")
    void desafioMfaNaoViraCredencial() throws Exception {
        UserEntity user = userRepository.findByEmail(EMAIL).orElseThrow();
        String desafio = jwtService.generateMfaChallengeToken(user).token();

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", desafio))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout revoga a sessão: access deixa de autenticar e refresh deixa de renovar")
    void logoutEncerraASessao() throws Exception {
        JsonNode login = login();
        String accessToken = login.get("access_token").asText();
        String refreshToken = login.get("refresh_token").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/user/findAll?page=0&size=10")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("senha errada devolve 401 e não emite token")
    void senhaErradaNaoAutentica() throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", "errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("requisição sem credencial é recusada com 401")
    void semCredencialRecebe401() throws Exception {
        mockMvc.perform(get("/api/v1/user/findAll?page=0&size=10"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────── apoio ───────────────────────────

    private JsonNode login() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resposta.getResponse().getContentAsString());
    }
}
