package br.com.archbase.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.archbase.security.adapter.ProfilePersistenceAdapter;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.usecase.ProfileUseCase;

import java.util.List;
import java.util.Optional;

@Service
public class ProfileService implements ProfileUseCase {

    private final ProfilePersistenceAdapter adapter;

    @Autowired
    public ProfileService(ProfilePersistenceAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public List<ProfileDto> findAllProfiles() {
        return adapter.findAllProfiles();
    }

    @Override
    public Optional<ProfileDto> findProfileById(String id) {
        return adapter.findProfileById(id);
    }

    @Override
    public ProfileDto createProfile(ProfileDto profileDto) {
        return adapter.createProfile(profileDto);
    }

    @Override
    public Optional<ProfileDto> updateProfile(String id, ProfileDto profileDto) {
        return adapter.updateProfile(id, profileDto);
    }

    @Override
    public void deleteProfile(String id) {
        adapter.deleteProfile(id);
    }

    @Override
    public void addPermission(String profileId, String actionId) {
        adapter.addPermission(profileId, actionId);
    }

    @Override
    public void removePermission(String permissionId) {
        adapter.removePermission(permissionId);
    }
}
