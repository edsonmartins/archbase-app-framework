package br.com.archbase.ddd.infraestructure.events;

import br.com.archbase.ddd.domain.contracts.EventHandler;
import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Method;

public class SpringEventHandler implements EventHandler {

    private final Class<?> eventType;
    private final String beanName;
    private final Method method;
    private final BeanFactory beanFactory;

    public SpringEventHandler(Class<?> eventType, String beanName, Method method, BeanFactory beanFactory) {
        this.eventType = eventType;
        this.beanName = beanName;
        this.method = method;
        this.beanFactory = beanFactory;
    }

    public boolean canHandle(Object event) {
        return eventType.isAssignableFrom(event.getClass());
    }

    @Override
    public void handle(Object event) {
        Object bean = beanFactory.getBean(beanName);
        try {
            method.invoke(bean, event);
        } catch (Exception e) {
            throw new ArchbaseEventsException(e);
        }
    }
}