package br.com.archbase.controlplane.it;

import br.com.archbase.controlplane.command.CommandProcessor;
import br.com.archbase.controlplane.command.CommandVerifier;
import br.com.archbase.controlplane.fixtures.RecordingProvisioner;
import br.com.archbase.controlplane.fixtures.RecordingProvisionerConfig;
import br.com.archbase.controlplane.heartbeat.HeartbeatService;
import br.com.archbase.controlplane.heartbeat.ManifestPublisher;
import br.com.archbase.controlplane.license.LicenseStatus;
import br.com.archbase.controlplane.mqtt.ControlPlaneMqttClient;
import br.com.archbase.controlplane.provisioning.TenantProvisioner;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.state.ControlPlaneStateStore;
import br.com.archbase.controlplane.tenant.TenantState;
import br.com.archbase.controlplane.token.JwksKeyStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Critérios de aceite da Fase C contra um Mosquitto em container e o {@link FakeControlPlane}.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ControlPlaneTestApplication.class)
@Import(RecordingProvisionerConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cp-mqtt;DB_CLOSE_DELAY=-1",
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
        "archbase.controlplane.product-version=2.4.0",
        "archbase.controlplane.instance-id=" + ControlPlaneMqttIntegrationTest.INSTANCE,
        "archbase.controlplane.mqtt.keep-alive=2s",
        "archbase.controlplane.mqtt.reconnect-min-delay=1s",
        "archbase.controlplane.mqtt.reconnect-max-delay=2s",
        "archbase.controlplane.heartbeat.interval=1h"
})
@DisplayName("Control Plane: comandos, status e heartbeat por MQTT")
class ControlPlaneMqttIntegrationTest {

    static final String INSTANCE = "inst-mqtt-01";
    static final String KID = "cp-c";
    static final String CREDENTIAL = UUID.randomUUID().toString();

    static final FakeControlPlane CP = new FakeControlPlane().addKey(KID, true);

    @SuppressWarnings("resource")
    static final GenericContainer<?> MOSQUITTO = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2"))
            .withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf")
            .withExposedPorts(1883);

