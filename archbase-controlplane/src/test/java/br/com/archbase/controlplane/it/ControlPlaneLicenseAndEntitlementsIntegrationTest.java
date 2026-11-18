package br.com.archbase.controlplane.it;

import br.com.archbase.controlplane.fixtures.MutableClock;
import br.com.archbase.controlplane.fixtures.MutableClockConfig;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.state.ControlPlaneStateStore;
import br.com.archbase.controlplane.tenant.TenantSnapshot;
import br.com.archbase.controlplane.tenant.TenantState;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Critérios de aceite da Fase B: licença, carência, modo restrito, tenant suspenso, entitlements e assentos. */
@SpringBootTest(classes = ControlPlaneTestApplication.class)
@AutoConfigureMockMvc
@Import({MutableClockConfig.class, br.com.archbase.controlplane.fixtures.MeterRegistryConfig.class})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cp-faseb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "archbase.security.jwt.token-expiration=3600000",
        "archbase.security.jwt.refresh-expiration=86400000",
        "archbase.security.whitelist=/api/publico/**",
        "archbase.security.cors.allowed-origins=*",
        "archbase.security.cors.allowed-methods=*",
        "archbase.security.cors.allowed-headers=*",
        "archbase.security.cors.allow-credentials=false",
        "archbase.app.tenant.fail-on-missing=true",
        "archbase.controlplane.enabled=true",
        "archbase.controlplane.url=" + FakeControlPlane.ISSUER,
        "archbase.controlplane.public-url=" + FakeControlPlane.AUDIENCE,
        "archbase.controlplane.product=powerview",
        "archbase.controlplane.instance-id=inst-pv-01",
        "archbase.controlplane.grace-period=7d"
})
@DisplayName("Control Plane: licença, entitlements e assentos")
class ControlPlaneLicenseAndEntitlementsIntegrationTest {

    static final String TENANT = "tenant-a";
    static final String KID = "cp-b";

    static final FakeControlPlane CP = new FakeControlPlane().addKey(KID, true);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
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
    ControlPlaneState state;
    @Autowired
    ControlPlaneStateStore store;
    @Autowired
    MutableClock clock;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UserJpaRepository users;
    @Autowired
    AccessTokenJpaRepository tokens;
    @Autowired
    io.micrometer.core.instrument.MeterRegistry meters;

    private double gauge(String name) {
        return meters.get(name).gauge().value();
    }

    /**
     * Estático: o contexto (e o estado nele) é compartilhado entre os testes, e snapshot com versão menor que a já
     * aplicada é ignorado — de propósito.
     */
    private static final java.util.concurrent.atomic.AtomicLong VERSION = new java.util.concurrent.atomic.AtomicLong();

    @BeforeEach
    void setUp() {
        clock.set(Instant.now());
        ArchbaseTenantContext.setTenantId(TENANT);
        tokens.deleteAll();
        users.deleteAll();
        ArchbaseTenantContext.clear();
        jdbc.update("DELETE FROM " + ControlPlaneStateStore.USAGE_TABLE);
        licencaValidaPor(Duration.ofDays(30));
        tenant(TenantState.ACTIVE, null, null);
    }

    @AfterEach
    void clear() {
        ArchbaseTenantContext.clear();
    }

    /** Licença com validade a partir do relógio do teste; sempre mais nova que a anterior. */
    private void licencaValidaPor(Duration duration) {
        Instant exp = clock.instant().plus(duration).plusSeconds(VERSION.incrementAndGet());
        // Licença mais curta que a atual seria ignorada: limpa a guardada para o cenário começar do zero.
        jdbc.update("DELETE FROM " + ControlPlaneStateStore.LICENSE_TABLE);
        resetLicense();
        state.applyLicense(CP.license(KID, "inst-pv-01", "powerview", exp).compact());
    }

