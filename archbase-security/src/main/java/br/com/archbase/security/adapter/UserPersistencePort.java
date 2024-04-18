package br.com.archbase.security.adapter;

import br.com.archbase.security.domain.entity.User;

import java.util.Optional;

public interface UserPersistencePort {

    User saveUser(User usuario) ;

    Optional<User> getUserById(String id) ;

    Optional<User> getUserByEmail(String email) ;

    boolean existeUserByEmail(String email) ;
}
