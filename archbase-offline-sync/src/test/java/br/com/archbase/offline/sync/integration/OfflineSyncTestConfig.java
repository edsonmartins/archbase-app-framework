package br.com.archbase.offline.sync.integration;

import br.com.archbase.offline.sync.dto.SyncOperationDTO;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperation;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.service.SyncOperationExecutor;
import br.com.archbase.offline.sync.service.SyncOperationProcessor;
import br.com.archbase.offline.sync.spi.SyncHandlerResult;
import br.com.archbase.offline.sync.spi.SyncOperationHandler;
import br.com.archbase.offline.sync.spi.SyncTenantProvider;
import br.com.archbase.offline.sync.spi.SyncUserProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

/**
 * Contexto compartilhado pelos testes de integração do módulo — H2, PostgreSQL e MySQL usam
 * exatamente o mesmo, para que a única variável entre eles seja o banco.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {ProcessedSyncOperation.class, SyncCounter.class})
@EnableJpaRepositories(basePackageClasses =
        {ProcessedSyncOperationRepository.class, SyncCounterRepository.class})
public class OfflineSyncTestConfig {

    @Bean
    SyncOperationExecutor syncOperationExecutor(ProcessedSyncOperationRepository repo,
                                                SyncTenantProvider tenant,
                                                ObjectProvider<SyncUserProvider> userProvider,
                                                List<SyncOperationHandler> handlers) {
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
            public String type() {
                return "OK";
            }

            public SyncHandlerResult handle(SyncOperationDTO op) {
                repo.save(new SyncCounter(op.aggregateId));
                return SyncHandlerResult.version(1L);
            }
        };
    }

    @Bean
    SyncOperationHandler boomHandler(SyncCounterRepository repo) {
        return new SyncOperationHandler() {
            public String type() {
                return "BOOM";
            }

            public SyncHandlerResult handle(SyncOperationDTO op) {
                repo.save(new SyncCounter(op.aggregateId)); // deve ser revertido
                throw new IllegalStateException("falha proposital");
            }
        };
    }
}
