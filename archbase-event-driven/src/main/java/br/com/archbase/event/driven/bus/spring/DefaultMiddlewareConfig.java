package br.com.archbase.event.driven.bus.spring;


import br.com.archbase.event.driven.bus.middleware.logging.LoggingMiddleware;
import br.com.archbase.event.driven.bus.middleware.metrics.MetricsMiddleware;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;
import br.com.archbase.event.driven.spec.spring.contracts.MiddlewareConfig;

import java.util.ArrayList;
import java.util.List;

public class DefaultMiddlewareConfig implements MiddlewareConfig {
    @Override
    public List<Middleware> getCommandMiddlewarePipeline() {
        List<Middleware> middlewares = new ArrayList<>();
        middlewares.add(new MetricsMiddleware());
        middlewares.add(new LoggingMiddleware());
        return middlewares;
    }

    @Override
    public List<Middleware> getQueryMiddlewarePipeline() {
        List<Middleware> middlewares = new ArrayList<>();
        middlewares.add(new MetricsMiddleware());
        middlewares.add(new LoggingMiddleware());
        return middlewares;
    }

    @Override
    public List<Middleware> getEventMiddlewarePipeline() {
        List<Middleware> middlewares = new ArrayList<>();
        middlewares.add(new MetricsMiddleware());
        middlewares.add(new LoggingMiddleware());
        return middlewares;
    }
}
