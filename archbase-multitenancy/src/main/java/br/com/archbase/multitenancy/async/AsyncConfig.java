package br.com.archbase.multitenancy.async;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuração padrão de executor assíncrono com propagação de tenant.
 *
 * <p>Ativada apenas se a aplicação não definir seu próprio {@link AsyncConfigurer}.
 * Para sobrescrever, basta criar uma classe {@code @Configuration} que implemente
 * {@link AsyncConfigurer} no projeto da aplicação.</p>
 *
 * @deprecated Nunca chegou ao contexto com o {@code archbase-starter}: registrada pela varredura, a condição
 * {@code @ConditionalOnMissingBean(AsyncConfigurer.class)} encontra a própria classe e o Spring a descarta. O
 * {@code @Async} sempre rodou no executor do {@code archbase-starter-core}, que a partir da 3.5.0 aplica o
 * {@code TaskDecorator} de tenant. Sem as anotações de configuração desde a 3.5.0 — o efeito era nenhum, e a definição
 * varrida atrapalhava o {@code ArchbaseTenantAwareAsyncConfigurer}. Mantida só por compatibilidade binária.
 */
@Deprecated(since = "3.5.0", forRemoval = true)
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
