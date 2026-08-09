package br.com.archbase.security.integration;

import br.com.archbase.security.audit.SecurityEventType;
import br.com.archbase.security.repository.SecurityEventJpaRepository;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Os eventos que a trilha do Envers não alcança.
 *
 * <p>Entrar no sistema, errar a senha e ter um acesso negado <b>não mudam tabela nenhuma</b> — o
 * Envers registra alterações de estado e, por construção, não vê nada disso. São justamente os
 * acontecimentos que respondem "quem tentou o quê", e sem eles a trilha conta metade da história:
 * saberíamos quem concedeu um acesso, e não quem o usou.
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
class EventosDeSegurancaTest {

    private static final String EMAIL = "auditoria@vendax.com.br";
    private static final String SENHA = "senha-de-teste";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    SecurityEventJpaRepository eventRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void limpar() {
        eventRepository.deleteAll();
        if (userRepository.findByEmail(EMAIL).isEmpty()) {
            userRepository.save(UserEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .createEntityDate(java.time.LocalDateTime.now())
                    .name("Pessoa de Auditoria")
                    .description("Pessoa de Auditoria")
                    .email(EMAIL)
                    .userName(EMAIL)
                    .password(passwordEncoder.encode(SENHA))
                    .isAdministrator(false)
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

    @Test
    @DisplayName("entrar no sistema deixa registro")
    void loginRegistra() throws Exception {
        autenticar(EMAIL, SENHA);

        var eventos = eventRepository.findAll();

        assertThat(eventos).anySatisfy(e -> {
            assertThat(e.getTipo()).isEqualTo(SecurityEventType.LOGIN);
            assertThat(e.getUsuario()).isEqualTo(EMAIL);
            assertThat(e.isSucesso()).isTrue();
        });
    }

    @Test
    @DisplayName("senha errada deixa registro, com o motivo")
    void loginFalhoRegistra() throws Exception {
        autenticar(EMAIL, "senha-errada");

        var eventos = eventRepository.findAll();

        // O motivo distingue erro de digitação de tentativa de invasão — sem ele, o registro diz
        // apenas que alguém não entrou, e isso não orienta ninguém.
        assertThat(eventos).anySatisfy(e -> {
            assertThat(e.getTipo()).isEqualTo(SecurityEventType.LOGIN_FALHOU);
            assertThat(e.isSucesso()).isFalse();
            assertThat(e.getDetalhe()).isNotBlank();
        });
    }

    @Test
    @DisplayName("o identificador tentado é guardado mesmo quando não existe ninguém com ele")
    void tentativaComUsuarioInexistente() throws Exception {
        autenticar("ninguem@vendax.com.br", "qualquer");

        var eventos = eventRepository.findAll();

        // É este valor que revela alguém varrendo e-mails; descartá-lo por "não corresponder a um
        // usuário" apagaria justamente o sinal de reconhecimento.
        assertThat(eventos).anySatisfy(e -> {
            assertThat(e.getTipo()).isEqualTo(SecurityEventType.LOGIN_FALHOU);
            assertThat(e.getUsuario()).isEqualTo("ninguem@vendax.com.br");
        });
    }

    private void autenticar(String email, String senha) throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", senha))));
    }
}
