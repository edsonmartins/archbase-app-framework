package br.com.archbase.security.integration;

import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.UserGroupEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.GroupJpaRepository;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>Ler usuário funciona sem open-in-view.</b>
 *
 * <h2>O defeito</h2>
 *
 * <p>As leituras do {@code UserService} não abriam transação. O adaptador busca a entidade e a
 * converte em seguida, e a conversão lê {@code groups}, que é lazy. Com
 * {@code spring.jpa.open-in-view=false} a sessão já estava fechada nesse ponto:
 * {@code LazyInitializationException}, que o {@code UserController} engolia num {@code catch}
 * devolvendo <b>500 sem corpo e sem log</b>.
 *
 * <p>O efeito era total: o front chama {@code GET /user/byEmail/{email}} logo depois do login, para
 * qualquer usuário. O login respondia 200 e ninguém entrava.
 *
 * <h2>Por que os outros testes não pegavam</h2>
 *
 * <p>O padrão do Spring Boot é open-in-view ligado, e é assim que as demais suítes de integração
 * sobem: a sessão da requisição cobria a leitura lazy. Esta sobe com a propriedade desligada e
 * com um usuário <b>que tem grupo</b>, que é o vínculo que a conversão atravessa.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-leitura-usuario-sem-osiv-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
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
@DisplayName("Leitura de usuário sem open-in-view (Spring + H2)")
class LeituraDeUsuarioSemOpenInViewTest {

    private static final String ID = "user-com-grupo";
    private static final String EMAIL = "pessoa.com.grupo@empresa.com.br";
    private static final String SENHA = "senha-de-teste";
    private static final String GRUPO = "VENDEDORES";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    GroupJpaRepository groupRepository;
    @Autowired
    AccessTokenJpaRepository tokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void criarUsuarioComGrupo() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();

        GroupEntity grupo = GroupEntity.builder()
                .id("group-" + GRUPO).name(GRUPO).description("Grupo " + GRUPO)
                .createEntityDate(LocalDateTime.now()).createdByUser("teste").build();
        grupo.setTenantId("tenant-teste");
        grupo = groupRepository.save(grupo);

        UserEntity user = UserEntity.builder()
                .id(ID)
                .createEntityDate(LocalDateTime.now())
                .name("Pessoa com Grupo")
                .description("Pessoa com Grupo")
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
        user = userRepository.save(user);

        Set<UserGroupEntity> vinculos = new HashSet<>();
        vinculos.add(UserGroupEntity.builder().id("ug-1").user(user).group(grupo).build());
        user.setGroups(vinculos);
        userRepository.save(user);
    }

    @Test
    @DisplayName("login seguido de GET /user/byEmail responde 200 com os grupos")
    void buscaPorEmailDepoisDoLogin() throws Exception {
        JsonNode usuario = ler("/api/v1/user/byEmail/" + EMAIL);

        assertThat(usuario.get("email").asText()).isEqualTo(EMAIL);
        assertThat(nomesDosGrupos(usuario))
                .as("os grupos são a associação lazy que derrubava a leitura")
                .contains(GRUPO);
    }

    @Test
    @DisplayName("login seguido de GET /user/{id} responde 200 com os grupos")
    void buscaPorIdDepoisDoLogin() throws Exception {
        JsonNode usuario = ler("/api/v1/user/" + ID);

        assertThat(usuario.get("id").asText()).isEqualTo(ID);
        assertThat(nomesDosGrupos(usuario)).contains(GRUPO);
    }

    @Test
    @DisplayName("a listagem paginada também lê os grupos sem sessão da requisição")
    void listagemDepoisDoLogin() throws Exception {
        JsonNode pagina = ler("/api/v1/user/findAll?page=0&size=10");

        assertThat(pagina.get("content")).hasSize(1);
        assertThat(nomesDosGrupos(pagina.get("content").get(0))).contains(GRUPO);
    }

    // ------------------------------------------------------------------ apoio

    private JsonNode ler(String caminho) throws Exception {
        var resposta = mockMvc.perform(get(caminho).header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resposta.getResponse().getContentAsString());
    }

    private static String nomesDosGrupos(JsonNode usuario) {
        return String.valueOf(usuario.get("groups"));
    }

    private String login() throws Exception {
        var resposta = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resposta.getResponse().getContentAsString())
                .get("access_token").asText();
    }
}
