package br.com.archbase.security.adapter.port;

import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserPersistencePort {

    UserDto createUser(UserDto userDto) ;

    public Optional<UserDto> updateUser(String id, UserDto userDto);

    public void removerUser(String id) ;

    Optional<User> getUserById(String id) ;

    List<UserDto> getAllUsersByGroup(String groupId);

    Optional<User> getUserByEmail(String email) ;

    boolean existeUserByEmail(String email) ;

    void addPermission(String userId, String actionId);

    void removePermission(String permissionId);

    List<UserDto> createUsers(List<UserDto> userDtos);

    List<User> getUsersByEmails(List<String> emails);

}
