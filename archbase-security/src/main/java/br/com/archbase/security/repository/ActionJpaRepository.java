package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.ActionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ActionJpaRepository extends ArchbaseCommonJpaRepository<ActionEntity, String, Long> {

    @Query("SELECT a FROM ActionEntity a " +
            "JOIN a.resource r " +
            "WHERE a.name = :actionName " +
            "AND r.name = :resourceName")
    Optional<ActionEntity> findByActionNameAndResourceName(
            @Param("actionName") String actionName,
            @Param("resourceName") String resourceName);
}
