package br.com.archbase.starter.multitenancy.auto.configuration;

import br.com.archbase.multitenancy.async.ArchbaseTenantAwareTaskDecorator;
import br.com.archbase.multitenancy.async.AsyncConfig;
import br.com.archbase.multitenancy.interceptor.ArchbaseTenantRequestInterceptor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
@ConditionalOnClass(CurrentTenantIdentifierResolver.class)
@ConditionalOnProperty(prefix = "archbase.multitenancy", name = "enabled", matchIfMissing = true)
public class ArchbaseMultitenancyAutoConfiguration implements WebMvcConfigurer {

    // ROLE_INFRASTRUCTURE: evita os WARN do BeanPostProcessorChecker no Spring Boot 4 quando
    // estes beans de infra são injetados cedo (ex.: no meterRegistryPostProcessor).
    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TaskDecorator tenantAwareTaskDecorator() {
        return new ArchbaseTenantAwareTaskDecorator();
    }

    @Bean
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AsyncConfig asyncConfig() {
        return new AsyncConfig();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ArchbaseTenantRequestInterceptor());
    }
}
