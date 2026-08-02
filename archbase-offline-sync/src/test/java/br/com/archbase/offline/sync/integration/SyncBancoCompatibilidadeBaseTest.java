package br.com.archbase.offline.sync.integration;

import br.com.archbase.offline.sync.dto.SyncBatchRequestDTO;
import br.com.archbase.offline.sync.dto.SyncBatchResponseDTO;
import br.com.archbase.offline.sync.dto.SyncOpStatus;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.service.SyncOperationProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roda a semântica do protocolo de sync contra bancos reais.
 *
 * <p><b>Por que não basta o H2.</b> O que este módulo garante — isolamento por operação com
 * {@code REQUIRES_NEW} e idempotência por chave composta — depende de comportamento transacional e
 * de restrição de unicidade, exatamente onde os bancos divergem. No módulo de segurança, um teste
 * equivalente revelou que o SQL nativo nunca funcionou em MySQL; aqui a pergunta é a mesma, feita
 * antes de alguém descobrir em produção.
 *
 * <p>As subclasses fornecem o container. Sem Docker, são puladas.
 */
@SpringBootTest(classes = OfflineSyncTestConfig.class)
abstract class SyncBancoCompatibilidadeBaseTest {

    @Autowired
    SyncOperationProcessor processor;
    @Autowired
    SyncCounterRepository counters;
    @Autowired
    ProcessedSyncOperationRepository processed;

    @AfterEach
    void limpar() {
        processed.deleteAll();
        counters.deleteAll();
    }

    @Test
    @DisplayName("o esquema do módulo é criado neste banco")
    void esquemaCriado() {
        assertThat(processed.count()).isZero();
        assertThat(counters.count()).isZero();
    }

    @Test
    @DisplayName("erro em uma operação reverte só a si — REQUIRES_NEW isola de verdade")
    void erroParcialIsolaPorOperacao() {
        SyncBatchRequestDTO req = new SyncBatchRequestDTO();
        req.operations.add(op("opOk", "OK", "c1"));
        req.operations.add(op("opBoom", "BOOM", "c2"));

        SyncBatchResponseDTO resp = processor.process(req);

        assertThat(status(resp, "opOk")).isEqualTo(SyncOpStatus.PROCESSED);
        assertThat(status(resp, "opBoom")).isEqualTo(SyncOpStatus.FAILED);

        assertThat(counters.findById("c1")).as("op OK deve persistir").isPresent();
        assertThat(counters.findById("c2")).as("op BOOM deve reverter só a si").isEmpty();

        assertThat(processed.findByTenantIdAndOperationId("t1", "opOk")).isPresent();
        assertThat(processed.findByTenantIdAndOperationId("t1", "opBoom")).isEmpty();
    }

    @Test
    @DisplayName("reenvio da mesma operação é ignorado — idempotência pela chave composta")
    void reenvioEhIdempotente() {
        SyncBatchRequestDTO primeiro = new SyncBatchRequestDTO();
        primeiro.operations.add(op("opX", "OK", "c9"));
        assertThat(status(processor.process(primeiro), "opX")).isEqualTo(SyncOpStatus.PROCESSED);

        SyncBatchRequestDTO reenvio = new SyncBatchRequestDTO();
        reenvio.operations.add(op("opX", "OK", "c9"));
        SyncOpStatus statusReenvio = status(processor.process(reenvio), "opX");

        assertThat(statusReenvio)
                .as("a segunda vez não pode reprocessar")
                .isIn(SyncOpStatus.SKIPPED, SyncOpStatus.PROCESSED);
        assertThat(processed.count())
                .as("a chave composta (tenant, operationId) impede linha duplicada")
                .isEqualTo(1);
    }

    private SyncOpStatus status(SyncBatchResponseDTO r, String id) {
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
