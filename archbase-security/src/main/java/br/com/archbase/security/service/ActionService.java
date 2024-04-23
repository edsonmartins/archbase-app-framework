package br.com.archbase.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import br.com.archbase.security.adapter.ActionPersistenceAdapter;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.usecase.ActionUseCase;

import java.util.List;
import java.util.Optional;

@Service
public class ActionService implements ActionUseCase {

    private final ActionPersistenceAdapter actionPersistenceAdapter;

    @Autowired
    public ActionService(ActionPersistenceAdapter actionPersistenceAdapter) {
        this.actionPersistenceAdapter = actionPersistenceAdapter;
    }

    @Override
    public List<ActionDto> findAllActions() {
        return actionPersistenceAdapter.findAllActions();
    }

    @Override
    public List<ActionDto> getAllActionsByResource(String resourceId) {
        return actionPersistenceAdapter.findAllActionsByResource(resourceId);
    }

    @Override
    public Optional<ActionDto> findActionById(String id) {
        return actionPersistenceAdapter.findActionById(id);
    }

    @Override
    public Optional<ActionDto> findActionByName(String name, String resourceId) {
        return actionPersistenceAdapter.findActionByName(name, resourceId);
    }

    @Override
    public ActionDto createAction(ActionDto actionDto) {
        return actionPersistenceAdapter.createAction(actionDto);
    }

    @Override
    public Optional<ActionDto> updateAction(String id, ActionDto actionDto) {
        return actionPersistenceAdapter.updateAction(id, actionDto);
    }

    @Override
    public void deleteAction(String id) {
        actionPersistenceAdapter.deleteAction(id);
    }
}
