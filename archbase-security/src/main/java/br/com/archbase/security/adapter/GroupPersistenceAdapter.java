package br.com.archbase.security.adapter;

import br.com.archbase.error.handling.ArchbaseRuntimeException;
import br.com.archbase.security.adapter.port.GroupPersistencePort;
import br.com.archbase.security.exception.ArchbaseSecurityException;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.GroupJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.archbase.security.domain.dto.GroupDto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class GroupPersistenceAdapter implements GroupPersistencePort {

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
}

