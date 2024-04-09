package br.com.archbase.workflow.process.contracts;

import br.com.archbase.workflow.process.flows.ResultData;

/**
 * Relatório de execução de uma unidade de trabalho.
 *
 * @author edsonmartins
 */
public interface WorkReport {

    /**
     * Obtenha o status de execução do trabalho.
     *
     * @return status execução
     */
    WorkStatus getStatus();

    /**
     * Obtenha o erro, se houver. Pode ser {@code null}, mas geralmente não é nulo quando
     * o status é {@link WorkStatus#FAILED}. Normalmente, a exceção inclui
     * o código de saída que pode ser usado para conduzir a execução do fluxo de acordo.
     *
     * @return erro
     */
    Throwable getError();

    /**
     * Obtenha o último contexto de trabalho do fluxo
     *
     * @return último contexto de trabalho do fluxo
     */
    WorkContext getWorkContext();

    /**
     * Obtém o resultado produzido pela execução do trabalho
     *
     * @return
     */
    ResultData getResultData();

}
