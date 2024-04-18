package br.com.archbase.starter.core.auto.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.UUID;

@Configuration
public class ArchbaseThreadConfiguration {

    @Bean
    @ConditionalOnMissingBean(TaskExecutor.class)
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setDaemon(true);
        executor.setThreadNamePrefix("archbase-task-executor");
        executor.initialize();
        return executor;
    }
}