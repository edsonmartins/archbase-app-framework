package br.com.archbase.oidc.provider;

import br.com.archbase.oidc.config.ArchbaseOidcProperties;
import br.com.archbase.oidc.it.FakeOidcProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cache da introspecção")
class OidcIntrospectorTest {

    /** Relógio que o teste avança. */
    static final class Relogio extends Clock {
        Instant agora = Instant.parse("2026-11-21T12:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private FakeOidcProvider idp;
    private Relogio relogio;
    private OidcIntrospector introspector;

    @BeforeEach
    void preparar() {
        idp = new FakeOidcProvider();
        relogio = new Relogio();
        ArchbaseOidcProperties props = new ArchbaseOidcProperties();
        ArchbaseOidcProperties.Provider p = new ArchbaseOidcProperties.Provider();
        p.getIntrospection().setEnabled(true);
        p.getIntrospection().setUri(idp.issuer("x") + "/introspect");
        p.getIntrospection().setClientId(FakeOidcProvider.INTROSPECTION_CLIENT);
        p.getIntrospection().setClientSecret(FakeOidcProvider.INTROSPECTION_SECRET);
        p.getIntrospection().setCacheTtl(Duration.ofSeconds(30));
        props.getProviders().put("x", p);
        introspector = new OidcIntrospector(props, relogio);
    }

    @AfterEach
    void desligar() {
        idp.close();
    }

    @Test
    @DisplayName("dentro do cache-ttl não consulta de novo; depois dele, consulta e vê a revogação")
    void expiraNoTtl() {
        Instant exp = relogio.agora.plusSeconds(3600);
        assertThat(introspector.check("x", "tok", exp)).isEqualTo(OidcIntrospector.Status.ACTIVE);
        idp.revoke("tok");

        relogio.agora = relogio.agora.plusSeconds(29);
        assertThat(introspector.check("x", "tok", exp)).as("ainda no cache").isEqualTo(OidcIntrospector.Status.ACTIVE);

        relogio.agora = relogio.agora.plusSeconds(2);
        assertThat(introspector.check("x", "tok", exp)).isEqualTo(OidcIntrospector.Status.INACTIVE);
        assertThat(idp.introspectionCalls()).isEqualTo(2);
    }

    @Test
    @DisplayName("o cache nunca passa do exp do token")
    void limitadoAoExp() {
        Instant exp = relogio.agora.plusSeconds(5);
        introspector.check("x", "tok", exp);

        relogio.agora = relogio.agora.plusSeconds(6);
        introspector.check("x", "tok", exp);

        assertThat(idp.introspectionCalls()).isEqualTo(2);
    }

    @Test
    @DisplayName("credencial do cliente errada ou IdP fora: UNAVAILABLE, sem cache")
    void indisponivel() {
        idp.introspectionDown(true);
        assertThat(introspector.check("x", "tok", null)).isEqualTo(OidcIntrospector.Status.UNAVAILABLE);
        idp.introspectionDown(false);
        assertThat(introspector.check("x", "tok", null)).isEqualTo(OidcIntrospector.Status.ACTIVE);
    }
}
