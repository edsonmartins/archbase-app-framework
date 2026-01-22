package br.com.archbase.security.service;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.domain.entity.Group;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.QGroupEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import br.com.archbase.security.adapter.GroupPersistenceAdapter;
import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.usecase.GroupUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GroupService implements GroupUseCase, FindDataWithFilterQuery<String, GroupDto> {

    private final GroupPersistenceAdapter adapter;

    private final EntityManager entityManager;

    @Autowired
    public GroupService(GroupPersistenceAdapter adapter, EntityManager entityManager) {
        this.adapter = adapter;
        this.entityManager = entityManager;
    }

    @Override
    public List<GroupDto> findAllGroups() {
        return adapter.findAllGroups();
    }

    @Override
    public Optional<GroupDto> findGroupById(String id) {
        return adapter.findGroupById(id);
    }

    @Override
    public GroupDto createGroup(GroupDto groupDto) {
        return adapter.createGroup(groupDto);
    }

    @Override
    public Optional<GroupDto> updateGroup(String id, GroupDto groupDto) {
        return adapter.updateGroup(id, groupDto);
    }

    @Override
    public void deleteGroup(String id) {
        adapter.deleteGroup(id);
    }

    @Override
    public void addPermission(String groupId, String actionId) {
        adapter.addPermission(groupId, actionId);
    }

    @Override
    public void removePermission(String permissionId) {
        adapter.removePermission(permissionId);
    }

    @Override
    public GroupDto findById(String s) {
        return adapter.findById(s);
    }

    @Override
    public Page<GroupDto> findAll(int page, int size) {
        return adapter.findAll(page,size);
    }

    @Override
    public Page<GroupDto> findAll(int page, int size, String[] sort) {
        return adapter.findAll(page,size,sort);
    }

    @Override
    public List<GroupDto> findAll(List<String> ids) {
        return adapter.findAll(ids);
    }

    @Override
    public Page<GroupDto> findWithFilter(String filter, int page, int size) {
        return adapter.findWithFilter(filter, page, size);
    }

    @Override
    public Page<GroupDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return adapter.findWithFilter(filter, page, size, sort);
    }

    public List<Group> findByNames(List<String> names) {
        if (names == null) {
            return new ArrayList<>();
        }
        QGroupEntity qGroup = QGroupEntity.groupEntity;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        List<GroupEntity> groups = queryFactory.selectFrom(qGroup)
                .where(qGroup.name.in(names))
                .fetch();
        return groups.stream().map(GroupEntity::toDomain).toList();
    }

}
