package br.com.archbase.security.service;

import br.com.archbase.security.util.ApiTokenHasher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @PersistenceContext
    private EntityManager entityManager;

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
    @Transactional
    public void migrate() {
        if (!migrationEnabled) {
            log.debug("Migração de hash de token de API desabilitada");
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
    @SuppressWarnings("unchecked")
    private int fillMissingHashes() {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "SELECT id_token_api, token FROM seguranca_token_api "
                                + "WHERE token_hash IS NULL AND token IS NOT NULL")
                .getResultList();

        int updated = 0;
        for (Object[] row : rows) {
            String id = String.valueOf(row[0]);
            String plaintext = (String) row[1];
            updated += entityManager.createNativeQuery(
                            "UPDATE seguranca_token_api SET token_hash = :hash WHERE id_token_api = :id")
                    .setParameter("hash", ApiTokenHasher.hash(plaintext))
                    .setParameter("id", id)
                    .executeUpdate();
        }
        return updated;
    }

    private int purgePlaintextValues() {
        return entityManager.createNativeQuery(
                        "UPDATE seguranca_token_api SET token = NULL "
                                + "WHERE token_hash IS NOT NULL AND token IS NOT NULL")
                .executeUpdate();
    }

    private long countPlaintextRemaining() {
        Object result = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM seguranca_token_api WHERE token IS NOT NULL")
                .getSingleResult();
        return result != null ? Long.parseLong(result.toString()) : 0L;
    }
}
