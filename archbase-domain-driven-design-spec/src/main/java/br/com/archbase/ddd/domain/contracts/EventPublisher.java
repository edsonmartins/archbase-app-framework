package br.com.archbase.ddd.domain.contracts;


public interface EventPublisher {

    void publish(Event event);

}
