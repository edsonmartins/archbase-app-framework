package br.com.archbase.event.driven.spec.spring.contracts;


import br.com.archbase.event.driven.spec.command.contracts.CommandHandlerFactory;
import br.com.archbase.event.driven.spec.query.contracts.QueryHandlerFactory;

public interface HandlerFactoryConfig {
    CommandHandlerFactory getCommandHandlerFactory();

    QueryHandlerFactory getQueryhandlerFactory();
}
