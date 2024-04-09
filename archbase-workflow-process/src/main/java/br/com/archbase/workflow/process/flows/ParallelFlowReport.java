package br.com.archbase.workflow.process.flows;

import br.com.archbase.workflow.process.contracts.WorkContext;
import br.com.archbase.workflow.process.contracts.WorkReport;
import br.com.archbase.workflow.process.contracts.WorkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Relatório agregado dos relatórios parciais das unidades de trabalho executados em fluxo paralelo.
 *
 * @author edsonmartins
 */
public class ParallelFlowReport implements WorkReport {

    private final List<WorkReport> reports;

    /**
     * Cria um novo {@link ParallelFlowReport}.
     */
    public ParallelFlowReport() {
        this(new ArrayList<>());
    }

    /**
     * Cria um novo {@link ParallelFlowReport}.
     *
     * @param reports dos trabalhos executados em paralelo
     */
    public ParallelFlowReport(List<WorkReport> reports) {
        this.reports = reports;
    }

    /**
     * Obtenha relatórios parciais.
     *
     * @return relatórios parciais.
     */
    public List<WorkReport> getReports() {
        return reports;
    }

    void add(WorkReport workReport) {
        reports.add(workReport);
    }

    void addAll(List<WorkReport> workReports) {
        reports.addAll(workReports);
    }

    /**
     * Retorne o status do fluxo paralelo.
     * <p>
     * O status de um fluxo paralelo é definido da seguinte forma:
     *
     * <ul>
     * <li> {@link WorkStatus # COMPLETED}: Se todas as unidades de trabalho foram concluídas com sucesso </li>
     * <li> {@link WorkStatus # FAILED}: Se uma das unidades de trabalho falhou </li>
     * </ul>
     *
     * @return status fluxo de trabalho
     */
    @Override
    public WorkStatus getStatus() {
        for (WorkReport report : reports) {
            if (report.getStatus().equals(WorkStatus.FAILED)) {
                return WorkStatus.FAILED;
            }
        }
        return WorkStatus.COMPLETED;
    }

    /**
     * Retorna o primeiro erro de relatórios parciais.
     *
     * @return o primeiro erro de relatórios parciais.
     */
    @Override
    public Throwable getError() {
        for (WorkReport report : reports) {
            Throwable error = report.getError();
            if (error != null) {
                return error;
            }
        }
        return null;
    }

    /**
     * O contexto de fluxo paralelo é a união de todos os contextos parciais. Em um fluxo
     * paralelo, cada unidade de trabalho deve ter suas próprias chaves exclusivas para evitar a substituição de chave
     * ao mesclar contextos parciais.
     *
     * @return a união de todos os contextos parciais
     */
    @Override
    public WorkContext getWorkContext() {
        WorkContext workContext = new WorkContext();
        for (WorkReport report : reports) {
            WorkContext partialContext = report.getWorkContext();
            for (Map.Entry<String, Object> entry : partialContext.getEntrySet()) {
                workContext.put(entry.getKey(), entry.getValue());
            }
        }
        return workContext;
    }

    @Override
    public ResultData getResultData() {
        return null;
    }
}
