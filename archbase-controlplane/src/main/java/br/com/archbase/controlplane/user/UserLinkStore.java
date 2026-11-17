package br.com.archbase.controlplane.user;

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
import java.util.List;
import java.util.Optional;

/**
 * Vínculo entre o usuário do Control Plane ({@code sub}) e o usuário local do produto, por tenant. Gravado no
 * primeiro acesso em que o usuário é encontrado pelo e-mail; dali em diante o e-mail pode mudar sem perder o
 * vínculo.
 * <p>
 * Tabela própria do módulo, acessada por JDBC e não por JPA: assim ela não entra no mapeamento das aplicações e
 * projetos com {@code ddl-auto=validate} não passam a exigi-la só por ter o jar no classpath.
 *
 * @since 3.4.6
 */
public class UserLinkStore {

    public static final String TABLE = "CONTROLPLANE_USUARIO";

    /** DDL em SQL padrão (H2, PostgreSQL, MySQL, SQL Server, Oracle 12c+). */
    public static final String DDL = "CREATE TABLE " + TABLE + " ("
            + "TENANT_ID VARCHAR(100) NOT NULL, "
            + "CP_USER_ID VARCHAR(100) NOT NULL, "
            + "USER_ID VARCHAR(40) NOT NULL, "
            + "DH_VINCULO TIMESTAMP NOT NULL, "
            + "PRIMARY KEY (TENANT_ID, CP_USER_ID))";

    private static final Logger log = LoggerFactory.getLogger(UserLinkStore.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public UserLinkStore(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public Optional<String> findLocalUserId(String tenantId, String cpUserId) {
        List<String> ids = jdbc.queryForList(
                "SELECT USER_ID FROM " + TABLE + " WHERE TENANT_ID = ? AND CP_USER_ID = ?",
                String.class, tenantId, cpUserId);
        return ids.stream().findFirst();
    }

    /** Grava o vínculo; um vínculo concorrente já gravado é mantido. */
    public void link(String tenantId, String cpUserId, String localUserId) {
        try {
            jdbc.update("INSERT INTO " + TABLE + " (TENANT_ID, CP_USER_ID, USER_ID, DH_VINCULO) VALUES (?, ?, ?, ?)",
                    tenantId, cpUserId, localUserId, Timestamp.from(Instant.now()));
            log.info("controlplane.user.linked tnt={} sub={} userId={}", tenantId, cpUserId, localUserId);
        } catch (DuplicateKeyException e) {
            log.debug("controlplane.user.link_exists tnt={} sub={}", tenantId, cpUserId);
        }
    }

    /** Cria a tabela se ela não existir. */
    public void createTableIfMissing() {
        if (tableExists()) {
            return;
        }
        jdbc.execute(DDL);
        log.info("controlplane.schema.created table={}", TABLE);
    }

    boolean tableExists() {
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
