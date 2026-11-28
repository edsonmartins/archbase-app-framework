package br.com.archbase.oidc.it;

import br.com.archbase.controlplane.fixtures.MeterRegistryConfig;
import br.com.archbase.controlplane.it.FakeControlPlane;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Control Plane e IdP externo (OIDC) ligados no mesmo contexto: os dois {@code ExternalBearerAuthenticator} convivem —
 * cada um aceita o próprio token e deixa o do outro seguir para o seguinte — e o login local continua.
 *
 * <p>Até aqui cada módulo era testado sozinho; nenhum teste subia os dois juntos.
 */
@SpringBootTest(classes = OidcTestApplication.class)
@AutoConfigureMockMvc
@Import(MeterRegistryConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cp-com-oidc;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.default.id=" + OidcTestApplication.TENANT,
        "archbase.app.tenant.fail-on-missing=true",
        "archbase.controlplane.enabled=true",
        "archbase.controlplane.url=" + FakeControlPlane.ISSUER,
        "archbase.controlplane.public-url=" + FakeControlPlane.AUDIENCE,
        "archbase.controlplane.product=powerview",
        "archbase.controlplane.legacy-jwt=accept",
        "archbase.controlplane.jwks.refresh-min-interval=0s"
})
@DisplayName("Control Plane e OIDC no mesmo contexto")
class ControlPlaneComOidcTest {

    static final String TENANT = OidcTestApplication.TENANT;
    static final String EMAIL = "ana@empresa.com.br";
    static final String AUD = "app-archbase";
    static final String KID_CP = "cp-2026-11";

    static final FakeOidcProvider IDP = new FakeOidcProvider().addRsaKey("rsa-oidc", true);
    static final FakeControlPlane CP = new FakeControlPlane().addKey(KID_CP, true);

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry r) {
        String p = "archbase.security.oidc.providers.archguard.";
        r.add(p + "issuer", () -> IDP.issuer("archguard"));
        r.add(p + "jwks-uri", () -> IDP.jwksUri("archguard"));
        r.add(p + "audiences[0]", () -> AUD);
        r.add(p + "allow-insecure-http", () -> "true");
        r.add(p + "tenant.claim", () -> "owner");
        r.add("archbase.controlplane.jwks.url", CP::jwksUrl);
    }

    @AfterAll
    static void parar() {
        IDP.close();
        CP.close();
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    UserJpaRepository users;
    @Autowired
    PasswordEncoder encoder;

    @BeforeEach
    void usuario() {
        if (users.findByEmail(EMAIL).isEmpty()) {
            UserEntity u = UserEntity.builder()
                    .id(UUID.randomUUID().toString()).createEntityDate(LocalDateTime.now())
                    .name(EMAIL).description(EMAIL).email(EMAIL).userName(EMAIL)
                    .password(encoder.encode("senha-local"))
                    .isAdministrator(false).accountDeactivated(false).accountLocked(false)
                    .changePasswordOnNextLogin(false).passwordNeverExpires(true).allowPasswordChange(true)
                    .allowMultipleLogins(true).unlimitedAccessHours(true)
                    .build();
            u.setTenantId(TENANT);
            users.save(u);
        }
    }

    @Test
    @DisplayName("token do IdP externo autentica")
    void tokenOidc() throws Exception {
        String token = IDP.token(IDP.issuer("archguard")).aud(AUD)
                .claim("sub", "sub-ana").claim("owner", TENANT)
                .claim("email", EMAIL).claim("email_verified", true).sign();
        mvc.perform(get("/teste/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.tenant").value(TENANT));
    }

    @Test
    @DisplayName("token de sessão do Control Plane autentica")
    void tokenControlPlane() throws Exception {
        mvc.perform(get("/teste/eu").header("Authorization", CP.session(KID_CP, "cp-ana", EMAIL, TENANT, "member").bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.tenant").value(TENANT));
    }

    @Test
    @DisplayName("token que nenhum dos dois reconhece é recusado")
    void tokenDesconhecido() throws Exception {
        mvc.perform(get("/teste/eu").header("Authorization", "Bearer eyJhbGciOiJub25lIn0.e30."))
                .andExpect(status().isUnauthorized());
    }
}
