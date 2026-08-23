package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.PermissionEntity;
import br.com.archbase.security.persistence.ProfileEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * <b>O grafo não é decoração.</b> A decisão de acesso lê o nome de quem concedeu
     * ({@code p.security}) e o nível mínimo da capacidade ({@code p.action}), e as duas associações
     * são {@code LAZY}. Sem carregá-las junto, essas leituras acontecem sobre entidades já
     * desanexadas — o interceptador não abre transação — e viram
     * {@code LazyInitializationException}, que o {@code CustomAuthorizationManager} converte em
     * negação. O sintoma seria o pior possível: <b>403 para quem tem a permissão</b>, funcionando
     * só para administradores, e apenas em aplicações com {@code spring.jpa.open-in-view=false}.
     *
     * <p>{@code @EntityGraph} em vez de {@code JOIN FETCH} no JPQL: o efeito é o mesmo e a
     * condição de filtro fica separada da decisão de carregamento — usar um alias de
     * {@code JOIN FETCH} no {@code WHERE} funciona no Hibernate, mas está fora do que a
     * especificação garante.
     */
    @EntityGraph(attributePaths = {"security", "action", "action.resource"})
    @Query("SELECT p FROM PermissionEntity p "
            + "WHERE p.security.id IN :securityIds "
            + "AND p.action.name = :actionName "
            + "AND p.action.resource.name = :resourceName "
            + "AND (:requireActive = false "
            + "     OR (p.action.active = true AND p.action.resource.active = true))")
    List<PermissionEntity> findBySecurityIdsAndActionNameAndResourceName(
            @Param("securityIds") Set<String> securityIds,
            @Param("actionName") String actionName,
            @Param("resourceName") String resourceName,
            @Param("requireActive") boolean requireActive);

    /**
     * Busca todas as permissões para um conjunto de IDs de segurança (user, groups, profile).
     * Faz join eagerly com action e resource para evitar N+1.
     *
     * @param securityIds Conjunto de IDs de segurança (usuário, grupos e perfil)
     * @return Lista de permissões com action e resource carregados
     */
    @EntityGraph(attributePaths = {"security", "action", "action.resource"})
    @Query("SELECT p FROM PermissionEntity p WHERE p.security.id IN :securityIds")
    List<PermissionEntity> findAllBySecurityIds(@Param("securityIds") Set<String> securityIds);

    /**
     * Igual à anterior, restrita a um recurso.
     *
     * <p>Existe para a tela: ela pergunta "o que posso neste recurso", e carregar todas as
     * permissões do usuário para filtrar em memória seria trocar uma consulta filtrada por uma
     * varredura — num tenant com milhares de concessões, a cada renderização.
     */
    @EntityGraph(attributePaths = {"security", "action", "action.resource"})
    @Query("SELECT p FROM PermissionEntity p "
            + "WHERE p.security.id IN :securityIds "
            + "AND p.action.resource.name = :resourceName")
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

    /** Os itens por trás de {@link #countPointingToInactive()}. */
    @Query("SELECT p FROM PermissionEntity p "
            + "JOIN FETCH p.action a JOIN FETCH a.resource r JOIN FETCH p.security "
            + "WHERE a.active = false OR r.active = false "
            + "ORDER BY r.name, a.name")
    Page<PermissionEntity> findPointingToInactive(Pageable pageable);

    /**
     * Quem recebeu concessão para uma ação — a consulta <b>reversa</b>.
     *
     * <p>Responde "quem consegue fazer isto?", que hoje só se responde abrindo grupo por grupo no
     * admin. O {@code JOIN FETCH} do destinatário é necessário: a resposta precisa do nome de quem
     * recebeu, e navegar a associação depois seria uma consulta por linha.
     *
     * <p>Devolve as concessões <b>de todas as vias</b> — usuário, grupo e perfil. Traduzir cada uma
     * para as pessoas que alcançam é trabalho do serviço, porque só ele sabe quem está em qual
     * grupo e quem tem qual perfil.
     */
    @Query("SELECT p FROM PermissionEntity p JOIN FETCH p.security "
            + "WHERE p.action.id = :actionId")
    List<PermissionEntity> findGrantsOfAction(@Param("actionId") String actionId);

    /** Total de concessões, para dar denominador ao número acima. */
    @Query("SELECT COUNT(p) FROM PermissionEntity p")
    long countAll();

    /** Quantas concessões cada tipo de destinatário recebeu — usuário, grupo ou perfil. */
    @Query("SELECT TYPE(p.security), COUNT(p) FROM PermissionEntity p GROUP BY TYPE(p.security)")
    List<Object[]> countGroupedBySecurityType();
}
