package br.com.archbase.security.repository;

import br.com.archbase.security.audit.SecurityEventEntity;
import br.com.archbase.security.audit.SecurityEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SecurityEventJpaRepository extends JpaRepository<SecurityEventEntity, String> {

    /**
     * A consulta da tela: por período, opcionalmente estreitada por pessoa e por tipo.
     *
     * <p>Os filtros são comparados com {@code :param IS NULL OR ...} em coluna de texto explícita —
     * sem o CAST o PostgreSQL infere bytea para o parâmetro nulo e a consulta falha com
     * "function lower(bytea) does not exist", erro que só aparece fora do H2.
     */
    @Query("SELECT e FROM SecurityEventEntity e "
            + "WHERE e.dataHora BETWEEN :inicio AND :fim "
            + "AND (:tipo IS NULL OR e.tipo = :tipo) "
            + "AND (LENGTH(:usuario) = 0 OR LOWER(e.usuario) LIKE LOWER(CONCAT('%', :usuario, '%'))) "
            + "ORDER BY e.dataHora DESC")
    Page<SecurityEventEntity> buscar(@Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim,
                                     @Param("tipo") SecurityEventType tipo,
                                     @Param("usuario") String usuario,
                                     Pageable pageable);

    /** A purga da retenção. Em lote, porque apagar um a um em milhões de linhas não termina. */
    @Modifying
    @Query("DELETE FROM SecurityEventEntity e WHERE e.dataHora < :limite")
    int apagarAnterioresA(@Param("limite") LocalDateTime limite);
}
