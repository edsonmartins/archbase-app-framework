package br.com.archbase.event.driven.bus.autoscan;

public interface BeanFactory {
    <R> R createBean(Class<R> beanClass);
}
