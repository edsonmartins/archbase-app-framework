package br.com.archbase.security.service;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import br.com.archbase.security.adapter.UserProfilePersistenceAdapter;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.usecase.ProfileUseCase;

import java.util.List;
import java.util.Optional;

@Service
public class UserProfileService implements ProfileUseCase, FindDataWithFilterQuery<String, ProfileDto> {

    private final UserProfilePersistenceAdapter adapter;

    @Autowired
    public UserProfileService(UserProfilePersistenceAdapter adapter) {
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


    @Override
    public ProfileDto findById(String s) {
        return adapter.findById(s);
    }

    @Override
    public Page<ProfileDto> findAll(int page, int size) {
        return adapter.findAll(page,size);
    }

    @Override
    public Page<ProfileDto> findAll(int page, int size, String[] sort) {
        return adapter.findAll(page,size,sort);
    }

    @Override
    public List<ProfileDto> findAll(List<String> strings) {
        return adapter.findAll(strings);
    }

    @Override
    public Page<ProfileDto> findWithFilter(String filter, int page, int size) {
        return adapter.findWithFilter(filter,page,size);
    }

    @Override
    public Page<ProfileDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return adapter.findWithFilter(filter,page,size,sort);
    }
}
