package br.com.archbase.controlplane.it;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.service.ArchbaseJwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ControlPlaneTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cp-reject;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=/api/publico/**",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=" + ControlPlaneTestApplication.DEFAULT_TENANT,
        "archbase.app.tenant.fail-on-missing=true",
        "archbase.controlplane.enabled=true",
        "archbase.controlplane.url=" + FakeControlPlane.ISSUER,
        "archbase.controlplane.public-url=" + FakeControlPlane.AUDIENCE,
        "archbase.controlplane.product=powerview",
        "archbase.controlplane.legacy-jwt=reject"
})
@DisplayName("Control Plane: legacy-jwt=reject")
class ControlPlaneRejectModeIntegrationTest {

    static final String TENANT = "tenant-a";
    static final String KID = "cp-reject";
    static final String EMAIL = "bia@cliente.com.br";

    static final FakeControlPlane CP = new FakeControlPlane().addKey(KID, true);

    @DynamicPropertySource
    static void jwks(DynamicPropertyRegistry registry) {
        registry.add("archbase.controlplane.jwks.url", CP::jwksUrl);
        registry.add("archbase.security.jwt.secret-key", TestSecrets::legacyJwtKey);
    }

    @AfterAll
    static void stop() {
        CP.close();
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    UserJpaRepository users;
    @Autowired
    AccessTokenJpaRepository tokens;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    ArchbaseJwtService jwtService;

    private UserEntity bia;

    @BeforeEach
    void setUp() {
        ArchbaseTenantContext.setTenantId(TENANT);
        tokens.deleteAll();
        users.deleteAll();
        ArchbaseTenantContext.clear();
        bia = TestUsers.create(users, encoder, TENANT, EMAIL, "senha", true);
    }

    @AfterEach
    void clear() {
        ArchbaseTenantContext.clear();
    }

    @Test
    @DisplayName("o token do Control Plane passa")
    void tokenNovoPassa() throws Exception {
        mvc.perform(get("/api/teste/quem")
                        .header("Authorization", CP.session(KID, "cp-bia", EMAIL, TENANT, "member").bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localUser").value(bia.getId()));
    }

    @Test
    @DisplayName("o JWT próprio do Archbase deixa de autenticar")
    void jwtLegadoRecusado() throws Exception {
        ArchbaseTenantContext.setTenantId(TENANT);
        String legado = jwtService.generateToken(bia).token();
        ArchbaseTenantContext.clear();

        mvc.perform(get("/api/teste/quem").header("Authorization", "Bearer " + legado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("as rotas de login local deixam de responder")
    void rotasDeLoginDesligadas() throws Exception {
        for (String rota : new String[]{"/api/v1/auth/authenticate", "/api/v1/auth/login",
                "/api/v1/auth/register", "/api/v1/auth/resetPassword", "/api/v1/auth/refresh-token",
                "/api/v1/auth/sendResetPasswordEmail/" + EMAIL}) {
            mvc.perform(post(rota).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("LOCAL_LOGIN_DISABLED"));
        }
        mvc.perform(get("/api/v1/auth/tenants").param("email", EMAIL))
                .andExpect(status().isNotFound());
    }
}
