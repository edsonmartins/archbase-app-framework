package br.com.archbase.oidc.it;

import br.com.archbase.oidc.user.ExternalIdentityStore;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * IdP externo de ponta a ponta (fase 1b): token do IdP falso → filtro → módulo OIDC → usuário local → endpoint.
 *
 * <p>Quatro provedores no mesmo contexto, cada um com uma estratégia diferente — é a garantia de que nenhum IdP tem
 * código próprio, só configuração:
 * <ul>
 * <li><b>archguard</b>: tenant por claim ({@code owner}), claim exigido ({@code tokenType}), JWKS explícito,
 * senha local desligada ao vincular;</li>
 * <li><b>keycloak</b>: JWKS por descoberta, tenant fixo;</li>
 * <li><b>entra</b>: emissor por modelo com {@code {tid}}, {@code allowed-tenants}, sujeito em {@code oid}, sem
 * vínculo por e-mail;</li>
 * <li><b>cabecalho</b>: tenant pelo {@code X-TENANT-ID}, só com vínculo prévio.</li>
 * </ul>
 */
@SpringBootTest(classes = OidcTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:archbase-oidc-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
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
@DisplayName("IdP externo OIDC de ponta a ponta (fase 1b)")
class OidcIntegrationTest {

    static final FakeOidcProvider IDP = new FakeOidcProvider();

    static final String TENANT = OidcTestApplication.TENANT;
    static final String ARCHGUARD_AUD = "app-archbase";
    static final String EMAIL = "ana@empresa.com.br";
    static final String SENHA = "senha-local";

    @DynamicPropertySource
    static void provedores(DynamicPropertyRegistry r) {
        String p = "archbase.security.oidc.providers.";
        r.add(p + "archguard.issuer", () -> IDP.issuer("archguard"));
        r.add(p + "archguard.jwks-uri", () -> IDP.jwksUri("archguard"));
        r.add(p + "archguard.audiences[0]", () -> ARCHGUARD_AUD);
        r.add(p + "archguard.allow-insecure-http", () -> "true");
        r.add(p + "archguard.required-claims.tokenType", () -> "access-token");
        r.add(p + "archguard.tenant.claim", () -> "owner");
        r.add(p + "archguard.users.local-password", () -> "disable-on-link");

        r.add(p + "keycloak.issuer", () -> IDP.issuer("realms/empresa"));
        r.add(p + "keycloak.audiences[0]", () -> "archbase-api");
        r.add(p + "keycloak.allow-insecure-http", () -> "true");
        r.add(p + "keycloak.tenant.fixed", () -> TENANT);

        r.add(p + "entra.issuer-pattern", () -> IDP.baseUrl() + "/{tid}/v2.0");
        r.add(p + "entra.jwks-uri", () -> IDP.jwksUri("common"));
        r.add(p + "entra.allowed-tenants.tid-cliente-a", () -> TENANT);
        r.add(p + "entra.audiences[0]", () -> "api://archbase");
        r.add(p + "entra.allow-insecure-http", () -> "true");
        r.add(p + "entra.claims.subject", () -> "oid");
        r.add(p + "entra.users.link-by-email", () -> "never");

        r.add(p + "cabecalho.issuer", () -> IDP.issuer("okta/oauth2/default"));
        r.add(p + "cabecalho.jwks-uri", () -> IDP.jwksUri("okta"));
        r.add(p + "cabecalho.audiences[0]", () -> "api://default");
        r.add(p + "cabecalho.allow-insecure-http", () -> "true");
        r.add(p + "cabecalho.tenant.header", () -> "true");
    }

    @AfterAll
    static void desligar() {
        IDP.close();
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserJpaRepository userRepository;
    @Autowired
    AccessTokenJpaRepository tokenRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ExternalIdentityStore links;
    @Autowired
    JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private UserEntity ana;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM " + ExternalIdentityStore.TABLE);
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        ana = userRepository.save(usuario(EMAIL));
        userRepository.save(usuario("bruno@empresa.com.br"));
        IDP.publish("rsa-1", true);
    }

    // ═══════════════════════════ estilo ArchGuard ═══════════════════════════

    @Nested
    @DisplayName("archguard: tenant por claim, claim exigido, senha local desligada ao vincular")
    class ArchGuard {

        @Test
        @DisplayName("token válido com e-mail verificado autentica e grava o vínculo")
        void autenticaEVincula() throws Exception {
            MockHttpServletResponse r = eu(archguard("sub-ana").sign());

            assertThat(r.getStatus()).isEqualTo(200);
            JsonNode corpo = json.readTree(r.getContentAsString());
            assertThat(corpo.get("email").asString()).isEqualTo(EMAIL);
            assertThat(corpo.get("tipo").asString()).isEqualTo("OidcAuthentication");
            assertThat(corpo.get("tenant").asString()).isEqualTo(TENANT);
            assertThat(links.findUserId("archguard", TENANT, "sub-ana")).contains(ana.getId());
        }

        @Test
        @DisplayName("depois do vínculo, o e-mail pode mudar no IdP sem perder o acesso")
        void vinculoSobreviveATrocaDeEmail() throws Exception {
            eu(archguard("sub-ana").sign());
            String novoEmail = archguard("sub-ana").claim("email", "ana.souza@outro.com").sign();

            assertThat(eu(novoEmail).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("e-mail NÃO verificado: 403 USER_NOT_LINKED, nenhum vínculo gravado")
        void emailNaoVerificado() throws Exception {
            MockHttpServletResponse r = eu(archguard("sub-ana").claim("email_verified", false).sign());

            assertRecusa(r, 403, "USER_NOT_LINKED");
            assertThat(links.findUserId("archguard", TENANT, "sub-ana")).isEmpty();
        }

        @Test
        @DisplayName("e-mail sem cadastro local: 403 USER_NOT_LINKED (nada é criado)")
        void semCadastro() throws Exception {
            assertRecusa(eu(archguard("sub-x").claim("email", "ninguem@empresa.com.br").sign()), 403, "USER_NOT_LINKED");
            assertThat(userRepository.findByEmail("ninguem@empresa.com.br")).isEmpty();
        }

        @Test
        @DisplayName("segunda identidade do IdP com o mesmo e-mail não assume usuário já vinculado")
        void conflitoDeVinculo() throws Exception {
            eu(archguard("sub-ana").sign());

            assertRecusa(eu(archguard("sub-impostor").sign()), 403, "USER_NOT_LINKED");
        }

        @Test
        @DisplayName("expirado: 401 TOKEN_EXPIRED com WWW-Authenticate")
        void expirado() throws Exception {
            MockHttpServletResponse r = eu(archguard("sub-ana").expiresAt(Instant.now().minusSeconds(600)).sign());

            assertRecusa(r, 401, "TOKEN_EXPIRED");
            assertThat(r.getHeader("WWW-Authenticate")).startsWith("Bearer");
        }

        @Test
        @DisplayName("sem exp: 401 — token sem validade não é aceito")
        void semExp() throws Exception {
            assertRecusa(eu(archguard("sub-ana").withoutExp().sign()), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("aud de outra aplicação: 401")
        void audErrado() throws Exception {
            assertRecusa(eu(archguard("sub-ana").aud("outra-app").sign()), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("refresh token do ArchGuard (tokenType=refresh-token) não vale como access: 401")
        void refreshNaoValeComoAccess() throws Exception {
            assertRecusa(eu(archguard("sub-ana").claim("tokenType", "refresh-token").sign()), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("assinado por chave não publicada: 401")
        void chaveDesconhecida() throws Exception {
            IDP.addRsaKey("rsa-intrusa", false);
            assertRecusa(eu(archguard("sub-ana").kid("rsa-intrusa").sign()), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("algoritmo fora da lista (ES256 com chave publicada, provedor só aceita RS256): 401")
        void algoritmoNaoPermitido() throws Exception {
            assertRecusa(eu(archguard("sub-ana").kid("ec-1").sign()), 401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("rotação de chave: kid novo publicado é buscado no JWKS e aceito")
        void rotacao() throws Exception {
            eu(archguard("sub-ana").sign());
            IDP.addRsaKey("rsa-2", true);
            IDP.publish("rsa-1", false);

            assertThat(eu(archguard("sub-ana").kid("rsa-2").sign()).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("X-TENANT-ID diferente do tenant do token: 403 TENANT_MISMATCH")
        void tenantDivergente() throws Exception {
            MockHttpServletResponse r = mockMvc.perform(get("/teste/eu")
                            .header("Authorization", "Bearer " + archguard("sub-ana").sign())
                            .header("X-TENANT-ID", "outro-tenant"))
                    .andReturn().getResponse();
            assertRecusa(r, 403, "TENANT_MISMATCH");
        }

        @Test
        @DisplayName("token sem o claim de tenant: 403 TENANT_REQUIRED")
        void semTenant() throws Exception {
            assertRecusa(eu(archguard("sub-ana").claim("owner", null).sign()), 403, "TENANT_REQUIRED");
        }

        @Test
        @DisplayName("usuário local bloqueado: 403 USER_DISABLED")
        void bloqueado() throws Exception {
            ana.setAccountLocked(true);
            userRepository.save(ana);
            assertRecusa(eu(archguard("sub-ana").sign()), 403, "USER_DISABLED");
        }

        @Test
        @DisplayName("local-password=disable-on-link: vinculada, Ana não entra mais com senha; Bruno, sim")
        void senhaLocalDesligadaAoVincular() throws Exception {
            assertThat(loginLocal(EMAIL).getStatus()).as("antes do vínculo").isEqualTo(200);

            eu(archguard("sub-ana").sign());

            assertThat(loginLocal(EMAIL).getStatus()).as("depois do vínculo").isEqualTo(401);
            assertThat(loginLocal("bruno@empresa.com.br").getStatus()).isEqualTo(200);
        }
    }

    // ═══════════════════════════ estilo Keycloak ═══════════════════════════

    @Nested
    @DisplayName("keycloak: JWKS por descoberta, tenant fixo")
    class Keycloak {

        @Test
        @DisplayName("descobre o JWKS pelo emissor e autentica no tenant fixo")
        void autentica() throws Exception {
            String token = IDP.token(IDP.issuer("realms/empresa")).aud("archbase-api")
                    .claim("sub", "kc-ana").claim("email", EMAIL).claim("email_verified", true).sign();

            MockHttpServletResponse r = eu(token);

            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(json.readTree(r.getContentAsString()).get("tenant").asString()).isEqualTo(TENANT);
            // Senha local deste provedor é keep (padrão): continua valendo.
            assertThat(loginLocal(EMAIL).getStatus()).isEqualTo(200);
        }
    }

    // ═══════════════════════════ estilo Entra ID ═══════════════════════════

    @Nested
    @DisplayName("entra: emissor por modelo {tid}, allowed-tenants, sujeito em oid, só vínculo prévio")
    class Entra {

        private FakeOidcProvider.Token entra(String tid) {
            return IDP.token(IDP.baseUrl() + "/" + tid + "/v2.0").aud("api://archbase")
                    .claim("sub", "pairwise-por-app").claim("oid", "oid-ana").claim("tid", tid)
                    .claim("email", EMAIL).claim("email_verified", true);
        }

        @Test
        @DisplayName("tid permitido e vínculo prévio pelo oid: autentica")
        void comVinculo() throws Exception {
            links.link("entra", TENANT, "oid-ana", ana.getId());

            assertThat(eu(entra("tid-cliente-a").sign()).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("tid permitido sem vínculo: 403 — link-by-email=never ignora até e-mail verificado")
        void semVinculo() throws Exception {
            assertRecusa(eu(entra("tid-cliente-a").sign()), 403, "USER_NOT_LINKED");
        }

        @Test
        @DisplayName("tid fora de allowed-tenants: 401")
        void tidNaoPermitido() throws Exception {
            links.link("entra", TENANT, "oid-ana", ana.getId());
            assertRecusa(eu(entra("tid-intruso").sign()), 401, "INVALID_TOKEN");
        }
    }

    // ═══════════════════════════ tenant pelo header ═══════════════════════════

    @Nested
    @DisplayName("cabecalho: tenant pelo X-TENANT-ID, só com vínculo naquele tenant")
    class Cabecalho {

        private String token() {
            return IDP.token(IDP.issuer("okta/oauth2/default")).aud("api://default")
                    .claim("sub", "okta-ana").claim("email", EMAIL).claim("email_verified", true).sign();
        }

        @Test
        @DisplayName("sem X-TENANT-ID: 403 TENANT_REQUIRED")
        void semHeader() throws Exception {
            assertRecusa(eu(token()), 403, "TENANT_REQUIRED");
        }

        @Test
        @DisplayName("com header e sem vínculo: 403 — e-mail verificado não basta quando o cliente escolhe o tenant")
        void semVinculo() throws Exception {
            assertRecusa(euNoTenant(token(), TENANT), 403, "USER_NOT_LINKED");
        }

        @Test
        @DisplayName("com header e vínculo naquele tenant: autentica")
        void comVinculo() throws Exception {
            links.link("cabecalho", TENANT, "okta-ana", ana.getId());
            assertThat(euNoTenant(token(), TENANT).getStatus()).isEqualTo(200);
        }
    }

    // ═══════════════════════════ convivência ═══════════════════════════

    @Nested
    @DisplayName("convivência com o login próprio")
    class Convivencia {

        @Test
        @DisplayName("JWT local continua autenticando com o módulo ligado")
        void jwtLocal() throws Exception {
            String access = json.readTree(loginLocal("bruno@empresa.com.br").getContentAsString())
                    .get("access_token").asString();

            MockHttpServletResponse r = eu(access);

            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(json.readTree(r.getContentAsString()).get("tipo").asString())
                    .isEqualTo("UsernamePasswordAuthenticationToken");
        }

        @Test
        @DisplayName("token de emissor desconhecido não é deste módulo: segue para o fluxo próprio e dá 401")
        void emissorDesconhecido() throws Exception {
            String token = IDP.token("https://idp-desconhecido.example.com").aud(ARCHGUARD_AUD).sign();
            assertThat(eu(token).getStatus()).isEqualTo(401);
        }
    }

    // ═══════════════════════════ apoio ═══════════════════════════

    private FakeOidcProvider.Token archguard(String sub) {
        return IDP.token(IDP.issuer("archguard")).aud(ARCHGUARD_AUD)
                .claim("sub", sub).claim("owner", TENANT).claim("tokenType", "access-token")
                .claim("email", EMAIL).claim("email_verified", true);
    }

    private MockHttpServletResponse eu(String token) throws Exception {
        return mockMvc.perform(get("/teste/eu").header("Authorization", "Bearer " + token)).andReturn().getResponse();
    }

    private MockHttpServletResponse euNoTenant(String token, String tenant) throws Exception {
        return mockMvc.perform(get("/teste/eu").header("Authorization", "Bearer " + token)
                .header("X-TENANT-ID", tenant)).andReturn().getResponse();
    }

    private MockHttpServletResponse loginLocal(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", SENHA))))
                .andReturn().getResponse();
    }

    private void assertRecusa(MockHttpServletResponse r, int status, String code) throws Exception {
        assertThat(r.getStatus()).isEqualTo(status);
        assertThat(json.readTree(r.getContentAsString()).get("code").asString()).isEqualTo(code);
    }

    private UserEntity usuario(String email) {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID().toString())
                .createEntityDate(LocalDateTime.now())
                .name(email).description(email)
                .email(email).userName(email)
                .password(passwordEncoder.encode(SENHA))
                .isAdministrator(false).accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true).allowPasswordChange(true)
                .allowMultipleLogins(true).unlimitedAccessHours(true)
                .build();
        user.setTenantId(TENANT);
        return user;
    }
}
