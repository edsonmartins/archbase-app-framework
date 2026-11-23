package br.com.archbase.oidc.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * Tokens de IdP que <b>esta aplicação</b> deixou de aceitar antes do {@code exp} — por logout feito nela.
 * <p>
 * Em tabela, e não em memória, para valer em todas as instâncias. Guarda só o SHA-256 do token e a expiração dele;
 * entradas vencidas são removidas a cada nova revogação (depois do {@code exp} a assinatura já recusa o token).
 * Mesma política de esquema da tabela de vínculo: JDBC, fora do JPA, conforme {@code archbase.security.schema.mode}.
 *
 * @since 3.5.0
 */
public class RevokedTokenStore {

    public static final String TABLE = "SEGURANCA_TOKEN_EXTERNO_REVOGADO";

    public static final String DDL = "CREATE TABLE " + TABLE + " ("
            + "TOKEN_HASH VARCHAR(64) NOT NULL PRIMARY KEY, "
            + "PROVIDER_ID VARCHAR(40) NOT NULL, "
            + "DH_EXPIRACAO TIMESTAMP NOT NULL, "
            + "DH_REVOGACAO TIMESTAMP NOT NULL)";

    private static final Logger log = LoggerFactory.getLogger(RevokedTokenStore.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public RevokedTokenStore(DataSource dataSource, Clock clock) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.clock = clock;
    }

    public boolean isRevoked(String token) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + TABLE + " WHERE TOKEN_HASH = ?", Integer.class,
                hash(token));
        return n != null && n > 0;
    }

    public void revoke(String token, String providerId, Instant expiresAt) {
        Instant now = clock.instant();
        jdbc.update("DELETE FROM " + TABLE + " WHERE DH_EXPIRACAO < ?", Timestamp.from(now));
        try {
            jdbc.update("INSERT INTO " + TABLE + " (TOKEN_HASH, PROVIDER_ID, DH_EXPIRACAO, DH_REVOGACAO) VALUES (?, ?, ?, ?)",
                    hash(token), providerId, Timestamp.from(expiresAt != null ? expiresAt : now.plusSeconds(86_400)),
                    Timestamp.from(now));
        } catch (DuplicateKeyException e) {
            log.debug("oidc.token.already_revoked provider={}", providerId);
        }
    }

    public void createTableIfMissing() {
        if (!tableExists()) {
            jdbc.execute(DDL);
            log.info("oidc.schema.created table={}", TABLE);
        }
    }

    public boolean tableExists() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            for (String name : List.of(TABLE, TABLE.toLowerCase())) {
                try (ResultSet rs = meta.getTables(connection.getCatalog(), null, name, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível verificar a tabela " + TABLE, e);
        }
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
