package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.SecurityEntity;
import org.springframework.stereotype.Repository;


@Repository
public interface SecurityJpaRepository extends ArchbaseCommonJpaRepository<SecurityEntity, String, Long> {
}
