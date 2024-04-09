package br.com.archbase.event.driven.spec.command.contracts;

public interface CommandHandlerFactory {
    <R> CommandHandler<Command<R>, R> createCommandHandler(String commandName);
}
