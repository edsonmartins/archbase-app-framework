package br.com.archbase.controlplane.it;

import br.com.archbase.controlplane.user.UserLinkStore;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critérios de aceite da Fase A com {@code legacy-jwt=accept}, contra o {@link FakeControlPlane}.
 */
@SpringBootTest(classes = ControlPlaneTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cp-accept;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.secret-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
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
        "archbase.controlplane.legacy-jwt=accept",
        "archbase.controlplane.jwks.refresh-min-interval=0s"
})
@DisplayName("Control Plane: token de sessão (legacy-jwt=accept)")
class ControlPlaneTokenIntegrationTest {

    static final String TENANT_A = "tenant-a";
    static final String TENANT_B = "tenant-b";
    static final String KID = "cp-2026-11";
    static final String EMAIL = "ana@cliente.com.br";
    static final String SENHA = "senha-de-teste";

    static final FakeControlPlane CP = new FakeControlPlane().addKey(KID, true);

    @DynamicPropertySource
    static void jwks(DynamicPropertyRegistry registry) {
        registry.add("archbase.controlplane.jwks.url", CP::jwksUrl);
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
    JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private UserEntity ana;

    @BeforeEach
    void setUp() {
        for (String tenant : new String[]{TENANT_A, TENANT_B}) {
            ArchbaseTenantContext.setTenantId(tenant);
            tokens.deleteAll();
            users.deleteAll();
        }
        ArchbaseTenantContext.clear();
        jdbc.update("DELETE FROM " + UserLinkStore.TABLE);
        ana = TestUsers.create(users, encoder, TENANT_A, EMAIL, SENHA, true);
    }

    @AfterEach
    void clear() {
        ArchbaseTenantContext.clear();
    }

    private String sessionA(String adm) {
        return CP.session(KID, "cp-user-ana", EMAIL, TENANT_A, adm).bearer();
    }

    @Nested
    @DisplayName("tenant")
    class Tenant {

        @Test
        @DisplayName("token do tenant A entra e o contexto fica no tenant A")
        void tokenDoTenantAEntra() throws Exception {
            mvc.perform(get("/api/teste/quem").header("Authorization", sessionA("member")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").value(TENANT_A))
                    .andExpect(jsonPath("$.sub").value("cp-user-ana"))
                    .andExpect(jsonPath("$.localUser").value(ana.getId()))
                    .andExpect(jsonPath("$.name").value(EMAIL));
        }

        @Test
        @DisplayName("o mesmo token com X-TENANT-ID do tenant B é recusado")
        void outroTenantNoCabecalhoERecusado() throws Exception {
            mvc.perform(get("/api/teste/quem").header("Authorization", sessionA("member"))
                            .header("X-TENANT-ID", TENANT_B))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("TENANT_MISMATCH"));
        }

        @Test
        @DisplayName("X-TENANT-ID igual ao do token é aceito")
        void mesmoTenantNoCabecalho() throws Exception {
            mvc.perform(get("/api/teste/quem").header("Authorization", sessionA("member"))
                            .header("X-TENANT-ID", TENANT_A))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("token de sessão sem tnt é recusado, mesmo com X-TENANT-ID")
        void sessaoSemTenantERecusada() throws Exception {
            String token = CP.session(KID, "cp-user-ana", EMAIL, TENANT_A, "member").without("tnt").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token).header("X-TENANT-ID", TENANT_A))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"));
        }

        @Test
        @DisplayName("sem token, o tenant padrão configurado nunca entra no contexto")
        void semTokenNaoCaiNoTenantPadrao() throws Exception {
            mvc.perform(get("/api/publico/tenant"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").doesNotExist());
        }
    }

    @Nested
    @DisplayName("token")
    class Token {

        @Test
        @DisplayName("aud de outra instância é recusado")
        void outraInstancia() throws Exception {
            String token = CP.session(KID, "cp-user-ana", EMAIL, TENANT_A, "member")
                    .audience("https://outra.instancia.teste").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("WRONG_AUDIENCE"));
        }

        @Test
        @DisplayName("token vencido é recusado")
        void vencido() throws Exception {
            String token = CP.session(KID, "cp-user-ana", EMAIL, TENANT_A, "member")
                    .expiresAt(Instant.now().minusSeconds(120)).bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        }

        @Test
        @DisplayName("assinatura de outra chave com o mesmo kid é recusada")
        void assinaturaErrada() throws Exception {
            String token = CP.session(KID, "cp-user-ana", EMAIL, TENANT_A, "member").signedByStranger().bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_SIGNATURE"));
        }

        @Test
        @DisplayName("emissor diferente é recusado")
        void outroEmissor() throws Exception {
            String token = CP.session(KID, "cp-user-ana", EMAIL, TENANT_A, "member")
                    .issuer("https://falso.teste").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("WRONG_ISSUER"));
        }

        @Test
        @DisplayName("kid desconhecido e ausente do JWKS é recusado")
        void kidDesconhecido() throws Exception {
            String kid = "nao-publicada-" + UUID.randomUUID();
            CP.addKey(kid, false);
            mvc.perform(get("/api/teste/quem")
                            .header("Authorization", CP.session(kid, "cp-user-ana", EMAIL, TENANT_A, "member").bearer()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNKNOWN_KEY"));
        }

        @Test
        @DisplayName("chave nova é aceita depois que o JWKS a publica (rotação)")
        void rotacao() throws Exception {
            String kid = "rotacionada-" + UUID.randomUUID();
            CP.addKey(kid, false);
            String token = CP.session(kid, "cp-user-ana", EMAIL, TENANT_A, "member").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isUnauthorized());

            CP.publish(kid);
            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("token de instância usa o X-TENANT-ID e exige que ele venha")
        void tokenDeInstancia() throws Exception {
            String token = CP.instance(KID, "inst-powerview-01").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token).header("X-TENANT-ID", TENANT_B))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").value(TENANT_B))
                    .andExpect(jsonPath("$.instance").value(true));

            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("TENANT_REQUIRED"));
        }
    }

