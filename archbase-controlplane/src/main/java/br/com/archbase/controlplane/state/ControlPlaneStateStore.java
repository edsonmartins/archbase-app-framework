package br.com.archbase.controlplane.state;

import br.com.archbase.controlplane.tenant.TenantSnapshot;
import br.com.archbase.controlplane.tenant.TenantState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência do estado do Control Plane no banco do produto, em tabelas do módulo e por JDBC (fora do mapeamento
 * JPA da aplicação):
 * <ul>
 * <li>{@code CONTROLPLANE_LICENCA}: a licença da instância (uma linha);</li>
 * <li>{@code CONTROLPLANE_TENANT}: o último {@link TenantSnapshot} de cada tenant, em JSON;</li>
 * <li>{@code CONTROLPLANE_CONSUMO}: consumo metered registrado localmente, até ser reportado ao Control Plane.</li>
 * </ul>
 * No banco, e não em arquivo, para a gravação do consumo e — na Fase C — a dos comandos aplicados entrarem na mesma
 * transação do produto.
 *
 * @since 3.4.6
 */
public class ControlPlaneStateStore {

    public static final String LICENSE_TABLE = "CONTROLPLANE_LICENCA";
    public static final String TENANT_TABLE = "CONTROLPLANE_TENANT";
    public static final String USAGE_TABLE = "CONTROLPLANE_CONSUMO";

    private static final String LICENSE_ROW = "INSTANCIA";

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneStateStore.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();

    public ControlPlaneStateStore(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
    }

    // ------------------------------------------------------------------ esquema

    public void createTablesIfMissing() {
        String text = textType();
        createIfMissing(LICENSE_TABLE, "CREATE TABLE " + LICENSE_TABLE + " ("
                + "ID VARCHAR(20) NOT NULL PRIMARY KEY, "
                + "TOKEN " + text + " NOT NULL, "
                + "DH_ATUALIZACAO TIMESTAMP NOT NULL)");
        createIfMissing(TENANT_TABLE, "CREATE TABLE " + TENANT_TABLE + " ("
                + "TENANT_ID VARCHAR(100) NOT NULL PRIMARY KEY, "
                + "VERSAO BIGINT NOT NULL, "
                + "SNAPSHOT " + text + " NOT NULL, "
                + "DH_ATUALIZACAO TIMESTAMP NOT NULL)");
        createIfMissing(USAGE_TABLE, "CREATE TABLE " + USAGE_TABLE + " ("
                + "ID VARCHAR(40) NOT NULL PRIMARY KEY, "
                + "TENANT_ID VARCHAR(100) NOT NULL, "
                + "CHAVE VARCHAR(200) NOT NULL, "
                + "QUANTIDADE BIGINT NOT NULL, "
                + "DH_CONSUMO TIMESTAMP NOT NULL, "
                + "REPORTADO INTEGER NOT NULL)");
    }

    private void createIfMissing(String table, String ddl) {
        if (!tableExists(table)) {
            jdbc.execute(ddl);
            log.info("controlplane.schema.created table={}", table);
        }
    }

