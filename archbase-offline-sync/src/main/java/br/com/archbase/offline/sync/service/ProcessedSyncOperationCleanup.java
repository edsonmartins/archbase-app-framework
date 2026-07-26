package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Poda a idempotência durável: remove registros de {@code processed_sync_operation}
 * mais antigos que a retenção (default 90 dias). Roda diariamente às 03:00.
 *
 * <p>Requer {@code @EnableScheduling} no app (padrão nas apps Archbase). Transação
 * própria ({@code REQUIRES_NEW}) para não amarrar a nenhuma outra.
 */
@Component
public class ProcessedSyncOperationCleanup {

    private static final Logger log = LoggerFactory.getLogger(ProcessedSyncOperationCleanup.class);
    private static final int RETENTION_DAYS = 90;

    private final ProcessedSyncOperationRepository repository;

    public ProcessedSyncOperationCleanup(ProcessedSyncOperationRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purgeOld() {
        final LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        final int removed = repository.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.info("offline-sync: {} operações idempotentes purgadas (< {})", removed, cutoff);
        }
    }
}
