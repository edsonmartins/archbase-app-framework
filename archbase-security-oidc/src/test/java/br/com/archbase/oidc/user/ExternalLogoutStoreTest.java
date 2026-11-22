package br.com.archbase.oidc.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cortes de logout avisados pelo IdP")
class ExternalLogoutStoreTest {

    private static final Instant AGORA = Instant.parse("2026-11-21T12:00:00Z");
    private static final Duration VIDA = Duration.ofHours(1);

    private DriverManagerDataSource ds;
    private ExternalLogoutStore store;

    @BeforeEach
    void preparar() {
        ds = new DriverManagerDataSource("jdbc:h2:mem:cortes;DB_CLOSE_DELAY=-1");
        store = new ExternalLogoutStore(ds, Clock.fixed(AGORA, ZoneOffset.UTC));
        store.createTableIfMissing();
        store.createTableIfMissing(); // idempotente
        new JdbcTemplate(ds).update("DELETE FROM " + ExternalLogoutStore.TABLE);
    }

    @Test
    @DisplayName("só sub: derruba os tokens emitidos até o corte, de qualquer sessão; os posteriores valem")
    void porSub() {
        store.record("idp", "ana", null, AGORA, VIDA);

        assertThat(store.isLoggedOut("idp", "ana", null, AGORA.minusSeconds(60))).isTrue();
        assertThat(store.isLoggedOut("idp", "ana", "qualquer-sid", AGORA.minusSeconds(60))).isTrue();
        assertThat(store.isLoggedOut("idp", "ana", null, AGORA)).as("mesmo segundo do corte").isTrue();
        assertThat(store.isLoggedOut("idp", "ana", null, AGORA.plusSeconds(1))).as("login depois").isFalse();
        assertThat(store.isLoggedOut("idp", "ana", null, null)).as("sem iat: não dá para provar que é depois")
                .isTrue();
        assertThat(store.isLoggedOut("idp", "bruno", null, AGORA.minusSeconds(60))).isFalse();
        assertThat(store.isLoggedOut("outro", "ana", null, AGORA.minusSeconds(60))).as("outro provedor").isFalse();
    }

    @Test
    @DisplayName("sub + sid: derruba a sessão do sid e os tokens sem sid; outra sessão com sid continua")
    void porSid() {
        store.record("idp", "ana", "s1", AGORA, VIDA);

        assertThat(store.isLoggedOut("idp", "ana", "s1", AGORA.plusSeconds(30))).as("sid é a sessão: sem iat")
                .isTrue();
        assertThat(store.isLoggedOut("idp", "ana", "s2", AGORA.minusSeconds(60))).isFalse();
        assertThat(store.isLoggedOut("idp", "ana", null, AGORA.minusSeconds(60)))
                .as("token sem sid não se liga a sessão nenhuma: cai junto").isTrue();
        assertThat(store.isLoggedOut("idp", "ana", null, AGORA.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("aviso repetido ou atrasado não recua o corte")
    void soAvanca() {
        store.record("idp", "ana", null, AGORA, VIDA);
        store.record("idp", "ana", null, AGORA.minusSeconds(300), VIDA);
        store.record("idp", "ana", null, AGORA, VIDA);

        assertThat(store.isLoggedOut("idp", "ana", null, AGORA.minusSeconds(60))).isTrue();
    }

    @Test
    @DisplayName("corte vencido é apagado no próximo aviso")
    void purga() {
        ExternalLogoutStore depois = new ExternalLogoutStore(ds,
                Clock.fixed(AGORA.plus(VIDA).plusSeconds(1), ZoneOffset.UTC));
        store.record("idp", "ana", null, AGORA, VIDA);
        depois.record("idp", "bruno", null, AGORA.plus(VIDA), VIDA);

        assertThat(store.isLoggedOut("idp", "ana", null, AGORA.minusSeconds(60))).isFalse();
        assertThat(store.isLoggedOut("idp", "bruno", null, AGORA)).isTrue();
    }
}
