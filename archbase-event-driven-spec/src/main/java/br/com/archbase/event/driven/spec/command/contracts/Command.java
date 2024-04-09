package br.com.archbase.event.driven.spec.command.contracts;


import br.com.archbase.event.driven.spec.message.contracts.Message;

public interface Command<R> extends Message<R> {
}
