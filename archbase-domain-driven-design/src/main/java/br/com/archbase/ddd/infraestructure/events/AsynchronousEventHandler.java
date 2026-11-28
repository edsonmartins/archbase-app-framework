package br.com.archbase.ddd.infraestructure.events;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Handler de evento que roda fora da thread de quem publicou.
 *
 * <p>Usa o {@link TaskExecutor} da aplicação, se houver um só (o do Spring Boot, com o {@code TaskDecorator} do
 * Archbase, leva tenant e usuário para a tarefa); senão, um {@link SimpleAsyncTaskExecutor} com o {@code TaskDecorator}
 * do contexto, se houver. Uma falha no handler é registrada pelo {@link SpringEventHandler} e não chega a quem
 * publicou.
 *
 * <p>Até a 3.5.0 esta classe não sobrescrevia nada: um handler com {@code asynchronous = true} rodava na thread de quem
 * publicou, como os demais.
 */
public class AsynchronousEventHandler extends SpringEventHandler {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AsynchronousEventHandler.class);

    private final BeanFactory beanFactory;
    private volatile Executor executor;

    public AsynchronousEventHandler(Class<?> eventType, String beanName,
                                    Method method, BeanFactory beanFactory) {
        super(eventType, beanName, method, beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    public void handle(Object event) {
        executor().execute(() -> {
            try {
                super.handle(event);
            } catch (RuntimeException e) {
                LOG.error("Falha no handler assíncrono de {}", event.getClass().getName(), e);
            }
        });
    }

    private Executor executor() {
        Executor atual = executor;
        if (atual == null) {
            atual = beanFactory.getBeanProvider(TaskExecutor.class).getIfUnique(() -> {
                SimpleAsyncTaskExecutor simples = new SimpleAsyncTaskExecutor("archbase-event-");
                if (beanFactory instanceof ListableBeanFactory) {
                    beanFactory.getBeanProvider(TaskDecorator.class).ifUnique(simples::setTaskDecorator);
                }
                return simples;
            });
            executor = atual;
        }
        return atual;
    }
}