    @Nested
    @DisplayName("usuário local")
    class UsuarioLocal {

        @Test
        @DisplayName("primeiro acesso vincula pelo e-mail; depois o vínculo vale mesmo com e-mail novo")
        void vinculoPeloEmail() throws Exception {
            mvc.perform(get("/api/teste/quem").header("Authorization", sessionA("member")))
                    .andExpect(jsonPath("$.localUser").value(ana.getId()));

            String comEmailNovo = CP.session(KID, "cp-user-ana", "ana.nova@cliente.com.br", TENANT_A, "member").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", comEmailNovo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.localUser").value(ana.getId()));
        }

        @Test
        @DisplayName("o vínculo é por tenant: o mesmo e-mail em outro tenant não é encontrado")
        void vinculoPorTenant() throws Exception {
            String noTenantB = CP.session(KID, "cp-user-ana", EMAIL, TENANT_B, "member").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", noTenantB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").value(TENANT_B))
                    .andExpect(jsonPath("$.localUser").doesNotExist());
        }

        @Test
        @DisplayName("usuário sem correspondente local entra só com a sessão")
        void semUsuarioLocal() throws Exception {
            String token = CP.session(KID, "cp-user-novo", "novo@cliente.com.br", TENANT_A, "member").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.localUser").doesNotExist())
                    .andExpect(jsonPath("$.name").value("novo@cliente.com.br"));
        }

        @Test
        @DisplayName("usuário local desativado é recusado com USER_DISABLED")
        void usuarioDesativado() throws Exception {
            TestUsers.create(users, encoder, TENANT_A, "inativo@cliente.com.br", SENHA, false);
            String token = CP.session(KID, "cp-user-inativo", "inativo@cliente.com.br", TENANT_A, "member").bearer();
            mvc.perform(get("/api/teste/quem").header("Authorization", token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USER_DISABLED"));
        }
    }

    @Nested
    @DisplayName("@RequiresAdmin")
    class Admin {

        @Test
        @DisplayName("member é recusado com NOT_ADMIN")
        void member() throws Exception {
            mvc.perform(get("/api/teste/admin-tenant").header("Authorization", sessionA("member")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_ADMIN"));
        }

        @Test
        @DisplayName("tenant_admin passa no escopo de tenant e não no de cliente")
        void tenantAdmin() throws Exception {
            mvc.perform(get("/api/teste/admin-tenant").header("Authorization", sessionA("tenant_admin")))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/teste/admin-cliente").header("Authorization", sessionA("tenant_admin")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_ADMIN"));
        }

        @Test
        @DisplayName("client_admin passa nos dois escopos")
        void clientAdmin() throws Exception {
            mvc.perform(get("/api/teste/admin-cliente").header("Authorization", sessionA("client_admin")))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/teste/admin-tenant").header("Authorization", sessionA("client_admin")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("na classe vale para todos os métodos; no método, prevalece sobre a da classe")
        void anotacaoNaClasse() throws Exception {
            mvc.perform(get("/api/teste/classe-cliente/padrao").header("Authorization", sessionA("tenant_admin")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_ADMIN"));
            mvc.perform(get("/api/teste/classe-cliente/padrao").header("Authorization", sessionA("client_admin")))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/teste/classe-cliente/de-tenant").header("Authorization", sessionA("tenant_admin")))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/teste/classe-cliente/de-tenant").header("Authorization", sessionA("member")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("token de instância nunca é admin")
        void instancia() throws Exception {
            mvc.perform(get("/api/teste/admin-tenant").header("Authorization", CP.instance(KID, "inst-1").bearer())
                            .header("X-TENANT-ID", TENANT_A))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_ADMIN"));
        }
    }

    @Nested
    @DisplayName("convivência com o JWT atual")
    class Convivencia {

        @Test
        @DisplayName("com accept, o JWT do login local continua funcionando no mesmo deploy, sem X-TENANT-ID")
        void jwtLegadoFunciona() throws Exception {
            var resposta = mvc.perform(post("/api/v1/auth/authenticate")
                            .header("X-TENANT-ID", TENANT_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(
                                    Map.of("email", EMAIL, "password", SENHA, "tenantId", TENANT_A))))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode login = json.readTree(resposta.getResponse().getContentAsString());
            String legado = login.get("access_token").asText();

            mvc.perform(get("/api/teste/quem").header("Authorization", "Bearer " + legado))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenant").value(TENANT_A))
                    .andExpect(jsonPath("$.name").value(EMAIL))
                    .andExpect(jsonPath("$.sub").doesNotExist());

            mvc.perform(get("/api/teste/quem").header("Authorization", sessionA("member")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sub").value("cp-user-ana"));
        }
    }
}
