package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.dto.SimpleUserDto;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserUseCase {

    List<UserDto> getAllUsersByGroup(String groupId);

    Optional<UserDto> findGroupById(String id);

    UserDto createUser(UserDto userDto);

    Optional<UserDto> updateUser(String id, UserDto userDto);

    void removeUser(String id);

    void addPermission(String userId, String actionId);

    void removePermission(String permissionId);

    Optional<User> getLoggedUser() ;

    Optional<User> getUserByEmail(String email);

    List<String> createUsers(List<SimpleUserDto> usersDtos);
}
