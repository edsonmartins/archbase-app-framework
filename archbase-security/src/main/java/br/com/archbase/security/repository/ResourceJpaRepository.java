package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ResourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ResourceJpaRepository extends ArchbaseCommonJpaRepository<ResourceEntity, String, Long> {
    public ResourceEntity findByName(String resourceName);

    /** Contagem de recursos por tipo e situação, para o painel de diagnóstico. */
    @Query("SELECT COUNT(r) FROM ResourceEntity r WHERE r.type = :type AND r.active = :active")
    long countByTypeAndActive(@Param("type") TipoRecurso type, @Param("active") boolean active);

    @Query("SELECT COUNT(r) FROM ResourceEntity r WHERE r.type = :type")
    long countByType(@Param("type") TipoRecurso type);

    /**
     * Recursos sem nenhuma ação.
     *
     * <p>Não é defeito: o frontend registra a tela na primeira renderização, então recurso vazio
     * costuma significar tela ainda não aberta neste tenant. Vale como indicador operacional.
     */
    @Query("SELECT COUNT(r) FROM ResourceEntity r "
            + "WHERE NOT EXISTS (SELECT 1 FROM ActionEntity a WHERE a.resource = r)")
    long countWithoutAnyAction();

    /** Os itens por trás de {@link #countByTypeAndActive(TipoRecurso, boolean)}. */
    @Query("SELECT r FROM ResourceEntity r WHERE r.type = :type AND r.active = :active "
            + "ORDER BY r.name")
    Page<ResourceEntity> findByTypeAndActive(@Param("type") TipoRecurso type,
                                             @Param("active") boolean active, Pageable pageable);

    /** Os itens por trás de {@link #countWithoutAnyAction()}. */
    @Query("SELECT r FROM ResourceEntity r "
            + "WHERE NOT EXISTS (SELECT 1 FROM ActionEntity a WHERE a.resource = r) "
            + "ORDER BY r.name")
    Page<ResourceEntity> findWithoutAnyAction(Pageable pageable);

    /** Ramo "Recursos" da árvore, paginado e filtrado no servidor.
     *
     * <p><b>Sem ramo {@code :filtro IS NULL}.</b> Um parâmetro solto num {@code IS NULL} não tem
     * tipo que o PostgreSQL consiga inferir: ele assume {@code bytea} e a consulta morre em
     * "function lower(bytea) does not exist". O H2 aceita, e foi por isso que passou nos testes e
     * quebrou no ambiente real. O serviço passa string vazia em vez de nulo, e {@code LIKE '%%'}
     * casa com tudo.
     */
    @Query("SELECT r FROM ResourceEntity r "
            + "WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :filtro, '%')) "
            + "ORDER BY r.name")
    Page<ResourceEntity> findForTree(@Param("filtro") String filtro, Pageable pageable);

    /**
     * Quantas ações cada recurso tem, e quantas delas estão desativadas.
     *
     * <p>Em lote, para os recursos da página: alimenta o número à direita do nó e o marcador de
     * problema, sem precisar abrir o ramo — que é justamente o ponto de ter o marcador.
     */
    @Query("SELECT a.resource.id, COUNT(a), SUM(CASE WHEN a.active = false THEN 1 ELSE 0 END) "
            + "FROM ActionEntity a WHERE a.resource.id IN :ids GROUP BY a.resource.id")
    java.util.List<Object[]> countActionsOf(@Param("ids") java.util.Collection<String> ids);
}
