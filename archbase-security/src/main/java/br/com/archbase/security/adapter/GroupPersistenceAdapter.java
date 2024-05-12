package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.adapter.port.GroupPersistencePort;
import br.com.archbase.security.exception.ArchbaseSecurityException;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.GroupJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import br.com.archbase.security.domain.dto.GroupDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GroupPersistenceAdapter implements GroupPersistencePort, FindDataWithFilterQuery<String, GroupDto> {

    @Autowired
    private GroupJpaRepository repository;

    @Autowired
    private ActionJpaRepository actionJpaRepository;

    @Autowired
    private PermissionJpaRepository permissionJpaRepository;

    @Override
    public List<GroupDto> findAllGroups() {
        return repository.findAll().stream().map(GroupEntity::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<GroupDto> findGroupById(String id) {
        return repository.findById(id).map(GroupEntity::toDto);
    }

    @Override
    public GroupDto createGroup(GroupDto groupDto) {
        return repository.save(GroupEntity.fromDomain(groupDto.toDomain())).toDto();
    }

    @Override
    public Optional<GroupDto> updateGroup(String id, GroupDto groupDto) {
        return Optional.of(repository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(existingEntity -> {
                    existingEntity.setName(groupDto.getName());
                    existingEntity.setDescription(groupDto.getDescription());
                    existingEntity.setCode(groupDto.getCode());
                    return repository.save(existingEntity).toDto();
                });
    }

    @Override
    public void deleteGroup(String id) {
        repository.deleteById(id);
    }
    @Override
    public void addPermission(String groupId, String actionId) {
        GroupEntity group = repository.findById(groupId).orElseThrow(() -> new ArchbaseSecurityException("Grupo %s não encontrado".formatted(groupId)));
        ActionEntity action = actionJpaRepository.findById(actionId).orElseThrow(() -> new ArchbaseSecurityException("Ação %s não encontrada".formatted(actionId)));

        PermissionEntity permission = new PermissionEntity();
        permission.setAction(action);
        permission.setSecurity(group);
        permissionJpaRepository.save(permission);
    }

    @Override
    public void removePermission(String permissionId) {
        PermissionEntity permission = permissionJpaRepository.findById(permissionId).orElseThrow(() -> new ArchbaseSecurityException("Permissão %s não encontrada.".formatted(permissionId)));
        permissionJpaRepository.delete(permission);
    }

    @Override
    public GroupDto findById(String id) {
        Optional<GroupEntity> byId = repository.findById(id);
        return byId.map(GroupEntity::toDto).orElse(null);
    }

    @Override
    public Page<GroupDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupEntity> result = repository.findAll(pageable);
        List<GroupDto> list = result.stream().map(GroupEntity::toDto).toList();
        return new GroupPersistenceAdapter.PageGroup(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<GroupDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<GroupEntity> result = repository.findAll(pageable);
        List<GroupDto> list = result.stream().map(GroupEntity::toDto).toList();
        return new GroupPersistenceAdapter.PageGroup(list, pageable, result.getTotalElements());
    }

    @Override
    public List<GroupDto> findAll(List<String> ids) {
        List<GroupEntity> result = repository.findAllById(ids);
        return result.stream().map(GroupEntity::toDto).toList();
    }

    @Override
    public Page<GroupDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupEntity> result = repository.findAll(filter, pageable);
        List<GroupDto> list = result.stream().map(GroupEntity::toDto).toList();
        return new GroupPersistenceAdapter.PageGroup(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<GroupDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<GroupEntity> result = repository.findAll(filter, pageable);
        List<GroupDto> list = result.stream().map(GroupEntity::toDto).toList();
        return new GroupPersistenceAdapter.PageGroup(list, pageable, result.getTotalElements());
    }

    static class PageGroup extends PageImpl<GroupDto> {
        public PageGroup(List<GroupDto> content) {
            super(content);
        }

        public PageGroup(List<GroupDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListGroup extends ArrayList<GroupDto> {
        public ListGroup(Collection<? extends GroupDto> c) {
            super(c);
        }
    }
}

