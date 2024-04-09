package br.com.archbase.event.driven.bus.message;

import br.com.archbase.event.driven.bus.exceptions.NoHandlerFoundException;
import br.com.archbase.event.driven.spec.message.contracts.Message;
import br.com.archbase.event.driven.spec.message.contracts.MessageBus;
import br.com.archbase.event.driven.spec.message.contracts.MessageHandler;
import br.com.archbase.event.driven.spec.message.contracts.MessageHandlerFactory;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;
import br.com.archbase.event.driven.spec.middleware.contracts.NextMiddlewareFunction;

import java.util.ArrayList;
import java.util.List;

public final class SimpleMessageBus implements MessageBus {
    private static final int FIRST_MIDDLEWARE_INDEX = 0;
    private final List<Middleware> middlewarePipeline;
    private MessageHandlerFactory messageHandlerFactory;

    public SimpleMessageBus(MessageHandlerFactory handlerFactory, List<Middleware> middlewareList) {
        messageHandlerFactory = handlerFactory;
        middlewarePipeline = new ArrayList<>(middlewareList);
    }

    /**
     * Encontre um {@link MessageHandler} que possa lidar com esta {@link Message} e despachá-la para o
     * manipulador.
     *
     * @param message a mensagem para despachar
     * @param <R>     o tipo de resultado produzido após o tratamento da mensagem
     * @return o resultado depois de lidar com a mensagem
     * @throws NoHandlerFoundException quando o {@link MessageBus} não consegue encontrar o correspondente
     *                                 {@link MessageHandler} para uma {@link Message}.
     * @throws Exception               possivelmente gerado por OldMiddleware ou MessageHandler
     */
    @Override
    @SuppressWarnings("unchecked")
    public <R> R dispatch(Message<R> message) {
        return (R) getNext(FIRST_MIDDLEWARE_INDEX).call((Message<Object>) message);
    }

    private <R> NextMiddlewareFunction<Message<R>, R> getNext(int nextMiddlewareIndex) {
        if (nextMiddlewareIndex < middlewarePipeline.size()) {
            // Lidar com o próximo middleware
            return message -> {
                Middleware nextMiddleware = middlewarePipeline.get(nextMiddlewareIndex);
                return nextMiddleware.handle(message, getNext(nextMiddlewareIndex + 1));
            };
        } else {
            // Manipular usando manipulador de mensagem
            return message -> {
                MessageHandler<Message<R>, R> handler = messageHandlerFactory.createHandler(message.getClass().getName());
                if (handler == null) {
                    throw new NoHandlerFoundException(message.getClass());
                }
                return handler.handle(message);
            };
        }
    }
}
