package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.AccessTokenEntity;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quem não pode ter várias sessões perde a anterior por inteiro ao entrar de novo.
 *
 * <p><b>O estado ambíguo que este teste elimina.</b> O login que encontrava um access token ainda
 * válido reaproveitava esse token e revogava apenas os refresh. Para quem tem
 * {@code allowMultipleLogins = false}, o resultado era uma sessão pela metade: a aba antiga seguia
 * navegando com o access válido — por até um dia inteiro — mas com o refresh morto, então toda
 * tentativa de renovação era negada.
 *
 * <p>O cliente não era deslogado, porque continuava conseguindo ler dados; ele apenas falhava ao
 * renovar, de novo e de novo. Em produção isso apareceu como um erro se repetindo a cada 60
 * segundos, sem causa visível, com o banco mostrando uma linha ACCESS viva e todas as REFRESH
 * revogadas. Levou uma investigação inteira para virar este teste.
 *
 * <p><b>Sessão única precisa significar sessão única:</b> entrou de novo, a de antes acaba. Um
 * meio-termo em que a sessão velha continua lendo mas não consegue se renovar não é mais seguro que
 * derrubá-la — é só mais difícil de entender.
 *
 * <p>O caso oposto, de quem <b>pode</b> ter várias sessões, está em
 * {@link RefreshComMultiplasSessoesTest}: lá o segundo login não pode encostar na primeira sessão.
 * Os dois testes juntos fixam que o comportamento depende do campo, e não do acaso.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-sessao-unica-it;DB_CLOSE_DELAY=-1",
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
@DisplayName("Sessão única (Spring + H2)")
class SessaoUnicaTest {

    private static final String EMAIL = "pessoa.sessao.unica@empresa.com.br";
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

        userRepository.save(UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Pessoa de Sessão Única")
                .description("Pessoa de Sessão Única")
                .email(EMAIL)
                .userName(EMAIL)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(true)
                .accountDeactivated(false)
                .accountLocked(false)
                .changePasswordOnNextLogin(false)
                .passwordNeverExpires(true)
                .allowPasswordChange(true)
                // O ponto do teste: o usuário NÃO pode ter várias sessões.
                .allowMultipleLogins(false)
                .unlimitedAccessHours(true)
                .build());
    }

    @Test
    @DisplayName("o segundo login entrega um access token novo, não o da sessão anterior")
    void segundoLoginNaoReusaOAccessDaSessaoAnterior() throws Exception {
        String accessDaPrimeira = login().get("access_token").asText();

        String accessDaSegunda = login().get("access_token").asText();

        // Reusar o access token era o que deixava a sessão antiga viva pela metade: ela continuava
        // valendo, mas sem refresh que a sustentasse depois.
        assertThat(accessDaSegunda)
                .as("cada login de sessão única precisa emitir credenciais próprias")
                .isNotEqualTo(accessDaPrimeira);
    }

    @Test
    @DisplayName("o access token da sessão anterior para de valer imediatamente")
    void accessDaSessaoAnteriorMorreNoSegundoLogin() throws Exception {
        String accessDaPrimeira = login().get("access_token").asText();

        login();

        // A linha do access antigo tem de estar revogada. É isso que impede a sessão anterior de
        // continuar lendo dados por horas depois de ter sido, supostamente, substituída.
        AccessTokenEntity linhaAntiga = tokenRepository.findAll().stream()
                .filter(t -> accessDaPrimeira.equals(t.getToken()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("o token da primeira sessão sumiu do banco"));

        assertThat(linhaAntiga.isRevoked())
                .as("sessão única: o access da sessão anterior não pode sobreviver ao novo login")
                .isTrue();
    }

    @Test
    @DisplayName("a renovação da sessão anterior é negada, e é o esperado")
    void renovacaoDaSessaoAnteriorEhNegada() throws Exception {
        String refreshDaPrimeira = login().get("refresh_token").asText();

        login();

        // Diferente do caso allowMultipleLogins=true, aqui NEGAR é o comportamento correto: a
        // sessão anterior acabou. O que não pode é ela seguir viva pela metade — por isso os dois
        // testes acima verificam que o access morreu junto.
        assertThat(renovar(refreshDaPrimeira).getResponse().getStatus())
                .as("a sessão anterior foi encerrada pelo novo login")
                .isNotEqualTo(200);
    }

    @Test
    @DisplayName("a sessão nova funciona por inteiro: renova e acessa")
    void sessaoNovaFuncionaPorInteiro() throws Exception {
        login();
        JsonNode segunda = login();

        // Controle: derrubar a sessão anterior não pode danificar a que acabou de entrar. Sem esta
        // verificação, revogar tudo e emitir depois poderia passar despercebido se a ordem
        // estivesse invertida — o novo token nasceria já revogado.
        assertThat(renovar(segunda.get("refresh_token").asText()).getResponse().getStatus())
                .as("a sessão recém-criada precisa conseguir renovar")
                .isEqualTo(200);
    }

    @Test
    @DisplayName("todo token gravado registra a data de criação")
    void tokenGravaDataDeCriacao() throws Exception {
        login();

        List<AccessTokenEntity> tokens = tokenRepository.findAll();
        assertThat(tokens).isNotEmpty();

        // Sem a data de criação não há como saber quando um token foi emitido, e a ordenação por
        // ela em findRefreshTokenByValue ordena sobre nada. Numa investigação real os horários de
        // emissão tiveram de ser deduzidos de trás para frente, a partir das expirações.
        assertThat(tokens)
                .as("token sem data de criação inviabiliza investigar qualquer problema de sessão")
                .allSatisfy(t -> assertThat(t.getCreateEntityDate()).isNotNull());
    }

    private MvcResult renovar(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", refreshToken))))
                .andReturn();
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
