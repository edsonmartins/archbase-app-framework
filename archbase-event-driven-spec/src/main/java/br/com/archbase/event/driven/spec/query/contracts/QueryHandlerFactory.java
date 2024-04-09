package br.com.archbase.event.driven.spec.query.contracts;

public interface QueryHandlerFactory {
    <R> QueryHandler<Query<R>, R> createQueryHandler(String queryName);
}
