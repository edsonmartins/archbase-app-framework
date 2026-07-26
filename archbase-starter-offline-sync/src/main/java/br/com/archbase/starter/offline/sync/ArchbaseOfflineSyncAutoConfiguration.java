package br.com.archbase.starter.offline.sync;

import br.com.archbase.offline.sync.persistence.ProcessedSyncOperation;
import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.service.SyncOperationProcessor;
import br.com.archbase.offline.sync.web.AppVersionGate;
import br.com.archbase.offline.sync.web.ArchbaseSyncController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuração do módulo offline-sync: registra processor/executor/controller,
 * a entidade de idempotência e seu repositório, e um {@link AppVersionGate} no-op
 * default. Plug-and-play — o app só implementa os {@code SyncOperationHandler} e o
 * {@code SyncTenantProvider}.
 *
 * <p>Obs.: @EntityScan/@EnableJpaRepositories aqui adicionam os pacotes do módulo
 * aos do app (o app mantém os seus).
 */
@AutoConfiguration
@ComponentScan(basePackageClasses = {SyncOperationProcessor.class, ArchbaseSyncController.class})
@EntityScan(basePackageClasses = ProcessedSyncOperation.class)
@EnableJpaRepositories(basePackageClasses = ProcessedSyncOperationRepository.class)
public class ArchbaseOfflineSyncAutoConfiguration {

    /** Default: aceita qualquer versão. O app sobrescreve para forçar update (426). */
    @Bean
    @ConditionalOnMissingBean(AppVersionGate.class)
    public AppVersionGate archbaseDefaultAppVersionGate() {
        return appVersion -> {
        };
    }
}
