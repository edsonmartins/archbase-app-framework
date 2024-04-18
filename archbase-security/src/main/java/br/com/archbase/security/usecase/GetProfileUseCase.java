package br.com.archbase.security.usecase;


import br.com.archbase.security.domain.entity.Profile;

import java.util.Optional;

public interface GetProfileUseCase {

    Optional<Profile> getProfileById(String id) ;
}
