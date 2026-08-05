package br.com.archbase.security.integration;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.persistence.AccessTokenEntity;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O logout precisa revogar mesmo quando o usuário não está no tenant padrão.
 *
 * <p><b>O defeito que este teste fixa.</b> O {@code LogoutFilter} do Spring roda <b>antes</b> do
 * {@code ArchbaseJwtAuthenticationFilter} — este entra via
 * {@code addFilterBefore(UsernamePasswordAuthenticationFilter)}, que vem depois na cadeia. Quando o
 * {@code ArchbaseLogoutService} executa, o {@code ArchbaseTenantContext} ainda está vazio.
 *
 * <p>Com as consultas em JPQL sobre {@code AccessTokenEntity} — que é {@code @TenantId} — o
 * Hibernate aplicava o discriminador com o tenant <i>padrão</i>. Para qualquer usuário de outro
 * tenant a linha não era encontrada, o serviço retornava cedo e o endpoint respondia <b>200 sem
 * revogar nada</b>: o cliente lia logout bem-sucedido e o token seguia valendo até expirar. O
 * logout, que foi a funcionalidade acrescentada nesta versão justamente porque o {@code /logout}
 * do Spring não fazia nada com sessão stateless, continuava não fazendo — agora com mais código.
 *
 * <p>A suíte anterior não pegava isso por duas razões somadas: todos os usuários de teste ficavam
 * no tenant padrão, e o resolver do harness devolvia um valor fixo, ignorando o contexto. Nenhum
 * cenário multi-tenant era representável.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-logout-mt-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Logout fora do tenant padrão (Spring + H2)")
class LogoutMultiTenantTest {

    /** De propósito diferente de {@code archbase.app.tenant.default.id}. */
    private static final String OUTRO_TENANT = "tenant-do-cliente";
    private static final String EMAIL = "pessoa@cliente.com.br";
    private static final String SENHA = "senha-de-teste";

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
    void criarUsuarioEmOutroTenant() {
        ArchbaseTenantContext.setTenantId(OUTRO_TENANT);
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Pessoa do Cliente")
                .description("Pessoa do Cliente")
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
        // Explícito, como nos demais testes: o campo é gravado por atribuição, não pelo resolver.
        user.setTenantId(OUTRO_TENANT);
        userRepository.save(user);
    }

    @AfterEach
    void limparContexto() {
        ArchbaseTenantContext.clear();
    }

    @Test
    @DisplayName("revoga os tokens de quem não está no tenant padrão")
    void revogaForaDoTenantPadrao() throws Exception {
        JsonNode login = login();
        String accessToken = login.get("access_token").asText();
        assertThat(accessToken).isNotBlank();

        // O logout roda sem contexto de tenant, como acontece na cadeia de filtros real.
        ArchbaseTenantContext.clear();
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        ArchbaseTenantContext.setTenantId(OUTRO_TENANT);
        Optional<AccessTokenEntity> depois = tokenRepository.findByToken(accessToken);
        assertThat(depois)
                .as("o token precisa continuar existindo para podermos afirmar algo sobre ele")
                .isPresent();
        assertThat(depois.get().isRevoked())
                .as("200 no logout sem revogar é pior que erro: o cliente acredita ter saído")
                .isTrue();
    }

    private JsonNode login() throws Exception {
        // O tenant vai no cabeçalho, e não só no corpo: com open-in-view a sessão do Hibernate
        // abre na cadeia de filtros, e é ali que o @TenantId é resolvido. Definir o contexto
        // depois, ao ler o corpo, já não alcança a sessão aberta.
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .header("X-TENANT-ID", OUTRO_TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", SENHA, "tenantId", OUTRO_TENANT))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resposta.getResponse().getContentAsString());
    }
}
