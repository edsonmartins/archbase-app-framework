package br.com.archbase.starter.core.auto.configuration;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.CommonArchbaseJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Registra dinamicamente repositórios JPA a partir de packages definidos
 * na propriedade archbase.app.jpa.repositories.
 *
 * Usa ClassPathScanning para descobrir interfaces de repositório e registrá-las
 * como JpaRepositoryFactoryBean, garantindo compatibilidade com Spring Boot 3.5.3+.
 */
public class ArchbaseDynamicJpaRepositoryConfigurer implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(ArchbaseDynamicJpaRepositoryConfigurer.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        String repositoriesProperty = environment.getProperty("archbase.app.jpa.repositories");
        if (!StringUtils.hasText(repositoriesProperty)) {
            return;
        }

        String[] packages = StringUtils.commaDelimitedListToStringArray(repositoriesProperty);
        for (String pkg : packages) {
            String trimmed = pkg.trim();
            if (StringUtils.hasText(trimmed)) {
                scanAndRegisterRepositories(trimmed, registry);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void scanAndRegisterRepositories(String basePackage, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Repository.class));
        scanner.setEnvironment(environment);

        Set<org.springframework.beans.factory.config.BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

        for (org.springframework.beans.factory.config.BeanDefinition candidate : candidates) {
            try {
                Class<?> repositoryInterface = ClassUtils.forName(
                        candidate.getBeanClassName(),
                        ArchbaseDynamicJpaRepositoryConfigurer.class.getClassLoader()
                );

                if (!repositoryInterface.isInterface()) {
                    continue;
                }

                String beanName = StringUtils.uncapitalize(ClassUtils.getShortName(repositoryInterface));

                if (registry.containsBeanDefinition(beanName)) {
                    logger.debug("Archbase: Repository '{}' já registrado, ignorando", beanName);
                    continue;
                }

                BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(JpaRepositoryFactoryBean.class);
                builder.addConstructorArgValue(repositoryInterface);
                builder.addPropertyValue("repositoryBaseClass", CommonArchbaseJpaRepository.class);
                builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
                builder.setLazyInit(false);

                registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
                logger.info("Archbase: Repository '{}' registrado automaticamente do package '{}'", beanName, basePackage);

            } catch (ClassNotFoundException e) {
                logger.warn("Archbase: Não foi possível carregar classe '{}': {}", candidate.getBeanClassName(), e.getMessage());
            }
        }
    }
}
