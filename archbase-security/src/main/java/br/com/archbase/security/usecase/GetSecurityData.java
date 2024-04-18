package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.entity.User;

public interface GetSecurityData {

    public User getLoggedUser();

}
