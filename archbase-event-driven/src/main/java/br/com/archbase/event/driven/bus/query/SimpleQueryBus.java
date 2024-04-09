package br.com.archbase.event.driven.bus.query;


import br.com.archbase.event.driven.bus.exceptions.NoHandlerFoundException;
import br.com.archbase.event.driven.bus.message.SimpleMessageBus;
import br.com.archbase.event.driven.spec.message.contracts.Message;
import br.com.archbase.event.driven.spec.message.contracts.MessageBus;
import br.com.archbase.event.driven.spec.message.contracts.MessageHandler;
import br.com.archbase.event.driven.spec.message.contracts.MessageHandlerFactory;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;
import br.com.archbase.event.driven.spec.query.contracts.Query;
import br.com.archbase.event.driven.spec.query.contracts.QueryBus;
import br.com.archbase.event.driven.spec.query.contracts.QueryHandler;
import br.com.archbase.event.driven.spec.query.contracts.QueryHandlerFactory;

import java.util.List;

public final class SimpleQueryBus implements QueryBus {

    private final MessageBus defaultMessageBus;

    public SimpleQueryBus(QueryHandlerFactory queryHandlerFactory, List<Middleware> middlewareList) {
        defaultMessageBus = new SimpleMessageBus(
                new QueryHandlerFactoryToMessageHandlerFactoryAdapter(queryHandlerFactory),
                middlewareList
        );
    }

    @Override
    public <R> R dispatch(Query<R> query) {
        return defaultMessageBus.dispatch(query);
    }

    // region adapter classes
    static class QueryHandlerFactoryToMessageHandlerFactoryAdapter implements MessageHandlerFactory {

        private final QueryHandlerFactory queryHandlerFactory;

        public QueryHandlerFactoryToMessageHandlerFactoryAdapter(
                QueryHandlerFactory queryHandlerFactory) {
            this.queryHandlerFactory = queryHandlerFactory;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> MessageHandler<Message<R>, R> createHandler(String messageName) {
            QueryHandler<Query<R>, R> queryHandler = queryHandlerFactory.createQueryHandler(messageName);
            if (queryHandler == null) {
                throw new NoHandlerFoundException(messageName);
            }

            return new QueryHandlerToMessageHandlerAdapter<>(queryHandler);
        }
    }

    static class QueryHandlerToMessageHandlerAdapter<M extends Message<R>, R>
            implements MessageHandler<M, R> {

        private final QueryHandler<Query<R>, R> queryHandler;

        QueryHandlerToMessageHandlerAdapter(QueryHandler<Query<R>, R> queryHandler) {
            this.queryHandler = queryHandler;
        }

        @Override
        public R handle(M message) {
            return queryHandler.handle(castToQuery(message));
        }

        @SuppressWarnings("unchecked")
        private Query<R> castToQuery(Message<R> message) {
            return (Query<R>) message;
        }
    }
    // endregion
}
