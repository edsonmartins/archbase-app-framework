package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.dto.ProfileDto;

import java.util.List;
import java.util.Optional;

public interface ProfileUseCase {
    public List<ProfileDto> findAllProfiles();

    public Optional<ProfileDto> findProfileById(String id);

    public ProfileDto createProfile(ProfileDto profileDto);

    public Optional<ProfileDto> updateProfile(String id, ProfileDto profileDto);

    public void deleteProfile(String id);

    public void addPermission(String actionId, String permissionId);

    public void removePermission(String permissionId);
}
