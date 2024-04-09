package br.com.archbase.event.driven.bus.spring;


import br.com.archbase.event.driven.bus.middleware.logging.LoggingMiddleware;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;
import br.com.archbase.event.driven.spec.spring.contracts.MiddlewareConfig;

import java.util.Collections;
import java.util.List;

public class DefaultMiddlewareConfig implements MiddlewareConfig {
    @Override
    public List<Middleware> getCommandMiddlewarePipeline() {
        return Collections.singletonList(
                new LoggingMiddleware()
        );
    }

    @Override
    public List<Middleware> getQueryMiddlewarePipeline() {
        return Collections.singletonList(
                new LoggingMiddleware()
        );
    }
}
