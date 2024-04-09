package br.com.archbase.event.driven.spec.event.contracts;

public interface EventSubscriber {

    public <R> R subscribe(Subscriber subscriber);

    public <R> R unsubscribe(Subscriber subscriber);
}
