package br.com.archbase.security.usecase;


import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.persistence.UserEntity;

import java.util.List;

public interface AccessTokenUseCase {
    public void revokeToken (String token);
}
