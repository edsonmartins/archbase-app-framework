package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;


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

    /**
     * Busca todas as permissões para um conjunto de IDs de segurança (user, groups, profile).
     * Faz join eagerly com action e resource para evitar N+1.
     *
     * @param securityIds Conjunto de IDs de segurança (usuário, grupos e perfil)
     * @return Lista de permissões com action e resource carregados
     */
    @Query("SELECT DISTINCT p FROM PermissionEntity p " +
            "JOIN FETCH p.action a " +
            "JOIN FETCH a.resource r " +
            "WHERE p.security.id IN :securityIds")
    List<PermissionEntity> findAllBySecurityIds(@Param("securityIds") Set<String> securityIds);
}
