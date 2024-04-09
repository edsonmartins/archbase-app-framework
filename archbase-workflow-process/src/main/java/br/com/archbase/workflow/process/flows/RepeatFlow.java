package br.com.archbase.workflow.process.flows;

import br.com.archbase.workflow.process.contracts.Work;
import br.com.archbase.workflow.process.contracts.WorkContext;
import br.com.archbase.workflow.process.contracts.WorkReport;
import br.com.archbase.workflow.process.contracts.WorkReportPredicate;

import java.util.UUID;

/**
 * Um fluxo de repetição executa um trabalho repetidamente até que seu relatório satisfaça um determinado predicado.
 *
 * @author edsonmartins
 */
public class RepeatFlow extends AbstractWorkFlow {

    private final Work work;
    private final WorkReportPredicate predicate;

    RepeatFlow(String name, Work work, WorkReportPredicate predicate) {
        super(name);
        this.work = work;
        this.predicate = predicate;
    }

    /**
     * {@inheritDoc}
     */
    public WorkReport execute(WorkContext workContext) {
        WorkReport workReport;
        do {
            workReport = work.execute(workContext);
        } while (predicate.apply(workReport));
        return workReport;
    }

    public static class Builder {

        private Builder() {
            // forçar o uso do método estático aNewRepeatFlow
        }

        public static NameStep aNewRepeatFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends RepeatStep {
            RepeatStep named(String name);
        }

        public interface RepeatStep {
            UntilStep repeat(Work work);
        }

        public interface UntilStep {
            BuildStep until(WorkReportPredicate predicate);

            BuildStep times(int times);
        }

        public interface BuildStep {
            RepeatFlow build();
        }

        private static class BuildSteps implements NameStep, RepeatStep, UntilStep, BuildStep {

            private String name;
            private Work work;
            private WorkReportPredicate predicate;

            BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.work = new NoOpWork();
                this.predicate = WorkReportPredicate.ALWAYS_FALSE;
            }

            @Override
            public RepeatStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public UntilStep repeat(Work work) {
                this.work = work;
                return this;
            }

            @Override
            public BuildStep until(WorkReportPredicate predicate) {
                this.predicate = predicate;
                return this;
            }

            @Override
            public BuildStep times(int times) {
                until(WorkReportPredicate.TimesPredicate.times(times));
                return this;
            }

            @Override
            public RepeatFlow build() {
                return new RepeatFlow(name, work, predicate);
            }
        }

    }
}
