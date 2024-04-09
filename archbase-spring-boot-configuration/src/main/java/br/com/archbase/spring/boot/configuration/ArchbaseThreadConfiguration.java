package br.com.archbase.spring.boot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.UUID;

@Configuration
public class ArchbaseThreadConfiguration {

    @Bean
    public TaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setDaemon(true);
        executor.setThreadNamePrefix("archbase_task_executor_" + UUID.randomUUID().toString());
        executor.initialize();
        return executor;
    }
}