package br.com.archbase.oidc.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Vínculo entre a identidade no IdP ({@code provedor + tenant + sub}) e o usuário local.
 * <p>
 * Tabela do módulo, por JDBC e não por JPA: não entra no mapeamento das aplicações, então projeto com
 * {@code ddl-auto=validate} só passa a precisar dela se adicionar este módulo — e mesmo assim ela é criada aqui,
 * conforme {@code archbase.security.schema.mode}. Generaliza a {@code CONTROLPLANE_USUARIO} do Control Plane.
 *
 * @since 3.5.0
 */
public class ExternalIdentityStore {

    public static final String TABLE = "SEGURANCA_IDENTIDADE_EXTERNA";

    /** SQL padrão (H2, PostgreSQL, MySQL, SQL Server, Oracle 12c+). */
    public static final String DDL = "CREATE TABLE " + TABLE + " ("
            + "PROVIDER_ID VARCHAR(40) NOT NULL, "
            + "TENANT_ID VARCHAR(100) NOT NULL, "
            + "SUBJECT VARCHAR(255) NOT NULL, "
            + "USER_ID VARCHAR(40) NOT NULL, "
            + "DH_VINCULO TIMESTAMP NOT NULL, "
            + "PRIMARY KEY (PROVIDER_ID, TENANT_ID, SUBJECT))";

    public static final String INDEX_DDL = "CREATE INDEX IX_SEG_IDENT_EXT_USUARIO ON " + TABLE + " (USER_ID)";

    private static final Logger log = LoggerFactory.getLogger(ExternalIdentityStore.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public ExternalIdentityStore(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public Optional<String> findUserId(String providerId, String tenantId, String subject) {
        return jdbc.queryForList("SELECT USER_ID FROM " + TABLE
                        + " WHERE PROVIDER_ID = ? AND TENANT_ID = ? AND SUBJECT = ?",
                String.class, providerId, tenantId, subject).stream().findFirst();
    }

    /** O {@code sub} ao qual o usuário já está vinculado neste provedor e tenant, se houver. */
    public Optional<String> findSubject(String providerId, String tenantId, String userId) {
        return jdbc.queryForList("SELECT SUBJECT FROM " + TABLE
                        + " WHERE PROVIDER_ID = ? AND TENANT_ID = ? AND USER_ID = ?",
                String.class, providerId, tenantId, userId).stream().findFirst();
    }

    /** Se o usuário tem vínculo com algum dos provedores. */
    public boolean isLinked(String userId, Collection<String> providerIds) {
        if (userId == null || providerIds.isEmpty()) {
            return false;
        }
        String in = String.join(",", providerIds.stream().map(p -> "?").toList());
        Object[] args = new Object[providerIds.size() + 1];
        args[0] = userId;
        int i = 1;
        for (String p : providerIds) {
            args[i++] = p;
        }
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM " + TABLE
                + " WHERE USER_ID = ? AND PROVIDER_ID IN (" + in + ")", Integer.class, args);
        return count != null && count > 0;
    }

    /** Grava o vínculo. Um vínculo concorrente já gravado para a mesma identidade é mantido. */
    public void link(String providerId, String tenantId, String subject, String userId) {
        try {
            jdbc.update("INSERT INTO " + TABLE + " (PROVIDER_ID, TENANT_ID, SUBJECT, USER_ID, DH_VINCULO) "
                    + "VALUES (?, ?, ?, ?, ?)", providerId, tenantId, subject, userId, Timestamp.from(Instant.now()));
            log.info("oidc.user.linked provider={} tenant={} userId={}", providerId, tenantId, userId);
        } catch (DuplicateKeyException e) {
            log.debug("oidc.user.link_exists provider={} tenant={}", providerId, tenantId);
        }
    }

    /** Um vínculo, para a API administrativa. */
    public record Link(String providerId, String tenantId, String subject, String userId, Instant linkedAt) {
    }

    public List<Link> findByUser(String tenantId, String userId) {
        return jdbc.query("SELECT PROVIDER_ID, TENANT_ID, SUBJECT, USER_ID, DH_VINCULO FROM " + TABLE
                        + " WHERE TENANT_ID = ? AND USER_ID = ? ORDER BY PROVIDER_ID, SUBJECT",
                (rs, i) -> new Link(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getTimestamp(5).toInstant()),
                tenantId, userId);
    }

    /** @return se havia vínculo para remover */
    public boolean unlink(String providerId, String tenantId, String subject) {
        int removed = jdbc.update("DELETE FROM " + TABLE + " WHERE PROVIDER_ID = ? AND TENANT_ID = ? AND SUBJECT = ?",
                providerId, tenantId, subject);
        if (removed > 0) {
            log.info("oidc.user.unlinked provider={} tenant={}", providerId, tenantId);
        }
        return removed > 0;
    }

    public void createTableIfMissing() {
        if (tableExists()) {
            return;
        }
        jdbc.execute(DDL);
        jdbc.execute(INDEX_DDL);
        log.info("oidc.schema.created table={}", TABLE);
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
}
