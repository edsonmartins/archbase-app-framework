package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.dto.ResourceDto;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserUseCase {

    public List<UserDto> getAllUsersByGroup(String groupId);

    public Optional<UserDto> findGroupById(String id);

    public UserDto createUser(UserDto userDto);

    public Optional<UserDto> updateUser(String id, UserDto userDto);

    public void removeUser(String id);

    public void addPermission(String userId, String actionId);

    public void removePermission(String permissionId);

    Optional<User> getLoggedUser() ;

    Optional<User> getUserByEmail(String email);
}
