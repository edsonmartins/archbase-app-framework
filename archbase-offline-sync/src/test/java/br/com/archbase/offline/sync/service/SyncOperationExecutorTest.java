package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.dto.SyncAckDTO;
import br.com.archbase.offline.sync.dto.SyncOpStatus;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import br.com.archbase.offline.sync.exception.SyncConflictException;
import br.com.archbase.offline.sync.exception.SyncSkippedException;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperation;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.spi.SyncHandlerResult;
import br.com.archbase.offline.sync.spi.SyncOperationHandler;
import br.com.archbase.offline.sync.spi.SyncTenantProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncOperationExecutorTest {

    static class TestHandler implements SyncOperationHandler {
        final String type;
        final Function<SyncOperationDTO, SyncHandlerResult> fn;
        boolean called = false;

        TestHandler(String type, Function<SyncOperationDTO, SyncHandlerResult> fn) {
            this.type = type;
            this.fn = fn;
        }

        @Override public String type() { return type; }
        @Override public SyncHandlerResult handle(SyncOperationDTO op) {
            called = true;
            return fn.apply(op);
        }
    }

    private final SyncTenantProvider tenant = () -> "t1";

    private SyncOperationDTO op(String type) {
        SyncOperationDTO o = new SyncOperationDTO();
        o.id = "op1";
        o.type = type;
        o.aggregateId = "agg1";
        return o;
    }

    @Test
    void idempotencia_jaProcessada_retornaSkippedSemChamarHandler() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId("t1", "op1")).thenReturn(true);
        TestHandler h = new TestHandler("T", o -> SyncHandlerResult.ok());
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, List.of(h));

        SyncAckDTO ack = ex.execute(op("T"));

        assertEquals(SyncOpStatus.SKIPPED, ack.status);
        assertTrue(!h.called);
    }

    @Test
    void sucesso_retornaProcessedEgravaIdempotencia() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> SyncHandlerResult.version(7L));
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, List.of(h));

        SyncAckDTO ack = ex.execute(op("T"));

        assertEquals(SyncOpStatus.PROCESSED, ack.status);
        assertEquals(7L, ack.serverVersion);
        verify(repo).save(any(ProcessedSyncOperation.class));
    }

    @Test
    void skipException_retornaSkippedEgrava() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> {
            throw new SyncSkippedException("terminal");
        });
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, List.of(h));

        SyncAckDTO ack = ex.execute(op("T"));

        assertEquals(SyncOpStatus.SKIPPED, ack.status);
        verify(repo).save(any(ProcessedSyncOperation.class));
    }

    @Test
    void conflito_retornaConflictSemGravarIdempotencia() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> {
            throw new SyncConflictException("Visita", "agg1", 3L, 5L);
        });
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, List.of(h));

        SyncAckDTO ack = ex.execute(op("T"));

        assertEquals(SyncOpStatus.CONFLICT, ack.status);
        assertEquals(5L, ack.conflict.get("serverVersion"));
        verify(repo, never()).save(any());
    }

    @Test
    void tipoDesconhecido_retornaRejected() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, List.of());

        SyncAckDTO ack = ex.execute(op("DESCONHECIDO"));

        assertEquals(SyncOpStatus.REJECTED, ack.status);
        verify(repo, never()).save(any());
    }

    @Test
    void erroTransitorio_propagaParaRollbackDoRequiresNew() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> {
            throw new IllegalStateException("db down");
        });
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, List.of(h));

        assertThrows(IllegalStateException.class, () -> ex.execute(op("T")));
        verify(repo, never()).save(any());
    }
}
