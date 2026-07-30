package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.dto.SyncAckDTO;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import br.com.archbase.offline.sync.exception.SyncConflictException;
import br.com.archbase.offline.sync.exception.SyncRejectedException;
import br.com.archbase.offline.sync.exception.SyncSkippedException;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperation;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.spi.SyncHandlerResult;
import br.com.archbase.offline.sync.spi.SyncOperationHandler;
import br.com.archbase.offline.sync.spi.SyncTenantProvider;
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
    private final Map<String, SyncOperationHandler> handlers = new HashMap<>();

    public SyncOperationExecutor(ProcessedSyncOperationRepository processedRepo,
                                 SyncTenantProvider tenantProvider,
                                 List<SyncOperationHandler> handlerBeans) {
        this.processedRepo = processedRepo;
        this.tenantProvider = tenantProvider;
        for (SyncOperationHandler h : handlerBeans) {
            this.handlers.put(h.type(), h);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncAckDTO execute(SyncOperationDTO op) {
        final String tenant = tenantProvider.currentTenantId();

        // Idempotência durável: já processada antes → no-op.
        if (processedRepo.existsByTenantIdAndOperationId(tenant, op.id)) {
            return SyncAckDTO.skipped(op.id);
        }

        final SyncOperationHandler handler = handlers.get(op.type);
        if (handler == null) {
            return SyncAckDTO.rejected(op.id, "Tipo não suportado: " + op.type);
        }

        try {
            final SyncHandlerResult result = handler.handle(op);
            processedRepo.save(new ProcessedSyncOperation(
                    tenant, op.id, "PROCESSED", op.type, op.aggregateId,
                    result == null ? null : result.getServerVersion(), LocalDateTime.now()));
            return SyncAckDTO.processed(op.id,
                    result == null ? null : result.getServerVersion());
        } catch (SyncSkippedException e) {
            processedRepo.save(new ProcessedSyncOperation(
                    tenant, op.id, "SKIPPED", op.type, op.aggregateId, null,
                    LocalDateTime.now()));
            return SyncAckDTO.skipped(op.id);
        } catch (SyncConflictException e) {
            // Não persiste como processada: permite reenvio após resolução.
            // Retorno normal = a transação (sem efeito) só faz commit vazio.
            return SyncAckDTO.conflict(op.id, e.getDetail());
        } catch (SyncRejectedException e) {
            // Erro de negócio TERMINAL: não persiste (não foi processada) e não
            // deve ser retentado às cegas. O cliente marca o registro em erro
            // com este motivo. Diferente de FAILED (transitório) abaixo.
            return SyncAckDTO.rejected(op.id, e.getMessage());
        }
        // Demais RuntimeException propagam → REQUIRES_NEW faz rollback só desta op;
        // o processador mapeia para FAILED (transitório).
    }
}
