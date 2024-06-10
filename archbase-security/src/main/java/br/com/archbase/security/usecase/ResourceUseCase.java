package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;

import java.util.List;
import java.util.Optional;

public interface ResourceUseCase {
    public List<ResourceDto> findAllResources();

    public Optional<ResourceDto> findResourceById(String id);

    public ResourceDto createResource(ResourceDto resourceDto);

    public Optional<ResourceDto> updateResource(String id, ResourceDto resourceDto);

    public void deleteResource(String id);

    public ResourcePermissionsDto findLoggedUserResourcePermissions(String resourceName);
}
