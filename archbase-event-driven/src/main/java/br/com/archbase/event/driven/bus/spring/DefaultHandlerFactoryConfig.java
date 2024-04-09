package br.com.archbase.event.driven.bus.spring;


import br.com.archbase.event.driven.spec.command.contracts.CommandHandlerFactory;
import br.com.archbase.event.driven.spec.query.contracts.QueryHandlerFactory;
import br.com.archbase.event.driven.spec.spring.contracts.HandlerFactoryConfig;
import org.springframework.context.ApplicationContext;

public class DefaultHandlerFactoryConfig implements HandlerFactoryConfig {
    private SpringAutoScanHandlerFactory springAutoScanHandlerFactory;

    public DefaultHandlerFactoryConfig(ApplicationContext context) {
        this.springAutoScanHandlerFactory = new SpringAutoScanHandlerFactory(context);
    }

    @Override
    public CommandHandlerFactory getCommandHandlerFactory() {
        return springAutoScanHandlerFactory;
    }

    @Override
    public QueryHandlerFactory getQueryhandlerFactory() {
        return (QueryHandlerFactory) this.getCommandHandlerFactory();
    }
}
