package br.com.archbase.security.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * A thread em que a trilha grava.
 *
 * <p>Separada da requisição de propósito: é isso que impede a falha da auditoria de alcançar o
 * fluxo auditado. A fila é limitada e a política de descarte é <b>abandonar o evento mais antigo</b>
 * — numa rajada, perder registro de trilha é aceitável; segurar a thread da requisição não é.
 */
@Configuration
public class ArchbaseAuditExecutorConfiguration {

    @Bean("archbaseAuditExecutor")
    public TaskExecutor archbaseAuditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Um punhado de threads dá conta: gravar uma linha é rápido, e o volume acompanha o de
        // logins, não o de requisições.
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("archbase-audit-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());
        // Sem isto, um shutdown no meio de uma rajada perderia o que já estava aceito na fila.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
