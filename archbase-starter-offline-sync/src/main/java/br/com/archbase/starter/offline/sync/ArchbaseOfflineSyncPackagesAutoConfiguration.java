package br.com.archbase.starter.offline.sync;

import br.com.archbase.offline.sync.persistence.ProcessedSyncOperation;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Soma o pacote de entidade e repositório do offline-sync aos da aplicação, sem substituí-los.
 *
 * <p>Precisa rodar antes das auto-configurações do JPA: a de repositórios lê a lista de pacotes e a fixa; o que for
 * somado depois não conta. Se a aplicação tem {@code @EntityScan} (e o Boot usa só esses pacotes), o pacote entra
 * nele também.
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"})
@Import(ArchbaseOfflineSyncPackagesAutoConfiguration.PacoteDoModulo.class)
public class ArchbaseOfflineSyncPackagesAutoConfiguration {

    static class PacoteDoModulo implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            String pacote = ProcessedSyncOperation.class.getPackageName();
            AutoConfigurationPackages.register(registry, pacote);
            if (registry.containsBeanDefinition(EntityScanPackages.class.getName())) {
                EntityScanPackages.register(registry, pacote);
            }
        }
    }
}
