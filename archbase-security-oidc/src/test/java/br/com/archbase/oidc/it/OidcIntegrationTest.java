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
import java.util.List;
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

    static final FakeOidcProvider IDP = new FakeOidcProvider().addRsaKey("rsa-fora", false);

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
        r.add(p + "archguard.logout.end-session-uri", () -> IDP.issuer("archguard") + "/api/logout");
        r.add(p + "archguard.logout.backchannel.enabled", () -> "true");

        r.add(p + "keycloak.issuer", () -> IDP.issuer("realms/empresa"));
        r.add(p + "keycloak.audiences[0]", () -> "archbase-api");
        r.add(p + "keycloak.allow-insecure-http", () -> "true");
        r.add(p + "keycloak.tenant.fixed", () -> TENANT);
        r.add(p + "keycloak.logout.backchannel.enabled", () -> "true");
        r.add(p + "keycloak.logout.backchannel.audiences[0]", () -> "archbase-web");

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

        r.add(p + "jit.issuer", () -> IDP.issuer("jit"));
        r.add(p + "jit.jwks-uri", () -> IDP.jwksUri("jit"));
        r.add(p + "jit.audiences[0]", () -> "app-jit");
        r.add(p + "jit.allow-insecure-http", () -> "true");
        r.add(p + "jit.tenant.fixed", () -> TENANT);
        r.add(p + "jit.users.provisioning", () -> "jit");

        r.add(p + "jitativo.issuer", () -> IDP.issuer("jitativo"));
        r.add(p + "jitativo.jwks-uri", () -> IDP.jwksUri("jitativo"));
        r.add(p + "jitativo.audiences[0]", () -> "app-jit");
        r.add(p + "jitativo.allow-insecure-http", () -> "true");
        r.add(p + "jitativo.tenant.fixed", () -> TENANT);
        r.add(p + "jitativo.users.provisioning", () -> "jit");
        r.add(p + "jitativo.users.jit.status", () -> "active");

        r.add(p + "papeis.issuer", () -> IDP.issuer("papeis"));
        r.add(p + "papeis.jwks-uri", () -> IDP.jwksUri("papeis"));
        r.add(p + "papeis.audiences[0]", () -> "app-papeis");
        r.add(p + "papeis.allow-insecure-http", () -> "true");
        r.add(p + "papeis.tenant.fixed", () -> TENANT);
        r.add(p + "papeis.claims.roles[0]", () -> "realm_access.roles");
        r.add(p + "papeis.claims.roles[1]", () -> "roles");
        r.add(p + "papeis.role-mapping.sync", () -> "add");
        r.add(p + "papeis.role-mapping.map.financeiro", () -> "Financeiro");
        r.add(p + "papeis.role-mapping.map.[cliente-a/vendas]", () -> "Vendas");
        r.add(p + "papeis.role-mapping.map.fantasma", () -> "GrupoQueNaoExiste");

        r.add(p + "espelho.issuer", () -> IDP.issuer("espelho"));
        r.add(p + "espelho.jwks-uri", () -> IDP.jwksUri("espelho"));
        r.add(p + "espelho.audiences[0]", () -> "app-papeis");
        r.add(p + "espelho.allow-insecure-http", () -> "true");
        r.add(p + "espelho.tenant.fixed", () -> TENANT);
        r.add(p + "espelho.claims.roles[0]", () -> "groups");
        r.add(p + "espelho.role-mapping.sync", () -> "mirror");
        r.add(p + "espelho.role-mapping.map.financeiro", () -> "Financeiro");
        r.add(p + "espelho.role-mapping.map.vendas", () -> "Vendas");

        for (String id : List.of("intro", "introaberta")) {
            r.add(p + id + ".issuer", () -> IDP.issuer(id));
            r.add(p + id + ".jwks-uri", () -> IDP.jwksUri(id));
            r.add(p + id + ".audiences[0]", () -> "app-intro");
            r.add(p + id + ".allow-insecure-http", () -> "true");
            r.add(p + id + ".tenant.fixed", () -> TENANT);
            r.add(p + id + ".introspection.enabled", () -> "true");
            r.add(p + id + ".introspection.uri", () -> IDP.issuer(id) + "/introspect");
            r.add(p + id + ".introspection.client-id", () -> FakeOidcProvider.INTROSPECTION_CLIENT);
            r.add(p + id + ".introspection.client-secret", () -> FakeOidcProvider.INTROSPECTION_SECRET);
        }
        r.add(p + "introaberta.introspection.on-error", () -> "allow");

        r.add("archbase.security.oidc.exchange.enabled", () -> "true");
        r.add("archbase.security.oidc.exchange.local-token-ttl", () -> "10m");
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
    org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    br.com.archbase.security.repository.GroupJpaRepository groupRepository;

    private final ObjectMapper json = new ObjectMapper();
    private UserEntity ana;

    @BeforeEach
    void preparar() {
        jdbc.update("DELETE FROM " + ExternalIdentityStore.TABLE);
        jdbc.update("DELETE FROM " + br.com.archbase.oidc.user.RevokedTokenStore.TABLE);
        jdbc.update("DELETE FROM " + br.com.archbase.oidc.user.ExternalLogoutStore.TABLE);
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();
        ana = userRepository.save(usuario(EMAIL));
        userRepository.save(usuario("bruno@empresa.com.br"));
        IDP.publish("rsa-1", true);
        IDP.resetIntrospection();
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

    // ═══════════════════════════ troca de token (fase 1c) ═══════════════════════════

    @Nested
    @DisplayName("troca de token: POST /api/v1/auth/oidc/exchange")
    class TrocaDeToken {

        @Test
        @DisplayName("token válido vira sessão local curta, sem refresh, que autentica como o login próprio")
        void troca() throws Exception {
            MockHttpServletResponse r = trocar(archguard("sub-ana").sign(), null);

            assertThat(r.getStatus()).isEqualTo(200);
            JsonNode corpo = json.readTree(r.getContentAsString());
            String access = corpo.get("access_token").asString();
            assertThat(corpo.has("refresh_token") && !corpo.get("refresh_token").isNull()).isFalse();

            JsonNode payload = json.readTree(java.util.Base64.getUrlDecoder().decode(access.split("\\.")[1]));
            assertThat(payload.get("exp").asLong() - payload.get("iat").asLong()).isLessThanOrEqualTo(600);

            MockHttpServletResponse eu = eu(access);
            assertThat(eu.getStatus()).isEqualTo(200);
            assertThat(json.readTree(eu.getContentAsString()).get("tipo").asString())
                    .isEqualTo("UsernamePasswordAuthenticationToken");
        }

        @Test
        @DisplayName("token inválido, ausente ou de emissor desconhecido: 401")
        void invalido() throws Exception {
            assertRecusa(trocar(archguard("sub-ana").aud("outra-app").sign(), null), 401, "INVALID_TOKEN");
            assertRecusa(trocar(null, null), 401, "INVALID_TOKEN");
            assertRecusa(trocar(IDP.token("https://desconhecido.example.com").aud(ARCHGUARD_AUD).sign(), null),
                    401, "INVALID_TOKEN");
        }

        @Test
        @DisplayName("usuário sem vínculo possível: 403 USER_NOT_LINKED, nenhuma sessão emitida")
        void semVinculo() throws Exception {
            assertRecusa(trocar(archguard("sub-ana").claim("email_verified", false).sign(), null), 403,
                    "USER_NOT_LINKED");
            assertThat(tokenRepository.count()).isZero();
        }

        @Test
        @DisplayName("mesma regra de tenant do filtro: divergente 403, header obrigatório quando é a fonte")
        void regraDeTenant() throws Exception {
            assertRecusa(trocar(archguard("sub-ana").sign(), "outro-tenant"), 403, "TENANT_MISMATCH");

            String okta = IDP.token(IDP.issuer("okta/oauth2/default")).aud("api://default")
                    .claim("sub", "okta-ana").claim("email", EMAIL).claim("email_verified", true).sign();
            assertRecusa(trocar(okta, null), 403, "TENANT_REQUIRED");
            links.link("cabecalho", TENANT, "okta-ana", ana.getId());
            assertThat(trocar(okta, TENANT).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("usuário de sessão única: a troca encerra a sessão local anterior")
        void sessaoUnica() throws Exception {
            ana.setAllowMultipleLogins(false);
            userRepository.save(ana);
            String anterior = json.readTree(loginLocalSemPolitica(EMAIL).getContentAsString())
                    .get("access_token").asString();
            assertThat(eu(anterior).getStatus()).isEqualTo(200);

            assertThat(trocar(keycloakAna(), null).getStatus()).isEqualTo(200);

            assertThat(eu(anterior).getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("usuário com várias sessões permitidas: a sessão local anterior continua valendo")
        void variasSessoes() throws Exception {
            String anterior = json.readTree(loginLocalSemPolitica(EMAIL).getContentAsString())
                    .get("access_token").asString();

            assertThat(trocar(keycloakAna(), null).getStatus()).isEqualTo(200);

            assertThat(eu(anterior).getStatus()).isEqualTo(200);
        }

        /** Keycloak tem local-password=keep: a senha local segue valendo para montar a sessão anterior. */
        private String keycloakAna() {
            return IDP.token(IDP.issuer("realms/empresa")).aud("archbase-api")
                    .claim("sub", "kc-ana").claim("email", EMAIL).claim("email_verified", true).sign();
        }

        private MockHttpServletResponse loginLocalSemPolitica(String email) throws Exception {
            MockHttpServletResponse r = loginLocal(email);
            assertThat(r.getStatus()).as("login local de " + email).isEqualTo(200);
            return r;
        }

        private MockHttpServletResponse trocar(String token, String tenantHeader) throws Exception {
            var req = post("/api/v1/auth/oidc/exchange").contentType(MediaType.APPLICATION_JSON)
                    .content(token == null ? "{}" : json.writeValueAsString(Map.of("token", token)));
            if (tenantHeader != null) {
                req.header("X-TENANT-ID", tenantHeader);
            }
            return mockMvc.perform(req).andReturn().getResponse();
        }
    }

    // ═══════════════════════════ fase 2a: criação no primeiro acesso ═══════════════════════════

    @Nested
    @DisplayName("jit: cria o usuário no primeiro acesso")
    class Jit {

        private FakeOidcProvider.Token jit(String provedor, String sub, String email) {
            return IDP.token(IDP.issuer(provedor)).aud("app-jit").claim("sub", sub)
                    .claim("email", email).claim("email_verified", true).claim("name", "Carla Nova");
        }

        @Test
        @DisplayName("padrão PENDING: cria inativo, sem senha, sem poder redefinir, não admin; entra depois de ativado")
        void pendenteAteAprovar() throws Exception {
            assertRecusa(eu(jit("jit", "sub-carla", "carla@empresa.com.br").sign()), 403, "USER_DISABLED");

            UserEntity carla = userRepository.findByEmail("carla@empresa.com.br").orElseThrow();
            assertThat(carla.getAccountDeactivated()).isTrue();
            assertThat(carla.getPassword()).isNull();
            assertThat(carla.getAllowPasswordChange()).isFalse();
            assertThat(carla.getIsAdministrator()).isFalse();
            assertThat(carla.getName()).isEqualTo("Carla Nova");
            assertThat(links.findUserId("jit", TENANT, "sub-carla")).contains(carla.getId());

            carla.setAccountDeactivated(false);
            userRepository.save(carla);
            assertThat(eu(jit("jit", "sub-carla", "carla@empresa.com.br").sign()).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("status ACTIVE: entra no primeiro acesso")
        void ativo() throws Exception {
            assertThat(eu(jit("jitativo", "sub-davi", "davi@empresa.com.br").sign()).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("e-mail não verificado: não cria")
        void naoVerificado() throws Exception {
            assertRecusa(eu(jit("jitativo", "sub-eva", "eva@empresa.com.br").claim("email_verified", false).sign()),
                    403, "USER_NOT_LINKED");
            assertThat(userRepository.findByEmail("eva@empresa.com.br")).isEmpty();
        }

        @Test
        @DisplayName("e-mail já cadastrado e verificado: vincula ao existente, não duplica")
        void naoDuplica() throws Exception {
            long antes = userRepository.count();
            assertThat(eu(jit("jitativo", "sub-bruno", "bruno@empresa.com.br").sign()).getStatus()).isEqualTo(200);
            assertThat(userRepository.count()).isEqualTo(antes);
        }

        @Test
        @DisplayName("e-mail já cadastrado mas não verificado: nem cria outro nem vincula")
        void existenteNaoVerificado() throws Exception {
            long antes = userRepository.count();
            assertRecusa(eu(jit("jitativo", "sub-bruno", "bruno@empresa.com.br").claim("email_verified", false)
                    .sign()), 403, "USER_NOT_LINKED");
            assertThat(userRepository.count()).isEqualTo(antes);
        }
    }

    // ═══════════════════════════ fase 2a: vínculo manual ═══════════════════════════

    @Nested
    @DisplayName("API administrativa de vínculo: /api/v1/security/external-identities")
    class VinculoManual {

        private static final String API = "/api/v1/security/external-identities";
        private String admin;

        @BeforeEach
        void administrador() throws Exception {
            UserEntity adm = usuario("admin@empresa.com.br");
            adm.setIsAdministrator(true);
            userRepository.save(adm);
            admin = json.readTree(loginLocal("admin@empresa.com.br").getContentAsString()).get("access_token").asString();
        }

        private FakeOidcProvider.Token entraAna() {
            return IDP.token(IDP.baseUrl() + "/tid-cliente-a/v2.0").aud("api://archbase")
                    .claim("oid", "oid-ana").claim("tid", "tid-cliente-a");
        }

        private MockHttpServletResponse vincular(String bearer, String provider, String subject, String userId)
                throws Exception {
            return mockMvc.perform(post(API).header("Authorization", "Bearer " + bearer)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("providerId", provider, "subject", subject,
                                    "userId", userId))))
                    .andReturn().getResponse();
        }

        @Test
        @DisplayName("administrador vincula; a identidade passa a entrar; desvincular corta o acesso")
        void vinculaEDesvincula() throws Exception {
            assertRecusa(eu(entraAna().sign()), 403, "USER_NOT_LINKED");

            assertThat(vincular(admin, "entra", "oid-ana", ana.getId()).getStatus()).isEqualTo(201);
            assertThat(eu(entraAna().sign()).getStatus()).isEqualTo(200);

            JsonNode lista = json.readTree(mockMvc.perform(get(API).param("userId", ana.getId())
                    .header("Authorization", "Bearer " + admin)).andReturn().getResponse().getContentAsString());
            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).get("providerId").asString()).isEqualTo("entra");

            assertThat(mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete(API + "/entra/oid-ana").header("Authorization", "Bearer " + admin))
                    .andReturn().getResponse().getStatus()).isEqualTo(204);
            assertRecusa(eu(entraAna().sign()), 403, "USER_NOT_LINKED");
        }

        @Test
        @DisplayName("quem não é administrador recebe 403, mesmo com admin-endpoints.policy=permit (padrão)")
        void naoAdministrador() throws Exception {
            String bruno = json.readTree(loginLocal("bruno@empresa.com.br").getContentAsString())
                    .get("access_token").asString();

            assertThat(vincular(bruno, "entra", "oid-bruno", ana.getId()).getStatus()).isEqualTo(403);
            assertThat(links.findUserId("entra", TENANT, "oid-bruno")).isEmpty();
        }

        @Test
        @DisplayName("conflitos e validações")
        void conflitos() throws Exception {
            assertThat(vincular(admin, "entra", "oid-ana", ana.getId()).getStatus()).isEqualTo(201);
            assertThat(vincular(admin, "entra", "oid-ana", ana.getId()).getStatus()).as("idempotente").isEqualTo(200);

            UserEntity bruno = userRepository.findByEmail("bruno@empresa.com.br").orElseThrow();
            assertRecusa(vincular(admin, "entra", "oid-ana", bruno.getId()), 409, "IDENTITY_ALREADY_LINKED");
            assertRecusa(vincular(admin, "entra", "oid-outra", ana.getId()), 409, "USER_ALREADY_LINKED");
            assertRecusa(vincular(admin, "nao-existe", "x", ana.getId()), 400, "UNKNOWN_PROVIDER");
            assertRecusa(vincular(admin, "entra", "oid-z", UUID.randomUUID().toString()), 404, "USER_NOT_FOUND");
            assertThat(mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete(API + "/entra/nao-vinculado").header("Authorization", "Bearer " + admin))
                    .andReturn().getResponse().getStatus()).isEqualTo(404);
        }
    }

    // ═══════════════════════════ fase 2b: papéis → grupos ═══════════════════════════

    @Nested
    @DisplayName("role-mapping: papéis do IdP viram participação em grupos locais")
    class Papeis {

        @BeforeEach
        void grupos() {
            for (String nome : List.of("Financeiro", "Vendas", "Manual")) {
                groupRepository.save(br.com.archbase.security.persistence.GroupEntity.builder()
                        .id(UUID.randomUUID().toString()).createEntityDate(LocalDateTime.now())
                        .name(nome).description(nome).build());
            }
        }

        private FakeOidcProvider.Token token(String provedor) {
            return IDP.token(IDP.issuer(provedor)).aud("app-papeis").claim("sub", "sub-ana-" + provedor)
                    .claim("email", EMAIL).claim("email_verified", true);
        }

        private java.util.Set<String> gruposDaAna() {
            return userRepository.findByIdWithGroupsAndProfile(ana.getId()).orElseThrow().getGroups().stream()
                    .map(ug -> ug.getGroup().getName()).collect(java.util.stream.Collectors.toSet());
        }

        private void colocarNoGrupo(String nome) {
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(t -> {
                UserEntity u = userRepository.findByIdWithGroupsAndProfile(ana.getId()).orElseThrow();
                var g = groupRepository.findAll().stream().filter(x -> nome.equals(x.getName())).findFirst().orElseThrow();
                u.getGroups().add(br.com.archbase.security.persistence.UserGroupEntity.builder()
                        .id(UUID.randomUUID().toString()).createEntityDate(LocalDateTime.now()).user(u).group(g).build());
                userRepository.save(u);
            });
        }

        @Test
        @DisplayName("add: papéis mapeados (caminho aninhado e nome com '/') viram grupo; não mapeados são ignorados")
        void adiciona() throws Exception {
            String t = token("papeis")
                    .claim("realm_access", Map.of("roles", List.of("financeiro", "papel-qualquer")))
                    .claim("roles", List.of("cliente-a/vendas"))
                    .sign();

            assertThat(eu(t).getStatus()).isEqualTo(200);
            assertThat(gruposDaAna()).containsExactlyInAnyOrder("Financeiro", "Vendas");
        }

        @Test
        @DisplayName("add nunca remove: papel que sumiu do token mantém o grupo")
        void addNaoRemove() throws Exception {
            eu(token("papeis").claim("roles", List.of("financeiro")).sign());
            eu(token("papeis").claim("roles", List.of()).sign());

            assertThat(gruposDaAna()).containsExactly("Financeiro");
        }

        @Test
        @DisplayName("grupo mapeado que não existe no tenant: ignorado, a requisição segue")
        void grupoInexistente() throws Exception {
            assertThat(eu(token("papeis").claim("roles", List.of("fantasma", "financeiro")).sign()).getStatus())
                    .isEqualTo(200);
            assertThat(gruposDaAna()).containsExactly("Financeiro");
        }

        @Test
        @DisplayName("mirror: remove dos grupos mapeados cujo papel sumiu, e nunca toca grupo atribuído à mão")
        void espelha() throws Exception {
            colocarNoGrupo("Manual");
            eu(token("espelho").claim("groups", List.of("financeiro", "vendas")).sign());
            assertThat(gruposDaAna()).containsExactlyInAnyOrder("Manual", "Financeiro", "Vendas");

            eu(token("espelho").claim("groups", List.of("vendas")).sign());

            assertThat(gruposDaAna()).containsExactlyInAnyOrder("Manual", "Vendas");
        }

        @Test
        @DisplayName("mirror com o claim AUSENTE: nada muda (ex.: overage de grupos do Entra)")
        void claimAusente() throws Exception {
            colocarNoGrupo("Financeiro");
            eu(token("espelho").sign());

            assertThat(gruposDaAna()).containsExactly("Financeiro");
        }

        @Test
        @DisplayName("mirror com o claim PRESENTE e vazio: o IdP disse 'nenhum papel' e os grupos mapeados saem")
        void claimVazio() throws Exception {
            colocarNoGrupo("Financeiro");
            colocarNoGrupo("Manual");
            eu(token("espelho").claim("groups", List.of()).sign());

            assertThat(gruposDaAna()).containsExactly("Manual");
        }

        @Test
        @DisplayName("papel nunca torna ninguém administrador")
        void nuncaAdmin() throws Exception {
            eu(token("papeis").claim("roles", List.of("financeiro", "admin", "administrator")).sign());
            assertThat(userRepository.findById(ana.getId()).orElseThrow().getIsAdministrator()).isFalse();
        }
    }

    // ═══════════════════════════ fase 2c: introspecção ═══════════════════════════

    @Nested
    @DisplayName("introspecção (RFC 7662): revogação antes do exp")
    class Introspeccao {

        private FakeOidcProvider.Token token(String provedor) {
            return IDP.token(IDP.issuer(provedor)).aud("app-intro").claim("sub", "sub-ana-" + provedor)
                    .claim("email", EMAIL).claim("email_verified", true);
        }

        @Test
        @DisplayName("ativo: autentica, e a segunda requisição com o mesmo token usa o cache")
        void ativoComCache() throws Exception {
            String t = token("intro").sign();

            assertThat(eu(t).getStatus()).isEqualTo(200);
            assertThat(eu(t).getStatus()).isEqualTo(200);
            assertThat(IDP.introspectionCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("revogado no IdP: 401 TOKEN_REVOKED, mesmo com assinatura e exp válidos")
        void revogado() throws Exception {
            String t = token("intro").sign();
            IDP.revoke(t);

            MockHttpServletResponse r = eu(t);
            assertRecusa(r, 401, "TOKEN_REVOKED");
            assertThat(r.getHeader("WWW-Authenticate")).startsWith("Bearer");
        }

        @Test
        @DisplayName("token forjado não chega ao IdP: a assinatura é conferida antes")
        void forjadoNaoConsulta() throws Exception {
            IDP.addRsaKey("rsa-forja", false);
            assertRecusa(eu(token("intro").kid("rsa-forja").sign()), 401, "INVALID_TOKEN");
            assertThat(IDP.introspectionCalls()).isZero();
        }

        @Test
        @DisplayName("IdP fora do ar com on-error=deny (padrão): 503 IDP_UNAVAILABLE; a falha não fica em cache")
        void foraDoArNega() throws Exception {
            String t = token("intro").sign();
            IDP.introspectionDown(true);
            assertRecusa(eu(t), 503, "IDP_UNAVAILABLE");

            IDP.introspectionDown(false);
            assertThat(eu(t).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("IdP fora do ar com on-error=allow: aceita pela assinatura")
        void foraDoArPermite() throws Exception {
            IDP.introspectionDown(true);
            assertThat(eu(token("introaberta").sign()).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("a troca de token também passa pela introspecção")
        void trocaTambem() throws Exception {
            String t = token("intro").sign();
            IDP.revoke(t);
            MockHttpServletResponse r = mockMvc.perform(post("/api/v1/auth/oidc/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("token", t)))).andReturn().getResponse();
            assertRecusa(r, 401, "TOKEN_REVOKED");
        }
    }

    // ═══════════════════════════ fase 2d: logout ═══════════════════════════

    @Nested
    @DisplayName("logout: POST /api/v1/auth/oidc/logout")
    class Logout {

        private MockHttpServletResponse sair(String bearer) throws Exception {
            var req = post("/api/v1/auth/oidc/logout");
            if (bearer != null) {
                req.header("Authorization", "Bearer " + bearer);
            }
            return mockMvc.perform(req).andReturn().getResponse();
        }

        @Test
        @DisplayName("revoga o token nesta aplicação e devolve o end_session_endpoint do IdP")
        void revogaEDevolveEndSession() throws Exception {
            String t = archguard("sub-ana").sign();
            assertThat(eu(t).getStatus()).isEqualTo(200);

            MockHttpServletResponse r = sair(t);

            assertThat(r.getStatus()).isEqualTo(200);
            JsonNode corpo = json.readTree(r.getContentAsString());
            assertThat(corpo.get("revoked").asBoolean()).isTrue();
            assertThat(corpo.get("end_session_endpoint").asString()).isEqualTo(IDP.issuer("archguard") + "/api/logout");

            assertRecusa(eu(t), 401, "TOKEN_REVOKED");
            assertThat(eu(archguard("sub-ana").sign()).getStatus()).as("token novo do IdP entra").isEqualTo(200);
        }

        @Test
        @DisplayName("token revogado aqui também não serve para a troca")
        void trocaRecusada() throws Exception {
            String t = archguard("sub-ana").sign();
            sair(t);
            MockHttpServletResponse r = mockMvc.perform(post("/api/v1/auth/oidc/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("token", t)))).andReturn().getResponse();
            assertRecusa(r, 401, "TOKEN_REVOKED");
        }

        @Test
        @DisplayName("provedor sem end-session-uri: só revoga")
        void semEndSession() throws Exception {
            String t = IDP.token(IDP.issuer("realms/empresa")).aud("archbase-api")
                    .claim("sub", "kc-ana").claim("email", EMAIL).claim("email_verified", true).sign();
            JsonNode corpo = json.readTree(sair(t).getContentAsString());
            assertThat(corpo.get("revoked").asBoolean()).isTrue();
            assertThat(corpo.has("end_session_endpoint")).isFalse();
        }

        @Test
        @DisplayName("sessão local ou anônima: 400 NOT_AN_OIDC_SESSION (o logout local é o do core)")
        void naoOidc() throws Exception {
            String local = json.readTree(loginLocal("bruno@empresa.com.br").getContentAsString())
                    .get("access_token").asString();
            assertRecusa(sair(local), 400, "NOT_AN_OIDC_SESSION");
            assertRecusa(sair(null), 400, "NOT_AN_OIDC_SESSION");
        }
    }

    @Nested
    @DisplayName("logout avisado pelo IdP: POST /api/v1/auth/oidc/backchannel-logout")
    class BackchannelLogout {

        private static final String EVENTO = "http://schemas.openid.net/event/backchannel-logout";

        private FakeOidcProvider.Token aviso(String issuer, String aud, String sub, String sid) {
            return IDP.token(issuer).aud(aud).withoutExp()
                    .claim("events", Map.of(EVENTO, Map.of()))
                    .claim("sub", sub).claim("sid", sid);
        }

        private FakeOidcProvider.Token avisoArchguard(String sub, String sid) {
            return aviso(IDP.issuer("archguard"), ARCHGUARD_AUD, sub, sid);
        }

        private MockHttpServletResponse avisar(String logoutToken) throws Exception {
            var req = post("/api/v1/auth/oidc/backchannel-logout")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED);
            if (logoutToken != null) {
                req.param("logout_token", logoutToken);
            }
            return mockMvc.perform(req).andReturn().getResponse();
        }

        private void assertAvisoRecusado(MockHttpServletResponse r) throws Exception {
            assertThat(r.getStatus()).isEqualTo(400);
            assertThat(r.getHeader("Cache-Control")).contains("no-store");
            assertThat(json.readTree(r.getContentAsString()).get("error").asString()).isEqualTo("invalid_request");
        }

        private FakeOidcProvider.Token emitidoHa(FakeOidcProvider.Token t, long segundos) {
            return t.claim("iat", java.util.Date.from(Instant.now().minusSeconds(segundos)));
        }

        @Test
        @DisplayName("só sub: tokens emitidos até o aviso caem; login depois do aviso entra")
        void porSub() throws Exception {
            String antes = emitidoHa(archguard("sub-ana"), 10).sign();
            assertThat(eu(antes).getStatus()).isEqualTo(200);

            MockHttpServletResponse r = avisar(avisoArchguard("sub-ana", null).sign());

            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.getHeader("Cache-Control")).contains("no-store");
            assertRecusa(eu(antes), 401, "TOKEN_REVOKED");
            String depois = archguard("sub-ana").claim("iat", java.util.Date.from(Instant.now().plusSeconds(2)))
                    .sign();
            assertThat(eu(depois).getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("com sid: cai a sessão do sid e o token sem sid; outra sessão continua")
        void porSid() throws Exception {
            String s1 = emitidoHa(archguard("sub-ana"), 10).claim("sid", "s1").sign();
            String s2 = emitidoHa(archguard("sub-ana"), 10).claim("sid", "s2").sign();
            String semSid = emitidoHa(archguard("sub-ana"), 10).sign();
            assertThat(eu(s1).getStatus()).isEqualTo(200);

            assertThat(avisar(avisoArchguard("sub-ana", "s1").sign()).getStatus()).isEqualTo(200);

            assertRecusa(eu(s1), 401, "TOKEN_REVOKED");
            assertThat(eu(s2).getStatus()).isEqualTo(200);
            assertRecusa(eu(semSid), 401, "TOKEN_REVOKED");
        }

        @Test
        @DisplayName("revoga também a sessão local da troca de token e impede a troca com o token antigo")
        void sessaoLocal() throws Exception {
            String doIdp = emitidoHa(archguard("sub-ana"), 10).sign();
            MockHttpServletResponse troca = mockMvc.perform(post("/api/v1/auth/oidc/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("token", doIdp)))).andReturn().getResponse();
            String local = json.readTree(troca.getContentAsString()).get("access_token").asString();
            assertThat(eu(local).getStatus()).isEqualTo(200);

            assertThat(avisar(avisoArchguard("sub-ana", null).sign()).getStatus()).isEqualTo(200);

            assertThat(eu(local).getStatus()).isEqualTo(401);
            MockHttpServletResponse denovo = mockMvc.perform(post("/api/v1/auth/oidc/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("token", doIdp)))).andReturn().getResponse();
            assertRecusa(denovo, 401, "TOKEN_REVOKED");
        }

        @Test
        @DisplayName("aud do aviso é o client_id (logout.backchannel.audiences), não o aud do access token")
        void audienciaPropria() throws Exception {
            String kc = "realms/empresa";
            assertAvisoRecusado(avisar(aviso(IDP.issuer(kc), "archbase-api", "kc-ana", null).sign()));
            assertThat(avisar(aviso(IDP.issuer(kc), "archbase-web", "kc-ana", null).sign()).getStatus())
                    .isEqualTo(200);
        }

        @Test
        @DisplayName("aviso inválido: 400 invalid_request, nada muda")
        void recusas() throws Exception {
            String antes = emitidoHa(archguard("sub-ana"), 10).sign();

            assertAvisoRecusado(avisar(null));
            assertAvisoRecusado(avisar("nao-e-jwt"));
            assertAvisoRecusado(avisar(archguard("sub-ana").sign()));                       // access token: sem events
            assertAvisoRecusado(avisar(avisoArchguard("sub-ana", null).claim("nonce", "n").sign())); // ID token
            assertAvisoRecusado(avisar(avisoArchguard("sub-ana", null).claim("events", Map.of("x", Map.of())).sign()));
            assertAvisoRecusado(avisar(avisoArchguard(null, null).sign()));                 // sem sub nem sid
            assertAvisoRecusado(avisar(emitidoHa(avisoArchguard("sub-ana", null), 600).sign())); // velho
            assertAvisoRecusado(avisar(avisoArchguard("sub-ana", null).claim("iat",
                    java.util.Date.from(Instant.now().plusSeconds(300))).sign()));          // futuro
            assertAvisoRecusado(avisar(avisoArchguard("sub-ana", null)
                    .claim("exp", java.util.Date.from(Instant.now().minusSeconds(120))).sign())); // expirado
            assertAvisoRecusado(avisar(avisoArchguard("sub-ana", null).kid("rsa-fora").sign())); // chave não publicada
            assertAvisoRecusado(avisar(aviso(IDP.issuer("jit"), "app-jit", "sub-ana", null).sign())); // desligado
            assertAvisoRecusado(avisar(aviso(IDP.issuer("desconhecido"), "x", "sub-ana", null).sign()));

            assertThat(eu(antes).getStatus()).as("nenhum aviso recusado derrubou a sessão").isEqualTo(200);
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
