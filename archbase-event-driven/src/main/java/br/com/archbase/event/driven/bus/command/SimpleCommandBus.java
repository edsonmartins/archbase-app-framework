package br.com.archbase.event.driven.bus.command;


import br.com.archbase.event.driven.bus.exceptions.NoHandlerFoundException;
import br.com.archbase.event.driven.bus.message.SimpleMessageBus;
import br.com.archbase.event.driven.spec.command.contracts.Command;
import br.com.archbase.event.driven.spec.command.contracts.CommandBus;
import br.com.archbase.event.driven.spec.command.contracts.CommandHandler;
import br.com.archbase.event.driven.spec.command.contracts.CommandHandlerFactory;
import br.com.archbase.event.driven.spec.message.contracts.Message;
import br.com.archbase.event.driven.spec.message.contracts.MessageBus;
import br.com.archbase.event.driven.spec.message.contracts.MessageHandler;
import br.com.archbase.event.driven.spec.message.contracts.MessageHandlerFactory;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;

import java.util.List;

public final class SimpleCommandBus implements CommandBus {
    private final MessageBus defaultMessageBus;

    public SimpleCommandBus(CommandHandlerFactory commandHandlerFactory,
                            List<Middleware> middlewareList) {
        this.defaultMessageBus = new SimpleMessageBus(
                new MessageHandlerFactoryAdapter(commandHandlerFactory), middlewareList
        );
    }

    @Override
    public <R> R dispatch(Command<R> command) {
        return defaultMessageBus.dispatch(command);
    }

    static class MessageHandlerFactoryAdapter implements MessageHandlerFactory {

        private final CommandHandlerFactory commandHandlerFactory;

        public MessageHandlerFactoryAdapter(CommandHandlerFactory commandHandlerFactory) {
            this.commandHandlerFactory = commandHandlerFactory;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> MessageHandler<Message<R>, R> createHandler(String messageName) {
            CommandHandler<Command<R>, R> handler = commandHandlerFactory.createCommandHandler(messageName);
            if (handler == null) {
                throw new NoHandlerFoundException(messageName);
            }

            return new MessageHandlerAdapter<>(handler);
        }
    }

    static class MessageHandlerAdapter<M extends Message<R>, R> implements MessageHandler<M, R> {

        private final CommandHandler<Command<R>, R> commandHandler;

        MessageHandlerAdapter(CommandHandler<Command<R>, R> commandHandler) {
            this.commandHandler = commandHandler;
        }

        @Override
        @SuppressWarnings("unchecked")
        public R handle(M message) {
            return commandHandler.handle((Command<R>) message);
        }
    }
}
