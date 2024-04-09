package br.com.archbase.event.driven.spec.command.contracts;

public interface CommandBus {
    <R> R dispatch(Command<R> command);
}
