package br.com.archbase.oidc.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Histórico das migrações de identidade: quem rodou, em que tenant, para qual provedor, com qual plano e o que foi
 * gravado. Dry-run também é registrado — o relatório conferido faz parte da prova.
 * <p>
 * Tabela própria, e não a trilha de segurança do core: o tipo de evento daquela trilha é um enum com
 * {@code CHECK}/{@code ENUM} no banco, e um tipo novo seria recusado em toda base já criada.
 *
 * @since 3.5.0
 */
public class IdentityMigrationJournal {

    public static final String TABLE = "SEGURANCA_MIGRACAO_IDENTIDADE";

    public static final String DDL = "CREATE TABLE " + TABLE + " ("
            + "ID_MIGRACAO VARCHAR(40) NOT NULL PRIMARY KEY, "
            + "PROVIDER_ID VARCHAR(40) NOT NULL, "
            + "TENANT_ID VARCHAR(100) NOT NULL, "
            + "USER_ID VARCHAR(40) NOT NULL, "
            + "MODO VARCHAR(10) NOT NULL, "
            + "PLAN_HASH VARCHAR(64) NOT NULL, "
            + "QT_ENTRADAS INTEGER NOT NULL, "
            + "QT_VINCULADOS INTEGER NOT NULL, "
            + "QT_IGNORADOS INTEGER NOT NULL, "
            + "DH_EXECUCAO TIMESTAMP NOT NULL)";

    public enum Mode { DRY_RUN, EXECUTE }

    public record Run(String id, String providerId, String tenantId, String userId, Mode mode, String planHash,
                      int entries, int linked, int skipped, Instant executedAt) {
    }

    private static final Logger log = LoggerFactory.getLogger(IdentityMigrationJournal.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public IdentityMigrationJournal(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void record(String providerId, String tenantId, String userId, Mode mode, String planHash,
                       int entries, int linked, int skipped) {
        jdbc.update("INSERT INTO " + TABLE + " (ID_MIGRACAO, PROVIDER_ID, TENANT_ID, USER_ID, MODO, PLAN_HASH, "
                        + "QT_ENTRADAS, QT_VINCULADOS, QT_IGNORADOS, DH_EXECUCAO) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), providerId, tenantId, userId, mode.name(), planHash, entries, linked,
                skipped, Timestamp.from(Instant.now()));
    }

    public List<Run> findByTenant(String tenantId) {
        return jdbc.query("SELECT ID_MIGRACAO, PROVIDER_ID, TENANT_ID, USER_ID, MODO, PLAN_HASH, QT_ENTRADAS, "
                        + "QT_VINCULADOS, QT_IGNORADOS, DH_EXECUCAO FROM " + TABLE
                        + " WHERE TENANT_ID = ? ORDER BY DH_EXECUCAO DESC",
                (rs, i) -> new Run(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        Mode.valueOf(rs.getString(5)), rs.getString(6), rs.getInt(7), rs.getInt(8), rs.getInt(9),
                        rs.getTimestamp(10).toInstant()),
                tenantId);
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
}
