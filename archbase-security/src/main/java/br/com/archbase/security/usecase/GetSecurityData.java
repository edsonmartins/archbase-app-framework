package br.com.archbase.security.usecase;

import br.com.archbase.security.domain.entity.User;

import java.security.Principal;

public interface GetSecurityData {
    public User getLoggedUser();
    public Principal getPrincipal();
}
