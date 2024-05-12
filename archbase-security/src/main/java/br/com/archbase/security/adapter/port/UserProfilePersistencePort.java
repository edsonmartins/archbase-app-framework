package br.com.archbase.security.adapter.port;

import br.com.archbase.security.domain.dto.ProfileDto;

import java.util.List;
import java.util.Optional;

public interface UserProfilePersistencePort {
    public List<ProfileDto> findAllProfiles();
    public Optional<ProfileDto> findProfileById(String id);
    public ProfileDto createProfile(ProfileDto profileDto);
    public Optional<ProfileDto> updateProfile(String id, ProfileDto profileDto) ;
    public void deleteProfile(String id);
    public void addPermission(String profileId, String actionId);
    public void removePermission(String permissionId);
}
