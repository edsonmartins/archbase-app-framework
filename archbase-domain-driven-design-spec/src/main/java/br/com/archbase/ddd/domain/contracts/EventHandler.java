package br.com.archbase.ddd.domain.contracts;

public interface EventHandler {

    boolean canHandle(Object event);

    void handle(Object event);

}
