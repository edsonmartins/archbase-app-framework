package br.com.archbase.event.driven.spec.query.contracts;

public interface QueryBus {
    <R> R dispatch(Query<R> query);
}
