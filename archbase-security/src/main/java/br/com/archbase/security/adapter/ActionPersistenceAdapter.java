package br.com.archbase.security.adapter;
import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.adapter.port.ActionPersistencePort;
import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.persistence.QActionEntity;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ActionPersistenceAdapter implements ActionPersistencePort, FindDataWithFilterQuery<String, ActionDto> {

    private final ActionJpaRepository repository;

    @Autowired
    public ActionPersistenceAdapter(ActionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ActionDto> findAllActions() {
        return repository.findAll().stream().map(ActionEntity::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ActionDto> findAllActionsByResource(String resourceId) {
        BooleanExpression predicate = QActionEntity.actionEntity.resource.id.eq(resourceId);
        List<ActionEntity> actions = (List<ActionEntity>) repository.findAll(predicate);
        return actions.stream().map(ActionEntity::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<ActionDto> findActionById(String id) {
        return repository.findById(id)
                .map(ActionEntity::toDto);
    }

    @Override
    public Optional<ActionDto> findActionByName(String name, String resourceId) {
        BooleanExpression byName = QActionEntity.actionEntity.name.eq(name);
        BooleanExpression byResourceId = QActionEntity.actionEntity.resource.id.eq(resourceId);
        BooleanExpression predicate = byName.and(byResourceId);

        return repository.findOne(predicate)
                .map(ActionEntity::toDto);
    }

    @Override
    public ActionDto createAction(ActionDto actionDto) {
        return repository.save(ActionEntity.fromDomain(actionDto.toDomain())).toDto();
    }

    @Override
    public Optional<ActionDto> updateAction(String id, ActionDto actionDto) {
        return repository.findById(id)
                .map(entity -> {
                    entity.setActionVersion(actionDto.getActionVersion());
                    entity.setActive(actionDto.getActive());
                    entity.setDescription(actionDto.getDescription());
                    entity.setName(actionDto.getName());
                    return repository.save(entity).toDto();
                });
    }

    @Override
    public void deleteAction(String id) {
        repository.deleteById(id);
    }

    @Override
    public ActionDto findById(String id) {
        Optional<ActionEntity> byId = repository.findById(id);
        return byId.map(ActionEntity::toDto).orElse(null);
    }

    @Override
    public Page<ActionDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActionEntity> result = repository.findAll(pageable);
        List<ActionDto> list = result.stream().map(ActionEntity::toDto).toList();
        return new ActionPersistenceAdapter.PageAction(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ActionDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ActionEntity> result = repository.findAll(pageable);
        List<ActionDto> list = result.stream().map(ActionEntity::toDto).toList();
        return new ActionPersistenceAdapter.PageAction(list, pageable, result.getTotalElements());
    }

    @Override
    public List<ActionDto> findAll(List<String> ids) {
        List<ActionEntity> result = repository.findAllById(ids);
        return result.stream().map(ActionEntity::toDto).toList();
    }

    @Override
    public Page<ActionDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ActionEntity> result = repository.findAll(filter, pageable);
        List<ActionDto> list = result.stream().map(ActionEntity::toDto).toList();
        return new ActionPersistenceAdapter.PageAction(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ActionDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ActionEntity> result = repository.findAll(filter, pageable);
        List<ActionDto> list = result.stream().map(ActionEntity::toDto).toList();
        return new ActionPersistenceAdapter.PageAction(list, pageable, result.getTotalElements());
    }

    static class PageAction extends PageImpl<ActionDto> {
        public PageAction(List<ActionDto> content) {
            super(content);
        }

        public PageAction(List<ActionDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListAction extends ArrayList<ActionDto> {
        public ListAction(Collection<? extends ActionDto> c) {
            super(c);
        }
    }
}
