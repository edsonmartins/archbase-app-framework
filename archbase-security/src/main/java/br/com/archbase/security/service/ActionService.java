package br.com.archbase.security.service;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import br.com.archbase.security.adapter.ActionPersistenceAdapter;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.usecase.ActionUseCase;

import java.util.List;
import java.util.Optional;

@Service
public class ActionService implements ActionUseCase, FindDataWithFilterQuery<String, ActionDto> {

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

    @Override
    public ActionDto findById(String s) {
        return null;
    }

    @Override
    public Page<ActionDto> findAll(int page, int size) {
        return actionPersistenceAdapter.findAll(page, size);
    }

    @Override
    public Page<ActionDto> findAll(int page, int size, String[] sort) {
        return actionPersistenceAdapter.findAll(page,size,sort);
    }

    @Override
    public List<ActionDto> findAll(List<String> strings) {
        return actionPersistenceAdapter.findAll(strings);
    }

    @Override
    public Page<ActionDto> findWithFilter(String filter, int page, int size) {
        return actionPersistenceAdapter.findWithFilter(filter,page,size);
    }

    @Override
    public Page<ActionDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return actionPersistenceAdapter.findWithFilter(filter,page,size,sort);
    }
}
