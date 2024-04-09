package br.com.archbase.workflow.process.flows;

import br.com.archbase.workflow.process.contracts.Work;
import br.com.archbase.workflow.process.contracts.WorkContext;
import br.com.archbase.workflow.process.contracts.WorkReport;
import br.com.archbase.workflow.process.contracts.WorkStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Um fluxo paralelo executa um conjunto de unidades de trabalho em paralelo. Um {@link ParallelFlow}
 * requer um {@link ExecutorService} para executar unidades de trabalho em paralelo usando vários
 * tópicos.
 *
 * <strong> É responsabilidade do chamador gerenciar o ciclo de vida do
 * serviço de executor. </strong>
 * <p>
 * O status de uma execução de fluxo paralelo é definido como:
 *
 * <ul>
 * <li> {@link WorkStatus # COMPLETED}: Se todas as unidades de trabalho foram concluídas com sucesso </li>
 * <li> {@link WorkStatus # FAILED}: Se uma das unidades de trabalho falhou </li>
 * </ul>
 *
 * @author edsonmartins
 */
public class ParallelFlow extends AbstractWorkFlow {

    private final List<Work> workUnits = new ArrayList<>();
    private final ParallelFlowExecutor workExecutor;

    ParallelFlow(String name, List<Work> workUnits, ParallelFlowExecutor parallelFlowExecutor) {
        super(name);
        this.workUnits.addAll(workUnits);
        this.workExecutor = parallelFlowExecutor;
    }

    /**
     * {@inheritDoc}
     */
    public ParallelFlowReport execute(WorkContext workContext) {
        ParallelFlowReport workFlowReport = new ParallelFlowReport();
        List<WorkReport> workReports = workExecutor.executeInParallel(workUnits, workContext);
        workFlowReport.addAll(workReports);
        return workFlowReport;
    }

    public static class Builder {

        private Builder() {
            // forçar o uso do método aNewParallelFlow
        }

        public static NameStep aNewParallelFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends ExecuteStep {
            ExecuteStep named(String name);
        }

        public interface ExecuteStep {
            WithStep execute(Work... workUnits);
        }

        public interface WithStep {
            /**
             * Um {@link ParallelFlow} requer um {@link ExecutorService} para
             * executar unidades de trabalho em paralelo usando vários threads.
             *
             * <strong> É responsabilidade do chamador gerenciar o ciclo de vida
             * do serviço do executor. </strong>
             *
             * @param executorService a ser usado para executar unidades de trabalho em paralelo
             * @return the builder instance
             */
            BuildStep with(ExecutorService executorService);
        }

        public interface BuildStep {
            ParallelFlow build();
        }

        private static class BuildSteps implements NameStep, ExecuteStep, WithStep, BuildStep {

            private final List<Work> works;
            private String name;
            private ExecutorService executorService;

            public BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.works = new ArrayList<>();
            }

            @Override
            public ExecuteStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public WithStep execute(Work... workUnits) {
                this.works.addAll(Arrays.asList(workUnits));
                return this;
            }

            @Override
            public BuildStep with(ExecutorService executorService) {
                this.executorService = executorService;
                return this;
            }

            @Override
            public ParallelFlow build() {
                return new ParallelFlow(
                        this.name, this.works,
                        new ParallelFlowExecutor(this.executorService));
            }
        }

    }
}
