package br.com.archbase.event.driven.spec.middleware.contracts;

public interface NextMiddlewareFunction<T, R> {
    R call(T message);
}
