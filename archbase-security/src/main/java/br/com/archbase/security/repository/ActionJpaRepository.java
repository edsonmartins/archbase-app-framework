package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ActionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Os itens por trás de {@link #countByActive(boolean)}, para o detalhe do panorama. */
    @Query("SELECT a FROM ActionEntity a JOIN FETCH a.resource WHERE a.active = :active "
            + "ORDER BY a.name")
    Page<ActionEntity> findByActive(@Param("active") boolean active, Pageable pageable);

    /** Os itens por trás de {@link #countWithoutAnyPermission()}. */
    @Query("SELECT a FROM ActionEntity a JOIN FETCH a.resource "
            + "WHERE NOT EXISTS (SELECT 1 FROM PermissionEntity p WHERE p.action = a) "
            + "ORDER BY a.name")
    Page<ActionEntity> findWithoutAnyPermission(Pageable pageable);

    /** Ramo "Ações de um recurso" da árvore, paginado e filtrado no servidor.
     *
     * <p><b>Sem ramo {@code :filtro IS NULL}.</b> Um parâmetro solto num {@code IS NULL} não tem
     * tipo que o PostgreSQL consiga inferir: ele assume {@code bytea} e a consulta morre em
     * "function lower(bytea) does not exist". O H2 aceita, e foi por isso que passou nos testes e
     * quebrou no ambiente real. O serviço passa string vazia em vez de nulo, e {@code LIKE '%%'}
     * casa com tudo.
     */
    @Query("SELECT a FROM ActionEntity a WHERE a.resource.id = :resourceId "
            + "AND LOWER(a.name) LIKE LOWER(CONCAT('%', :filtro, '%')) "
            + "ORDER BY a.name")
    Page<ActionEntity> findForTree(@Param("resourceId") String resourceId,
                                   @Param("filtro") String filtro, Pageable pageable);

    /** Quantas concessões alcançam cada ação — o número "quem alcança" do nó folha. */
    @Query("SELECT p.action.id, COUNT(p) FROM PermissionEntity p "
            + "WHERE p.action.id IN :ids GROUP BY p.action.id")
    java.util.List<Object[]> countPermissionsOf(@Param("ids") java.util.Collection<String> ids);
}
