package br.com.archbase.event.driven.spec.message.contracts;

public interface MessageBus {
    <R> R dispatch(Message<R> message);
}
