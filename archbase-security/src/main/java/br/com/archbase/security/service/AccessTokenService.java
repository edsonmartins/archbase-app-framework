package br.com.archbase.security.service;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.adapter.AccessTokenPersistenceAdapter;
import br.com.archbase.security.adapter.SecurityAdapter;
import br.com.archbase.security.domain.dto.AccessTokenDto;
import br.com.archbase.security.domain.entity.ApiToken;
import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import br.com.archbase.security.usecase.AccessTokenUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccessTokenService implements AccessTokenUseCase, FindDataWithFilterQuery<String, AccessTokenDto> {

    @Autowired
    private AccessTokenJpaRepository accessTokenRepository;
    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private AccessTokenPersistenceAdapter accessTokenPersistenceAdapter;

    @Autowired
    private SecurityAdapter securityAdapter;

    @Override
    public AccessTokenDto findById(String s) {
        return null;
    }

    @Override
    public Page<AccessTokenDto> findAll(int page, int size) {
        return accessTokenPersistenceAdapter.findAll(page, size);
    }

    @Override
    public Page<AccessTokenDto> findAll(int page, int size, String[] sort) {
        return accessTokenPersistenceAdapter.findAll(page, size, sort);
    }

    @Override
    public List<AccessTokenDto> findAll(List<String> strings) {
        return accessTokenPersistenceAdapter.findAll(strings);
    }

    @Override
    public Page<AccessTokenDto> findWithFilter(String filter, int page, int size) {
        return accessTokenPersistenceAdapter.findWithFilter(filter,page,size);
    }

    @Override
    public Page<AccessTokenDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return accessTokenPersistenceAdapter.findWithFilter(filter,page,size,sort);
    }


    @Override
    public void revokeToken(String token) {
        Optional<AccessTokenEntity> apiToken = accessTokenRepository.findByToken(token);
        if (apiToken.isPresent()){
            apiToken.get().setRevoked(true);
            accessTokenRepository.save(apiToken.get());
        }
    }

}