    private boolean tableExists(String table) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            for (String name : List.of(table, table.toLowerCase(Locale.ROOT))) {
                try (ResultSet rs = meta.getTables(connection.getCatalog(), null, name, new String[]{"TABLE"})) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível verificar a tabela " + table, e);
        }
    }

    /** Tipo de texto longo do banco em uso. */
    private String textType() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            if (product.contains("postgres")) {
                return "TEXT";
            }
            if (product.contains("mysql") || product.contains("mariadb")) {
                return "LONGTEXT";
            }
            if (product.contains("microsoft") || product.contains("sql server")) {
                return "NVARCHAR(MAX)";
            }
            return "CLOB";
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível identificar o banco", e);
        }
    }

    // ------------------------------------------------------------------ licença

    public Optional<String> loadLicenseToken() {
        return jdbc.queryForList("SELECT TOKEN FROM " + LICENSE_TABLE + " WHERE ID = ?", String.class, LICENSE_ROW)
                .stream().findFirst();
    }

    public void saveLicenseToken(String token) {
        Timestamp now = Timestamp.from(Instant.now());
        int updated = jdbc.update("UPDATE " + LICENSE_TABLE + " SET TOKEN = ?, DH_ATUALIZACAO = ? WHERE ID = ?",
                token, now, LICENSE_ROW);
        if (updated == 0) {
            jdbc.update("INSERT INTO " + LICENSE_TABLE + " (ID, TOKEN, DH_ATUALIZACAO) VALUES (?, ?, ?)",
                    LICENSE_ROW, token, now);
        }
    }

    // ------------------------------------------------------------------ tenants

    public Map<String, TenantSnapshot> loadTenants() {
        Map<String, TenantSnapshot> result = new HashMap<>();
        jdbc.query("SELECT TENANT_ID, SNAPSHOT FROM " + TENANT_TABLE, rs -> {
            String tenant = rs.getString(1);
            try {
                result.put(tenant, fromJson(rs.getString(2)));
            } catch (RuntimeException e) {
                log.error("controlplane.state.tenant_unreadable tnt={} erro={}", tenant, e.getMessage());
            }
        });
        return result;
    }

    public void saveTenant(TenantSnapshot snapshot) {
        String body = toJson(snapshot);
        Timestamp now = Timestamp.from(Instant.now());
        int updated = jdbc.update("UPDATE " + TENANT_TABLE
                        + " SET VERSAO = ?, SNAPSHOT = ?, DH_ATUALIZACAO = ? WHERE TENANT_ID = ?",
                snapshot.version(), body, now, snapshot.tenantId());
        if (updated == 0) {
            jdbc.update("INSERT INTO " + TENANT_TABLE + " (TENANT_ID, VERSAO, SNAPSHOT, DH_ATUALIZACAO) "
                    + "VALUES (?, ?, ?, ?)", snapshot.tenantId(), snapshot.version(), body, now);
        }
    }

    // ------------------------------------------------------------------ consumo

    public void recordUsage(String tenantId, String key, long amount, Instant at) {
        jdbc.update("INSERT INTO " + USAGE_TABLE + " (ID, TENANT_ID, CHAVE, QUANTIDADE, DH_CONSUMO, REPORTADO) "
                        + "VALUES (?, ?, ?, ?, ?, 0)",
                UUID.randomUUID().toString(), tenantId, key, amount, Timestamp.from(at));
    }

    /** Soma do consumo ainda não reportado ao Control Plane. */
    public long unreportedUsage(String tenantId, String key) {
        Long sum = jdbc.queryForObject("SELECT COALESCE(SUM(QUANTIDADE), 0) FROM " + USAGE_TABLE
                + " WHERE TENANT_ID = ? AND CHAVE = ? AND REPORTADO = 0", Long.class, tenantId, key);
        return sum == null ? 0 : sum;
    }

    // ------------------------------------------------------------------ JSON

    String toJson(TenantSnapshot s) {
        ObjectNode node = json.createObjectNode();
        node.put("tenantId", s.tenantId());
        node.put("version", s.version());
        node.put("state", s.state().name());
        node.put("suspendedAt", s.suspendedAt() == null ? null : s.suspendedAt().toString());
        node.put("edition", s.edition());
        ObjectNode entitlements = node.putObject("entitlements");
        s.entitlements().forEach(entitlements::put);
        ObjectNode baseline = node.putObject("usageBaseline");
        s.usageBaseline().forEach(baseline::put);
        if (s.seatHolders() != null) {
            ObjectNode seats = node.putObject("seatHolders");
            s.seatHolders().forEach(seats::put);
        }
        node.put("updatedAt", s.updatedAt() == null ? null : s.updatedAt().toString());
        return json.writeValueAsString(node);
    }

    TenantSnapshot fromJson(String body) {
        JsonNode node = json.readTree(body);
        Map<String, Long> entitlements = new LinkedHashMap<>();
        node.path("entitlements").properties().forEach(e -> entitlements.put(e.getKey(), e.getValue().asLong()));
        Map<String, Long> baseline = new LinkedHashMap<>();
        node.path("usageBaseline").properties().forEach(e -> baseline.put(e.getKey(), e.getValue().asLong()));
        Map<String, String> seats = null;
        if (node.hasNonNull("seatHolders")) {
            seats = new LinkedHashMap<>();
            Map<String, String> target = seats;
            node.get("seatHolders").properties().forEach(e -> target.put(e.getKey(), e.getValue().asString()));
        }
        return new TenantSnapshot(
                node.path("tenantId").asString(),
                node.path("version").asLong(),
                TenantState.parse(node.path("state").asString(null)),
                instant(node, "suspendedAt"),
                node.path("edition").asString(null),
                entitlements,
                baseline,
                seats,
                instant(node, "updatedAt"));
    }

    private static Instant instant(JsonNode node, String field) {
        return node.hasNonNull(field) ? Instant.parse(node.get(field).asString()) : null;
    }
}
