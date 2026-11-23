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
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cortes de logout avisados pelo IdP (Back-Channel Logout): a partir deles, tokens daquele usuário ou daquela sessão
 * emitidos até o aviso deixam de valer nesta aplicação.
 * <p>
 * O access token do IdP não está em lugar nenhum daqui — só a assinatura o valida —, então não há o que apagar. O
 * que se guarda é a regra: "token do provedor P com {@code sub} S e {@code iat} até T não vale" ou "token com
 * {@code sid} X não vale". Três tipos de corte:
 * <ul>
 * <li>{@link #SUB}: aviso só com {@code sub} — todas as sessões do usuário;</li>
 * <li>{@link #SID}: aviso com {@code sid} — o token que traz esse {@code sid};</li>
 * <li>{@link #SUB_SEM_SID}: aviso com {@code sub} e {@code sid} — tokens do usuário que <b>não</b> trazem
 * {@code sid} (o ArchGuard legado, por exemplo) não têm como ser ligados à sessão encerrada, então caem também.
 * Erra para o lado de deslogar a mais.</li>
 * </ul>
 * Uma consulta por requisição, só para provedor com back-channel ligado. Registros vencidos
 * ({@code token-lifetime} depois do corte) são apagados a cada novo aviso.
 *
 * @since 3.5.0
 */
public class ExternalLogoutStore {

    public static final String TABLE = "SEGURANCA_LOGOUT_EXTERNO";

    public static final String DDL = "CREATE TABLE " + TABLE + " ("
            + "PROVIDER_ID VARCHAR(40) NOT NULL, "
            + "TIPO VARCHAR(12) NOT NULL, "
            + "VALOR VARCHAR(255) NOT NULL, "
            + "DH_CORTE TIMESTAMP NOT NULL, "
            + "DH_EXPIRACAO TIMESTAMP NOT NULL, "
            + "PRIMARY KEY (PROVIDER_ID, TIPO, VALOR))";

    static final String SUB = "SUB";
    static final String SID = "SID";
    static final String SUB_SEM_SID = "SUB_SEM_SID";

    private static final Logger log = LoggerFactory.getLogger(ExternalLogoutStore.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ExternalLogoutStore(DataSource dataSource, Clock clock) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.clock = clock;
    }

    /**
     * Registra o aviso de logout.
     *
     * @param subject  {@code sub} do aviso, ou {@code null}
     * @param sid      {@code sid} do aviso, ou {@code null}
     * @param cutoff   {@code iat} do aviso — o relógio do IdP, o mesmo dos tokens que ele emite
     * @param lifetime por quanto tempo o corte vale
     */
    public void record(String providerId, String subject, String sid, Instant cutoff, java.time.Duration lifetime) {
        Instant now = clock.instant();
        jdbc.update("DELETE FROM " + TABLE + " WHERE DH_EXPIRACAO < ?", Timestamp.from(now));
        Instant expires = (cutoff.isAfter(now) ? cutoff : now).plus(lifetime);
        if (sid != null) {
            upsert(providerId, SID, sid, cutoff, expires);
        }
        if (subject != null) {
            upsert(providerId, sid == null ? SUB : SUB_SEM_SID, subject, cutoff, expires);
        }
    }

    /**
     * @param subject  {@code sub} do access token
     * @param sid      {@code sid} do access token, ou {@code null}
     * @param issuedAt {@code iat} do access token; sem ele, qualquer corte do usuário o alcança
     */
    public boolean isLoggedOut(String providerId, String subject, String sid, Instant issuedAt) {
        Timestamp iat = Timestamp.from(issuedAt != null ? issuedAt : Instant.EPOCH);
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + TABLE + " WHERE PROVIDER_ID = ? AND (");
        List<Object> args = new ArrayList<>();
        args.add(providerId);
        List<String> terms = new ArrayList<>();
        if (subject != null) {
            terms.add("(TIPO = ? AND VALOR = ? AND DH_CORTE >= ?)");
            args.add(SUB);
            args.add(subject);
            args.add(iat);
            if (sid == null) {
                terms.add("(TIPO = ? AND VALOR = ? AND DH_CORTE >= ?)");
                args.add(SUB_SEM_SID);
                args.add(subject);
                args.add(iat);
            }
        }
        if (sid != null) {
            terms.add("(TIPO = ? AND VALOR = ?)");
            args.add(SID);
            args.add(sid);
        }
        if (terms.isEmpty()) {
            return false;
        }
        sql.append(String.join(" OR ", terms)).append(")");
        Integer n = jdbc.queryForObject(sql.toString(), Integer.class, args.toArray());
        return n != null && n > 0;
    }

    /** Só avança: aviso repetido ou atrasado não recua um corte já registrado. */
    private void upsert(String providerId, String type, String value, Instant cutoff, Instant expires) {
        int updated = jdbc.update("UPDATE " + TABLE + " SET DH_CORTE = ?, DH_EXPIRACAO = ? "
                        + "WHERE PROVIDER_ID = ? AND TIPO = ? AND VALOR = ? AND DH_CORTE < ?",
                Timestamp.from(cutoff), Timestamp.from(expires), providerId, type, value, Timestamp.from(cutoff));
        if (updated > 0) {
            return;
        }
        try {
            jdbc.update("INSERT INTO " + TABLE + " (PROVIDER_ID, TIPO, VALOR, DH_CORTE, DH_EXPIRACAO) "
                    + "VALUES (?, ?, ?, ?, ?)", providerId, type, value, Timestamp.from(cutoff), Timestamp.from(expires));
        } catch (DuplicateKeyException e) {
            log.debug("oidc.backchannel.cutoff_already_newer provider={} type={}", providerId, type);
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
}
