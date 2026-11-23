package br.com.archbase.oidc.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Lista de tokens revogados")
class RevokedTokenStoreTest {

    @Test
    @DisplayName("guarda só o hash, reconhece o revogado e remove os vencidos na próxima revogação")
    void revogaEPurga() {
        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:h2:mem:revogados;DB_CLOSE_DELAY=-1");
        Instant agora = Instant.parse("2026-11-21T12:00:00Z");
        RevokedTokenStore store = new RevokedTokenStore(ds, Clock.fixed(agora, ZoneOffset.UTC));
        store.createTableIfMissing();
        store.createTableIfMissing(); // idempotente

        store.revoke("token-antigo", "idp", agora.minusSeconds(60));
        store.revoke("token-atual", "idp", agora.plusSeconds(600));

        assertThat(store.isRevoked("token-atual")).isTrue();
        assertThat(store.isRevoked("token-antigo")).as("vencido, purgado").isFalse();
        assertThat(store.isRevoked("outro")).isFalse();
        assertThat(new JdbcTemplate(ds).queryForObject("SELECT COUNT(*) FROM " + RevokedTokenStore.TABLE
                + " WHERE TOKEN_HASH = 'token-atual'", Integer.class)).as("em claro, nunca").isZero();
    }
}
