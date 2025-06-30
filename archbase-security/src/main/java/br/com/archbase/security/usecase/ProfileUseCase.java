package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.domain.entity.Profile;

import java.util.List;
import java.util.Optional;

public interface ProfileUseCase {
    List<ProfileDto> findAllProfiles();

    Optional<ProfileDto> findProfileById(String id);

    ProfileDto createProfile(ProfileDto profileDto);

    Optional<ProfileDto> updateProfile(String id, ProfileDto profileDto);

    void deleteProfile(String id);

    void addPermission(String actionId, String permissionId);

    void removePermission(String permissionId);

    Optional<Profile> findByName(String name);
}