    static BrokerObserver observer;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        if (!MOSQUITTO.isRunning()) {
            MOSQUITTO.start();
        }
        registry.add("archbase.controlplane.jwks.url", CP::jwksUrl);
        registry.add("archbase.controlplane.mqtt.uri", ControlPlaneMqttIntegrationTest::brokerUri);
        registry.add("archbase.controlplane.instance-credential", () -> CREDENTIAL);
        registry.add("archbase.controlplane.heartbeat.url",
                () -> CP.baseUrl() + "/api/v1/instances/" + INSTANCE + "/heartbeat");
        registry.add("archbase.security.jwt.secret-key", TestSecrets::legacyJwtKey);
    }

    static String brokerUri() {
        return "tcp://" + MOSQUITTO.getHost() + ":" + MOSQUITTO.getMappedPort(1883);
    }

    @AfterAll
    static void stop() {
        if (observer != null) {
            observer.close();
        }
        CP.close();
        MOSQUITTO.stop();
    }

    @Autowired
    RecordingProvisioner provisioner;
    @Autowired
    ControlPlaneMqttClient mqtt;
    @Autowired
    ControlPlaneState state;
    @Autowired
    ControlPlaneStateStore store;
    @Autowired
    HeartbeatService heartbeat;
    @Autowired
    ManifestPublisher manifest;
    @Autowired
    JwksKeyStore keyStore;
    @Autowired
    ObjectProvider<TenantProvisioner> provisionerProvider;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() throws Exception {
        provisioner.reset();
        if (observer == null) {
            observer = new BrokerObserver(brokerUri());
        }
        await().atMost(Duration.ofSeconds(20)).until(mqtt::isConnected);
    }

    private String provisionCommand(String commandId, String tenant) {
        return CP.command(KID, INSTANCE, commandId, "tenant.provision", Map.of(
                "tenantId", tenant,
                "clientId", "cli-1",
                "edition", "pro",
                "entitlements", Map.of("powerview.export_excel", true, "powerview.assentos", 25),
                "preset", "varejo",
                "presetParameters", Map.of("filiais", 3),
                "admin", Map.of("id", "cp-admin", "email", "admin@cliente.com.br"))).compact();
    }

    @Test
    @DisplayName("tenant.provision assinado chama o TenantProvisioner uma vez, mesmo entregue duas vezes")
    void provisionIdempotente() throws Exception {
        String id = "cmd-" + UUID.randomUUID();
        String tenant = "t-" + UUID.randomUUID();
        String command = provisionCommand(id, tenant);

        observer.publishCommand(INSTANCE, command);
        observer.publishCommand(INSTANCE, command);

        await().atMost(Duration.ofSeconds(15)).until(() -> observer.matching(BrokerObserver.result(id)).size() == 2);
        assertThat(provisioner.count("provision", tenant)).isEqualTo(1);
        var results = observer.matching(BrokerObserver.result(id));
        assertThat(results).allSatisfy(r -> assertThat(r.body().path("status").asString()).isEqualTo("applied"));
        assertThat(results).anySatisfy(r -> assertThat(r.body().path("duplicate").asBoolean()).isTrue());

        RecordingProvisioner.Call call = provisioner.calls().getFirst();
        assertThat(call.contextTenant()).as("o produto roda no tenant do comando").isEqualTo(tenant);
        assertThat(call.spec().edition()).isEqualTo("pro");
        assertThat(call.spec().entitlements()).containsEntry("powerview.export_excel", 1L)
                .containsEntry("powerview.assentos", 25L);
        assertThat(call.spec().presetParameters()).containsEntry("filiais", 3L);
        assertThat(call.spec().adminEmail()).isEqualTo("admin@cliente.com.br");

        assertThat(state.tenant(tenant)).hasValueSatisfying(s -> {
            assertThat(s.state()).isEqualTo(TenantState.ACTIVE);
            assertThat(s.entitlements()).containsEntry("powerview.assentos", 25L);
        });
    }

    @Test
    @DisplayName("comando sem assinatura válida não chama nada e é recusado")
    void assinaturaInvalida() throws Exception {
        String id = "cmd-" + UUID.randomUUID();
        String tenant = "t-" + UUID.randomUUID();
        String forjado = CP.command(KID, INSTANCE, id, "tenant.provision", Map.of("tenantId", tenant))
                .signedByStranger().compact();

        observer.publishCommand(INSTANCE, forjado);

        BrokerObserver.Received result = observer.await(BrokerObserver.result(id), Duration.ofSeconds(15));
        assertThat(result.body().path("status").asString()).isEqualTo("rejected");
        assertThat(provisioner.calls()).isEmpty();
        assertThat(store.commandApplied(id)).isFalse();
    }

    @Test
    @DisplayName("comando para outra instância é recusado")
    void outraInstancia() throws Exception {
        String id = "cmd-" + UUID.randomUUID();
        String paraOutra = CP.command(KID, "inst-outra", id, "tenant.provision", Map.of("tenantId", "t-x")).compact();

        observer.publishCommand(INSTANCE, paraOutra);

        BrokerObserver.Received result = observer.await(BrokerObserver.result(id), Duration.ofSeconds(15));
        assertThat(result.body().path("status").asString()).isEqualTo("rejected");
        assertThat(provisioner.calls()).isEmpty();
    }

    @Test
    @DisplayName("falha no produto publica failed, não grava nada, e o reenvio é aplicado")
    void falhaEReenvio() throws Exception {
        String id = "cmd-" + UUID.randomUUID();
        String tenant = "t-" + UUID.randomUUID();
        String command = provisionCommand(id, tenant);

        provisioner.failWith("preset varejo indisponível");
        observer.publishCommand(INSTANCE, command);
        BrokerObserver.Received failed = observer.await(BrokerObserver.result(id), Duration.ofSeconds(15));
        assertThat(failed.body().path("status").asString()).isEqualTo("failed");
        assertThat(failed.body().path("reason").asString()).isEqualTo("preset varejo indisponível");
        assertThat(store.commandApplied(id)).as("a transação desfaz o registro do comando").isFalse();
        assertThat(state.tenant(tenant)).as("o cache não anuncia o que o produto não aplicou").isEmpty();

        provisioner.failWith(null);
        observer.publishCommand(INSTANCE, command);
        await().atMost(Duration.ofSeconds(15)).until(() -> observer.matching(BrokerObserver.result(id)).stream()
                .anyMatch(r -> "applied".equals(r.body().path("status").asString())));
        assertThat(provisioner.count("provision", tenant)).isEqualTo(2);
        assertThat(store.commandApplied(id)).isTrue();
    }

    @Test
    @DisplayName("comandos do módulo atualizam os caches sem chamar o produto")
    void comandosDoModulo() throws Exception {
        String tenant = "t-" + UUID.randomUUID();
        observer.publishCommand(INSTANCE, CP.command(KID, INSTANCE, "cmd-" + UUID.randomUUID(),
                "entitlements.changed", Map.of("tenantId", tenant, "version", 10,
                        "entitlements", Map.of("powerview.bi", true), "usageBaseline", Map.of("ia", 40))).compact());
        await().atMost(Duration.ofSeconds(15)).until(() -> state.tenant(tenant).isPresent());
        assertThat(state.tenant(tenant).orElseThrow().entitlements()).containsEntry("powerview.bi", 1L);

        observer.publishCommand(INSTANCE, CP.command(KID, INSTANCE, "cmd-" + UUID.randomUUID(),
                "membership.changed", Map.of("tenantId", tenant, "version", 11,
                        "seatHolders", Map.of("cp-ana", "seat-1"))).compact());
        await().atMost(Duration.ofSeconds(15)).until(() -> state.tenant(tenant).orElseThrow().seatsKnown());
        assertThat(state.tenant(tenant).orElseThrow().seatHolders()).containsEntry("cp-ana", "seat-1");
        assertThat(state.tenant(tenant).orElseThrow().entitlements()).as("membership não apaga entitlements")
                .containsEntry("powerview.bi", 1L);

        String license = CP.license(KID, INSTANCE, "powerview", Instant.now().plus(Duration.ofDays(400))).compact();
        observer.publishCommand(INSTANCE, CP.command(KID, INSTANCE, "cmd-" + UUID.randomUUID(),
                "license.renewed", Map.of("license", license)).compact());
        await().atMost(Duration.ofSeconds(15)).until(() -> state.licenseStatus() == LicenseStatus.VALID);

        assertThat(provisioner.calls()).isEmpty();
    }

    @Test
    @DisplayName("com data.version explícita, comando atrasado com versão menor é ignorado")
    void versaoExplicitaAntigaIgnorada() throws Exception {
        String tenant = "t-" + UUID.randomUUID();
        String novo = "cmd-" + UUID.randomUUID();
        observer.publishCommand(INSTANCE, CP.command(KID, INSTANCE, novo, "entitlements.changed",
                Map.of("tenantId", tenant, "version", 20, "entitlements", Map.of("powerview.assentos", 50))).compact());
        observer.await(BrokerObserver.result(novo), Duration.ofSeconds(15));

        String atrasado = "cmd-" + UUID.randomUUID();
        observer.publishCommand(INSTANCE, CP.command(KID, INSTANCE, atrasado, "entitlements.changed",
                Map.of("tenantId", tenant, "version", 15, "entitlements", Map.of("powerview.assentos", 10))).compact());
        observer.await(BrokerObserver.result(atrasado), Duration.ofSeconds(15));

        assertThat(state.tenant(tenant).orElseThrow().version()).isEqualTo(20);
        assertThat(state.tenant(tenant).orElseThrow().entitlements()).containsEntry("powerview.assentos", 50L);
    }

    @Test
    @DisplayName("suspend e resume mudam o estado do tenant depois que o produto aplica")
    void suspendResume() throws Exception {
        String tenant = "t-" + UUID.randomUUID();
        String provision = "cmd-" + UUID.randomUUID();
        observer.publishCommand(INSTANCE, provisionCommand(provision, tenant));
        observer.await(BrokerObserver.result(provision), Duration.ofSeconds(15));

        String suspend = "cmd-" + UUID.randomUUID();
        observer.publishCommand(INSTANCE, CP.command(KID, INSTANCE, suspend, "tenant.suspend",
                Map.of("tenantId", tenant)).compact());
        observer.await(BrokerObserver.result(suspend), Duration.ofSeconds(15));
        assertThat(state.tenant(tenant).orElseThrow().state()).isEqualTo(TenantState.SUSPENDED);
        assertThat(state.tenant(tenant).orElseThrow().suspendedAt()).isNotNull();

        String resume = "cmd-" + UUID.randomUUID();
        observer.publishCommand(INSTANCE, CP.command(KID, INSTANCE, resume, "tenant.resume",
                Map.of("tenantId", tenant)).compact());
        observer.await(BrokerObserver.result(resume), Duration.ofSeconds(15));
        assertThat(state.tenant(tenant).orElseThrow().state()).isEqualTo(TenantState.ACTIVE);
        assertThat(provisioner.count("suspend", tenant)).isEqualTo(1);
        assertThat(provisioner.count("resume", tenant)).isEqualTo(1);
    }

    @Test
    @DisplayName("a instância se anuncia online e, se cair sem se despedir, o Last Will publica offline")
    void lastWill() throws Exception {
        String instance = "inst-lwt-" + UUID.randomUUID();
        ControlPlaneMqttClient client = otherInstanceClient(instance);
        client.start();
        observer.await(BrokerObserver.status(instance, "online"), Duration.ofSeconds(15));

        client.abortWithoutDisconnect();

        BrokerObserver.Received offline = observer.await(BrokerObserver.status(instance, "offline"),
                Duration.ofSeconds(30));
        assertThat(offline.body().path("instanceId").asString()).isEqualTo(instance);
    }

    @Test
    @DisplayName("comando publicado com a instância fora chega quando ela volta (sessão persistente)")
    void comandoPendenteChegaNaVolta() throws Exception {
        String instance = "inst-persist-" + UUID.randomUUID();
        ControlPlaneMqttClient primeira = otherInstanceClient(instance);
        primeira.start();
        observer.await(BrokerObserver.status(instance, "online"), Duration.ofSeconds(15));
        primeira.abortWithoutDisconnect();
        observer.await(BrokerObserver.status(instance, "offline"), Duration.ofSeconds(30));

        String id = "cmd-" + UUID.randomUUID();
        String tenant = "t-" + UUID.randomUUID();
        observer.publishCommand(instance, CP.command(KID, instance, id, "tenant.provision",
                Map.of("tenantId", tenant, "edition", "pro")).compact());
        assertThat(provisioner.calls()).as("instância fora: nada aplicado ainda").isEmpty();

        ControlPlaneMqttClient volta = otherInstanceClient(instance);
        try {
            volta.start();
            BrokerObserver.Received result = observer.await(BrokerObserver.result(id), Duration.ofSeconds(20));
            assertThat(result.body().path("status").asString()).isEqualTo("applied");
            assertThat(provisioner.count("provision", tenant)).isEqualTo(1);
        } finally {
            volta.stop();
        }
    }

    @Test
    @DisplayName("broker fora do ar: o heartbeat cai para HTTPS; quando volta, o MQTT reconecta e recebe comandos")
    void brokerForaDoAr() throws Exception {
        state.recordUsage("t-uso", "copilot.ia_creditos", 7);
        int antes = CP.heartbeats().size();

        MOSQUITTO.getDockerClient().pauseContainerCmd(MOSQUITTO.getContainerId()).exec();
        try {
            await().atMost(Duration.ofSeconds(20)).until(() -> !mqtt.isConnected());
            assertThat(heartbeat.send()).isEqualTo(HeartbeatService.Channel.HTTPS);
        } finally {
            MOSQUITTO.getDockerClient().unpauseContainerCmd(MOSQUITTO.getContainerId()).exec();
        }
        assertThat(CP.heartbeats()).hasSize(antes + 1);
        assertThat(CP.heartbeats().getLast()).contains("\"instanceId\":\"" + INSTANCE + "\"")
                .contains("copilot.ia_creditos");
        assertThat(CP.heartbeatAuth().getLast()).isEqualTo("Bearer " + CREDENTIAL);
        assertThat(store.unreportedRows(100)).as("consumo confirmado pelo Control Plane sai da fila").isEmpty();

        await().atMost(Duration.ofSeconds(30)).until(mqtt::isConnected);
        String id = "cmd-" + UUID.randomUUID();
        String tenant = "t-" + UUID.randomUUID();
        observer.publishCommand(INSTANCE, provisionCommand(id, tenant));
        observer.await(BrokerObserver.result(id), Duration.ofSeconds(20));
        assertThat(provisioner.count("provision", tenant)).isEqualTo(1);
    }

    @Test
    @DisplayName("com o broker no ar, o heartbeat vai pelo MQTT com o consumo agregado")
    void heartbeatPeloMqtt() {
        state.recordUsage("t-hb", "copilot.ia_creditos", 5);
        state.recordUsage("t-hb", "copilot.ia_creditos", 3);

        assertThat(heartbeat.send()).isEqualTo(HeartbeatService.Channel.MQTT);

        BrokerObserver.Received hb = observer.await(r -> r.topic().endsWith("/evt")
                && "heartbeat".equals(r.body().path("event").asString(null))
                && r.body().path("usage").toString().contains("t-hb"), Duration.ofSeconds(15));
        assertThat(hb.body().path("version").asString()).isEqualTo("2.4.0");
        assertThat(hb.body().path("usage").toString()).contains("\"amount\":8");
        assertThat(store.unreportedRows(100)).isEmpty();
    }

    @Test
    @DisplayName("presets e manifesto são publicados uma vez por versão do produto")
    void manifesto() {
        await().atMost(Duration.ofSeconds(15)).until(() -> store.meta("manifest.version").isPresent());
        assertThat(store.meta("manifest.version")).contains("2.4.0");
        assertThat(manifest.publishIfChanged()).as("mesma versão não publica de novo").isFalse();
    }

    private ControlPlaneMqttClient otherInstanceClient(String instance) {
        CommandProcessor processor = new CommandProcessor(
                new CommandVerifier(keyStore, FakeControlPlane.ISSUER, instance), store, state, provisionerProvider,
                new TransactionTemplate(transactionManager));
        return new ControlPlaneMqttClient(brokerUri(), instance, instance, "x", instance, "powerview", "2.4.0",
                Duration.ofHours(1), Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(2), processor);
    }
}
