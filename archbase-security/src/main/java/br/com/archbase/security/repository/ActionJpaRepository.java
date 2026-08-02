package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.domain.entity.TipoRecurso;
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

    /** Contagem de ações por situação, para o painel de diagnóstico. */
    @Query("SELECT COUNT(a) FROM ActionEntity a WHERE a.active = :active")
    long countByActive(@Param("active") boolean active);

    @Query("SELECT COUNT(a) FROM ActionEntity a JOIN a.resource r WHERE r.type = :type")
    long countByResourceType(@Param("type") TipoRecurso type);

    /** Ações que ninguém recebeu — no catálogo e sem nenhuma concessão. */
    @Query("SELECT COUNT(a) FROM ActionEntity a "
            + "WHERE NOT EXISTS (SELECT 1 FROM PermissionEntity p WHERE p.action = a)")
    long countWithoutAnyPermission();
}
