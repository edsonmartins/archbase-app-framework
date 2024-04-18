package br.com.archbase.security.usecase;


import br.com.archbase.security.domain.entity.User;

import java.util.Optional;

public interface GetUserUseCase {
    Optional<User> getUserById(String id) ;

    Optional<User> getUserByEmail(String email) ;
}
