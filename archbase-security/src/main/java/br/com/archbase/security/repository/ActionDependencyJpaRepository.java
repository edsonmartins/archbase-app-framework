package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.domain.entity.DependencySource;
import br.com.archbase.security.persistence.ActionDependencyEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ActionDependencyJpaRepository
        extends ArchbaseCommonJpaRepository<ActionDependencyEntity, String, Long> {

    /**
     * Todas as arestas de uma origem declarante, com a ação carregada.
     *
     * <p>O grafo não é decoração: a reconciliação compara pelo id da ação de origem, e navegar a
     * associação lazy depois seria uma consulta por linha — numa base com centenas de capacidades,
     * uma varredura de subida inteira gasta em N+1.
     */
    @EntityGraph(attributePaths = {"action"})
    @Query("SELECT d FROM ActionDependencyEntity d WHERE d.declaredBy = :declaredBy")
    List<ActionDependencyEntity> findAllDeclaredBy(@Param("declaredBy") DependencySource declaredBy);

    /** As arestas de uma capacidade, para a poda escopada do registro de tela. */
    @EntityGraph(attributePaths = {"action"})
    @Query("SELECT d FROM ActionDependencyEntity d "
            + "WHERE d.action.id = :actionId AND d.declaredBy = :declaredBy")
    List<ActionDependencyEntity> findByActionIdAndDeclaredBy(@Param("actionId") String actionId,
                                                            @Param("declaredBy") DependencySource declaredBy);

    /**
     * As dependências diretas de um conjunto de capacidades, como pares
     * {@code [idDaOrigem, capacidadeRequerida]}.
     *
     * <p>Projeta em vez de devolver a entidade de propósito. O catálogo é lido fora de transação, e
     * {@code d.action} é {@code LAZY}: navegar a associação para pegar o identificador dependeria de
     * o proxy responder sem ir ao banco — o que vale para o getter do id em algumas configurações e
     * não em outras, e o modo como falha é {@code LazyInitializationException} em produção depois de
     * passar nos testes. O identificador já está na chave estrangeira; pedi-lo direto elimina a
     * dúvida.
     */
    @Query("SELECT d.action.id, d.requiredCapability FROM ActionDependencyEntity d "
            + "WHERE d.action.id IN :actionIds")
    List<Object[]> findCapabilitiesRequiredBy(@Param("actionIds") Collection<String> actionIds);

    /**
     * Todas as arestas com a capacidade de <b>origem</b> resolvida em nomes.
     *
     * <p>Uma consulta, e não uma por capacidade. Quem chama — o relatório de efetivo e o fecho
     * transitivo — precisa cruzar centenas de capacidades de uma vez, e a tabela de arestas é
     * pequena por natureza: uma linha por dependência declarada no código, não por concessão.
     */
    @Query("SELECT d FROM ActionDependencyEntity d JOIN FETCH d.action a JOIN FETCH a.resource")
    List<ActionDependencyEntity> findAllWithOrigin();

    /** Arestas cujo alvo ainda não foi encontrado no catálogo. */
    @Query("SELECT d FROM ActionDependencyEntity d WHERE d.requiredAction IS NULL")
    List<ActionDependencyEntity> findUnresolved();

    @Query("SELECT COUNT(d) FROM ActionDependencyEntity d WHERE d.requiredAction IS NULL")
    long countUnresolved();
}
