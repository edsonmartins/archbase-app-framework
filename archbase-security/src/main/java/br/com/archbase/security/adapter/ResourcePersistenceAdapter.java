package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.adapter.port.ResourcePersistencePort;
import br.com.archbase.security.domain.dto.PermissionWithTypesDto;
import br.com.archbase.security.domain.dto.ResoucePermissionsWithTypeDto;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.domain.dto.SecurityType;
import br.com.archbase.security.persistence.*;
import br.com.archbase.security.repository.ResourceJpaRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.sql.SQLQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResourcePersistenceAdapter implements ResourcePersistencePort, FindDataWithFilterQuery<String, ResourceDto> {

    private final ResourceJpaRepository repository;
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

    public List<ResoucePermissionsWithTypeDto> findUserResourcesPermissions(String userId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QUserEntity user = QUserEntity.userEntity;
        QUserGroupEntity userGroup = QUserGroupEntity.userGroupEntity;
        QGroupEntity group = QGroupEntity.groupEntity;
        QProfileEntity profile = QProfileEntity.profileEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        List<Tuple> userPermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.USER))
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

    public List<ResoucePermissionsWithTypeDto> findProfileResourcesPermissions(String profileId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QUserEntity user = QUserEntity.userEntity;
        QProfileEntity profile = QProfileEntity.profileEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        List<Tuple> profilePermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.PROFILE))
                .from(permission)
                .join(permission.security, profile._super)
                .join(user).on(user.profile.eq(profile))
                .where(profile.id.eq(profileId))
                .fetch();

        return groupTuplesToResourcePermissions(profilePermissions, permission);
    }

    public List<ResoucePermissionsWithTypeDto> findGroupResourcesPermissions(String groupId) {

        QPermissionEntity permission = QPermissionEntity.permissionEntity;
        QUserGroupEntity userGroup = QUserGroupEntity.userGroupEntity;
        QGroupEntity group = QGroupEntity.groupEntity;

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        List<Tuple> groupPermissions = queryFactory
                .select(permission.action.resource.id, permission.action.resource.description, permission.action.id, permission.action.description, Expressions.constant(SecurityType.GROUP))
                .from(permission)
                .join(permission.security, group._super)
                .join(userGroup).on(userGroup.group.eq(group))
                .where(group.id.eq(groupId))
                .fetch();


        return groupTuplesToResourcePermissions(groupPermissions, permission);
    }

    private static List<ResoucePermissionsWithTypeDto> groupTuplesToResourcePermissions(List<Tuple> permissionsTuple, QPermissionEntity permission) {
        Map<String, Map<String, List<Tuple>>> groupedByResource = permissionsTuple.stream()
                .collect(Collectors.groupingBy(t -> t.get(permission.action.resource.id) + ":" + t.get(permission.action.resource.description),
                        Collectors.groupingBy(t -> t.get(permission.action.id))));

        return groupedByResource.entrySet().stream()
                .map(entry -> {
                    List<String> resourceIdName = Arrays.stream(entry.getKey().split(":")).toList();
                    List<PermissionWithTypesDto> permissions = entry.getValue().entrySet().stream()
                            .map(actionEntry -> {
                                String actionId = actionEntry.getKey();
                                Set<SecurityType> types = actionEntry.getValue().stream()
                                        .map(t -> t.get(4, SecurityType.class))
                                        .collect(Collectors.toSet());
                                String actionName = actionEntry.getValue().get(0).get(permission.action.description);
                                return PermissionWithTypesDto.builder()
                                        .actionName(actionName)
                                        .actionId(actionId)
                                        .types(types)
                                        .build();
                            })
                            .collect(Collectors.toList());
                    return ResoucePermissionsWithTypeDto.builder()
                            .resourceId(resourceIdName.get(0))
                            .resourceName(resourceIdName.get(1))
                            .permissions(permissions)
                            .build();
                })
                .collect(Collectors.toList());
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
