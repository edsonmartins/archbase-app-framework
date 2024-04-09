package br.com.archbase.workflow.process.flows;

import br.com.archbase.workflow.process.contracts.Work;
import br.com.archbase.workflow.process.contracts.WorkContext;
import br.com.archbase.workflow.process.contracts.WorkReport;
import br.com.archbase.workflow.process.contracts.WorkStatus;
import br.com.archbase.workflow.process.engine.SimpleWorkReport;

import java.util.UUID;

/**
 * Sem operação no trabalho.
 *
 * @author edsonmartins
 */
public class NoOpWork implements Work {

    @Override
    public String getName() {
        return UUID.randomUUID().toString();
    }

    @Override
    public WorkReport execute(WorkContext workContext) {
        return SimpleWorkReport.of(WorkStatus.COMPLETED, workContext, null);
    }
}
