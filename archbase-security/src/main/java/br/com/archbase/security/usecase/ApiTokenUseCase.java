package br.com.archbase.security.usecase;


import br.com.archbase.security.domain.dto.ApiTokenDto;
import br.com.archbase.security.domain.entity.ApiToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ApiTokenUseCase {

    public ApiTokenDto createToken(String email, LocalDateTime expirationDate, String name, String description);

    public void revokeToken (String token);

    public boolean validateToken (String token);

    public Optional<ApiToken> getApiToken(String token);
}