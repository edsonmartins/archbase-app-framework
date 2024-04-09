package br.com.archbase.ddd.infraestructure.events;

import br.com.archbase.ddd.domain.annotations.DomainEventListener;
import br.com.archbase.ddd.domain.contracts.EventHandler;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Registra métodos de spring beans como manipuladores de eventos no publicador de eventos.
 * (se necessário).
 */
@Component
public class EventListenerBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private BeanFactory beanFactory;
    private SimpleEventPublisher eventPublisher;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        for (Method method : bean.getClass().getMethods()) {
            if (method.isAnnotationPresent(DomainEventListener.class)) {
                DomainEventListener listenerAnnotation = method.getAnnotation(DomainEventListener.class);
                Class<?> eventType = method.getParameterTypes()[0];
                EventHandler handler = null;
                if (listenerAnnotation.asynchronous()) {
                    handler = new AsynchronousEventHandler(eventType, beanName, method, beanFactory);
                } else {
                    handler = new SpringEventHandler(eventType, beanName, method, beanFactory);
                }
                eventPublisher.registerEventHandler(handler);
            }

        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        eventPublisher = beanFactory.getBean(SimpleEventPublisher.class);
    }
}