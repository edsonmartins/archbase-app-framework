package br.com.archbase.security.service;


import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.adapter.SecurityAdapter;
import br.com.archbase.security.adapter.UserPersistenceAdapter;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.usecase.UserUseCase;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserService implements UserUseCase, FindDataWithFilterQuery<String, UserDto> {

    private final UserPersistenceAdapter persistenceAdapter;
    private final SecurityAdapter securityAdapter;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceListener userServiceListener;

    public UserService(UserPersistenceAdapter persistenceAdapter, SecurityAdapter securityAdapter, PasswordEncoder passwordEncoder, UserServiceListener userServiceListener) {
        this.persistenceAdapter =  persistenceAdapter;
        this.securityAdapter = securityAdapter;
        this.passwordEncoder = passwordEncoder;
        this.userServiceListener = userServiceListener;
    }

    @Override
    public UserDto findById(String id) {
        return persistenceAdapter.findById(id);
    }

    @Override
    public Page<UserDto> findAll(int page, int size) {
        return persistenceAdapter.findAll(page, size);
    }

    @Override
    public Page<UserDto> findAll(int page, int size, String[] sort) {
        return persistenceAdapter.findAll(page, size, sort);
    }

    @Override
    public Optional<User> getUserByEmail(String email)  {
        Optional<User> usuarioOptional = persistenceAdapter.getUserByEmail(email);
        if(usuarioOptional.isEmpty()) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s  não foi encontrado.",email));
        }
        return usuarioOptional;
    }
    @Override
    public List<UserDto> findAll(List<String> ids) {
        return persistenceAdapter.findAll(ids);
    }

    @Override
    public Page<UserDto> findWithFilter(String filter, int page, int size) {
        return persistenceAdapter.findWithFilter(filter, page, size);
    }

    @Override
    public Page<UserDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return persistenceAdapter.findWithFilter(filter, page, size, sort);
    }

    @Override
    public List<UserDto> getAllUsersByGroup(String groupId) {
        return persistenceAdapter.getAllUsersByGroup(groupId);
    }

    @Override
    public Optional<UserDto> findGroupById(String id) {
        return Optional.empty();
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        userServiceListener.onBeforeCreate(userDto);
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        UserDto user = persistenceAdapter.createUser(userDto);
        userServiceListener.onAfterCreate(userDto,user);
        return user;
    }

    @Override
    public Optional<UserDto> updateUser(String id, UserDto userDto) {
        userServiceListener.onBeforeUpdate(userDto);
        if (!StringUtils.isBlank(userDto.getPassword())) {
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        Optional<UserDto> result = persistenceAdapter.updateUser(id, userDto);
        userServiceListener.onAfterUpdate(userDto, result.get());
        return result;
    }

    @Override
    public void removeUser(String id) {
        UserDto userDto = findById(id);
        if (userDto==null){
            throw new ArchbaseValidationException("Usuário não encontrada.");
        }
        userServiceListener.onBeforeRemove(userDto);
        persistenceAdapter.removerUser(id);
        userServiceListener.onAfterRemove(userDto);
    }

    @Override
    public void addPermission(String userId, String actionId) {
        persistenceAdapter.addPermission(userId, actionId);
    }

    @Override
    public void removePermission(String permissionId) {
        persistenceAdapter.removePermission(permissionId);
    }

    @Override
    public Optional<User> getLoggedUser() {
        return Optional.ofNullable(securityAdapter.getLoggedUser());
    }
}
