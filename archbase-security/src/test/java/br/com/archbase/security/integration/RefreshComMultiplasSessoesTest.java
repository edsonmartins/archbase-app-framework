package br.com.archbase.security.integration;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Um segundo login não pode derrubar a renovação da sessão que já estava aberta.
 *
 * <p><b>O defeito que este teste fixa.</b> O login que encontra um access token ainda válido
 * reaproveita esse token e chama {@code revokeAllRefreshTokens}, matando <b>todos</b> os refresh
 * anteriores. A intenção era impedir acúmulo — dez logins deixavam dez refresh vivos, desfazendo a
 * rotação. Mas a revogação ignora {@code allowMultipleLogins}, que é justamente o campo com que o
 * framework modela "este usuário pode ter várias sessões".
 *
 * <p>Na prática: abrir uma segunda aba, recarregar a página ou qualquer caminho que reautentique
 * revoga o refresh que a primeira sessão guardava. Ela continua funcionando enquanto o access token
 * durar e, quando tenta renovar, recebe 400 — e o usuário é deslogado sem ter feito nada.
 *
 * <p>O estado em produção que motivou este teste era exatamente esse: uma linha ACCESS viva e
 * <b>todas</b> as linhas REFRESH revogadas, com o cliente renovando a cada 60s e sendo negado
 * sempre.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-refresh-sessoes-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false"
})
@DisplayName("Renovação com múltiplas sessões (Spring + H2)")
class RefreshComMultiplasSessoesTest {

    private static final String EMAIL = "pessoa@empresa.com.br";
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
    void criarUsuario() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Pessoa da Empresa")
                .description("Pessoa da Empresa")
                .email(EMAIL)
                .userName(EMAIL)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(true)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                // O ponto do teste: o usuário PODE ter várias sessões.
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("segundo login não invalida a renovação da primeira sessão")
    void segundoLoginNaoDerrubaPrimeiraSessao() throws Exception {
        // Primeira sessão — a aba que já estava aberta.
        String refreshDaPrimeira = login().get("refresh_token").asText();
        assertThat(refreshDaPrimeira).isNotBlank();

        // Segunda sessão: outra aba, um recarregamento, qualquer reautenticação. Cai no caminho que
        // reaproveita o access token ainda válido.
        login();

        // A primeira sessão tenta renovar, como faz de tempos em tempos.
        var resposta = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", refreshDaPrimeira))))
                .andReturn();

        assertThat(resposta.getResponse().getStatus())
                .as("negar aqui desloga uma sessão legítima de quem tem allowMultipleLogins")
                .isEqualTo(200);
    }

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
