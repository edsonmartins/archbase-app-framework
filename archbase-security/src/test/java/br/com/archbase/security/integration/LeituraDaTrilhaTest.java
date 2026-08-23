package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.UserEntity;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quem pode ler a trilha.
 *
 * <p>A resposta é: só administrador — e a verificação é explícita no controller, não delegada a
 * {@code admin-endpoints.policy}, cujo padrão {@code permit} deixaria qualquer autenticado entrar.
 *
 * <p>A distinção importa porque a trilha não é um relatório qualquer: ela mostra quem tem acesso a
 * quê, quem tentou o que não podia e quando cada proteção foi afrouxada. É material de
 * reconhecimento para quem quiser abusar.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.audit.enabled=true",
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
class LeituraDaTrilhaTest {

    private static final String SENHA = "senha-de-teste";
    private static final String ADMIN = "admin-trilha@vendax.com.br";
    private static final String COMUM = "comum-trilha@vendax.com.br";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void criarPessoas() {
        criar(ADMIN, true);
        criar(COMUM, false);
    }

    @Test
    @DisplayName("administrador lê a trilha")
    void administradorLe() throws Exception {
        mockMvc.perform(get("/api/v1/security/audit/events")
                        .header("Authorization", "Bearer " + entrar(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("usuário comum autenticado NÃO lê a trilha")
    void comumNaoLe() throws Exception {
        // Autenticado e legítimo — e ainda assim recusado. É a diferença entre "quem entrou" e
        // "quem administra", e é ela que a trilha precisa respeitar para não virar reconhecimento.
        mockMvc.perform(get("/api/v1/security/audit/events")
                        .header("Authorization", "Bearer " + entrar(COMUM)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sem autenticação nenhuma, nem chega ao controller")
    void anonimoNaoLe() throws Exception {
        mockMvc.perform(get("/api/v1/security/audit/events"))
                .andExpect(status().is4xxClientError());
    }

    private String entrar(String email) throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", SENHA))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(resposta).get("access_token").asString();
    }

    private void criar(String email, boolean administrador) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        userRepository.save(UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name(email)
                .description(email)
                .email(email)
                .userName(email)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(administrador)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(true)
                .unlimitedAccessHours(true)
                .tenantId("tenant-teste")
                .build());
    }
}
