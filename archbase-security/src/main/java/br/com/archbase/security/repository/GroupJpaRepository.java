package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.GroupEntity;
import org.springframework.stereotype.Repository;


@Repository
public interface GroupJpaRepository extends ArchbaseCommonJpaRepository<GroupEntity, String, Long> {
}
