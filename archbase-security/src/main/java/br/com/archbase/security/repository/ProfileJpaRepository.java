package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.ProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ProfileJpaRepository extends ArchbaseCommonJpaRepository<ProfileEntity, String, Long> {

    /** Ramo "Perfis" da árvore, paginado e filtrado no servidor. */
    @Query("SELECT p FROM ProfileEntity p "
            + "WHERE (:filtro IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :filtro, '%'))) "
            + "ORDER BY p.name")
    Page<ProfileEntity> findForTree(@Param("filtro") String filtro, Pageable pageable);
}
