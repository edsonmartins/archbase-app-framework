package br.com.archbase.event.driven.spec.event.contracts;

public interface EventPublisher {

    public <R> R publish(Event<R> event);
}
