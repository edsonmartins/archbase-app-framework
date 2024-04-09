package br.com.archbase.event.driven.spec.event.contracts;


import br.com.archbase.event.driven.spec.message.contracts.MessageHandler;

public interface EventHandler<E extends Event<R>, R> extends MessageHandler<E, R> {
    R handle(E event);
}