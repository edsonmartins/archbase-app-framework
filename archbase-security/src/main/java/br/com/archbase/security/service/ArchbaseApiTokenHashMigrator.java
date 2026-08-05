package br.com.archbase.security.service;

import br.com.archbase.security.util.ApiTokenHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Preenche o {@code TOKEN_HASH} das linhas de token de API criadas antes da 3.0.11, quando o valor
 * era guardado em claro.
 *
 * <p><b>Por que em Java e não na migration SQL.</b> Nem toda aplicação usa
 * {@code archbase-starter-flyway}; quem gera schema por {@code hbm2ddl} ganharia a coluna vazia e
 * todos os tokens já emitidos parariam de autenticar. Rodando na subida da aplicação, a conversão
 * acontece em qualquer arranjo — e o cálculo é o mesmo {@link ApiTokenHasher} usado na
 * autenticação, sem depender de {@code sha256()} existir no banco.
 *
 * <p><b>Não destrutivo por padrão.</b> Só <i>acrescenta</i> o hash; o valor em claro permanece, de
 * modo que voltar para uma versão anterior do framework continua funcionando. Apagar o texto em
 * claro — que é o que de fato encerra o achado — é passo separado e explícito:
 *
 * <pre>archbase.security.api-token.purge-plaintext=true</pre>
 *
 * <p>Esse purge é <b>irreversível</b>: depois dele, um rollback do framework deixa todos os tokens
 * de API sem meio de autenticar, e não há como recuperá-los. Ligue-o num segundo deploy, depois de
 * confirmar que a versão nova está estável.
 */
@Component
@Slf4j
public class ArchbaseApiTokenHashMigrator {

    /**
     * Conexão JDBC própria, e <b>não</b> o {@code EntityManager}.
     *
     * <p>Este método rodava dentro de {@code @Transactional}, e por isso o {@code catch (Exception)}
     * abaixo não cumpria o que promete: uma consulta que falha pelo {@code EntityManager} marca a
     * transação como rollback-only, capturar a exceção não desfaz isso, e o commit ao sair do
     * método estoura {@code UnexpectedRollbackException} — que, escapando de um listener de
     * {@code ApplicationReadyEvent}, derruba a subida da aplicação. Exatamente o desfecho que o
     * javadoc do {@code catch} diz evitar, numa aplicação que use estes beans sem o schema.
     *
     * <p>Com conexão própria em autocommit, cada comando se resolve sozinho: uma falha aborta a
     * migração e nada mais.
     */
    @Autowired(required = false)
    private DataSource dataSource;

    @Value("${archbase.security.api-token.hash-migration-enabled:true}")
    private boolean migrationEnabled;

    @Value("${archbase.security.api-token.purge-plaintext:false}")
    private boolean purgePlaintext;

    /**
     * Roda antes do {@code ArchbaseSecurityHardeningValidator}: a checagem de
     * {@code purge-plaintext} avalia o resultado desta migração.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    public void migrate() {
        if (!migrationEnabled) {
            log.debug("Migração de hash de token de API desabilitada");
            return;
        }
        if (dataSource == null) {
            log.debug("Sem DataSource; migração de hash de token de API não executada");
            return;
        }
        try {
            int hashed = fillMissingHashes();
            if (hashed > 0) {
                log.info("Migração de token de API: {} token(s) tiveram o hash calculado", hashed);
            }
            if (purgePlaintext) {
                int purged = purgePlaintextValues();
                if (purged > 0) {
                    log.warn("Migração de token de API: valor em claro removido de {} token(s). "
                            + "Esta operação é irreversível.", purged);
                }
            } else if (countPlaintextRemaining() > 0) {
                log.warn("Ainda existem {} token(s) de API com o valor em claro no banco. "
                                + "Configure archbase.security.api-token.purge-plaintext=true para removê-los "
                                + "(irreversível) depois de validar esta versão.",
                        countPlaintextRemaining());
            }
        } catch (Exception e) {
            // Não impedir a subida da aplicação: a autenticação continua funcionando pelo ramo de
            // compatibilidade (token_hash IS NULL AND token = ?) enquanto isto não roda.
            log.error("Falha ao migrar hashes de token de API; a autenticação segue pelo valor em claro", e);
        }
    }

    /**
     * Consulta e atualização em SQL nativo de propósito: {@code ApiTokenEntity} tem
     * {@code @TenantId}, e via JPA a migração enxergaria apenas o tenant presente no contexto no
     * momento da subida — deixando os demais para trás sem nenhum sinal.
     */
    private int fillMissingHashes() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             Statement statement = conexao.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT id_token_api, token FROM seguranca_token_api "
                             + "WHERE token_hash IS NULL AND token IS NOT NULL")) {
            while (rs.next()) {
                rows.add(new Object[]{rs.getString(1), rs.getString(2)});
            }
        }

        int updated = 0;
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(
                     "UPDATE seguranca_token_api SET token_hash = ? WHERE id_token_api = ?")) {
            for (Object[] row : rows) {
                ps.setString(1, ApiTokenHasher.hash((String) row[1]));
                ps.setString(2, String.valueOf(row[0]));
                updated += ps.executeUpdate();
            }
        }
        return updated;
    }

    private int purgePlaintextValues() throws Exception {
        try (Connection conexao = dataSource.getConnection();
             Statement statement = conexao.createStatement()) {
            return statement.executeUpdate(
                    "UPDATE seguranca_token_api SET token = NULL "
                            + "WHERE token_hash IS NOT NULL AND token IS NOT NULL");
        }
    }

    private long countPlaintextRemaining() throws Exception {
        try (Connection conexao = dataSource.getConnection();
             Statement statement = conexao.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM seguranca_token_api WHERE token IS NOT NULL")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}
