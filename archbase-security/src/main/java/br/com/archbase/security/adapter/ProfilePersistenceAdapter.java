package br.com.archbase.security.adapter;

import br.com.archbase.security.adapter.port.ProfilePersistencePort;
import br.com.archbase.security.exception.ArchbaseSecurityException;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.GroupEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ProfileJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.archbase.security.domain.dto.ProfileDto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProfilePersistenceAdapter implements ProfilePersistencePort {

    @Autowired
    private ProfileJpaRepository repository;

    @Autowired
    private ActionJpaRepository actionJpaRepository;

    @Autowired
    private PermissionJpaRepository permissionJpaRepository;

    @Override
    public List<ProfileDto> findAllProfiles() {
        return repository.findAll().stream().map(ProfileEntity::toDto).collect(Collectors.toList());
    }

    @Override
    public Optional<ProfileDto> findProfileById(String id) {
        return repository.findById(id).map(ProfileEntity::toDto);
    }

    @Override
    public ProfileDto createProfile(ProfileDto profileDto) {
        return repository.save(ProfileEntity.fromDomain(profileDto.toDomain())).toDto();
    }

    @Override
    public Optional<ProfileDto> updateProfile(String id, ProfileDto profileDto) {
        return Optional.of(repository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(existingEntity -> {
                    existingEntity.setName(profileDto.getName());
                    existingEntity.setDescription(profileDto.getDescription());
                    existingEntity.setCode(profileDto.getCode());
                    return repository.save(existingEntity).toDto();
                });
    }

    @Override
    public void deleteProfile(String id) {
        repository.deleteById(id);
    }

    @Override
    public void addPermission(String profileId, String actionId) {
        ProfileEntity profile = repository.findById(profileId).orElseThrow(() -> new ArchbaseSecurityException("Perfil %s não encontrado".formatted(profileId)));
        ActionEntity action = actionJpaRepository.findById(actionId).orElseThrow(() -> new ArchbaseSecurityException("Ação %s não encontrada".formatted(actionId)));

        PermissionEntity permission = new PermissionEntity();
        permission.setAction(action);
        permission.setSecurity(profile);
        permissionJpaRepository.save(permission);
    }

    @Override
    public void removePermission(String permissionId) {
        PermissionEntity permission = permissionJpaRepository.findById(permissionId).orElseThrow(() -> new ArchbaseSecurityException("Permissão %s não encontrada.".formatted(permissionId)));
        permissionJpaRepository.delete(permission);
    }
}
