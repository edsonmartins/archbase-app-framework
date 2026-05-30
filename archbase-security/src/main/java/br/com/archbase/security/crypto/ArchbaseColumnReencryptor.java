package br.com.archbase.security.crypto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper reutilizável para re-cifrar em massa o conteúdo de uma coluna de texto, de um formato
 * legado (texto puro, AES-ECB, etc.) para o formato canônico do archbase: AES-GCM marcado com o
 * prefixo {@value ArchbaseEncryptedStringConverter#GCM_PREFIX} (mesmo formato escrito pelo
 * {@link ArchbaseEncryptedStringConverter}).
 *
 * <p>Trabalha sobre uma {@link Connection} JDBC pura — não depende do Flyway nem de Hibernate —
 * para poder ser chamado de uma migração Flyway/Java, de um runner de boot, ou de um script de
 * manutenção. Cada projeto declara apenas a tabela/coluna e como decodificar o valor legado:
 *
 * <pre>{@code
 * // texto puro -> GCM
 * reencryptor.reencryptColumn(conn, "PARAMETROCONEXAO", "ID_PARAMETROCONEXAO", "VALOR",
 *         ArchbaseColumnReencryptor.PLAINTEXT);
 *
 * // AES-ECB legado -> GCM (decoder específico do projeto)
 * reencryptor.reencryptColumn(conn, "AUTORIZACAO", "ID_AUTORIZACAO", "SENHA", ecb::decrypt);
 * }</pre>
 *
 * <p><b>Idempotente:</b> linhas cujo valor já começa com o prefixo {@code gcm:} são ignoradas, de
 * modo que rodar a migração mais de uma vez (ou sobre dados parcialmente migrados) é seguro.
 *
 * <p>Os nomes de tabela/coluna são interpolados diretamente no SQL; <b>devem vir de código
 * confiável</b> (constantes da própria aplicação), nunca de entrada de usuário.
 */
public class ArchbaseColumnReencryptor {

    private static final Logger log = LoggerFactory.getLogger(ArchbaseColumnReencryptor.class);

    /** Decoder identidade para colunas cujo legado é texto puro (sem cifragem). */
    public static final UnaryOperator<String> PLAINTEXT = value -> value;

    private final ArchbaseCryptoService cryptoService;
    private final int batchSize;

    public ArchbaseColumnReencryptor(ArchbaseCryptoService cryptoService) {
        this(cryptoService, 500);
    }

    public ArchbaseColumnReencryptor(ArchbaseCryptoService cryptoService, int batchSize) {
        this.cryptoService = cryptoService;
        this.batchSize = batchSize;
    }

    /**
     * Re-cifra para AES-GCM todas as linhas da {@code valueColumn} cujo valor ainda não esteja no
     * formato GCM. {@code null} e valores já prefixados são pulados.
     *
     * @param connection   conexão JDBC ativa (transação controlada pelo chamador)
     * @param table        nome da tabela (confiável)
     * @param pkColumn      nome da coluna de chave primária (confiável)
     * @param valueColumn  nome da coluna a re-cifrar (confiável)
     * @param legacyDecoder converte o valor legado armazenado em texto claro antes de re-cifrar
     *                      (use {@link #PLAINTEXT} quando o legado já for texto puro)
     * @return número de linhas efetivamente re-cifradas
     */
    public int reencryptColumn(Connection connection, String table, String pkColumn,
                               String valueColumn, UnaryOperator<String> legacyDecoder) throws SQLException {

        if (!tableExists(connection, table)) {
            log.info("Re-cifragem GCM: tabela {} não existe ainda — pulando", table);
            return 0;
        }

        Map<String, String> pending = loadPending(connection, table, pkColumn, valueColumn);
        if (pending.isEmpty()) {
            log.info("Re-cifragem GCM: {}.{} já está no formato GCM (nada a fazer)", table, valueColumn);
            return 0;
        }

        String updateSql = "UPDATE " + table + " SET " + valueColumn + " = ? WHERE " + pkColumn + " = ?";
        int updated = 0;
        int inBatch = 0;
        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
            for (Map.Entry<String, String> row : pending.entrySet()) {
                String pk = row.getKey();
                String clearText = legacyDecoder.apply(row.getValue());
                String gcmValue = ArchbaseEncryptedStringConverter.GCM_PREFIX + cryptoService.encrypt(clearText);

                update.setString(1, gcmValue);
                update.setString(2, pk);
                update.addBatch();

                if (++inBatch == batchSize) {
                    updated += sum(update.executeBatch());
                    inBatch = 0;
                }
            }
            if (inBatch > 0) {
                updated += sum(update.executeBatch());
            }
        }

        log.info("Re-cifragem GCM: {}.{} -> {} linha(s) re-cifrada(s)", table, valueColumn, updated);
        return updated;
    }

    private Map<String, String> loadPending(Connection connection, String table, String pkColumn,
                                            String valueColumn) throws SQLException {
        // Coleta em memória para não manter o cursor de leitura aberto durante os UPDATEs na mesma
        // conexão (alguns drivers não suportam SELECT e UPDATE simultâneos por conexão).
        String selectSql = "SELECT " + pkColumn + ", " + valueColumn + " FROM " + table
                + " WHERE " + valueColumn + " IS NOT NULL"
                + " AND " + valueColumn + " NOT LIKE '" + ArchbaseEncryptedStringConverter.GCM_PREFIX + "%'";

        Map<String, String> pending = new LinkedHashMap<>();
        try (PreparedStatement select = connection.prepareStatement(selectSql);
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                pending.put(rs.getString(1), rs.getString(2));
            }
        }
        return pending;
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        // Tenta o nome como declarado e suas variações de caixa (MySQL pode ser case-sensitive).
        for (String candidate : new String[]{table, table.toUpperCase(), table.toLowerCase()}) {
            try (ResultSet rs = connection.getMetaData()
                    .getTables(connection.getCatalog(), null, candidate, new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int sum(int[] batchResults) {
        int total = 0;
        for (int r : batchResults) {
            // Statement.SUCCESS_NO_INFO (-2) conta como 1 linha; EXECUTE_FAILED (-3) não deveria
            // ocorrer (lançaria BatchUpdateException antes), mas por segurança não conta.
            total += (r >= 0) ? r : (r == java.sql.Statement.SUCCESS_NO_INFO ? 1 : 0);
        }
        return total;
    }
}
