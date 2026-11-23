package br.com.archbase.security.integration;

import br.com.archbase.security.auth.external.ExternalAuthenticationPolicy;
import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 1a do IdP externo: dois {@code ExternalAuthenticationPolicy} no mesmo contexto (Control Plane + IdP OIDC).
 *
 * <p>Até a 3.5.0 o filtro recebia um único bean por {@code @Autowired}: com dois, a injeção era ambígua e a aplicação
 * <b>não subia</b>. Agora recebe a lista e combina de forma conservadora. O contexto deste teste registra uma política
 * que aceita tudo e outra que recusa o JWT legado — a combinação tem de recusar.
 */
@SpringBootTest(classes = ArchbaseSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-duas-politicas-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=tenant-teste",
        "teste.duas-politicas.enabled=true"
})
@DisplayName("Duas políticas de provedor externo no mesmo contexto (fase 1a)")
class DuasPoliticasExternasNoContextoTest {

    private static final String EMAIL = "duas-politicas@fase1a.com.br";
    private static final String SENHA = "senha-de-teste";

    @Autowired
    ArchbaseJwtAuthenticationFilter filtro;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    AccessTokenJpaRepository tokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("o contexto sobe e a política efetiva é a combinação conservadora das duas")
    void sobeECombina() throws Exception {
        ExternalAuthenticationPolicy efetiva =
                (ExternalAuthenticationPolicy) ReflectionTestUtils.getField(filtro, "externalPolicy");

        assertThat(efetiva).isNotNull();
        assertThat(efetiva.acceptLegacyJwt()).as("uma das duas recusa o JWT legado").isFalse();
        assertThat(efetiva.allowDefaultTenant()).isTrue();

        // E o efeito chega na requisição: o login local ainda emite o token, mas ele não autentica.
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name("Duas Políticas").description("Duas Políticas")
                .email(EMAIL).userName(EMAIL)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(true).accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true).allowPasswordChange(true)
                .allowMultipleLogins(true).unlimitedAccessHours(true)
                .build();
        user.setTenantId(ArchbaseSecurityTestApplication.TENANT);
        userRepository.save(user);

        ObjectMapper json = new ObjectMapper();
        String access = json.readTree(mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", EMAIL, "password", SENHA))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("access_token").asText();

        mockMvc.perform(get("/api/v1/user/findAll?page=0&size=10").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }
}
