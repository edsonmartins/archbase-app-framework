package br.com.archbase.starter.core.auto.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

/**
 * Registrar que configura automaticamente component scan, entity scan e repository scan
 * para packages adicionais definidos via propriedades.
 *
 * Propriedades suportadas:
 * - archbase.app.component.scan: packages para component scan (separados por vírgula)
 * - archbase.app.jpa.entities: packages para entity scan (separados por vírgula)
 * - archbase.app.jpa.repositories: packages para repository scan (separados por vírgula)
 *
 * Compatible with Spring Boot 3.5.3+
 */
public class ArchbaseAdditionalScanConfigurer implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(ArchbaseAdditionalScanConfigurer.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        registerComponentScan(registry);
        registerEntityScan(registry);
        registerRepositoryScan(registry);
    }

    private void registerComponentScan(BeanDefinitionRegistry registry) {
        String componentScanProperty = environment.getProperty("archbase.app.component.scan");
        if (!StringUtils.hasText(componentScanProperty)) {
            return;
        }

        String[] packages = StringUtils.commaDelimitedListToStringArray(componentScanProperty);
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);

        for (String pkg : packages) {
            String trimmed = pkg.trim();
            if (StringUtils.hasText(trimmed)) {
                int count = scanner.scan(trimmed);
                logger.info("Archbase: Component scan registrado automaticamente em '{}' ({} beans encontrados)", trimmed, count);
            }
        }
    }

    private void registerEntityScan(BeanDefinitionRegistry registry) {
        String entitiesProperty = environment.getProperty("archbase.app.jpa.entities");
        if (!StringUtils.hasText(entitiesProperty)) {
            return;
        }

        String[] packages = StringUtils.commaDelimitedListToStringArray(entitiesProperty);
        ClassPathBeanDefinitionScanner entityScanner = new ClassPathBeanDefinitionScanner(registry, false);
        entityScanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.Entity.class));
        entityScanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.MappedSuperclass.class));
        entityScanner.addIncludeFilter(new AnnotationTypeFilter(jakarta.persistence.Embeddable.class));

        for (String pkg : packages) {
            String trimmed = pkg.trim();
            if (StringUtils.hasText(trimmed)) {
                int count = entityScanner.scan(trimmed);
                logger.info("Archbase: Entity scan registrado automaticamente em '{}' ({} entidades encontradas)", trimmed, count);
            }
        }
    }

    private void registerRepositoryScan(BeanDefinitionRegistry registry) {
        String repositoriesProperty = environment.getProperty("archbase.app.jpa.repositories");
        if (!StringUtils.hasText(repositoriesProperty)) {
            return;
        }

        // Repositórios JPA são registrados via ArchbaseDynamicJpaRepositoryConfigurer
        // que usa o mecanismo correto do Spring Data JPA
        String[] packages = StringUtils.commaDelimitedListToStringArray(repositoriesProperty);
        for (String pkg : packages) {
            String trimmed = pkg.trim();
            if (StringUtils.hasText(trimmed)) {
                logger.info("Archbase: Repository package '{}' será registrado via ArchbaseDynamicJpaRepositoryConfigurer", trimmed);
            }
        }
    }
}
