package br.com.archbase.ddd.infraestructure.events;

import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Method;

public class AsynchronousEventHandler extends SpringEventHandler {

    /**
     * @param eventType
     * @param beanName
     * @param method
     * @param beanFactory
     */
    public AsynchronousEventHandler(Class<?> eventType, String beanName,
                                    Method method, BeanFactory beanFactory) {
        super(eventType, beanName, method, beanFactory);
    }

}