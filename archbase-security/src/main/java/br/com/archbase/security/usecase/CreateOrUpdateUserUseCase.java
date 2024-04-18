package br.com.archbase.security.usecase;


import br.com.archbase.security.domain.entity.User;

public interface CreateOrUpdateUserUseCase {

    User createUser(User user) ;
    User updateUser(User user) ;
}
