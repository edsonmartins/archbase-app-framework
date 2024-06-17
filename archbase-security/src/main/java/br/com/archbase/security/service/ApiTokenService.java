package br.com.archbase.security.service;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.domain.dto.ApiTokenDto;
import br.com.archbase.security.domain.entity.ApiToken;
import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.ApiTokenRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.usecase.ApiTokenUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiTokenService implements ApiTokenUseCase, FindDataWithFilterQuery<String, ApiTokenDto> {

    @Autowired
    private ApiTokenRepository apiTokenRepository;
    @Autowired
    private UserJpaRepository userRepository;

    @Override
    public ApiTokenDto findById(String s) {
        return null;
    }

    @Override
    public Page<ApiTokenDto> findAll(int page, int size) {
        return null;
    }

    @Override
    public Page<ApiTokenDto> findAll(int page, int size, String[] sort) {
        return null;
    }

    @Override
    public List<ApiTokenDto> findAll(List<String> strings) {
        return List.of();
    }

    @Override
    public Page<ApiTokenDto> findWithFilter(String filter, int page, int size) {
        return null;
    }

    @Override
    public Page<ApiTokenDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return null;
    }

    @Override
    public ApiTokenDto createToken(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        String token = UUID.randomUUID().toString();
        LocalDateTime expirationDate = LocalDateTime.now().plusDays(30);

        ApiTokenEntity apiToken = ApiTokenEntity.builder()
                .token(token)
                .user(user)
                .expirationDate(expirationDate)
                .revoked(false)
                .build();

        return apiTokenRepository.save(apiToken).toDto();
    }

    @Override
    public void revokeToken(String token) {
        Optional<ApiTokenEntity> apiToken = apiTokenRepository.findByToken(token);
        if (apiToken.isPresent()){
            apiToken.get().setRevoked(true);
            apiTokenRepository.save(apiToken.get());
        }
    }

    @Override
    public boolean validateToken(String token) {
        return apiTokenRepository.findByToken(token)
                .map(t -> !t.getRevoked() && t.getExpirationDate().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Override
    public Optional<ApiToken> getApiToken(String token) {
        Optional<ApiTokenEntity> optionalApiTokenEntity = apiTokenRepository.findByToken(token);
        return optionalApiTokenEntity.map(ApiTokenEntity::toDomain);
    }
}
