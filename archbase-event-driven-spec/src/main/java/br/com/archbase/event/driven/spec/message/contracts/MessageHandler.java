package br.com.archbase.event.driven.spec.message.contracts;

public interface MessageHandler<T extends Message<R>, R> {
    R handle(T message);
}

