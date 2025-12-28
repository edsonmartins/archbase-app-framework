package br.com.archbase.event.driven.spec.spring.contracts;


import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;

import java.util.Collections;
import java.util.List;

public interface MiddlewareConfig {
    List<Middleware> getCommandMiddlewarePipeline();

    List<Middleware> getQueryMiddlewarePipeline();

    /**
     * Retorna o pipeline de middleware para eventos.
     * Implementação padrão retorna lista vazia.
     *
     * @return Lista de middlewares para eventos
     */
    default List<Middleware> getEventMiddlewarePipeline() {
        return Collections.emptyList();
    }
}
