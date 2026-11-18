package br.com.archbase.controlplane.metrics;

import br.com.archbase.controlplane.license.LicenseStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneMetricsTest {

    @Test
    void contadoresPorTagSemTenant() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ControlPlaneMetrics metrics = new ControlPlaneMetrics(registry);

        metrics.tokenRejected("WRONG_AUDIENCE");
        metrics.tokenRejected("WRONG_AUDIENCE");
        metrics.command("tenant.provision", "failed");
        metrics.command(null, "rejected");
        metrics.restrictedDenied("LICENSE_EXPIRED", "report");

        assertThat(registry.get(ControlPlaneMetrics.TOKEN_REJECTED).tag("code", "WRONG_AUDIENCE").counter().count())
                .isEqualTo(2);
        assertThat(registry.get(ControlPlaneMetrics.COMMANDS).tags("type", "tenant.provision", "status", "failed")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get(ControlPlaneMetrics.COMMANDS).tags("type", "unknown", "status", "rejected")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get(ControlPlaneMetrics.RESTRICTED_DENIED).tag("enforcement", "report").counter().count())
                .isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(m -> assertThat(m.getId().getTag("tenant")).isNull());
    }

    @Test
    void noopNaoFalha() {
        ControlPlaneMetrics.NOOP.heartbeat("none");
        ControlPlaneMetrics.NOOP.usageExceeded("x");
    }

    @Test
    void codigoDaLicenca() {
        assertThat(ControlPlaneGauges.licenseCode(LicenseStatus.VALID)).isZero();
        assertThat(ControlPlaneGauges.licenseCode(LicenseStatus.GRACE)).isEqualTo(1);
        assertThat(ControlPlaneGauges.licenseCode(LicenseStatus.EXPIRED)).isEqualTo(2);
    }
}
