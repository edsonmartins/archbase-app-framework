package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.ActionEntity;
import org.springframework.stereotype.Repository;


@Repository
public interface ActionJpaRepository extends ArchbaseCommonJpaRepository<ActionEntity, String, Long> {

    public ActionEntity findByActionNameAndResourceName(String actionName, String resourceName);
}
