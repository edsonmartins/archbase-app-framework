package br.com.archbase.security.adapter.port;

import br.com.archbase.security.domain.dto.ResourceDto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ResourcePersistencePort {

    public List<ResourceDto> findAllResources();
    public Optional<ResourceDto> findResourceById(String id);
    public ResourceDto createResource(ResourceDto resourceDto);
    public Optional<ResourceDto> updateResource(String id, ResourceDto resourceDto);
    public void deleteResource(String id) ;
}
