package br.com.archbase.workflow.process.contracts;

/**
 * Interface para um mecanismo de fluxo de trabalho.
 *
 * @author edsonmartins
 */
public interface WorkFlowEngine {

    /**
     * Execute o fluxo de trabalho fornecido e retorne seu relatório.
     *
     * @param workFlow    para executar
     * @param workContext contexto no qual o fluxo de trabalho será executado
     * @return relatório de fluxo de trabalho
     */
    WorkReport run(WorkFlow workFlow, WorkContext workContext);

}
