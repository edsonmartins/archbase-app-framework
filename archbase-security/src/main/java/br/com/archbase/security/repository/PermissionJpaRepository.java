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
            "JOIN p.security u " +
            "JOIN p.action a " +
            "JOIN a.resource r " +
            "WHERE u.id = :securityId " +  // Corrigido para corresponder ao parâmetro do método
            "AND a.name = :actionName " +
            "AND r.name = :resourceName")
    List<PermissionEntity> findBySecurityIdAndActionNameAndResourceName(
            @Param("securityId") String securityId,
            @Param("actionName") String actionName,
            @Param("resourceName") String resourceName);
}
