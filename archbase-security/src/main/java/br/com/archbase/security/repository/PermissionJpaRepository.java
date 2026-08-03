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
     * Busca permissões para um conjunto de IDs de segurança (usuário, grupos e perfil)
     * filtradas por ação e recurso. Usado na verificação de autorização em tempo de
     * execução para considerar permissões concedidas diretamente ao usuário, aos
     * grupos do usuário e ao perfil do usuário.
     *
     * @param securityIds Conjunto de IDs de segurança (usuário, grupos e perfil)
     * @param actionName Nome da ação
     * @param resourceName Nome do recurso
     * @return Lista de permissões correspondentes
     */
    /**
     * <b>JOIN FETCH, e não JOIN.</b> A decisão de acesso lê o nome de quem concedeu
     * ({@code p.security}) e o nível mínimo da capacidade ({@code p.action}), e as duas associações
     * são {@code LAZY}. Sem o fetch, essas leituras acontecem sobre entidades já desanexadas —
     * o interceptador não abre transação — e viram {@code LazyInitializationException}, que o
     * {@code CustomAuthorizationManager} converte em negação. O sintoma seria o pior possível:
     * <b>403 para quem tem a permissão</b>, funcionando só para administradores, e apenas em
     * aplicações com {@code spring.jpa.open-in-view=false}.
     */
    @Query("SELECT DISTINCT p FROM PermissionEntity p " +
            "JOIN FETCH p.security u " +
            "JOIN FETCH p.action a " +
            "JOIN FETCH a.resource r " +
            "WHERE u.id IN :securityIds " +
            "AND a.name = :actionName " +
            "AND r.name = :resourceName")
    List<PermissionEntity> findBySecurityIdsAndActionNameAndResourceName(
            @Param("securityIds") Set<String> securityIds,
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
            "JOIN FETCH p.security s " +
            "JOIN FETCH p.action a " +
            "JOIN FETCH a.resource r " +
            "WHERE s.id IN :securityIds")
    List<PermissionEntity> findAllBySecurityIds(@Param("securityIds") Set<String> securityIds);

    /**
     * Igual à anterior, restrita a um recurso.
     *
     * <p>Existe para a tela: ela pergunta "o que posso neste recurso", e carregar todas as
     * permissões do usuário para filtrar em memória seria trocar uma consulta filtrada por uma
     * varredura — num tenant com milhares de concessões, a cada renderização.
     */
    @Query("SELECT DISTINCT p FROM PermissionEntity p " +
            "JOIN FETCH p.security s " +
            "JOIN FETCH p.action a " +
            "JOIN FETCH a.resource r " +
            "WHERE s.id IN :securityIds AND r.name = :resourceName")
    List<PermissionEntity> findAllBySecurityIdsAndResourceName(
            @Param("securityIds") Set<String> securityIds,
            @Param("resourceName") String resourceName);

    /**
     * Quantas concessões apontam para ação ou recurso inativo.
     *
     * <p>É o número que importa antes de ligar
     * {@code archbase.security.permission.require-active}: são exatamente as permissões que a tela
     * já ignora e que o {@code @HasPermission} ainda honra. No gestor-rq eram 1.279 de 2.229.
     */
    @Query("SELECT COUNT(p) FROM PermissionEntity p "
            + "JOIN p.action a JOIN a.resource r "
            + "WHERE a.active = false OR r.active = false")
    long countPointingToInactive();

    /** Total de concessões, para dar denominador ao número acima. */
    @Query("SELECT COUNT(p) FROM PermissionEntity p")
    long countAll();

    /** Quantas concessões cada tipo de destinatário recebeu — usuário, grupo ou perfil. */
    @Query("SELECT TYPE(p.security), COUNT(p) FROM PermissionEntity p GROUP BY TYPE(p.security)")
    List<Object[]> countGroupedBySecurityType();
}
