package br.com.archbase.security.adapter;
import br.com.archbase.security.adapter.port.ActionPersistencePort;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.persistence.QActionEntity;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ActionPersistenceAdapter implements ActionPersistencePort {

    private final ActionJpaRepository repository;
    private final JPAQueryFactory queryFactory;

    @Autowired
    public ActionPersistenceAdapter(ActionJpaRepository repository,JPAQueryFactory queryFactory) {
        this.repository = repository;
        this.queryFactory = queryFactory;
    }

    @Override
    public List<ActionDto> findAllActions() {
        return repository.findAll().stream().map(ActionEntity::toDto).collect(Collectors.toList());
    }

    @Override
    public List<ActionDto> findAllActionsByResource(String resourceId) {
        QActionEntity action = QActionEntity.actionEntity;
        return queryFactory.selectFrom(action)
                .where(action.resource.id.eq(resourceId))
                .fetch()
                .stream().map(ActionEntity::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<ActionDto> findActionById(String id) {
        return Optional.ofNullable(queryFactory.selectFrom(QActionEntity.actionEntity)
                        .where(QActionEntity.actionEntity.id.eq(id))
                        .fetchOne())
                .map(ActionEntity::toDto);
    }

    @Override
    public Optional<ActionDto> findActionByName(String name, String resourceId) {
        BooleanExpression byName = QActionEntity.actionEntity.name.eq(name);
        BooleanExpression byResourceId = QActionEntity.actionEntity.resource.id.eq(resourceId);
        return Optional.ofNullable(queryFactory.selectFrom(QActionEntity.actionEntity)
                        .where(byName.and(byResourceId))
                        .fetchOne())
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
}
