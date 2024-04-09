package br.com.archbase.event.driven.spec.command.contracts;


import br.com.archbase.event.driven.spec.message.contracts.MessageHandler;

public interface CommandHandler<C extends Command<R>, R> extends MessageHandler<C, R> {
    R handle(C command);
}