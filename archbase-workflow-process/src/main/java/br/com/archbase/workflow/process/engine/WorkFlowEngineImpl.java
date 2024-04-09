package br.com.archbase.workflow.process.engine;

import br.com.archbase.workflow.process.contracts.WorkContext;
import br.com.archbase.workflow.process.contracts.WorkFlow;
import br.com.archbase.workflow.process.contracts.WorkFlowEngine;
import br.com.archbase.workflow.process.contracts.WorkReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class WorkFlowEngineImpl implements WorkFlowEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkFlowEngineImpl.class);

    public WorkReport run(WorkFlow workFlow, WorkContext workContext) {
        LOGGER.info("Fluxo de trabalho em execução ''{}''", workFlow.getName());
        return workFlow.execute(workContext);
    }

}
