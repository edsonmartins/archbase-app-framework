package br.com.archbase.controlplane.token;

import br.com.archbase.controlplane.it.FakeControlPlane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwksKeyStoreTest {

    @TempDir
    Path dir;

    @Test
    void buscaGravaCopiaLocalQueServeOffline() throws Exception {
        Path local = dir.resolve("jwks.json");
        try (FakeControlPlane cp = new FakeControlPlane().addKey("k1", true)) {
            JwksKeyStore online = new JwksKeyStore(cp.jwksUrl(), local.toString(), Duration.ZERO,
                    Duration.ofSeconds(2), Clock.systemUTC());
            online.initialize();
            assertThat(online.knownKeyIds()).containsExactly("k1");
            assertThat(Files.readString(local)).contains("\"k1\"");

            cp.setUp(false);
            JwksKeyStore offline = new JwksKeyStore(cp.jwksUrl(), local.toString(), Duration.ZERO,
                    Duration.ofSeconds(2), Clock.systemUTC());
            offline.initialize();
            assertThat(offline.resolve("k1")).isPresent();
        }
    }

    @Test
    void buscaQueFalhaMantemAsChavesAtuais() {
        try (FakeControlPlane cp = new FakeControlPlane().addKey("k1", true)) {
            JwksKeyStore store = new JwksKeyStore(cp.jwksUrl(), null, Duration.ZERO, Duration.ofSeconds(2),
                    Clock.systemUTC());
            store.initialize();
            cp.setUp(false);
            assertThat(store.resolve("desconhecida")).isEmpty();
            assertThat(store.resolve("k1")).isPresent();
        }
    }

    @Test
    void kidDesconhecidoNaoBuscaAntesDoIntervaloMinimo() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-17T10:00:00Z"));
        try (FakeControlPlane cp = new FakeControlPlane().addKey("k1", true)) {
            JwksKeyStore store = new JwksKeyStore(cp.jwksUrl(), null, Duration.ofSeconds(60), Duration.ofSeconds(2),
                    clock);
            store.initialize();
            int depoisDaSubida = cp.jwksRequests();

            store.resolve("x1");
            store.resolve("x2");
            store.resolve("x3");
            assertThat(cp.jwksRequests()).as("token forjado com kid aleatório não pode virar enxurrada")
                    .isEqualTo(depoisDaSubida);

            clock.advance(Duration.ofSeconds(61));
            store.resolve("x4");
            assertThat(cp.jwksRequests()).isEqualTo(depoisDaSubida + 1);
        }
    }

    @Test
    void ignoraEntradasQueNaoSaoEd25519() {
        String jwks = "{\"keys\":[{\"kty\":\"oct\",\"kid\":\"hmac\",\"k\":\"c2VncmVkby1kZS0zMi1ieXRlcy1wYXJhLXRlc3RlLTAx\"}]}";
        assertThat(JwksKeyStore.parse(jwks)).isEmpty();
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
