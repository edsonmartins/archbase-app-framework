package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.ResourceEntity;
import org.springframework.stereotype.Repository;


@Repository
public interface ResourceJpaRepository extends ArchbaseCommonJpaRepository<ResourceEntity, String, Long> {
    public ResourceEntity findByName(String resourceName);
}
