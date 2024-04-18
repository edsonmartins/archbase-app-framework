package br.com.archbase.security.usecase;


import br.com.archbase.security.domain.entity.Profile;

public interface CreateOrUpdateProfileUseCase {

    Profile createProfile(Profile profile) ;
    Profile updateProfile(Profile profile) ;

}
