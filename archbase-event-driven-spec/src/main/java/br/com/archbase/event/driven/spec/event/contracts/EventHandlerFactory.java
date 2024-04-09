package br.com.archbase.event.driven.spec.event.contracts;


public interface EventHandlerFactory {
    <R> EventHandler<Event<R>, R> createCommandHandler(String commandName);
}
