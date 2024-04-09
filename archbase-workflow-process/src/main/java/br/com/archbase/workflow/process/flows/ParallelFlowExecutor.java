package br.com.archbase.workflow.process.flows;


import br.com.archbase.workflow.process.contracts.Work;
import br.com.archbase.workflow.process.contracts.WorkContext;
import br.com.archbase.workflow.process.contracts.WorkReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * Classe responsável pela execução de trabalhos em pararelo.
 *
 * @author edsonmartins
 */
class ParallelFlowExecutor {

    private final ExecutorService workExecutor;

    ParallelFlowExecutor(ExecutorService workExecutor) {
        this.workExecutor = workExecutor;
    }

    List<WorkReport> executeInParallel(List<Work> workUnits, WorkContext workContext) {
        // preparar tarefas para envio paralelo
        List<Callable<WorkReport>> tasks = new ArrayList<>(workUnits.size());
        workUnits.forEach(work -> tasks.add(() -> work.execute(workContext)));

        // enviar unidades de trabalho e esperar pelos resultados
        List<Future<WorkReport>> futures;
        try {
            futures = this.workExecutor.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new RuntimeException("O fluxo paralelo foi interrompido durante a execução das unidades de trabalho", e);
        }
        Map<Work, Future<WorkReport>> workToReportFuturesMap = new HashMap<>();
        for (int index = 0; index < workUnits.size(); index++) {
            workToReportFuturesMap.put(workUnits.get(index), futures.get(index));
        }

        // reunir relatórios
        List<WorkReport> workReports = new ArrayList<>();
        for (Map.Entry<Work, Future<WorkReport>> entry : workToReportFuturesMap.entrySet()) {
            try {
                workReports.add(entry.getValue().get());
            } catch (InterruptedException e) {
                String message = String.format("O fluxo paralelo foi interrompido enquanto aguardava o resultado da unidade de trabalho '%s'", entry.getKey().getName());
                throw new RuntimeException(message, e);
            } catch (ExecutionException e) {
                String message = String.format("Incapaz de executar a unidade de trabalho '%s'", entry.getKey().getName());
                throw new RuntimeException(message, e);
            }
        }

        return workReports;
    }
}
