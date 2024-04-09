package br.com.archbase.event.driven.bus.spring;


import br.com.archbase.event.driven.bus.command.SimpleCommandBus;
import br.com.archbase.event.driven.bus.query.SimpleQueryBus;
import br.com.archbase.event.driven.spec.command.contracts.CommandBus;
import br.com.archbase.event.driven.spec.query.contracts.QueryBus;
import br.com.archbase.event.driven.spec.spring.contracts.HandlerFactoryConfig;
import br.com.archbase.event.driven.spec.spring.contracts.MiddlewareConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageBusSpringConfiguration {
    private final Log log = LogFactory.getLog(MessageBusSpringConfiguration.class);

    private ApplicationContext applicationContext;

    @Autowired
    public MessageBusSpringConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public CommandBus getCommandBus() {
        return new SimpleCommandBus(
                findHandlerFactoryConfig().getCommandHandlerFactory(),
                findMiddlewareConfig().getQueryMiddlewarePipeline()
        );
    }

    @Bean
    public QueryBus getQueryBus() {
        return new SimpleQueryBus(
                findHandlerFactoryConfig().getQueryhandlerFactory(),
                findMiddlewareConfig().getQueryMiddlewarePipeline()
        );
    }

    private HandlerFactoryConfig findHandlerFactoryConfig() {
        HandlerFactoryConfig scannedHandlerFactoryConfig = scanHandlerFactoryConfig();
        if (scannedHandlerFactoryConfig != null) {
            return scannedHandlerFactoryConfig;
        }

        log.info("Custom @Configuration-annotated HandlerFactoryConfig não encontrado, usando DefaultHandlerFactoryConfig");
        return new DefaultHandlerFactoryConfig(applicationContext);
    }

    private MiddlewareConfig findMiddlewareConfig() {
        MiddlewareConfig scannedMiddlewareConfig = scanMiddlewareConfig();
        if (scannedMiddlewareConfig != null) {
            return scannedMiddlewareConfig;
        }

        log.info("Custom @Configuration-annotated MiddlewareConfig não encontrado, usando DefaultMiddlewareConfig");
        return new DefaultMiddlewareConfig();
    }

    private HandlerFactoryConfig scanHandlerFactoryConfig() {
        return (HandlerFactoryConfig) applicationContext.getBeansWithAnnotation(Configuration.class).values().stream()
                .filter(c -> c instanceof HandlerFactoryConfig)
                .findFirst()
                .orElse(null);
    }

    private MiddlewareConfig scanMiddlewareConfig() {
        return (MiddlewareConfig) applicationContext.getBeansWithAnnotation(Configuration.class).values().stream()
                .filter(c -> c instanceof MiddlewareConfig)
                .findFirst()
                .orElse(null);
    }
}
