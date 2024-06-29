package br.com.archbase.security.service;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.adapter.ActionPersistenceAdapter;
import br.com.archbase.security.adapter.SecurityAdapter;
import br.com.archbase.security.domain.dto.*;
import br.com.archbase.security.domain.entity.User;
import com.google.common.collect.Lists;
import br.com.archbase.security.domain.dto.ResourcePermissionsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import br.com.archbase.security.adapter.ResourcePersistenceAdapter;
import br.com.archbase.security.usecase.ResourceUseCase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ResourceService implements ResourceUseCase, FindDataWithFilterQuery<String, ResourceDto> {

    private final ResourcePersistenceAdapter adapter;
    private final SecurityAdapter securityAdapter;
    private final ActionPersistenceAdapter actionPersistenceAdapter;

    @Autowired
    public ResourceService(ResourcePersistenceAdapter adapter, SecurityAdapter securityAdapter, ActionPersistenceAdapter actionPersistenceAdapter) {
        this.adapter = adapter;
        this.securityAdapter = securityAdapter;
        this.actionPersistenceAdapter = actionPersistenceAdapter;
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
    public ResourcePermissionsDto registerResource(ResourceRegisterDto resourceRegister) {
        ResourceDto resourceDto;
        resourceDto = adapter.findResource(resourceRegister.getResource().getResourceName());
        if (resourceDto == null) {
            ResourceDto resource = ResourceDto.builder()
                    .name(resourceRegister.getResource().getResourceName())
                    .description(resourceRegister.getResource().getResourceDescription())
                    .createEntityDate(LocalDateTime.now())
                    .createdByUser("archbase")
                    .version(0L)
                    .actions(Lists.newArrayList())
                    .build();
            resourceDto = adapter.createResource(resource);
        }
        ResourceDto finalResourceDto = resourceDto;
        List<String> actionNames = resourceRegister.getActions().stream().map(SimpleActionDto::getActionName).toList();
        List<ActionDto> missingActions = actionPersistenceAdapter.findMissingActionsByNames(actionNames, finalResourceDto.getId());
        missingActions.forEach(missingAction -> {
            missingAction.setActive(false);
            actionPersistenceAdapter.updateAction(missingAction.getId(), missingAction);
        });
        resourceRegister.getActions().forEach(simpleActionDto -> {
            Optional<ActionDto> actionOptional = actionPersistenceAdapter
                    .findActionByName(simpleActionDto.getActionName(), finalResourceDto.getId());
            if (actionOptional.isEmpty()) {
                ActionDto action = ActionDto.builder()
                        .name(simpleActionDto.getActionName())
                        .description(simpleActionDto.getActionDescription())
                        .resource(finalResourceDto)
                        .createEntityDate(LocalDateTime.now())
                        .createdByUser("archbase")
                        .version(0L)
                        .build();
                actionPersistenceAdapter.createAction(action);
            } else {
                ActionDto foundAction = actionOptional.get();
                if (!foundAction.getActive()) {
                    foundAction.setActive(true);
                    actionPersistenceAdapter.updateAction(foundAction.getId(), foundAction);
                }
            }
        });
        return adapter.findLoggedUserResourcePermissions(finalResourceDto.getName());
    }

    @Override
    public ResourcePermissionsDto findLoggedUserResourcePermissions(String resourceName) {
        return adapter.findLoggedUserResourcePermissions(resourceName);
    }

    public List<ResoucePermissionsWithTypeDto> findResourcesPermissions(String securityId, SecurityType securityType) {
        if (SecurityType.USER.equals(securityType)) {
            return adapter.findUserResourcesPermissions(securityId);
        }
        if (SecurityType.PROFILE.equals(securityType)) {
            return adapter.findProfileResourcesPermissions(securityId);
        }
        if (SecurityType.GROUP.equals(securityType)) {
            return adapter.findGroupResourcesPermissions(securityId);
        }
        return Lists.newArrayList();
    }

    public List<ResoucePermissionsWithTypeDto> findAllResourcesPermissions() {
        return adapter.findAllResourcesPermissions();
    }

    public PermissionDto findPermission(String securityId, String actionId) {
        return adapter.findPermission(securityId, actionId);
    }

    public PermissionDto grantPermission(PermissionDto permissionDto) {
        User user = securityAdapter.getLoggedUser();
        permissionDto.setCreatedByUser(user.getUserName());
        permissionDto.setLastModifiedByUser(user.getUserName());
        permissionDto.setUpdateEntityDate(LocalDateTime.now());
        permissionDto.setCreateEntityDate(LocalDateTime.now());
        return adapter.grantPermission(permissionDto);
    }

    public void deletePermission(String id) {
        adapter.deletePermission(id);
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
