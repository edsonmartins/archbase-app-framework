package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.adapter.port.ResourcePersistencePort;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.*;
import br.com.archbase.security.repository.ResourceJpaRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import br.com.archbase.security.domain.dto.ResourceDto;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResourcePersistenceAdapter implements ResourcePersistencePort, FindDataWithFilterQuery<String, ResourceDto> {

    private final ResourceJpaRepository repository;
    private final SecurityAdapter securityAdapter;

    private final EntityManager entityManager;

    @Override
    public List<ResourceDto> findAllResources() {
        return repository.findAll().stream().map(ResourceEntity::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<ResourceDto> findResourceById(String id) {
        return repository.findById(id).map(ResourceEntity::toDto);
    }

    @Override
    public ResourceDto createResource(ResourceDto resourceDto) {
        return repository.save(ResourceEntity.fromDomain(resourceDto.toDomain())).toDto();
    }

    @Override
    public Optional<ResourceDto> updateResource(String id, ResourceDto resourceDto) {
        return Optional.of(repository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(existingEntity -> {
                    existingEntity.setDescription(resourceDto.getDescription());
                    existingEntity.setActive(resourceDto.getActive());
                    existingEntity.setName(resourceDto.getName());
                    return repository.save(existingEntity).toDto();
                });
    }

    @Override
    public void deleteResource(String id) {
        repository.deleteById(id);
    }

    @Override
    public ResourcePermissionsDto findLoggedUserResourcePermissions(String resourceName) {
        User user = securityAdapter.getLoggedUser();

        QPermissionEntity permissionEntity = QPermissionEntity.permissionEntity;
        QUserGroupEntity userGroupEntity = QUserGroupEntity.userGroupEntity;
        QGroupEntity groupEntity = QGroupEntity.groupEntity;
        QProfileEntity profileEntity = QProfileEntity.profileEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        // Construção das condições individuais
        BooleanExpression resourceCondition = permissionEntity.action.resource.name.eq(resourceName);
        BooleanExpression userCondition = permissionEntity.security.id.eq(user.getId().toString());
        BooleanExpression groupCondition = permissionEntity.security.eq(groupEntity._super)
                .and(userGroupEntity.group.eq(groupEntity))
                .and(userGroupEntity.user.id.eq(user.getId().toString()));
        BooleanExpression profileCondition = permissionEntity.security.eq(profileEntity._super)
                .and(profileEntity.id.eq(user.getProfile().getId().toString()));

        // Combinação das condições com AND e OR
        BooleanExpression predicate = resourceCondition.and(userCondition.or(groupCondition).or(profileCondition));

        // Execução da consulta com junções apropriadas
        List<PermissionEntity> permissionEntities = queryFactory
                .selectFrom(permissionEntity)
                .leftJoin(permissionEntity.security, groupEntity._super)
                .leftJoin(userGroupEntity).on(userGroupEntity.group.eq(groupEntity))
                .leftJoin(permissionEntity.security, profileEntity._super)
                .where(predicate)
                .fetch();

        return ResourcePermissionsDto.builder()
                .resourceName(resourceName)
                .permissions(permissionEntities.stream()
                        .map(permission -> permission.getAction().getName())
                        .collect(Collectors.toSet()))
                .build();
    }


    @Override
    public ResourceDto findById(String id) {
        Optional<ResourceEntity> byId = repository.findById(id);
        return byId.map(ResourceEntity::toDto).orElse(null);
    }

    @Override
    public Page<ResourceDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ResourceEntity> result = repository.findAll(pageable);
        List<ResourceDto> list = result.stream().map(ResourceEntity::toDto).toList();
        return new ResourcePersistenceAdapter.PageResource(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ResourceDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ResourceEntity> result = repository.findAll(pageable);
        List<ResourceDto> list = result.stream().map(ResourceEntity::toDto).toList();
        return new ResourcePersistenceAdapter.PageResource(list, pageable, result.getTotalElements());
    }

    @Override
    public List<ResourceDto> findAll(List<String> ids) {
        List<ResourceEntity> result = repository.findAllById(ids);
        return result.stream().map(ResourceEntity::toDto).toList();
    }

    @Override
    public Page<ResourceDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ResourceEntity> result = repository.findAll(filter, pageable);
        List<ResourceDto> list = result.stream().map(ResourceEntity::toDto).toList();
        return new ResourcePersistenceAdapter.PageResource(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ResourceDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ResourceEntity> result = repository.findAll(filter, pageable);
        List<ResourceDto> list = result.stream().map(ResourceEntity::toDto).toList();
        return new ResourcePersistenceAdapter.PageResource(list, pageable, result.getTotalElements());
    }

    static class PageResource extends PageImpl<ResourceDto> {
        public PageResource(List<ResourceDto> content) {
            super(content);
        }

        public PageResource(List<ResourceDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListResource extends ArrayList<ResourceDto> {
        public ListResource(Collection<? extends ResourceDto> c) {
            super(c);
        }
    }
}
