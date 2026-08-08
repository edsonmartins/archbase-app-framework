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

    /** Ramo "Perfis" da árvore, paginado e filtrado no servidor.
     *
     * <p><b>Sem ramo {@code :filtro IS NULL}.</b> Um parâmetro solto num {@code IS NULL} não tem
     * tipo que o PostgreSQL consiga inferir: ele assume {@code bytea} e a consulta morre em
     * "function lower(bytea) does not exist". O H2 aceita, e foi por isso que passou nos testes e
     * quebrou no ambiente real. O serviço passa string vazia em vez de nulo, e {@code LIKE '%%'}
     * casa com tudo.
     */
    @Query("SELECT p FROM ProfileEntity p "
            + "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :filtro, '%')) "
            + "ORDER BY p.name")
    Page<ProfileEntity> findForTree(@Param("filtro") String filtro, Pageable pageable);
}
