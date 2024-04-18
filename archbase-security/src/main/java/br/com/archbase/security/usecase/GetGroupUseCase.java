package br.com.archbase.security.usecase;


import br.com.archbase.security.domain.entity.Group;

import java.util.Optional;

public interface GetGroupUseCase {

    Optional<Group> getGroupById(String id) ;
}
