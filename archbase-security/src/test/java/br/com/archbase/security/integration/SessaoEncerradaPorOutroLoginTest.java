package br.com.archbase.security.integration;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
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
 * Usuário de sessão única ({@code allowMultipleLogins=false}): o segundo login revoga o token do
 * primeiro. A sessão que ficou para trás recebe 401 — e o 401 precisa dizer que a sessão foi
 * encerrada, não parecer credencial errada.
 *
 * <p>Caso real (VendaX, 13/11): cinco logins do mesmo admin em 12 minutos, cada um derrubando o
 * anterior, e a tela dizendo "Usuário/senha não encontrados". A investigação levou horas porque a
 * mensagem apontava para a senha e para o reinício do servidor, nunca para o outro login.</p>
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-sessao-encerrada-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("401 de sessão encerrada por outro login (Spring + H2)")
class SessaoEncerradaPorOutroLoginTest {

    private static final String TENANT = "tenant-teste";
    private static final String EMAIL = "admin@sessao-unica.com.br";
    private static final String SENHA = "senha-de-teste";
    private static final String PROTEGIDO = "/api/v1/qualquer-coisa-protegida";

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
    void criarUsuarioDeSessaoUnica() {
        ArchbaseTenantContext.setTenantId(TENANT);
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Admin")
                .description("Admin")
                .email(EMAIL)
                .userName(EMAIL)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(true)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(false)
                .unlimitedAccessHours(true)
                .build();
        user.setTenantId(TENANT);
        userRepository.save(user);
    }

    @AfterEach
    void limparContexto() {
        ArchbaseTenantContext.clear();
    }

    @Test
    @DisplayName("o token derrubado por outro login recebe 401 SESSAO_ENCERRADA")
    void outroLoginEncerraASessao() throws Exception {
        String primeiro = login();
        String segundo = login();
        assertThat(segundo).isNotEqualTo(primeiro);

        var resposta = mockMvc.perform(get(PROTEGIDO)
                        .header("X-TENANT-ID", TENANT)
                        .header("Authorization", "Bearer " + primeiro))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();

        JsonNode corpo = objectMapper.readTree(resposta.getContentAsString());
        assertThat(corpo.path("error").asText()).isEqualTo("SESSAO_ENCERRADA");
        assertThat(corpo.path("message").asText()).contains("outro login");
        assertThat(resposta.getHeader("WWW-Authenticate")).contains("invalid_token");
    }

    @Test
    @DisplayName("sem token, ou com token que não é nosso, o 401 continua genérico")
    void outros401SeguemComoAntes() throws Exception {
        var semToken = mockMvc.perform(get(PROTEGIDO).header("X-TENANT-ID", TENANT))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();
        assertThat(semToken.getContentAsString()).doesNotContain("SESSAO_ENCERRADA");

        var lixo = mockMvc.perform(get(PROTEGIDO)
                        .header("X-TENANT-ID", TENANT)
                        .header("Authorization", "Bearer nao.e.um.jwt"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse();
        assertThat(lixo.getContentAsString()).doesNotContain("SESSAO_ENCERRADA");
    }

    private String login() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .header("X-TENANT-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", SENHA, "tenantId", TENANT))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resposta.getResponse().getContentAsString()).get("access_token").asText();
    }
}
