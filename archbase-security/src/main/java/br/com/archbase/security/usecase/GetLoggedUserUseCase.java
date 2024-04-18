package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.entity.User;

import java.util.Optional;

public interface GetLoggedUserUseCase {
    Optional<User> getLoggedUser() ;

}
