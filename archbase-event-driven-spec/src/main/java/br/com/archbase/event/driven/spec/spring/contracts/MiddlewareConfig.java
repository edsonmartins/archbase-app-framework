package br.com.archbase.event.driven.spec.spring.contracts;


import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;

import java.util.List;

public interface MiddlewareConfig {
    List<Middleware> getCommandMiddlewarePipeline();

    List<Middleware> getQueryMiddlewarePipeline();
}
