package br.com.archbase.workflow.process.engine;

import br.com.archbase.workflow.process.contracts.WorkFlowEngine;

/**
 * Ponto de entrada principal para criar instâncias {@link WorkFlowEngine}.
 *
 * @author edsonmartins
 */
public class WorkFlowEngineBuilder {

    private WorkFlowEngineBuilder() {
    }

    /**
     * Cria um novo {@link WorkFlowEngineBuilder}.
     *
     * @return um novo {@link WorkFlowEngineBuilder}.
     */
    public static WorkFlowEngineBuilder aNewWorkFlowEngine() {
        return new WorkFlowEngineBuilder();
    }

    /**
     * Cria um novo {@link WorkFlowEngine}.
     *
     * @return um novo {@link WorkFlowEngine}.
     */
    public WorkFlowEngine build() {
        return new WorkFlowEngineImpl();
    }
}
