package br.com.archbase.event.driven.spec.event.contracts;

public interface Subscriber {

    <R> void onEvent(Event<R> event);

}
