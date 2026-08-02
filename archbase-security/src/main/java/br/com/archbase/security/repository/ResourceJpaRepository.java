package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.domain.entity.TipoRecurso;
import br.com.archbase.security.persistence.ResourceEntity;
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
}
