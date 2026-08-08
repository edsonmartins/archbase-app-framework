package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.GroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface GroupJpaRepository extends ArchbaseCommonJpaRepository<GroupEntity, String, Long> {

    /** Ramo "Grupos" da árvore, paginado e filtrado no servidor.
     *
     * <p><b>Sem ramo {@code :filtro IS NULL}.</b> Um parâmetro solto num {@code IS NULL} não tem
     * tipo que o PostgreSQL consiga inferir: ele assume {@code bytea} e a consulta morre em
     * "function lower(bytea) does not exist". O H2 aceita, e foi por isso que passou nos testes e
     * quebrou no ambiente real. O serviço passa string vazia em vez de nulo, e {@code LIKE '%%'}
     * casa com tudo.
     */
    @Query("SELECT g FROM GroupEntity g "
            + "WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :filtro, '%')) "
            + "ORDER BY g.name")
    Page<GroupEntity> findForTree(@Param("filtro") String filtro, Pageable pageable);

    /**
     * Quantos membros cada grupo tem.
     *
     * <p>Em lote, para os grupos da página aberta: o número fica à direita do nó, e buscá-lo grupo
     * a grupo transformaria uma página de vinte itens em vinte e uma consultas.
     */
    @Query("SELECT ug.group.id, COUNT(ug) FROM UserGroupEntity ug "
            + "WHERE ug.group.id IN :ids GROUP BY ug.group.id")
    java.util.List<Object[]> countMembersOf(@Param("ids") java.util.Collection<String> ids);
}
