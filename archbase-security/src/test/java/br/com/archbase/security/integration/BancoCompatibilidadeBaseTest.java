package br.com.archbase.security.integration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseJwtService;
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
 * Cenários que precisam rodar em <b>todos</b> os bancos suportados.
 *
 * <p><b>Por que existe.</b> O teste com H2 prova a lógica, não a portabilidade. Esta auditoria já
 * produziu dois construtos que só funcionam no PostgreSQL: a migration com
 * {@code add column if not exists} e um {@code UPDATE} com subconsulta sobre a própria tabela, que
 * o MySQL recusa com ERROR 1093. O primeiro foi encontrado por leitura, o segundo por revisão — e
 * os dois passaram por build verde. Enquanto o SQL não roda contra o banco de verdade, "é
 * portável" é opinião.
 *
 * <p>As subclasses fornecem o container. Os containers são efêmeros e geram as próprias
 * credenciais a cada execução; nada de ambiente real é usado aqui.
 *
 * <p>Sem Docker, as subclasses se desabilitam em vez de falhar.
 *
 * <p><b>Rodando localmente.</b> Com Docker Desktop funciona direto. Com colima ou outro runtime
 * cujo socket não esteja em {@code /var/run/docker.sock}, aponte o Testcontainers para ele:
 *
 * <pre>
 * export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
 * export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
 * </pre>
 *
 * <p><b>Limitação conhecida no MySQL.</b> O índice único de {@code SEGURANCA_TOKEN_ACESSO.TOKEN}
 * não é criado: a coluna é {@code varchar(5000)} e, em utf8mb4, o índice passaria de 3072 bytes.
 * O Hibernate registra o erro do DDL e segue; a aplicação funciona, mas sem a garantia de
 * unicidade no banco. É anterior a esta auditoria e depende de uma decisão de esquema
 * (encurtar a coluna, indexar um hash, ou abrir mão da constraint).
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
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
abstract class BancoCompatibilidadeBaseTest {

    protected static final String EMAIL = "usuario@vendax.com.br";
    protected static final String SENHA = "senha-de-teste";

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

    @Test
    @DisplayName("o esquema das entidades de segurança é criado neste banco")
    void esquemaCriado() {
        assertThat(userRepository.findByEmail(EMAIL)).isPresent();
    }

    @Test
    @DisplayName("login emite o par de credenciais e o access autentica")
    void loginFunciona() throws Exception {
        JsonNode login = login();

        mockMvc.perform(get("/api/v1/user/findAll?page=0&size=10")
                        .header("Authorization", "Bearer " + login.get("access_token").asText()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("refresh renova o par")
    void refreshFunciona() throws Exception {
        String refreshToken = login().get("refresh_token").asText();

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", refreshToken))))
                .andExpect(status().isOk());
    }

    /**
     * O cenário que motivou esta classe.
     *
     * <p>O logout executa um {@code UPDATE ... WHERE t.user.id = :userId} em lote. A versão
     * anterior derivava o {@code userId} por subconsulta sobre a mesma tabela e o MySQL a recusava
     * com ERROR 1093 — logout quebrado, sem nada revogado, num defeito que nenhum teste em H2
     * revelaria.
     */
    @Test
    @DisplayName("logout executa o UPDATE em lote e encerra a sessão")
    void logoutFuncionaNesteBanco() throws Exception {
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
    @DisplayName("desafio de MFA não vira credencial pelo /refresh-token")
    void desafioMfaRecusado() throws Exception {
        UserEntity user = userRepository.findByEmail(EMAIL).orElseThrow();
        String desafio = jwtService.generateMfaChallengeToken(user).token();

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", desafio))))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode login() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", SENHA))))
                .andReturn();

        // Corpo e exceção na mensagem: o /authenticate não tem catch genérico, então uma falha de
        // banco vira 500 sem nada no log — e o teste sozinho diria apenas "esperava 200, veio 500".
        assertThat(resposta.getResponse().getStatus())
                .withFailMessage("login falhou: status=%d, corpo=%s, exceção=%s",
                        resposta.getResponse().getStatus(),
                        resposta.getResponse().getContentAsString(),
                        resposta.getResolvedException() != null
                                ? resposta.getResolvedException().toString()
                                : descreveCausa(resposta.getRequest().getAttribute("jakarta.servlet.error.exception")))
                .isEqualTo(200);
        return objectMapper.readTree(resposta.getResponse().getContentAsString());
    }

    private String descreveCausa(Object excecao) {
        if (!(excecao instanceof Throwable t)) {
            return "não capturada";
        }
        StringBuilder sb = new StringBuilder(t.toString());
        for (Throwable causa = t.getCause(); causa != null; causa = causa.getCause()) {
            sb.append(" <- ").append(causa);
        }
        return sb.toString();
    }
}
