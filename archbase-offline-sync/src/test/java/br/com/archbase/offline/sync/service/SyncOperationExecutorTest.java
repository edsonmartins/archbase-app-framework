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
import br.com.archbase.offline.sync.spi.SyncUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.function.Function;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // ObjectProvider vazio (mock devolve null em getIfAvailable) → userId nulo.
    @SuppressWarnings("unchecked")
    private final ObjectProvider<SyncUserProvider> noUser =
            (ObjectProvider<SyncUserProvider>) mock(ObjectProvider.class);

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
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, noUser, List.of(h));

        SyncAckDTO ack = ex.execute(op("T"));

        assertEquals(SyncOpStatus.SKIPPED, ack.status);
        assertTrue(!h.called);
    }

    @Test
    void sucesso_retornaProcessedEgravaIdempotencia() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> SyncHandlerResult.version(7L));
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, noUser, List.of(h));

        SyncAckDTO ack = ex.execute(op("T"));

        assertEquals(SyncOpStatus.PROCESSED, ack.status);
        assertEquals(7L, ack.serverVersion);
        verify(repo).save(any(ProcessedSyncOperation.class));
    }

    @Test
    void auditoria_userId_populadoQuandoProviderPresente() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> SyncHandlerResult.ok());
        @SuppressWarnings("unchecked")
        ObjectProvider<SyncUserProvider> comUser =
                (ObjectProvider<SyncUserProvider>) mock(ObjectProvider.class);
        when(comUser.getIfAvailable()).thenReturn(() -> "promotor-joao");
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, comUser, List.of(h));

        ex.execute(op("T"));

        ArgumentCaptor<ProcessedSyncOperation> cap =
                ArgumentCaptor.forClass(ProcessedSyncOperation.class);
        verify(repo).save(cap.capture());
        assertEquals("promotor-joao", cap.getValue().getUserId());
        assertEquals("t1", cap.getValue().getTenantId());
    }

    @Test
    void auditoria_userId_nuloSemProvider() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> SyncHandlerResult.ok());
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, noUser, List.of(h));

        ex.execute(op("T"));

        ArgumentCaptor<ProcessedSyncOperation> cap =
                ArgumentCaptor.forClass(ProcessedSyncOperation.class);
        verify(repo).save(cap.capture());
        assertNull(cap.getValue().getUserId());
    }

    @Test
    void skipException_propaga_semGravarNoExecute() {
        // SYNC-004: execute() não captura mais; propaga para o processador traduzir
        // (e gravar o SKIPPED via recordSkipped, fora desta transação).
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> {
            throw new SyncSkippedException("terminal");
        });
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, noUser, List.of(h));

        assertThrows(SyncSkippedException.class, () -> ex.execute(op("T")));
        verify(repo, never()).save(any());
    }

    @Test
    void recordSkipped_gravaLedgerEdevolveSkipped() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, noUser, List.of());

        SyncAckDTO ack = ex.recordSkipped(op("T"));

        assertEquals(SyncOpStatus.SKIPPED, ack.status);
        verify(repo).save(any(ProcessedSyncOperation.class));
    }

    @Test
    void conflito_propaga_semGravarIdempotencia() {
        // SYNC-004: propaga; o processador mapeia para CONFLICT depois do rollback.
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        TestHandler h = new TestHandler("T", o -> {
            throw new SyncConflictException("Visita", "agg1", 3L, 5L);
        });
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, noUser, List.of(h));

        assertThrows(SyncConflictException.class, () -> ex.execute(op("T")));
        verify(repo, never()).save(any());
    }

    @Test
    void tipoDesconhecido_retornaRejected() {
        ProcessedSyncOperationRepository repo = mock(ProcessedSyncOperationRepository.class);
        when(repo.existsByTenantIdAndOperationId(any(), any())).thenReturn(false);
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, noUser, List.of());

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
        SyncOperationExecutor ex = new SyncOperationExecutor(repo, tenant, noUser, List.of(h));

        assertThrows(IllegalStateException.class, () -> ex.execute(op("T")));
        verify(repo, never()).save(any());
    }
}
