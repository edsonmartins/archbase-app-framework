package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.AccessTokenEntity;

import java.util.Optional;

public interface AccessTokenJpaRepository extends ArchbaseCommonJpaRepository<AccessTokenEntity, String, Long> {

  Optional<AccessTokenEntity> findByToken(String token);
}
