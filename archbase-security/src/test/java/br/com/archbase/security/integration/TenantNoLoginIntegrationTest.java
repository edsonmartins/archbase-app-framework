package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.ratelimit.ArchbaseAuthRateLimiter;
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
 * O tenant no login: descoberta que não vaza e resposta que informa.
 *
 * <p><b>Os defeitos que estes cenários fixam.</b>
 *
 * <ol>
 *   <li>{@code GET /api/v1/auth/tenants} preenchia {@code nome}/{@code descricao} com as colunas
 *       {@code NOME}/{@code DESCRICAO} da linha de <b>usuário</b> — devolvia, sem autenticação, o
 *       nome da pessoa dona daquele e-mail. Como não existe tabela de tenant no archbase-security,
 *       não havia nome de organização algum ali; o seletor de tenant do cliente exibia o nome do
 *       próprio usuário no lugar da empresa.</li>
 *   <li>O endpoint era anônimo e sem contagem nenhuma: dava para varrer uma lista de e-mails à
 *       vontade e descobrir quais têm conta.</li>
 *   <li>A resposta de login não dizia em que tenant o login aconteceu, o que obrigava o cliente a
 *       saber o tenant <b>antes</b> de logar — na prática, embutido no bundle em tempo de build.</li>
 * </ol>
 *
 * <p>O limite de tentativas é apertado aqui (3) só para o cenário caber no teste; o padrão de
 * produção é 10.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-tenant-login-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.security.rate-limit.max-attempts=3",
        // A descoberta tem limite próprio, folgado em produção: apertado aqui só para o
        // cenário caber no teste. Reusar o limite do login foi o defeito que levou a
        // separá-los — ver ArchbaseAuthenticationController#tenantsForEmail.
        "archbase.security.rate-limit.discovery.max-attempts=3",
        "archbase.app.tenant.default.id=tenant-teste"
})
@DisplayName("Tenant no login (Spring + H2)")
class TenantNoLoginIntegrationTest {

    private static final String EMAIL = "usuario@vendax.com.br";
    private static final String SENHA = "senha-de-teste";
    private static final String NOME_DA_PESSOA = "Fulano de Tal";
    private static final String TENANT = "tenant-teste";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    AccessTokenJpaRepository tokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ArchbaseAuthRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void preparar() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name(NOME_DA_PESSOA)
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

        // O limitador é singleton em memória e sobrevive entre cenários; sem zerar, o segundo teste
        // herdaria a contagem do primeiro e falharia por um motivo que não é o que ele investiga.
        rateLimiter.recordSuccess(ArchbaseAuthRateLimiter.key("tenants", EMAIL));
        rateLimiter.recordSuccess(ArchbaseAuthRateLimiter.key("tenants-ip", "127.0.0.1"));
        rateLimiter.recordSuccess(ArchbaseAuthRateLimiter.key("login", EMAIL));
    }

    // ─────────────────── descoberta que não vaza ───────────────────

    @Test
    @DisplayName("a descoberta devolve o tenant e NÃO devolve o nome da pessoa")
    void descobertaNaoVazaONomeDaPessoa() throws Exception {
        JsonNode tenants = consultarTenants();

        assertThat(tenants.size()).isEqualTo(1);
        JsonNode opcao = tenants.get(0);
        assertThat(opcao.get("tenantId").asText()).isEqualTo(TENANT);
        assertThat(opcao.get("nome").isNull())
                .as("era o nome do titular do e-mail sendo devolvido a qualquer anônimo")
                .isTrue();
        assertThat(opcao.get("descricao").isNull()).isTrue();
    }

    @Test
    @DisplayName("e-mail sem conta devolve lista vazia")
    void emailSemContaDevolveVazio() throws Exception {
        var resposta = mockMvc.perform(get("/api/v1/auth/tenants").param("email", "ninguem@x.com"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(objectMapper.readTree(resposta.getResponse().getContentAsString())).isEmpty();
    }

    // ─────────────────── contagem de tentativas ───────────────────

    @Test
    @DisplayName("varredura da descoberta é cortada com 429, e cada e-mail novo NÃO ganha orçamento novo")
    void varreduraEhCortada() throws Exception {
        // Chave por origem: e-mails diferentes a cada palpite, que é exatamente como a enumeração
        // acontece. Se a contagem fosse só por e-mail, isto nunca bloquearia.
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/auth/tenants").param("email", "alvo" + i + "@x.com"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/auth/tenants").param("email", "alvo99@x.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Retry-After")).isNotBlank());
    }

    @Test
    @DisplayName("bloquear a descoberta não tranca o login de quem tem aquele e-mail")
    void bloqueioDaDescobertaNaoTrancaOLogin() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/api/v1/auth/tenants").param("email", EMAIL));
        }

        mockMvc.perform(get("/api/v1/auth/tenants").param("email", EMAIL))
                .andExpect(status().isTooManyRequests());

        // Senão bastaria martelar a descoberta para deixar a vítima fora da própria conta.
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk());
    }

    // ─────────────────── tenant na resposta do login ───────────────────

    @Test
    @DisplayName("o login informa em que tenant autenticou")
    void loginInformaOTenant() throws Exception {
        JsonNode login = login();

        JsonNode tenant = login.get("tenant");
        assertThat(tenant.isNull()).isFalse();
        assertThat(tenant.get("tenantId").asText()).isEqualTo(TENANT);
        assertThat(tenant.get("nome").isNull())
                .as("sem resolver registrado, o framework não inventa nome de organização")
                .isTrue();
    }

    @Test
    @DisplayName("o refresh também informa o tenant")
    void refreshInformaOTenant() throws Exception {
        String refreshToken = login().get("refresh_token").asText();

        var resposta = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(resposta.getResponse().getContentAsString());
        assertThat(json.get("tenant").get("tenantId").asText()).isEqualTo(TENANT);
    }

    // ─────────────────────────── apoio ───────────────────────────

    private JsonNode consultarTenants() throws Exception {
        var resposta = mockMvc.perform(get("/api/v1/auth/tenants").param("email", EMAIL))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resposta.getResponse().getContentAsString());
    }

    private JsonNode login() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resposta.getResponse().getContentAsString());
    }
}
