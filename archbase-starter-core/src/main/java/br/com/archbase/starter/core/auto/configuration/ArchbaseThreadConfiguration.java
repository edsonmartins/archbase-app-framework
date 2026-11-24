package br.com.archbase.starter.core.auto.configuration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ArchbaseThreadConfiguration {

    /**
     * Executor padrão — e, como é o único {@code TaskExecutor}, o que o {@code @Async} usa.
     *
     * <p>Aplica o {@link TaskDecorator} da aplicação, se houver um só. Com o {@code archbase-starter-multitenancy} é o
     * que leva tenant, empresa e usuário autenticado para dentro da tarefa. Até a 3.5.0 este executor ignorava o
     * decorator e o {@code @Async} rodava sem tenant: o {@code AsyncConfigurer} que o multitenancy pretendia registrar
     * nunca entrava no contexto.
     */
    @Bean
    @ConditionalOnMissingBean(TaskExecutor.class)
    public TaskExecutor threadPoolTaskExecutor(ObjectProvider<TaskDecorator> decorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setDaemon(true);
        executor.setThreadNamePrefix("archbase-task-executor");
        decorator.ifUnique(executor::setTaskDecorator);
        executor.initialize();
        return executor;
    }
}
