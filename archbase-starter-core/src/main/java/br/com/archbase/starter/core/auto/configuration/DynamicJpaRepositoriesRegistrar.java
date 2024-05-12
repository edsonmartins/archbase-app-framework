package br.com.archbase.starter.core.auto.configuration;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.config.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class DynamicJpaRepositoriesRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        String repositoriesProperty = environment.getProperty("archbase.app.jpa.repositories");
        String[] basePackages = {"br.com.archbase.security.repository"};

        if (repositoriesProperty != null && !repositoriesProperty.isEmpty()) {
            basePackages = repositoriesProperty.split(",");
        }

        for (String basePackage : basePackages) {
            registerRepositoriesFromPackage(basePackage, registry);
        }
    }

    private void registerRepositoriesFromPackage(String basePackage, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Repository.class));
        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);
        for (BeanDefinition bd : beanDefinitions) {
            try {
                Class<?> clazz = ClassUtils.forName(bd.getBeanClassName(), DynamicJpaRepositoriesRegistrar.class.getClassLoader());

                if (JpaRepository.class.isAssignableFrom(clazz)) {
                    registerJpaRepository(registry, clazz);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to load class for bean definition.", e);
            }
        }
    }

    private void registerJpaRepository(BeanDefinitionRegistry registry, Class<?> repositoryInterface) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(JpaRepositoryFactoryBean.class);
        beanDefinition.getPropertyValues().add("repositoryInterface", repositoryInterface);
        beanDefinition.getPropertyValues().add("entityManager", "entityManager");

        // The bean name could be derived from the repository interface name or configured explicitly
        String beanName = ClassUtils.getShortName(repositoryInterface);
        registry.registerBeanDefinition(beanName, beanDefinition);
    }
}