package br.com.archbase.starter.offline.sync;

import br.com.archbase.offline.sync.persistence.ProcessedSyncOperationRepository;
import br.com.archbase.offline.sync.service.SyncOperationProcessor;
import br.com.archbase.offline.sync.web.AppVersionGate;
import br.com.archbase.offline.sync.web.ArchbaseSyncController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuração do módulo offline-sync: registra processor/executor/controller,
 * a entidade de idempotência e seu repositório, e um {@link AppVersionGate} no-op
 * default. Plug-and-play — o app só implementa os {@code SyncOperationHandler} e o
 * {@code SyncTenantProvider}.
 *
 * <p><b>Entidade e repositório somam aos da aplicação.</b> Até a 3.5.0 esta classe declarava {@code @EntityScan} e
 * {@code @EnableJpaRepositories}, e os dois tiram da aplicação o que é dela: com um {@code @EntityScan} em qualquer
 * lugar o Spring Boot para de procurar entidades no pacote da aplicação, e com um {@code @EnableJpaRepositories} a
 * auto-configuração de repositórios do Boot recua. Adicionar o starter fazia sumir os repositórios da aplicação.
 *
 * <p>Agora o pacote do módulo entra nos pacotes de auto-configuração ({@link ArchbaseOfflineSyncPackagesAutoConfiguration},
 * antes do JPA), e o repositório é encontrado pela varredura do Boot. Só quando a aplicação declara o próprio
 * {@code @EnableJpaRepositories} (e o Boot recua) o repositório do módulo é declarado aqui, depois do JPA.
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration")
@ComponentScan(basePackageClasses = {SyncOperationProcessor.class, ArchbaseSyncController.class})
public class ArchbaseOfflineSyncAutoConfiguration {

    /** Default: aceita qualquer versão. O app sobrescreve para forçar update (426). */
    @Bean
    @ConditionalOnMissingBean(AppVersionGate.class)
    public AppVersionGate archbaseDefaultAppVersionGate() {
        return appVersion -> {
        };
    }

    /** Só quando a aplicação declarou o próprio {@code @EnableJpaRepositories} e o Boot não varreu o pacote. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(ProcessedSyncOperationRepository.class)
    @EnableJpaRepositories(basePackageClasses = ProcessedSyncOperationRepository.class)
    static class Repositorio {
    }
}
