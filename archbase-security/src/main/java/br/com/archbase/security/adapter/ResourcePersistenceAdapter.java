package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.adapter.port.ResourcePersistencePort;
import br.com.archbase.security.domain.dto.*;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.*;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import br.com.archbase.security.repository.ResourceJpaRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ResourcePersistenceAdapter implements ResourcePersistencePort, FindDataWithFilterQuery<String, ResourceDto> {

    private final ResourceJpaRepository repository;
    private final SecurityAdapter securityAdapter;
    private final PermissionJpaRepository permissionRepository;
    private final JPAQueryFactory queryFactory;

    @Autowired
    public ResourcePersistenceAdapter(ResourceJpaRepository repository, SecurityAdapter securityAdapter, PermissionJpaRepository permissionRepository, EntityManager entityManager) {
        this.repository = repository;
        this.securityAdapter = securityAdapter;
        this.permissionRepository = permissionRepository;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

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
    public ResourceDto findResource(String resourceName) {
        QResourceEntity resource = QResourceEntity.resourceEntity;
        ResourceEntity resourceEntity = queryFactory.selectFrom(resource).where(resource.name.eq(resourceName)).fetchOne();
        if (resourceEntity == null) {
            return null;
        }
        return resourceEntity.toDto();
    }

    @Override
    public ResourcePermissionsDto findLoggedUserResourcePermissions(String resourceName) {
        User user = securityAdapter.getLoggedUser();

        QPermissionEntity permissionEntity = QPermissionEntity.permissionEntity;
        QUserGroupEntity userGroupEntity = QUserGroupEntity.userGroupEntity;
        QGroupEntity groupEntity = QGroupEntity.groupEntity;
        QProfileEntity profileEntity = QProfileEntity.profileEntity;


        // Construção das condições individuais
        BooleanExpression resourceCondition = permissionEntity.action.resource.name.eq(resourceName);
        BooleanExpression userCondition = permissionEntity.security.id.eq(user.getId().toString())
                .and(permissionEntity.action.active.isTrue());

        BooleanExpression groupCondition = permissionEntity.security.eq(groupEntity._super)
                .and(userGroupEntity.group.eq(groupEntity))
                .and(userGroupEntity.user.id.eq(user.getId().toString()))
                .and(permissionEntity.action.active.isTrue());

        BooleanExpression securityCondition = userCondition.or(groupCondition);

        if (user.getProfile() != null) {
            BooleanExpression profileCondition = permissionEntity.security.eq(profileEntity._super)
                    .and(profileEntity.id.eq(user.getProfile().getId().toString()))
                    .and(permissionEntity.action.active.isTrue());

            securityCondition = securityCondition.or(profileCondition);
        }

        BooleanExpression predicate = resourceCondition
                .and(securityCondition);

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
    public List<ResoucePermissionsWithTypeDto> findUserResourcesPermissions(String userId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QUserEntity user = QUserEntity.userEntity;
        QUserGroupEntity userGroup = QUserGroupEntity.userGroupEntity;
        QGroupEntity group = QGroupEntity.groupEntity;
        QProfileEntity profile = QProfileEntity.profileEntity;

        List<Tuple> userPermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.USER), permission.id)
                .from(permission)
                .where(permission.security.id.eq(userId))
                .fetch();

        List<Tuple> profilePermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.PROFILE))
                .from(permission)
                .join(permission.security, profile._super)
                .join(user).on(user.profile.eq(profile))
                .where(user.id.eq(userId))
                .fetch();

        List<Tuple> groupPermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.GROUP))
                .from(permission)
                .join(permission.security, group._super)
                .join(userGroup).on(userGroup.group.eq(group))
                .where(userGroup.user.id.eq(userId))
                .fetch();

        List<Tuple> permissionsTuple = new ArrayList<>();
        permissionsTuple.addAll(userPermissions);
        permissionsTuple.addAll(profilePermissions);
        permissionsTuple.addAll(groupPermissions);

        return groupTuplesToResourcePermissions(permissionsTuple, permission);
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findProfileResourcesPermissions(String profileId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QUserEntity user = QUserEntity.userEntity;
        QProfileEntity profile = QProfileEntity.profileEntity;

        List<Tuple> profilePermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.PROFILE), permission.id)
                .from(permission)
                .join(permission.security, profile._super)
                .join(user).on(user.profile.eq(profile))
                .where(profile.id.eq(profileId))
                .fetch();

        return groupTuplesToResourcePermissions(profilePermissions, permission);
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findGroupResourcesPermissions(String groupId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QUserGroupEntity userGroup = QUserGroupEntity.userGroupEntity;
        QGroupEntity group = QGroupEntity.groupEntity;

        List<Tuple> groupPermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.GROUP), permission.id)
                .from(permission)
                .join(permission.security, group._super)
                .join(userGroup).on(userGroup.group.eq(group))
                .where(group.id.eq(groupId))
                .fetch();


        return groupTuplesToResourcePermissions(groupPermissions, permission);
    }

    @Override
    public List<ResoucePermissionsWithTypeDto> findAllResourcesPermissions() {
        QActionEntity action = QActionEntity.actionEntity;

        List<Tuple> permissionsTuple = queryFactory
                .select(action.resource.id, action.resource.description, action.id, action.description)
                .from(action)
                .fetch();

        Map<String, Map<String, List<Tuple>>> groupedByResource = permissionsTuple.stream()
                .collect(Collectors.groupingBy(t -> t.get(action.resource.id) + ":" + t.get(action.resource.description),
                        Collectors.groupingBy(t -> t.get(action.id))));

        return groupedByResource.entrySet().stream()
                .map(entry -> {
                    List<String> resourceIdName = Arrays.stream(entry.getKey().split(":")).toList();
                    List<PermissionWithTypesDto> permissions = entry.getValue().entrySet().stream()
                            .map(actionEntry -> {
                                String actionId = actionEntry.getKey();
                                String actionName = actionEntry.getValue().get(0).get(action.description);
                                return PermissionWithTypesDto.builder()
                                        .actionDescription(actionName)
                                        .actionId(actionId)
                                        .build();
                            })
                            .collect(Collectors.toList());
                    return ResoucePermissionsWithTypeDto.builder()
                            .resourceId(resourceIdName.get(0))
                            .resourceDescription(resourceIdName.get(1))
                            .permissions(permissions)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static List<ResoucePermissionsWithTypeDto> groupTuplesToResourcePermissions(List<Tuple> permissionsTuple, QPermissionEntity permission) {
        Map<String, Map<String, List<Tuple>>> groupedByResource = permissionsTuple.stream()
                .collect(Collectors.groupingBy(t -> t.get(permission.action.resource.id) + ":" + t.get(permission.action.resource.description),
                        Collectors.groupingBy(t -> t.get(permission.action.id))));

        return groupedByResource.entrySet().stream()
                .map(entry -> {
                    List<String> resourceIdDescription = Arrays.stream(entry.getKey().split(":")).toList();
                    List<PermissionWithTypesDto> permissions = entry.getValue().entrySet().stream()
                            .map(actionEntry -> {
                                String actionId = actionEntry.getKey();
                                Set<SecurityType> types = actionEntry.getValue().stream()
                                        .map(t -> t.get(4, SecurityType.class))
                                        .collect(Collectors.toSet());
                                String actionDescription = actionEntry.getValue().get(0).get(permission.action.description);
                                String permissionId = actionEntry.getValue().get(0).get(permission.id);
                                PermissionWithTypesDto permissionWithTypesDto = PermissionWithTypesDto.builder()
                                        .actionDescription(actionDescription)
                                        .actionId(actionId)
                                        .types(types)
                                        .build();

                                if (permissionId != null) {
                                    permissionWithTypesDto.setPermissionId(permissionId);
                                }

                                return permissionWithTypesDto;
                            })
                            .collect(Collectors.toList());
                    return ResoucePermissionsWithTypeDto.builder()
                            .resourceId(resourceIdDescription.get(0))
                            .resourceDescription(resourceIdDescription.get(1))
                            .permissions(permissions)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deletePermission(String id) {
        permissionRepository.deleteById(id);
    }

    @Override
    public PermissionDto grantPermission(PermissionDto permissionDto) {
        return permissionRepository.save(PermissionEntity.fromDomain(permissionDto.toDomain())).toDto();
    }

    @Override
    public PermissionDto findPermission(String securityId, String actionId) {
        QPermissionEntity permission = QPermissionEntity.permissionEntity;

        PermissionEntity permissionEntity = queryFactory.selectFrom(permission)
                .where(permission.action.id.eq(actionId).and(permission.security.id.eq(securityId)))
                .fetchFirst();
        if (permissionEntity == null) {
            return null;
        }
        return permissionEntity.toDto();
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
