package br.com.archbase.controlplane.metrics;

import br.com.archbase.controlplane.license.InstanceLicense;
import br.com.archbase.controlplane.license.LicenseVerifier;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.state.ControlPlaneStateStore;
import br.com.archbase.controlplane.token.JwksKeyStore;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As regras em {@code alerts/controlplane-alerts.yml} referenciam métricas pelo nome no formato Prometheus — o que o
 * VictoriaMetrics coleta. Este teste falha se uma regra citar um nome que o módulo não exporta.
 */
class PrometheusNamesTest {

    @Test
    void toda_metrica_citada_nas_regras_existe() throws Exception {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        ControlPlaneMetrics metrics = new ControlPlaneMetrics(registry);
        metrics.tokenRejected("X");
        metrics.accessDenied("X");
        metrics.restrictedDenied("X", "enforce");
        metrics.command("t", "failed");
        metrics.heartbeat("none");
        metrics.usageExceeded("k");

        ControlPlaneState state = Mockito.mock(ControlPlaneState.class);
        Mockito.when(state.clock()).thenReturn(Clock.systemUTC());
        Mockito.when(state.licenseStatus()).thenReturn(br.com.archbase.controlplane.license.LicenseStatus.VALID);
        Mockito.when(state.license()).thenReturn(Optional.of(
                new InstanceLicense("i", "p", null, Instant.now().plusSeconds(60), "t")));
        Mockito.when(state.graceEndsAt()).thenReturn(Optional.of(Instant.now().plus(Duration.ofDays(1))));
        Mockito.when(state.tenantIds()).thenReturn(Set.of());
        ControlPlaneStateStore store = Mockito.mock(ControlPlaneStateStore.class);
        JwksKeyStore keyStore = new JwksKeyStore(null, null, Duration.ZERO, Duration.ofSeconds(1), Clock.systemUTC());
        ControlPlaneGauges.bind(registry, state, store, keyStore);
        br.com.archbase.controlplane.mqtt.ControlPlaneMqttClient mqtt =
                Mockito.mock(br.com.archbase.controlplane.mqtt.ControlPlaneMqttClient.class);
        ControlPlaneGauges.bindMqtt(registry, mqtt);

        String scrape = registry.scrape();
        Set<String> exported = new LinkedHashSet<>();
        Matcher m = Pattern.compile("(?m)^(controlplane_[a-z_]+)").matcher(scrape);
        while (m.find()) {
            exported.add(m.group(1));
        }

        String rules = Files.readString(Path.of("alerts/controlplane-alerts.yml"));
        Set<String> cited = new LinkedHashSet<>();
        Matcher r = Pattern.compile("controlplane_[a-z_]+").matcher(rules);
        while (r.find()) {
            cited.add(r.group());
        }

        assertThat(cited).isNotEmpty();
        assertThat(exported).containsAll(cited);
    }
}
