package br.com.archbase.security.service;


import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.adapter.SecurityAdapter;
import br.com.archbase.security.adapter.UserPersistenceAdapter;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.usecase.CreateOrUpdateUserUseCase;
import br.com.archbase.security.usecase.GetLoggedUserUseCase;
import br.com.archbase.security.usecase.GetUserUseCase;
import br.com.archbase.security.usecase.RemoveUserUseCase;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserService implements CreateOrUpdateUserUseCase, GetUserUseCase, RemoveUserUseCase, FindDataWithFilterQuery<String, UserDto>, GetLoggedUserUseCase {

    private final UserPersistenceAdapter persistenceAdapter;
    private final SecurityAdapter securityAdapter;

    public UserService(UserPersistenceAdapter persistenceAdapter, SecurityAdapter securityAdapter) {
        this.persistenceAdapter =  persistenceAdapter;
        this.securityAdapter = securityAdapter;
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
    public Optional<User> getUserById(String id)  {
        return persistenceAdapter.getUserById(id);
    }


    @Override
    public User createUser(User user)  {
        return null;
    }

    @Override
    public User updateUser(User user)  {
        return null;
    }

    @Override
    public User removeUser(String id)  {
        return null;
    }

    @Override
    public Optional<User> getLoggedUser() {
        return Optional.ofNullable(securityAdapter.getLoggedUser());
    }
}
