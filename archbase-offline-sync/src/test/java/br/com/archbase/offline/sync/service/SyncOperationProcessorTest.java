package br.com.archbase.offline.sync.service;

import br.com.archbase.offline.sync.dto.SyncAckDTO;
import br.com.archbase.offline.sync.dto.SyncBatchRequestDTO;
import br.com.archbase.offline.sync.dto.SyncBatchResponseDTO;
import br.com.archbase.offline.sync.dto.SyncOpStatus;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncOperationProcessorTest {

    /** Executor fake: devolve ACKs programados ou lança (transitório/tipado). */
    static class FakeExecutor implements SyncOperationExecutorPort {
        final Map<String, SyncAckDTO> responses = new HashMap<>();
        final Set<String> throwFor = new HashSet<>();
        final Map<String, RuntimeException> throwTyped = new HashMap<>();
        final List<String> executed = new ArrayList<>();
        final List<String> skipRecorded = new ArrayList<>();

        @Override
        public SyncAckDTO execute(SyncOperationDTO op) {
            executed.add(op.id);
            if (throwTyped.containsKey(op.id)) throw throwTyped.get(op.id);
            if (throwFor.contains(op.id)) throw new RuntimeException("boom");
            return responses.getOrDefault(op.id, SyncAckDTO.processed(op.id, 1L));
        }

        @Override
        public SyncAckDTO recordSkipped(SyncOperationDTO op) {
            skipRecorded.add(op.id);
            return SyncAckDTO.skipped(op.id);
        }
    }

    private SyncOperationDTO op(String id, String dependsOn) {
        SyncOperationDTO o = new SyncOperationDTO();
        o.id = id;
        o.type = "T";
        o.aggregateId = "agg-" + id;
        o.dependsOn = dependsOn;
        return o;
    }

    private SyncBatchRequestDTO batch(SyncOperationDTO... ops) {
        SyncBatchRequestDTO r = new SyncBatchRequestDTO();
        for (SyncOperationDTO o : ops) r.operations.add(o);
        return r;
    }

    private SyncAckDTO ackOf(SyncBatchResponseDTO r, String id) {
        return r.results.stream().filter(a -> a.id.equals(id)).findFirst().orElseThrow();
    }

    @Test
    void erroParcial_umaFalhaNaoDerrubaAsOutras() {
        FakeExecutor ex = new FakeExecutor();
        ex.throwFor.add("opB"); // opB falha (transitório)
        SyncOperationProcessor p = new SyncOperationProcessor(ex);

        SyncBatchResponseDTO r = p.process(batch(op("opA", null), op("opB", null), op("opC", null)));

        assertEquals(SyncOpStatus.PROCESSED, ackOf(r, "opA").status);
        assertEquals(SyncOpStatus.FAILED, ackOf(r, "opB").status);
        assertEquals(SyncOpStatus.PROCESSED, ackOf(r, "opC").status);
        assertFalse(r.isSuccess());
        assertEquals(3, r.results.size());
        assertEquals(2, r.counts.get("PROCESSED"));
        assertEquals(1, r.counts.get("FAILED"));
    }

    @Test
    void dependencia_bloqueiaQuandoDependenciaNaoConclui() {
        FakeExecutor ex = new FakeExecutor();
        ex.responses.put("opA", SyncAckDTO.failed("opA", "x")); // A falha
        SyncOperationProcessor p = new SyncOperationProcessor(ex);

        SyncBatchResponseDTO r = p.process(batch(op("opA", null), op("opB", "opA")));

        assertEquals(SyncOpStatus.FAILED, ackOf(r, "opA").status);
        assertEquals(SyncOpStatus.BLOCKED, ackOf(r, "opB").status);
        assertTrue(ex.executed.contains("opA"));
        assertFalse(ex.executed.contains("opB"), "dependente bloqueado não executa");
    }

    @Test
    void dependencia_liberaQuandoDependenciaConclui() {
        FakeExecutor ex = new FakeExecutor(); // A processa (default)
        SyncOperationProcessor p = new SyncOperationProcessor(ex);

        SyncBatchResponseDTO r = p.process(batch(op("opA", null), op("opB", "opA")));

        assertEquals(SyncOpStatus.PROCESSED, ackOf(r, "opA").status);
        assertEquals(SyncOpStatus.PROCESSED, ackOf(r, "opB").status);
        assertTrue(ex.executed.contains("opB"));
    }

    @Test
    void mapeiaExcecoesTipadasEmAcks_forDaTransacao() {
        FakeExecutor ex = new FakeExecutor();
        ex.throwTyped.put("opSkip",
                new br.com.archbase.offline.sync.exception.SyncSkippedException("já"));
        ex.throwTyped.put("opConf",
                new br.com.archbase.offline.sync.exception.SyncConflictException("versão"));
        ex.throwTyped.put("opRej",
                new br.com.archbase.offline.sync.exception.SyncRejectedException("negócio"));
        SyncOperationProcessor p = new SyncOperationProcessor(ex);

        SyncBatchResponseDTO r = p.process(
                batch(op("opSkip", null), op("opConf", null), op("opRej", null)));

        assertEquals(SyncOpStatus.SKIPPED, ackOf(r, "opSkip").status);
        assertEquals(SyncOpStatus.CONFLICT, ackOf(r, "opConf").status);
        assertEquals(SyncOpStatus.REJECTED, ackOf(r, "opRej").status);
        // skip roteia para o registro durável do ledger, fora da tx do executor.
        assertTrue(ex.skipRecorded.contains("opSkip"));
        assertFalse(ex.skipRecorded.contains("opConf"));
        assertFalse(ex.skipRecorded.contains("opRej"));
    }

    @Test
    void dependencia_ausenteDoLoteEhAssumidaSatisfeita() {
        FakeExecutor ex = new FakeExecutor();
        SyncOperationProcessor p = new SyncOperationProcessor(ex);

        SyncBatchResponseDTO r = p.process(batch(op("opB", "opX-fora-do-lote")));

        assertEquals(SyncOpStatus.PROCESSED, ackOf(r, "opB").status);
        assertTrue(ex.executed.contains("opB"));
    }
}
