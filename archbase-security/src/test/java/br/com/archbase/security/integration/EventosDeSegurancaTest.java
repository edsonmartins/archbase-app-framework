package br.com.archbase.security.integration;

import br.com.archbase.security.audit.SecurityEventType;
import br.com.archbase.security.persistence.SecurityEventEntity;
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

        esperarPor(e -> e.getTipo() == SecurityEventType.LOGIN
                && EMAIL.equals(e.getUsuario())
                && e.isSucesso());
    }

    @Test
    @DisplayName("senha errada deixa registro, com o motivo")
    void loginFalhoRegistra() throws Exception {
        autenticar(EMAIL, "senha-errada");

        // O motivo distingue erro de digitação de tentativa de invasão — sem ele, o registro diz
        // apenas que alguém não entrou, e isso não orienta ninguém.
        // O e-mail entra no predicado de propósito: o deleteAll do @BeforeEach pode correr com uma
        // gravação em voo do teste vizinho, e sem essa checagem o LOGIN_FALHOU dele satisfaria esta
        // espera — o teste passaria sem ter verificado o próprio cenário.
        esperarPor(e -> e.getTipo() == SecurityEventType.LOGIN_FALHOU
                && EMAIL.equals(e.getUsuario())
                && !e.isSucesso()
                && e.getDetalhe() != null && !e.getDetalhe().isBlank());
    }

    @Test
    @DisplayName("o identificador tentado é guardado mesmo quando não existe ninguém com ele")
    void tentativaComUsuarioInexistente() throws Exception {
        autenticar("ninguem@vendax.com.br", "qualquer");

        // É este valor que revela alguém varrendo e-mails; descartá-lo por "não corresponder a um
        // usuário" apagaria justamente o sinal de reconhecimento.
        esperarPor(e -> e.getTipo() == SecurityEventType.LOGIN_FALHOU
                && "ninguem@vendax.com.br".equals(e.getUsuario()));
    }

    /**
     * Espera o registro aparecer, em vez de olhar uma vez e concluir.
     *
     * <p><b>Por que precisou existir.</b> A gravação do evento saiu da thread da requisição —
     * precisou sair, porque falhar ao gravar estava derrubando o próprio login. Só que estes testes
     * continuaram consultando o repositório na linha seguinte à requisição, e isso virou uma corrida:
     * passavam nesta máquina, onde a outra thread termina primeiro, e falharam no CI, mais carregado.
     * Foi assim que a publicação da 3.1.15 caiu.
     *
     * <p>Um teste que depende de quem chega primeiro não afirma nada sobre o comportamento — afirma
     * sobre a máquina. Esperar até um limite generoso mantém a verificação real (o evento é gravado,
     * com o conteúdo certo) e remove a dependência de velocidade.
     */
    private void esperarPor(java.util.function.Predicate<SecurityEventEntity> condicao)
            throws InterruptedException {
        long limite = System.currentTimeMillis() + 10_000;
        java.util.List<SecurityEventEntity> ultimaLeitura = java.util.List.of();

        while (System.currentTimeMillis() < limite) {
            ultimaLeitura = eventRepository.findAll();
            if (ultimaLeitura.stream().anyMatch(condicao)) {
                return;
            }
            Thread.sleep(50);
        }

        // Falha com o que havia na tabela: "nenhum evento casou" sem dizer o que existia obrigaria a
        // reproduzir o teste só para descobrir se o problema é conteúdo errado ou registro ausente.
        assertThat(ultimaLeitura)
                .withFailMessage("nenhum evento correspondeu em 10s; a trilha continha: %s", ultimaLeitura)
                .anyMatch(condicao);
    }

    private void autenticar(String email, String senha) throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", senha))));
    }
}
