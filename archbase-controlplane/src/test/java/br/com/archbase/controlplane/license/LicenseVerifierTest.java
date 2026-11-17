package br.com.archbase.controlplane.license;

import br.com.archbase.controlplane.it.FakeControlPlane;
import br.com.archbase.controlplane.token.JwksKeyStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicenseVerifierTest {

    private FakeControlPlane cp;
    private LicenseVerifier verifier;

    @BeforeEach
    void setUp() {
        cp = new FakeControlPlane().addKey("k1", true);
        JwksKeyStore store = new JwksKeyStore(cp.jwksUrl(), null, Duration.ZERO, Duration.ofSeconds(2),
                Clock.systemUTC());
        store.initialize();
        verifier = new LicenseVerifier(store, FakeControlPlane.ISSUER, "inst-1", "powerview");
    }

    @AfterEach
    void tearDown() {
        cp.close();
    }

    @Test
    void licencaValida() {
        Instant exp = Instant.now().plus(Duration.ofDays(30));
        InstanceLicense license = verifier.verify(cp.license("k1", "inst-1", "powerview", exp).compact());
        assertThat(license.instanceId()).isEqualTo("inst-1");
        assertThat(license.product()).isEqualTo("powerview");
        assertThat(license.expiresAt().getEpochSecond()).isEqualTo(exp.getEpochSecond());
    }

    @Test
    void licencaVencidaComAssinaturaBoaEAceitaParaOStatusDecidir() {
        Instant exp = Instant.now().minus(Duration.ofDays(2));
        InstanceLicense license = verifier.verify(cp.license("k1", "inst-1", "powerview", exp).compact());
        assertThat(license.statusAt(Instant.now(), Duration.ofDays(7))).isEqualTo(LicenseStatus.GRACE);
        assertThat(license.statusAt(Instant.now(), Duration.ofDays(1))).isEqualTo(LicenseStatus.EXPIRED);
    }

    @Test
    void recusaOutraInstanciaOutroProdutoForjadaOuQueNaoELicenca() {
        Instant exp = Instant.now().plus(Duration.ofDays(30));
        assertThatThrownBy(() -> verifier.verify(cp.license("k1", "inst-2", "powerview", exp).compact()))
                .isInstanceOf(LicenseRejectedException.class).hasMessageContaining("outra instância");
        assertThatThrownBy(() -> verifier.verify(cp.license("k1", "inst-1", "vendax", exp).compact()))
                .isInstanceOf(LicenseRejectedException.class).hasMessageContaining("outro produto");
        assertThatThrownBy(() -> verifier.verify(
                cp.license("k1", "inst-1", "powerview", exp).signedByStranger().compact()))
                .isInstanceOf(LicenseRejectedException.class);
        assertThatThrownBy(() -> verifier.verify(
                cp.session("k1", "inst-1", "a@b.c", "t1", "member").claim("prd", "powerview").compact()))
                .isInstanceOf(LicenseRejectedException.class).hasMessageContaining("token_use");
        assertThatThrownBy(() -> verifier.verify(
                cp.license("k1", "inst-1", "powerview", exp).issuer("https://falso").compact()))
                .isInstanceOf(LicenseRejectedException.class);
    }

    @Test
    void semInstanceIdNaoHaComoAceitar() {
        JwksKeyStore store = new JwksKeyStore(cp.jwksUrl(), null, Duration.ZERO, Duration.ofSeconds(2),
                Clock.systemUTC());
        store.initialize();
        LicenseVerifier semInstancia = new LicenseVerifier(store, FakeControlPlane.ISSUER, null, "powerview");
        assertThatThrownBy(() -> semInstancia.verify(
                cp.license("k1", "inst-1", "powerview", Instant.now().plusSeconds(60)).compact()))
                .isInstanceOf(LicenseRejectedException.class).hasMessageContaining("instance-id");
    }

    @Test
    void statusNasFronteiras() {
        Instant exp = Instant.parse("2026-12-01T00:00:00Z");
        InstanceLicense license = new InstanceLicense("i", "p", null, exp, "t");
        Duration grace = Duration.ofDays(7);
        assertThat(license.statusAt(exp, grace)).isEqualTo(LicenseStatus.VALID);
        assertThat(license.statusAt(exp.plusSeconds(1), grace)).isEqualTo(LicenseStatus.GRACE);
        assertThat(license.statusAt(exp.plus(grace), grace)).isEqualTo(LicenseStatus.GRACE);
        assertThat(license.statusAt(exp.plus(grace).plusSeconds(1), grace)).isEqualTo(LicenseStatus.EXPIRED);
    }
}
