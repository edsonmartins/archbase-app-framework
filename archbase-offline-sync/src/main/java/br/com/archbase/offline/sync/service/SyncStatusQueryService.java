package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.dto.SyncStatusRequestDTO;
import br.com.archbase.offline.sync.dto.SyncStatusResponseDTO;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperation;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.spi.SyncTenantProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Reconciliação por transação (padrão idempotente): dado um conjunto de
 * {@code operationId} que o cliente ainda não confirmou, responde quais já
 * foram processados no servidor (existem no ledger {@code processed_sync_operation}).
 *
 * <p>Resolve o caso "o servidor deu commit mas o cliente perdeu o response":
 * o cliente pergunta e marca localmente como enviado o que já passou, sem
 * reexecutar a mutação (consulta read-only). É por transação individual, então
 * não conflita com dados novos que o usuário produza no intervalo.
 */
@Service
public class SyncStatusQueryService {

    private final ProcessedSyncOperationRepository processedRepo;
    private final SyncTenantProvider tenantProvider;

    public SyncStatusQueryService(ProcessedSyncOperationRepository processedRepo,
                                  SyncTenantProvider tenantProvider) {
        this.processedRepo = processedRepo;
        this.tenantProvider = tenantProvider;
    }

    @Transactional(readOnly = true)
    public SyncStatusResponseDTO processed(SyncStatusRequestDTO request) {
        final List<String> ids =
                request == null || request.operationIds == null
                        ? List.of()
                        : request.operationIds;
        if (ids.isEmpty()) {
            return new SyncStatusResponseDTO(List.of());
        }
        final String tenant = tenantProvider.currentTenantId();
        final List<String> processados = processedRepo
                .findByTenantIdAndOperationIdIn(tenant, ids)
                .stream()
                .map(ProcessedSyncOperation::getOperationId)
                .collect(Collectors.toList());
        return new SyncStatusResponseDTO(processados);
    }
}
