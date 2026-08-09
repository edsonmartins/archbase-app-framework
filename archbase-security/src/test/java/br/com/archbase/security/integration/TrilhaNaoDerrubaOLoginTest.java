package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A trilha não pode derrubar o que ela audita.
 *
 * <p>É a promessa central do registro de eventos, e ela <b>falhou em produção</b>: com a tabela de
 * eventos ausente, o login parou de funcionar para todo mundo. O {@code try/catch} estava lá e até
 * registrou o aviso — mas engolir a exceção em Java não desfaz o estrago no banco. No PostgreSQL,
 * um erro dentro da transação aborta a transação inteira, e a consulta seguinte morre com
 * "current transaction is aborted".
 *
 * <p>Este teste faz o que os outros não faziam: exercita o <b>caminho de falha</b>. Os anteriores
 * verificavam que o evento é gravado quando tudo funciona, que é justamente o caso que nunca
 * quebra.
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
// Este teste destrói a tabela de eventos de propósito, e o contexto do Spring é compartilhado
// entre as classes de teste com a mesma configuração. Sem marcá-lo como sujo, o banco em memória
// segue sem a tabela e as outras suítes da trilha falham por consequência — foi o que aconteceu.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TrilhaNaoDerrubaOLoginTest {

    private static final String EMAIL = "sem-trilha@vendax.com.br";
    private static final String SENHA = "senha-de-teste";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    EntityManager entityManager;
    @Autowired
    TransactionTemplate transacao;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void prepararCenario() {
        if (userRepository.findByEmail(EMAIL).isEmpty()) {
            userRepository.save(UserEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .createEntityDate(LocalDateTime.now())
                    .name("Pessoa sem trilha")
                    .description("Pessoa sem trilha")
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

        // Reproduz o cenário real: a chave ligada e a tabela ausente — o que acontece quando alguém
        // liga a auditoria antes de criar o schema, e foi exatamente o que derrubou o login.
        transacao.executeWithoutResult(t ->
                entityManager.createNativeQuery("DROP TABLE IF EXISTS SEGURANCA_EVENTO").executeUpdate());
    }

    @Test
    @DisplayName("com a tabela de eventos ausente, o login continua funcionando")
    void loginSobreviveASemTabela() throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("e a senha errada continua devolvendo 401, não erro interno")
    void falhaDeLoginSobreviveASemTabela() throws Exception {
        // Aqui a trilha tenta gravar LOGIN_FALHOU e falha. Sem isolamento, o erro vira 500 e some a
        // informação que o usuário precisa: que a senha está errada.
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", EMAIL, "password", "errada"))))
                .andExpect(status().isUnauthorized());
    }
}
