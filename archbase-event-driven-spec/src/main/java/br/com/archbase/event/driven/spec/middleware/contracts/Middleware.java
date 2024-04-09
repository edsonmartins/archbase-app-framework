package br.com.archbase.event.driven.spec.middleware.contracts;


import br.com.archbase.event.driven.spec.message.contracts.Message;

public interface Middleware {
    <R> R handle(Message<R> message, NextMiddlewareFunction<Message<R>, R> next);
}
