package br.com.archbase.multitenancy.async;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

/**
 * Executor do {@code @Async} com o decorator de tenant — só no caso em que o Spring cairia no fallback.
 *
 * <p><b>O defeito que isto corrige.</b> Com o {@code archbase-starter} há dois {@code TaskExecutor} no contexto (o
 * {@code archbaseAuditExecutor} da segurança e o {@code applicationTaskExecutor} do Spring Boot) e nenhum se chama
 * {@code taskExecutor}. O Spring então não escolhe nenhum e roda o {@code @Async} num {@link SimpleAsyncTaskExecutor}
 * próprio, sem decorator. O tenant chegava à tarefa só porque o contexto de tenant é herdado pela thread nova; o
 * usuário autenticado não chegava, apesar de {@code archbase.multitenancy.async.propagate-security-context=true}.
 *
 * <p><b>O que não muda.</b> A escolha do executor é a do Spring, passo a passo: um único {@code TaskExecutor}; senão,
 * o bean {@code taskExecutor}. Se ela encontra um, este configurer devolve {@code null} e o Spring segue com ele,
 * exatamente como antes. Só no fallback entra o mesmo {@link SimpleAsyncTaskExecutor} — uma thread por tarefa, sem
 * limite, como antes — agora com o decorator.
 */
public class ArchbaseTenantAwareAsyncConfigurer implements AsyncConfigurer {

    private final ListableBeanFactory beanFactory;
    private final TaskDecorator decorator;

    public ArchbaseTenantAwareAsyncConfigurer(ListableBeanFactory beanFactory, TaskDecorator decorator) {
        this.beanFactory = beanFactory;
        this.decorator = decorator;
    }

    @Override
    public Executor getAsyncExecutor() {
        if (decorator == null || springEncontraExecutor()) {
            return null;
        }
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setTaskDecorator(decorator);
        return executor;
    }

    /** Mesma ordem de {@code AsyncExecutionAspectSupport.getDefaultExecutor}. */
    private boolean springEncontraExecutor() {
        try {
            beanFactory.getBean(TaskExecutor.class);
            return true;
        } catch (NoUniqueBeanDefinitionException e) {
            return temTaskExecutorPorNome();
        } catch (NoSuchBeanDefinitionException e) {
            return temTaskExecutorPorNome();
        }
    }

    private boolean temTaskExecutorPorNome() {
        return beanFactory.containsBean("taskExecutor")
                && beanFactory.isTypeMatch("taskExecutor", Executor.class);
    }
}
