package br.com.archbase.security.repository;

import br.com.archbase.security.persistence.SecurityEventEntity;
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
            + "AND (LENGTH(:tenant) = 0 OR e.tenantId = :tenant) "
            + "ORDER BY e.dataHora DESC")
    Page<SecurityEventEntity> buscar(@Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim,
                                     @Param("tipo") SecurityEventType tipo,
                                     @Param("usuario") String usuario,
                                     @Param("tenant") String tenant,
                                     Pageable pageable);

    /**
     * A assinatura anterior ao filtro de tenant, preservada para não quebrar quem já chamava este
     * repositório. Delega sem estreitar, que é exatamente o comportamento que ela tinha.
     *
     * @deprecated use a sobrecarga com {@code tenant}; esta enxerga todos os tenants
     */
    @Deprecated(since = "3.4.0")
    default Page<SecurityEventEntity> buscar(LocalDateTime inicio, LocalDateTime fim,
                                             SecurityEventType tipo, String usuario,
                                             Pageable pageable) {
        return buscar(inicio, fim, tipo, usuario, "", pageable);
    }

    /**
     * A purga da retenção. Em lote, porque apagar um a um em milhões de linhas não termina.
     *
     * <p><b>Global de propósito, e é por isso que o isolamento de tenant não vive numa anotação.</b>
     * A correção óbvia para o vazamento da leitura seria marcar {@code tenantId} com {@code @TenantId}
     * e deixar o Hibernate filtrar, como faz com as outras catorze entidades do módulo. Aqui isso
     * quebraria: {@code ArchbaseAuditRetentionJob} é {@code @Scheduled} e roda fora de requisição,
     * sem tenant no contexto — o DELETE passaria a alcançar só o tenant padrão e a tabela cresceria
     * para sempre nos demais. Por isso o filtro fica na consulta de leitura, que é onde o vazamento
     * acontecia, e a purga segue varrendo tudo.
     */
    @Modifying
    @Query("DELETE FROM SecurityEventEntity e WHERE e.dataHora < :limite")
    int apagarAnterioresA(@Param("limite") LocalDateTime limite);
}
