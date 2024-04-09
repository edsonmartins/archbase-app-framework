package br.com.archbase.workflow.process.contracts;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Uma interface de predicado no relatório de trabalho.
 *
 * @author edsonmartins
 */
@FunctionalInterface
public interface WorkReportPredicate {

    WorkReportPredicate ALWAYS_TRUE = workReport -> true;
    WorkReportPredicate ALWAYS_FALSE = workReport -> false;
    WorkReportPredicate COMPLETED = workReport -> workReport.getStatus().equals(WorkStatus.COMPLETED);
    WorkReportPredicate FAILED = workReport -> workReport.getStatus().equals(WorkStatus.FAILED);

    /**
     * Aplique o predicado no relatório de trabalho fornecido.
     *
     * @param workReport no qual o predicado deve ser aplicado
     * @return true se o predicado se aplica ao relatório fornecido, false caso contrário
     */
    boolean apply(WorkReport workReport);

    /**
     * Um predicado que retorna verdadeiro após um determinado número de vezes.
     *
     * @author edsonmartins
     */
    class TimesPredicate implements WorkReportPredicate {

        private final int times;

        private final AtomicInteger counter = new AtomicInteger();

        public TimesPredicate(int times) {
            this.times = times;
        }

        public static TimesPredicate times(int times) {
            return new TimesPredicate(times);
        }

        @Override
        public boolean apply(WorkReport workReport) {
            return counter.incrementAndGet() != times;
        }
    }


}
