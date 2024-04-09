package br.com.archbase.workflow.process.engine;


import br.com.archbase.workflow.process.contracts.WorkContext;
import br.com.archbase.workflow.process.contracts.WorkReport;
import br.com.archbase.workflow.process.contracts.WorkStatus;
import br.com.archbase.workflow.process.flows.ResultData;

/**
 * Implementação padrão para {@link WorkReport}.
 *
 * @author edsonmartins
 */

public class SimpleWorkReport implements WorkReport {

    private final WorkStatus status;
    private final WorkContext workContext;
    private Throwable error;
    private ResultData resultData;

    /**
     * Cria um novo {@link SimpleWorkReport}.
     *
     * @param status      do trabalho
     * @param workContext objecto contendo informações do contexto de trabalho
     */
    private SimpleWorkReport(WorkStatus status, WorkContext workContext, ResultData resultData) {
        this.status = status;
        this.workContext = workContext;
        this.resultData = resultData;
    }

    /**
     * Cria um novo {@link SimpleWorkReport}.
     *
     * @param status do trabalho
     * @param error  se houve algum no trabalho
     */
    private SimpleWorkReport(WorkStatus status, WorkContext workContext, Throwable error, ResultData resultData) {
        this(status, workContext, resultData);
        this.error = error;
    }

    public static WorkReport of(WorkStatus status, WorkContext workContext, ResultData resultData) {
        return new SimpleWorkReport(status, workContext, resultData);
    }

    public static WorkReport of(WorkStatus status, WorkContext workContext, Throwable error, ResultData resultData) {
        return new SimpleWorkReport(status, workContext, error, resultData);
    }

    public WorkStatus getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public WorkContext getWorkContext() {
        return workContext;
    }

    @Override
    public ResultData getResultData() {
        return resultData;
    }

    @Override
    public String toString() {
        return "SimpleWorkReport{" +
                "status=" + status +
                ", workContext=" + workContext +
                ", error=" + error +
                ", resultData=" + resultData +
                '}';
    }
}
