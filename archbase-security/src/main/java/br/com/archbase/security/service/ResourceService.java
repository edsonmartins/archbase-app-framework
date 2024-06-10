package br.com.archbase.security.service;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import br.com.archbase.security.adapter.ResourcePersistenceAdapter;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.usecase.ResourceUseCase;

import java.util.List;
import java.util.Optional;

@Service
public class ResourceService implements ResourceUseCase, FindDataWithFilterQuery<String, ResourceDto> {

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

    @Override
    public ResourcePermissionsDto findLoggedUserResourcePermissions(String resourceName) {
        return adapter.findLoggedUserResourcePermissions(resourceName);
    }

    @Override
    public ResourceDto findById(String s) {
        return adapter.findById(s);
    }

    @Override
    public Page<ResourceDto> findAll(int page, int size) {
        return adapter.findAll(page,size);
    }

    @Override
    public Page<ResourceDto> findAll(int page, int size, String[] sort) {
        return adapter.findAll(page,size,sort);
    }

    @Override
    public List<ResourceDto> findAll(List<String> strings) {
        return adapter.findAll(strings);
    }

    @Override
    public Page<ResourceDto> findWithFilter(String filter, int page, int size) {
        return adapter.findWithFilter(filter,page,size);
    }

    @Override
    public Page<ResourceDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return adapter.findWithFilter(filter,page,size,sort);
    }
}
