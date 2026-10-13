package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseJwtService;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * <b>Trocar a chave JWT não derruba o login de quem já tinha token.</b>
 *
 * <h2>O defeito</h2>
 *
 * <p>O login procura o access token guardado do usuário para reaproveitá-lo e pergunta ao
 * {@code ArchbaseJwtService} se ele expirou. A pergunta decodifica o JWT com a chave <b>atual</b>:
 * um token assinado com a chave anterior não volta "expirado", lança erro de assinatura — e o
 * login respondia <b>500</b>. Depois de rotacionar {@code archbase.security.jwt.secret-key}, todo
 * usuário com token guardado ficava de fora até alguém limpar {@code seguranca_token_acesso} à mão.
 *
 * <h2>O que vale agora</h2>
 *
 * <p>Token guardado que não valida é tratado como inexistente: o login revoga o que havia e emite
 * um novo. A tolerância é só nesse ponto — apresentar o token antigo numa requisição continua
 * sendo negado, e o último cenário existe para que afrouxar isso não passe despercebido.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-rotacao-de-chave-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=" + RotacaoDeChaveJwtTest.K1,
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste"
})
@DisplayName("Rotação da chave JWT (Spring + H2)")
class RotacaoDeChaveJwtTest {

    static final String K1 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    static final String K2 = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private static final String EMAIL = "pessoa.rotacao@empresa.com.br";
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
    }

    @AfterEach
    void devolverAChave() {
        // O contexto do Spring é reaproveitado entre os cenários desta classe.
        trocarChave(K1);
    }

    @Test
    @DisplayName("várias sessões: login com K1, troca para K2, novo login responde 200 com token novo")
    void loginDepoisDaTrocaComVariasSessoes() throws Exception {
        loginDepoisDaTroca(true);
    }

    @Test
    @DisplayName("sessão única: login com K1, troca para K2, novo login responde 200 com token novo")
    void loginDepoisDaTrocaComSessaoUnica() throws Exception {
        loginDepoisDaTroca(false);
    }

    @Test
    @DisplayName("o token assinado com a chave antiga continua não autenticando requisição")
    void tokenAntigoNaoAutentica() throws Exception {
        usuario(true);
        String antigo = corpo(login()).get("access_token").asText();

        trocarChave(K2);

        // Controle da correção: a tolerância é para REAPROVEITAR o token no login, não para
        // aceitá-lo. Quem apresenta um token assinado com a chave anterior segue de fora.
        int status = mockMvc.perform(get("/api/v1/user/findAll?page=0&size=10")
                        .header("Authorization", "Bearer " + antigo))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(200);
    }

    private void loginDepoisDaTroca(boolean variasSessoes) throws Exception {
        usuario(variasSessoes);
        String antigo = corpo(login()).get("access_token").asText();

        trocarChave(K2);
        MvcResult segundo = login();

        assertThat(segundo.getResponse().getStatus())
                .as("rotação de chave não pode exigir limpeza manual da tabela de tokens")
                .isEqualTo(200);

        String novo = corpo(segundo).get("access_token").asText();
        assertThat(novo).isNotEqualTo(antigo);
        assertThat(jwtService.isTokenExpired(novo))
                .as("o token novo é assinado com a chave atual")
                .isFalse();

        AccessTokenEntity linhaAntiga = tokenRepository.findAll().stream()
                .filter(t -> antigo.equals(t.getToken()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("o token da primeira sessão sumiu do banco"));
        assertThat(linhaAntiga.isRevoked())
                .as("o token que não valida mais fica revogado, e não esperando a próxima tentativa")
                .isTrue();
    }

    // ------------------------------------------------------------------ apoio

    private void trocarChave(String chave) {
        ReflectionTestUtils.setField(jwtService, "secretKey", chave);
    }

    private void usuario(boolean variasSessoes) {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Pessoa da Rotação")
                .description("Pessoa da Rotação")
                .email(EMAIL)
                .userName(EMAIL)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(true)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                .allowMultipleLogins(variasSessoes)
                .unlimitedAccessHours(true)
                .build();
        user.setTenantId("tenant-teste");
        userRepository.save(user);
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", SENHA))))
                .andReturn();
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }
}
