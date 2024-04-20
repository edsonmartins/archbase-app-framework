package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PermissionJpaRepository extends ArchbaseCommonJpaRepository<PermissionEntity, String, Long> {
    @Query("SELECT p FROM PermissionEntity p " +
            "JOIN p.user u " +
            "JOIN p.action a " +
            "JOIN a.resource r " +
            "WHERE u.id = :userId " +
            "AND a.name = :actionName " +
            "AND r.name = :resourceName")
    List<PermissionEntity> findByUserIdAndActionNameAndResourceName(
            @Param("userId") String userId,
            @Param("actionName") String actionName,
            @Param("resourceName") String resourceName);
}
