package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.ApiTokenEntity;

import java.util.Optional;

public interface ApiTokenRepository extends ArchbaseCommonJpaRepository<ApiTokenEntity, String, Long> {

    Optional<ApiTokenEntity> findByToken(String token);

    Optional<ApiTokenEntity> findByTokenAndTenantId(String token, String tenantId);
}