    /** Esquece a licença em memória, para o cenário aplicar uma do zero. */
    private void resetLicense() {
        try {
            var field = ControlPlaneState.class.getDeclaredField("license");
            field.setAccessible(true);
            field.set(state, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void tenant(TenantState tenantState, Instant suspendedAt, Map<String, String> seatHolders) {
        state.applyTenant(new TenantSnapshot(TENANT, VERSION.incrementAndGet(), tenantState, suspendedAt, "pro",
                Map.of("powerview.export_excel", 1L, "powerview.assentos", 25L, "copilot.ia_creditos", 1000L),
                Map.of("copilot.ia_creditos", 900L), seatHolders, Instant.now()));
    }

    private String sessao(String seat) {
        return CP.session(KID, "cp-ana", "ana@x.com", TENANT, "member").claim("seat", seat).bearer();
    }

    @Nested
    @DisplayName("entitlements")
    class EntitlementsTests {

        @Test
        @DisplayName("@RequiresEntitlement passa com o entitlement e recusa sem ele com NOT_ENTITLED")
        void requiresEntitlement() throws Exception {
            mvc.perform(get("/api/teste/b/excel").header("Authorization", sessao(null)))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/teste/b/bi").header("Authorization", sessao(null)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NOT_ENTITLED"));
        }

        @Test
        @DisplayName("a API do produto lê estado, licença, entitlements e assentos do cache")
        void apiDoProduto() throws Exception {
            mvc.perform(get("/api/teste/b/estado").header("Authorization", sessao(null)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenantState").value("ACTIVE"))
                    .andExpect(jsonPath("$.license").value("VALID"))
                    .andExpect(jsonPath("$.excel").value(true))
                    .andExpect(jsonPath("$.assentos").value(25))
                    .andExpect(jsonPath("$.seat").value(false));
        }

        @Test
        @DisplayName("consumo é registrado localmente; estouro é tolerado e informado")
        void consumo() throws Exception {
            mvc.perform(post("/api/teste/b/consumir").param("qtd", "50").header("Authorization", sessao(null)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.used").value(950))
                    .andExpect(jsonPath("$.remaining").value(50))
                    .andExpect(jsonPath("$.exceeded").value(false));
            mvc.perform(post("/api/teste/b/consumir").param("qtd", "120").header("Authorization", sessao(null)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.used").value(1070))
                    .andExpect(jsonPath("$.remaining").value(0))
                    .andExpect(jsonPath("$.exceeded").value(true));
            assertThat(store.unreportedUsage(TENANT, "copilot.ia_creditos")).isEqualTo(170);
            assertThat(meters.get("controlplane.usage.exceeded").tag("key", "copilot.ia_creditos").counter().count())
                    .isGreaterThanOrEqualTo(1);
            assertThat(gauge("controlplane.usage.unreported")).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("snapshot com versão antiga é ignorado (comando repetido ou fora de ordem)")
        void versaoAntigaIgnorada() {
            long atual = state.tenant(TENANT).orElseThrow().version();
            boolean aplicado = state.applyTenant(new TenantSnapshot(TENANT, atual - 1, TenantState.SUSPENDED,
                    Instant.now(), "pro", Map.of(), Map.of(), null, Instant.now()));
            assertThat(aplicado).isFalse();
            assertThat(state.tenant(TENANT).orElseThrow().state()).isEqualTo(TenantState.ACTIVE);
        }

        @Test
        @DisplayName("o snapshot sobrevive à reinicialização (lido do banco)")
        void persistencia() {
            TenantSnapshot doBanco = store.loadTenants().get(TENANT);
            assertThat(doBanco).isEqualTo(state.tenant(TENANT).orElseThrow());
        }
    }

    @Nested
    @DisplayName("assentos")
    class Assentos {

        @Test
        @DisplayName("@RequiresSeat recusa token sem assento com NO_SEAT")
        void semAssento() throws Exception {
            mvc.perform(get("/api/teste/b/com-assento").header("Authorization", sessao(null)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NO_SEAT"));
            mvc.perform(get("/api/teste/b/com-assento").header("Authorization", sessao("seat-1")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("assento retirado pelo Control Plane vale antes do token vencer")
        void assentoRetirado() throws Exception {
            tenant(TenantState.ACTIVE, null, Map.of("cp-outro", "seat-9"));
            mvc.perform(get("/api/teste/b/com-assento").header("Authorization", sessao("seat-1")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NO_SEAT"));

            tenant(TenantState.ACTIVE, null, Map.of("cp-ana", "seat-1"));
            mvc.perform(get("/api/teste/b/com-assento").header("Authorization", sessao("seat-1")))
                    .andExpect(status().isOk());
            mvc.perform(get("/api/teste/b/estado").header("Authorization", sessao("seat-1")))
                    .andExpect(jsonPath("$.seat").value(true));
        }

        @Test
        @DisplayName("token de instância não tem assento")
        void instancia() throws Exception {
            mvc.perform(get("/api/teste/b/com-assento").header("Authorization", CP.instance(KID, "inst-x").bearer())
                            .header("X-TENANT-ID", TENANT))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("NO_SEAT"));
        }
    }

    @Nested
    @DisplayName("licença e carência")
    class Licenca {

        @Test
        @DisplayName("Control Plane fora: a sessão segue até o fim da carência e depois entra em modo restrito")
        void carenciaEModoRestrito() throws Exception {
            CP.setUp(false);
            try {
                licencaValidaPor(Duration.ofDays(1));

                mvc.perform(post("/api/teste/b/escrita").header("Authorization", sessao(null)))
                        .andExpect(status().isOk());
                assertThat(gauge("controlplane.license.status")).isZero();

                clock.advance(Duration.ofDays(3));   // venceu, dentro da carência de 7 dias
                assertThat(gauge("controlplane.license.status")).isEqualTo(1);
                assertThat(gauge("controlplane.license.expiry.seconds")).isNegative();
                assertThat(gauge("controlplane.license.grace.end.seconds")).isPositive();
                mvc.perform(get("/api/teste/b/estado").header("Authorization", sessao(null)))
                        .andExpect(jsonPath("$.license").value("GRACE"));
                mvc.perform(post("/api/teste/b/escrita").header("Authorization", sessao(null)))
                        .andExpect(status().isOk());

                clock.advance(Duration.ofDays(6));   // além da carência
                mvc.perform(get("/api/teste/b/estado").header("Authorization", sessao(null)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.license").value("EXPIRED"));
                var negadas = meters.find("controlplane.restricted.denied").tag("code", "LICENSE_EXPIRED").counter();
                double antes = negadas == null ? 0 : negadas.count();
                mvc.perform(post("/api/teste/b/escrita").header("Authorization", sessao(null)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("LICENSE_EXPIRED"));
                assertThat(gauge("controlplane.license.status")).isEqualTo(2);
                assertThat(gauge("controlplane.license.grace.end.seconds")).isNegative();
                assertThat(meters.get("controlplane.restricted.denied").tags("code", "LICENSE_EXPIRED",
                        "enforcement", "enforce").counter().count()).isEqualTo(antes + 1);
                mvc.perform(post("/api/teste/b/exportar").header("Authorization", sessao(null)))
                        .andExpect(status().isOk());
            } finally {
                CP.setUp(true);
            }
        }

        @Test
        @DisplayName("modo restrito não bloqueia o login legado")
        void loginLegadoLiberado() throws Exception {
            clock.advance(Duration.ofDays(60));
            mvc.perform(post("/api/v1/auth/authenticate").header("X-TENANT-ID", TENANT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ninguem@x.com\",\"password\":\"x\",\"tenantId\":\"" + TENANT + "\"}"))
                    .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                            .doesNotContain("LICENSE_EXPIRED"));
        }

        @Test
        @DisplayName("licença nova reaplicada não encurta a validade")
        void licencaMaisAntigaIgnorada() {
            Instant atual = state.license().orElseThrow().expiresAt();
            boolean aplicada = state.applyLicense(
                    CP.license(KID, "inst-pv-01", "powerview", atual.minus(Duration.ofDays(1))).compact());
            assertThat(aplicada).isFalse();
            assertThat(state.license().orElseThrow().expiresAt()).isEqualTo(atual);
        }
    }

    @Nested
    @DisplayName("tenant suspenso")
    class Suspenso {

        @Test
        @DisplayName("sessão anterior à suspensão lê e exporta, mas não escreve")
        void sessaoExistente() throws Exception {
            tenant(TenantState.SUSPENDED, Instant.now().plus(Duration.ofMinutes(1)), null);
            assertThat(meters.get("controlplane.tenants").tag("state", "SUSPENDED").gauge().value()).isEqualTo(1);
            mvc.perform(get("/api/teste/b/estado").header("Authorization", sessao(null)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tenantState").value("SUSPENDED"));
            mvc.perform(post("/api/teste/b/escrita").header("Authorization", sessao(null)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("TENANT_SUSPENDED"));
            mvc.perform(post("/api/teste/b/exportar").header("Authorization", sessao(null)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("login novo (token emitido depois da suspensão) é recusado")
        void loginNovo() throws Exception {
            tenant(TenantState.SUSPENDED, Instant.now().minus(Duration.ofHours(1)), null);
            mvc.perform(get("/api/teste/b/estado").header("Authorization", sessao(null)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("TENANT_SUSPENDED"));
        }
    }
}
