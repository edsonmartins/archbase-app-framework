package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.AccessScheduleEntity;
import org.springframework.stereotype.Repository;


@Repository
public interface AccessScheduleJpaRepository extends ArchbaseCommonJpaRepository<AccessScheduleEntity, String, Long> {
}
