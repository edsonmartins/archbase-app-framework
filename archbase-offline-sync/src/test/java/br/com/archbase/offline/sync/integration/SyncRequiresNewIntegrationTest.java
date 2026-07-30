package br.com.archbase.offline.sync.integration;

import br.com.archbase.offline.sync.dto.SyncBatchRequestDTO;
import br.com.archbase.offline.sync.dto.SyncBatchResponseDTO;
import br.com.archbase.offline.sync.dto.SyncOpStatus;
import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperation;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.service.SyncOperationExecutor;
import br.com.archbase.offline.sync.service.SyncOperationProcessor;
import br.com.archbase.offline.sync.spi.SyncHandlerResult;
import br.com.archbase.offline.sync.spi.SyncOperationHandler;
import br.com.archbase.offline.sync.spi.SyncTenantProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova, com Spring + H2, a semântica que quebrou na tentativa anterior:
 * <b>transação por operação (REQUIRES_NEW)</b> — uma op que falha reverte SÓ a si
 * mesma; a op que teve sucesso no mesmo lote permanece persistida.
 *
 * <p>Usa {@code @SpringBootTest} (sem transação de teste) para observar de fato o
 * commit das transações REQUIRES_NEW.
 */
@SpringBootTest(
        classes = SyncRequiresNewIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SyncRequiresNewIntegrationTest {

    @Autowired
    SyncOperationProcessor processor;
    @Autowired
    SyncCounterRepository counters;
    @Autowired
    ProcessedSyncOperationRepository processed;

    @Test
    void erroParcial_revertimentoIsoladoPorOperacao() {
        SyncBatchRequestDTO req = new SyncBatchRequestDTO();
        req.operations.add(op("opOk", "OK", "c1"));
        req.operations.add(op("opBoom", "BOOM", "c2"));

        SyncBatchResponseDTO resp = processor.process(req);

        assertEquals(SyncOpStatus.PROCESSED, ack(resp, "opOk"));
        assertEquals(SyncOpStatus.FAILED, ack(resp, "opBoom"));

        // A op OK persiste; a op BOOM foi revertida (REQUIRES_NEW isolou).
        assertTrue(counters.findById("c1").isPresent(), "op OK deve persistir");
        assertFalse(counters.findById("c2").isPresent(), "op BOOM deve reverter só a si");

        // Idempotência gravada só para a OK.
        assertTrue(processed.findByTenantIdAndOperationId("t1", "opOk").isPresent());
        assertFalse(processed.findByTenantIdAndOperationId("t1", "opBoom").isPresent());

        // Limpeza (contexto não é transacional).
        counters.deleteById("c1");
        processed.deleteAll();
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

    // --- Infra de teste ---

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ProcessedSyncOperation.class, SyncCounter.class})
    @EnableJpaRepositories(basePackageClasses =
            {ProcessedSyncOperationRepository.class, SyncCounterRepository.class})
    static class Config {
        @Bean
        SyncOperationExecutor syncOperationExecutor(ProcessedSyncOperationRepository repo,
                                                    SyncTenantProvider tenant,
                                                    org.springframework.beans.factory.ObjectProvider<
                                                            br.com.archbase.offline.sync.spi.SyncUserProvider> userProvider,
                                                    java.util.List<SyncOperationHandler> handlers) {
            return new SyncOperationExecutor(repo, tenant, userProvider, handlers);
        }

        @Bean
        SyncOperationProcessor syncOperationProcessor(SyncOperationExecutor executor) {
            return new SyncOperationProcessor(executor);
        }

        @Bean
        SyncTenantProvider tenantProvider() {
            return () -> "t1";
        }

        @Bean
        SyncOperationHandler okHandler(SyncCounterRepository repo) {
            return new SyncOperationHandler() {
                public String type() { return "OK"; }
                public SyncHandlerResult handle(SyncOperationDTO op) {
                    repo.save(new SyncCounter(op.aggregateId));
                    return SyncHandlerResult.version(1L);
                }
            };
        }

        @Bean
        SyncOperationHandler boomHandler(SyncCounterRepository repo) {
            return new SyncOperationHandler() {
                public String type() { return "BOOM"; }
                public SyncHandlerResult handle(SyncOperationDTO op) {
                    repo.save(new SyncCounter(op.aggregateId)); // deve ser revertido
                    throw new IllegalStateException("falha proposital");
                }
            };
        }
    }
}
