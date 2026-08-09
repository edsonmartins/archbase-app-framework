package br.com.archbase.offline.sync.integration;

import br.com.archbase.offline.sync.dto.SyncBatchRequestDTO;
import br.com.archbase.offline.sync.dto.SyncBatchResponseDTO;
import br.com.archbase.offline.sync.dto.SyncOpStatus;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.service.SyncOperationProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SYNC-004: efeito de domínio + ledger de idempotência no MESMO commit, e a
 * tradução de exceção em ACK DEPOIS do rollback (fora da transação do executor).
 *
 * <p>Prova, com Spring + H2 (sem transação de teste, para observar o commit real):
 * <ul>
 *   <li>REJECT que escreve domínio e depois lança → ACK REJECTED, domínio revertido
 *       e SEM linha no ledger (atomicidade: nada meio-aplicado);</li>
 *   <li>SKIP → ACK SKIPPED com o ledger gravado (idempotência durável);</li>
 *   <li>CONFLICT → ACK CONFLICT sem persistir no ledger (permite reenvio).</li>
 * </ul>
 */
@SpringBootTest(
        classes = OfflineSyncTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SyncAtomicidadeIntegrationTest {

    @Autowired
    SyncOperationProcessor processor;
    @Autowired
    SyncCounterRepository counters;
    @Autowired
    ProcessedSyncOperationRepository processed;

    @AfterEach
    void limpar() {
        counters.deleteAll();
        processed.deleteAll();
    }

    @Test
    void reject_aposEscrita_reverteDominioEnaoGravaLedger() {
        SyncBatchRequestDTO req = new SyncBatchRequestDTO();
        req.operations.add(op("opRej", "REJECT", "cRej"));

        SyncBatchResponseDTO resp = processor.process(req);

        assertEquals(SyncOpStatus.REJECTED, ack(resp, "opRej"));
        assertFalse(counters.findById("cRej").isPresent(),
                "SYNC-004: domínio escrito pelo handler deve reverter junto");
        assertFalse(processed.findByTenantIdAndOperationId("t1", "opRej").isPresent(),
                "rejeitada não entra no ledger");
    }

    @Test
    void skip_gravaLedgerEdevolveSkipped() {
        SyncBatchRequestDTO req = new SyncBatchRequestDTO();
        req.operations.add(op("opSkip", "SKIP", "cSkip"));

        SyncBatchResponseDTO resp = processor.process(req);

        assertEquals(SyncOpStatus.SKIPPED, ack(resp, "opSkip"));
        assertTrue(processed.findByTenantIdAndOperationId("t1", "opSkip").isPresent(),
                "skip grava o ledger (idempotência durável)");
    }

    @Test
    void conflict_naoPersisteEdevolveConflict() {
        SyncBatchRequestDTO req = new SyncBatchRequestDTO();
        req.operations.add(op("opConf", "CONFLICT", "cConf"));

        SyncBatchResponseDTO resp = processor.process(req);

        assertEquals(SyncOpStatus.CONFLICT, ack(resp, "opConf"));
        assertFalse(processed.findByTenantIdAndOperationId("t1", "opConf").isPresent(),
                "conflito não entra no ledger (permite reenvio após resolução)");
    }

    private SyncOpStatus ack(SyncBatchResponseDTO r, String id) {
        return r.results.stream().filter(a -> a.id.equals(id)).findFirst().orElseThrow().status;
    }

    private SyncOperationDTO op(String id, String type, String aggId) {
        SyncOperationDTO o = new SyncOperationDTO();
        o.id = id;
        o.type = type;
        o.aggregateId = aggId;
        return o;
    }
}
