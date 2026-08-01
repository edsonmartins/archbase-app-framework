package br.com.archbase.multitenancy.async;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuração padrão de executor assíncrono com propagação de tenant.
 *
 * <p>Ativada apenas se a aplicação não definir seu próprio {@link AsyncConfigurer}.
 * Para sobrescrever, basta criar uma classe {@code @Configuration} que implemente
 * {@link AsyncConfigurer} no projeto da aplicação.</p>
 */
@Configuration
@EnableAsync
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ConditionalOnMissingBean(AsyncConfigurer.class)
public class AsyncConfig extends AsyncConfigurerSupport {

    /**
     * Leva o usuário autenticado para dentro do {@code @Async}. Sem isto, a auditoria grava
     * {@code createdByUser} vazio nas tarefas assíncronas e checagens de permissão feitas ali negam.
     */
    @Value("${archbase.multitenancy.async.propagate-security-context:true}")
    private boolean propagateSecurityContext;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(7);
        executor.setMaxPoolSize(42);
        executor.setQueueCapacity(11);
        executor.setThreadNamePrefix("TenantAwareTaskExecutor-");
        executor.setTaskDecorator(new ArchbaseTenantAwareTaskDecorator(propagateSecurityContext));
        executor.initialize();

        return executor;
    }

}
