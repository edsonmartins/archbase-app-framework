package br.com.archbase.workflow.process.flows;

import br.com.archbase.workflow.process.contracts.Work;
import br.com.archbase.workflow.process.contracts.WorkContext;
import br.com.archbase.workflow.process.contracts.WorkReport;
import br.com.archbase.workflow.process.contracts.WorkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Um fluxo sequencial executa um conjunto de unidades de trabalho em sequência.
 * <p>
 * Se uma unidade de trabalho falhar, as próximas unidades de trabalho no pipeline serão ignoradas.
 *
 * @author edsonmartins
 */
public class SequentialFlow extends AbstractWorkFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequentialFlow.class.getName());

    private final List<Work> workUnits = new ArrayList<>();

    SequentialFlow(String name, List<Work> workUnits) {
        super(name);
        this.workUnits.addAll(workUnits);
    }

    /**
     * {@inheritDoc}
     */
    public WorkReport execute(WorkContext workContext) {
        WorkReport workReport = null;
        for (Work work : workUnits) {
            workReport = work.execute(workContext);
            if (workReport != null && WorkStatus.FAILED.equals(workReport.getStatus())) {
                LOGGER.info("Unidade de trabalho ''{}'' falhou, ignorando unidades de trabalho subsequentes", work.getName());
                break;
            }
        }
        return workReport;
    }

    public static class Builder {

        private Builder() {
            // forçar o uso do método estático aNewSequentialFlow
        }

        public static NameStep aNewSequentialFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends ExecuteStep {
            ExecuteStep named(String name);
        }

        public interface ExecuteStep {
            ThenStep execute(Work initialWork);

            ThenStep execute(List<Work> initialWorkUnits);
        }

        public interface ThenStep {
            ThenStep then(Work nextWork);

            ThenStep then(List<Work> nextWorkUnits);

            SequentialFlow build();
        }

        private static class BuildSteps implements NameStep, ExecuteStep, ThenStep {

            private final List<Work> works;
            private String name;

            BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.works = new ArrayList<>();
            }

            public ExecuteStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public ThenStep execute(Work initialWork) {
                this.works.add(initialWork);
                return this;
            }

            @Override
            public ThenStep execute(List<Work> initialWorkUnits) {
                this.works.addAll(initialWorkUnits);
                return this;
            }

            @Override
            public ThenStep then(Work nextWork) {
                this.works.add(nextWork);
                return this;
            }

            @Override
            public ThenStep then(List<Work> nextWorkUnits) {
                this.works.addAll(nextWorkUnits);
                return this;
            }

            @Override
            public SequentialFlow build() {
                return new SequentialFlow(this.name, this.works);
            }
        }
    }
}
