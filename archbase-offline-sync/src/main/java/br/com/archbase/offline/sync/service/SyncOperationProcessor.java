package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.dto.SyncAckDTO;
import br.com.archbase.offline.sync.dto.SyncBatchRequestDTO;
import br.com.archbase.offline.sync.dto.SyncBatchResponseDTO;
import br.com.archbase.offline.sync.dto.SyncOpStatus;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Orquestra o lote de sync. NÃO é transacional: cada operação roda na sua própria
 * transação via {@link SyncOperationExecutorPort} (REQUIRES_NEW). Assim uma falha
 * não desfaz as operações já aplicadas — o ACK é individual e honesto.
 */
@Service
public class SyncOperationProcessor {

    private final SyncOperationExecutorPort executor;

    public SyncOperationProcessor(SyncOperationExecutorPort executor) {
        this.executor = executor;
    }

    public SyncBatchResponseDTO process(SyncBatchRequestDTO request) {
        final SyncBatchResponseDTO resp = new SyncBatchResponseDTO();
        final Map<String, SyncOpStatus> statusById = new HashMap<>();

        if (request == null || request.operations == null) {
            resp.recount();
            resp.serverTime = nowIso();
            return resp;
        }

        for (SyncOperationDTO op : request.operations) {
            // Dependência: se a dependência veio no MESMO lote e não concluiu, bloqueia.
            // Dependência ausente do lote é assumida satisfeita (a idempotência
            // durável cobre o caso de já ter sido processada antes).
            if (op.dependsOn != null && statusById.containsKey(op.dependsOn)) {
                final SyncOpStatus dep = statusById.get(op.dependsOn);
                if (dep != SyncOpStatus.PROCESSED && dep != SyncOpStatus.SKIPPED) {
                    final SyncAckDTO blocked = SyncAckDTO.blocked(op.id, op.dependsOn);
                    resp.results.add(blocked);
                    statusById.put(op.id, blocked.status);
                    continue;
                }
            }

            SyncAckDTO ack;
            try {
                ack = executor.execute(op);
            } catch (RuntimeException e) {
                // Erro transitório: o REQUIRES_NEW já reverteu SÓ esta operação.
                ack = SyncAckDTO.failed(op.id, e.getMessage());
            }
            resp.results.add(ack);
            statusById.put(op.id, ack.status);
        }

        resp.recount();
        resp.serverTime = nowIso();
        return resp;
    }

    private String nowIso() {
        return ZonedDateTime.now(ZoneOffset.UTC).toString();
    }
}
