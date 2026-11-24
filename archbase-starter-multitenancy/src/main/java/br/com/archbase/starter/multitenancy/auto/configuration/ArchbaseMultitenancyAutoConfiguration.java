package br.com.archbase.starter.multitenancy.auto.configuration;

import br.com.archbase.multitenancy.async.ArchbaseTenantAwareAsyncConfigurer;
import br.com.archbase.multitenancy.async.ArchbaseTenantAwareTaskDecorator;
import br.com.archbase.multitenancy.interceptor.ArchbaseTenantRequestInterceptor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// Antes da auto-configuração de tarefas do Spring Boot, para que o archbaseTenantAwareAsyncConfigurer seja avaliado
// primeiro e o do Boot (applicationTaskExecutorAsyncConfigurer) desista. Sem isto, o @Async trocaria de executor —
// do SimpleAsyncTaskExecutor, uma thread por tarefa, para o pool de 8 threads do Boot — e quem usa @Async para
// tarefa longa passaria a ver fila onde antes havia paralelismo.
@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration")
@EnableAsync
// Resolvedor de tenant do Hibernate, TenantProperties e aspecto de empresa. Até a 3.5.0 só entravam pela varredura do
// archbase-starter-core: este starter sozinho punha o tenant no contexto e o Hibernate não tinha quem o lesse. Varrer
// o mesmo pacote que o core varre não duplica nada — o Spring descarta a definição idêntica.
@ComponentScan(basePackages = "br.com.archbase.multitenancy")
@ConditionalOnClass(CurrentTenantIdentifierResolver.class)
@ConditionalOnProperty(prefix = "archbase.multitenancy", name = "enabled", matchIfMissing = true)
public class ArchbaseMultitenancyAutoConfiguration implements WebMvcConfigurer {

    // ROLE_INFRASTRUCTURE: evita os WARN do BeanPostProcessorChecker no Spring Boot 4 quando
    // estes beans de infra são injetados cedo (ex.: no meterRegistryPostProcessor).
    /**
     * Propaga o SecurityContext para o {@code @Async}. Lido aqui e repassado ao decorator porque
     * este bean é construído com {@code new}: um {@code @Value} dentro do decorator não seria
     * processado, e o desligamento configurado não teria efeito nenhum.
     */
    @Value("${archbase.multitenancy.async.propagate-security-context:true}")
    private boolean propagateSecurityContext;

    /** Aceita tenant/company na query string, além do header. */
    @Value("${archbase.app.tenant.accept-query-param:true}")
    private boolean acceptTenantQueryParam;

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TaskDecorator tenantAwareTaskDecorator() {
        return new ArchbaseTenantAwareTaskDecorator(propagateSecurityContext);
    }


    /**
     * {@code @Async} com tenant e usuário autenticado quando o Spring cairia no executor de fallback. Não muda a
     * escolha do executor: ver {@link ArchbaseTenantAwareAsyncConfigurer}.
     */
    @Bean
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AsyncConfigurer archbaseTenantAwareAsyncConfigurer(ListableBeanFactory beanFactory,
                                                              ObjectProvider<TaskDecorator> decorator) {
        return new ArchbaseTenantAwareAsyncConfigurer(beanFactory, decorator.getIfUnique());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ArchbaseTenantRequestInterceptor(acceptTenantQueryParam));
    }
}
