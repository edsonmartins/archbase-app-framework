package br.com.archbase.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.archbase.security.adapter.ResourcePersistenceAdapter;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.usecase.ResourceUseCase;

import java.util.List;
import java.util.Optional;

@Service
public class ResourceService implements ResourceUseCase {

    private final ResourcePersistenceAdapter adapter;

    @Autowired
    public ResourceService(ResourcePersistenceAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public List<ResourceDto> findAllResources() {
        return adapter.findAllResources();
    }

    @Override
    public Optional<ResourceDto> findResourceById(String id) {
        return adapter.findResourceById(id);
    }

    @Override
    public ResourceDto createResource(ResourceDto resourceDto) {
        return adapter.createResource(resourceDto);
    }

    @Override
    public Optional<ResourceDto> updateResource(String id, ResourceDto resourceDto) {
        return adapter.updateResource(id, resourceDto);
    }

    @Override
    public void deleteResource(String id) {
        adapter.deleteResource(id);
    }
}
