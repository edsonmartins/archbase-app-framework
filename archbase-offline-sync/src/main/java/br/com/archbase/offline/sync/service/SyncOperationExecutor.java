package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.dto.SyncAckDTO;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperation;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.spi.SyncHandlerResult;
import br.com.archbase.offline.sync.spi.SyncOperationHandler;
import br.com.archbase.offline.sync.spi.SyncTenantProvider;
import br.com.archbase.offline.sync.spi.SyncUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executa cada operação em transação PRÓPRIA ({@link Propagation#REQUIRES_NEW}).
 * É a peça que garante o "erro parcial": um CONFLICT/FAILED reverte só esta
 * operação, jamais o lote inteiro (o bug que derrubou a tentativa anterior).
 *
 * <p>Idempotência durável: grava/consulta {@code processed_sync_operation} DENTRO
 * da mesma transação do efeito — commit atômico.
 */
@Service
public class SyncOperationExecutor implements SyncOperationExecutorPort {

    private final ProcessedSyncOperationRepository processedRepo;
    private final SyncTenantProvider tenantProvider;
    private final ObjectProvider<SyncUserProvider> userProvider;
    private final Map<String, SyncOperationHandler> handlers = new HashMap<>();

    public SyncOperationExecutor(ProcessedSyncOperationRepository processedRepo,
                                 SyncTenantProvider tenantProvider,
                                 ObjectProvider<SyncUserProvider> userProvider,
                                 List<SyncOperationHandler> handlerBeans) {
        this.processedRepo = processedRepo;
        this.tenantProvider = tenantProvider;
        this.userProvider = userProvider;
        for (SyncOperationHandler h : handlerBeans) {
            this.handlers.put(h.type(), h);
        }
    }

    /**
     * Resolve o usuário corrente para auditoria. Opcional e à prova de falha:
     * sem bean {@link SyncUserProvider}, ou se ele lançar, devolve {@code null}
     * (a auditoria é best-effort — nunca derruba a operação de sync).
     */
    private String resolveUserId() {
        final SyncUserProvider provider = userProvider.getIfAvailable();
        if (provider == null) {
            return null;
        }
        try {
            return provider.currentUserId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncAckDTO execute(SyncOperationDTO op) {
        final String tenant = tenantProvider.currentTenantId();
        final String userId = resolveUserId();

        // Idempotência durável: já processada antes → no-op.
        if (processedRepo.existsByTenantIdAndOperationId(tenant, op.id)) {
            return SyncAckDTO.skipped(op.id);
        }

        final SyncOperationHandler handler = handlers.get(op.type);
        if (handler == null) {
            return SyncAckDTO.rejected(op.id, "Tipo não suportado: " + op.type);
        }

        // SYNC-004: efeito de domínio + gravação no ledger no MESMO commit desta
        // REQUIRES_NEW. Se o insert do ledger falhar, o efeito de domínio reverte
        // junto — nunca "domínio aplicado sem registro no ledger" (que fazia o
        // reenvio duplicar). As exceções de negócio (skip/conflict/rejected) e as
        // transitórias NÃO são capturadas aqui: propagam para o
        // SyncOperationProcessor traduzir em ACK DEPOIS do rollback. Capturá-las
        // aqui e retornar normalmente estouraria UnexpectedRollbackException
        // quando o service de domínio tivesse marcado a transação rollback-only.
        final SyncHandlerResult result = handler.handle(op);
        processedRepo.save(new ProcessedSyncOperation(
                tenant, op.id, "PROCESSED", op.type, op.aggregateId, userId,
                result == null ? null : result.getServerVersion(), LocalDateTime.now()));
        return SyncAckDTO.processed(op.id,
                result == null ? null : result.getServerVersion());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncAckDTO recordSkipped(SyncOperationDTO op) {
        // Skip não tem efeito de domínio a preservar; gravar o ledger numa tx
        // própria, depois do rollback do execute, é seguro e mantém a idempotência
        // durável (um reenvio do mesmo id volta como SKIPPED pelo dedup).
        final String tenant = tenantProvider.currentTenantId();
        if (!processedRepo.existsByTenantIdAndOperationId(tenant, op.id)) {
            processedRepo.save(new ProcessedSyncOperation(
                    tenant, op.id, "SKIPPED", op.type, op.aggregateId, resolveUserId(),
                    null, LocalDateTime.now()));
        }
        return SyncAckDTO.skipped(op.id);
    }
}
