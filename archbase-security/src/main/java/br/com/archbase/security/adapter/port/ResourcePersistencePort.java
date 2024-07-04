package br.com.archbase.security.adapter.port;

import br.com.archbase.security.domain.dto.PermissionDto;
import br.com.archbase.security.domain.dto.ResoucePermissionsWithTypeDto;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ResourcePersistencePort {

    public List<ResourceDto> findAllResources();
    public Optional<ResourceDto> findResourceById(String id);
    public ResourceDto createResource(ResourceDto resourceDto);
    public Optional<ResourceDto> updateResource(String id, ResourceDto resourceDto);
    public void deleteResource(String id) ;
    public ResourcePermissionsDto findLoggedUserResourcePermissions(String resourceName);
    public ResourceDto findResource(String resourceName);
    public List<ResoucePermissionsWithTypeDto> findUserResourcesPermissions(String userId);
    public List<ResoucePermissionsWithTypeDto> findProfileResourcesPermissions(String profileId);
    public List<ResoucePermissionsWithTypeDto> findGroupResourcesPermissions(String groupId);
    public List<ResoucePermissionsWithTypeDto> findAllResourcesPermissions();
    public void deletePermission(String id);
    public PermissionDto grantPermission(PermissionDto permissionDto);
    public PermissionDto findPermission(String securityId, String actionId);
}
