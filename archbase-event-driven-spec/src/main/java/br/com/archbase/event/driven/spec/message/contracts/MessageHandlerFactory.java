package br.com.archbase.event.driven.spec.message.contracts;

public interface MessageHandlerFactory {
    <R> MessageHandler<Message<R>, R> createHandler(String messageName);
}
