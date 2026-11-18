package br.com.archbase.controlplane.metrics;

import br.com.archbase.controlplane.license.LicenseStatus;
import br.com.archbase.controlplane.mqtt.ControlPlaneMqttClient;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.state.ControlPlaneStateStore;
import br.com.archbase.controlplane.tenant.TenantSnapshot;
import br.com.archbase.controlplane.tenant.TenantState;
import br.com.archbase.controlplane.token.JwksKeyStore;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;

/**
 * Gauges e contadores lidos do estado do módulo a cada coleta.
 *
 * @since 3.4.6
 */
public final class ControlPlaneGauges {

    public static final String LICENSE_STATUS = "controlplane.license.status";
    public static final String LICENSE_EXPIRY = "controlplane.license.expiry.seconds";
    public static final String LICENSE_GRACE_END = "controlplane.license.grace.end.seconds";
    public static final String TENANTS = "controlplane.tenants";
    public static final String JWKS_KEYS = "controlplane.jwks.keys";
    public static final String JWKS_FETCH = "controlplane.jwks.fetch";
    public static final String MQTT_CONNECTED = "controlplane.mqtt.connected";
    public static final String MQTT_CONNECT_FAILURES = "controlplane.mqtt.connect.failures";
    public static final String MQTT_DISCONNECTS = "controlplane.mqtt.disconnects";
    public static final String USAGE_UNREPORTED = "controlplane.usage.unreported";

    private ControlPlaneGauges() {
    }

    /** Licença, tenants, JWKS e consumo pendente. */
    public static void bind(MeterRegistry registry, ControlPlaneState state, ControlPlaneStateStore store,
                            JwksKeyStore keyStore) {
        Gauge.builder(LICENSE_STATUS, state, s -> licenseCode(s.licenseStatus()))
                .description("Licença da instância: 0 VALID, 1 GRACE, 2 EXPIRED (ou ausente)")
                .register(registry);
        Gauge.builder(LICENSE_EXPIRY, state, s -> s.license()
                        .map(l -> (double) Duration.between(s.clock().instant(), l.expiresAt()).toSeconds())
                        .orElse(Double.NaN))
                .description("Segundos até o exp da licença (negativo depois dele)")
                .baseUnit("seconds")
                .register(registry);
        Gauge.builder(LICENSE_GRACE_END, state, s -> s.graceEndsAt()
                        .map(end -> (double) Duration.between(s.clock().instant(), end).toSeconds())
                        .orElse(Double.NaN))
                .description("Segundos até o fim da carência; abaixo de zero, a instância está em modo restrito")
                .baseUnit("seconds")
                .register(registry);
        for (TenantState tenantState : TenantState.values()) {
            Gauge.builder(TENANTS, state, s -> s.tenantIds().stream()
                            .map(s::tenant)
                            .filter(t -> t.map(TenantSnapshot::state).orElse(TenantState.UNKNOWN) == tenantState)
                            .count())
                    .description("Tenants atendidos por estado")
                    .tag("state", tenantState.name())
                    .register(registry);
        }
        Gauge.builder(JWKS_KEYS, keyStore, k -> k.knownKeyIds().size())
                .description("Chaves do Control Plane carregadas; 0 = nenhum token é aceito")
                .register(registry);
        FunctionCounter.builder(JWKS_FETCH, keyStore, JwksKeyStore::fetchSuccesses)
                .description("Buscas do JWKS").tag("result", "success").register(registry);
        FunctionCounter.builder(JWKS_FETCH, keyStore, JwksKeyStore::fetchFailures)
                .description("Buscas do JWKS").tag("result", "failure").register(registry);
        Gauge.builder(USAGE_UNREPORTED, store, ControlPlaneStateStore::countUnreported)
                .description("Linhas de consumo metered ainda não reportadas ao Control Plane")
                .register(registry);
    }

    /** Conexão MQTT. */
    public static void bindMqtt(MeterRegistry registry, ControlPlaneMqttClient mqtt) {
        Gauge.builder(MQTT_CONNECTED, mqtt, m -> m.isConnected() ? 1 : 0)
                .description("1 = conectado ao broker do Control Plane")
                .register(registry);
        FunctionCounter.builder(MQTT_CONNECT_FAILURES, mqtt, ControlPlaneMqttClient::connectFailures)
                .description("Tentativas da primeira conexão ao broker que falharam")
                .register(registry);
        FunctionCounter.builder(MQTT_DISCONNECTS, mqtt, ControlPlaneMqttClient::disconnects)
                .description("Quedas da conexão com o broker")
                .register(registry);
    }

    static double licenseCode(LicenseStatus status) {
        return switch (status) {
            case VALID -> 0;
            case GRACE -> 1;
            case EXPIRED -> 2;
        };
    }
}
