package br.com.archbase.workflow.process.flows;


import br.com.archbase.workflow.process.contracts.WorkFlow;

/**
 * Fluxo abstrato de trabalho.
 *
 * @author edsonmartins
 */
abstract class AbstractWorkFlow implements WorkFlow {

    private final String name;

    AbstractWorkFlow(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
