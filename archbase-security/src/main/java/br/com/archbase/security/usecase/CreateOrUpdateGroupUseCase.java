package br.com.archbase.security.usecase;


import br.com.archbase.security.domain.entity.Group;


public interface CreateOrUpdateGroupUseCase {

    Group createGroup(Group group) ;
    Group updateGroup(Group group) ;
}
