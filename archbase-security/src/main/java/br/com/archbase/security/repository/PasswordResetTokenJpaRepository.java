package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.PasswordResetTokenEntity;

public interface PasswordResetTokenJpaRepository extends ArchbaseCommonJpaRepository<PasswordResetTokenEntity, String, Long> {

}
