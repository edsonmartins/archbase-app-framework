package br.com.archbase.event.driven.spec.query.contracts;


import br.com.archbase.event.driven.spec.message.contracts.MessageHandler;

public interface QueryHandler<Q extends Query<R>, R> extends MessageHandler<Q, R> {
    R handle(Q query);
}

