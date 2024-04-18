package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserJpaRepository extends ArchbaseCommonJpaRepository<UserEntity, String, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
