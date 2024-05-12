package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.adapter.port.UserProfilePersistencePort;
import br.com.archbase.security.exception.ArchbaseSecurityException;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.ProfileJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import br.com.archbase.security.domain.dto.ProfileDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserProfilePersistenceAdapter implements UserProfilePersistencePort, FindDataWithFilterQuery<String, ProfileDto> {

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

    @Override
    public ProfileDto findById(String id) {
        Optional<ProfileEntity> byId = repository.findById(id);
        return byId.map(ProfileEntity::toDto).orElse(null);
    }

    @Override
    public Page<ProfileDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProfileEntity> result = repository.findAll(pageable);
        List<ProfileDto> list = result.stream().map(ProfileEntity::toDto).toList();
        return new UserProfilePersistenceAdapter.PageProfile(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ProfileDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ProfileEntity> result = repository.findAll(pageable);
        List<ProfileDto> list = result.stream().map(ProfileEntity::toDto).toList();
        return new UserProfilePersistenceAdapter.PageProfile(list, pageable, result.getTotalElements());
    }

    @Override
    public List<ProfileDto> findAll(List<String> ids) {
        List<ProfileEntity> result = repository.findAllById(ids);
        return result.stream().map(ProfileEntity::toDto).toList();
    }

    @Override
    public Page<ProfileDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProfileEntity> result = repository.findAll(filter, pageable);
        List<ProfileDto> list = result.stream().map(ProfileEntity::toDto).toList();
        return new UserProfilePersistenceAdapter.PageProfile(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ProfileDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ProfileEntity> result = repository.findAll(filter, pageable);
        List<ProfileDto> list = result.stream().map(ProfileEntity::toDto).toList();
        return new UserProfilePersistenceAdapter.PageProfile(list, pageable, result.getTotalElements());
    }

    static class PageProfile extends PageImpl<ProfileDto> {
        public PageProfile(List<ProfileDto> content) {
            super(content);
        }

        public PageProfile(List<ProfileDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListProfile extends ArrayList<ProfileDto> {
        public ListProfile(Collection<? extends ProfileDto> c) {
            super(c);
        }
    }
}
