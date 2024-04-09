package br.com.archbase.ddd.domain.contracts;

import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.history.RevisionRepository;

import java.util.List;

/**
 * Repositórios são classes ou componentes que encapsulam a lógica necessária para acessar fontes de dados.
 * Eles centralizam a funcionalidade comum de acesso a dados, melhorando a sustentabilidade e desacoplando a
 * infraestrutura ou a tecnologia usada para acessar os bancos de dados da camada do modelo de domínio.
 * Um repositório executa as tarefas de um intermediário entre as camadas de modelo de domínio e o mapeamento
 * de dados, funcionando de maneira semelhante a um conjunto de objetos de domínio na memória. Os objetos de
 * clientes criam consultas de forma declarativa e enviam-nas para os repositórios buscando respostas.
 * Conceitualmente, um repositório encapsula um conjunto de objetos armazenados no banco de dados e as operações
 * que podem ser executadas neles, fornecendo uma maneira que é mais próxima da camada de persistência.
 * Os repositórios também oferecem a capacidade de separação, de forma clara e em uma única direção, a dependência
 * entre o domínio de trabalho e a alocação de dados ou o mapeamento.
 *
 * @param <T>  Tipo de entidade
 * @param <ID> Tipo de ID da entidade
 * @author edsonmartins
 */
@SuppressWarnings("rawtypes")
public interface Repository<T, ID, N extends Number & Comparable<N>> extends QuerydslPredicateExecutor<T>, PagingAndSortingRepository<T, ID>,
        RevisionRepository<T, ID, N>, InsideAssociationResolver, CrudRepository<T, ID> {

    /**
     * Recupera todas as entidades
     *
     * @return Lista de entidades
     */
    List<T> findAll();

    /**
     * Recupera todas as entidades com ordenação.
     *
     * @param sort ordenação
     * @return Lista de entidades
     */
    List<T> findAll(Sort sort);

    /**
     * Recupera todas as entidades de estão na lista de ID's
     * passadas como parâmetro.
     *
     * @param ids Lista de ID's
     * @return Lista de entidades
     */
    List<T> findAllById(Iterable<ID> ids);

    /**
     * Salva uma lista de entidades
     *
     * @param entities Entidades para salvar
     * @param <S>      Tipo de entidade
     * @return Lista de entidades salvas.
     */
    <S extends T> List<S> saveAll(Iterable<S> entities);

    /**
     * Libera todas as alterações pendentes no banco de dados.
     */
    void flush();

    /**
     * Salva uma entidade e elimina as alterações instantaneamente.
     *
     * @param entity
     * @return a entidade salva
     */
    <S extends T> S saveAndFlush(S entity);

    /**
     * Exclui as entidades fornecidas em um lote, o que significa que criará um único comando.
     *
     * @param entities
     */
    void deleteInBatch(Iterable<T> entities);

    /**
     * Exclui todas as entidades em uma chamada em lote.
     */
    void deleteAllInBatch();

    /**
     * Retorna uma referência à entidade com o identificador fornecido.
     *
     * @param id não deve ser {@literal null}.
     * @return uma referência à entidade com o identificador fornecido.
     */
    T getOne(ID id);

    /**
     * Salva uma lista de entidades
     *
     * @param iterable Lista de entidades
     * @return Lista de entidades salvas
     */
    List<T> save(T... iterable);

    /**
     * Recupera todas as entidades que atendam o predicado.
     *
     * @param predicate Predicado.
     * @return Lista de entidades
     */
    @Override
    List<T> findAll(Predicate predicate);

    /**
     * Recupera todas as entidades que atendam o predicado e retorna
     * de forma ordenada.
     *
     * @param predicate Predicado
     * @param sort      Ordenação
     * @return Lista de entidades ordenada.
     */
    @Override
    List<T> findAll(Predicate predicate, Sort sort);

    /**
     * Recupera todas as entidades que atendam o predicado e retorna
     * de forma ordenada.
     *
     * @param predicate
     * @param orderSpecifiers
     * @return Lista de entidades ordenada.
     */
    @Override
    List<T> findAll(Predicate predicate, OrderSpecifier<?>... orderSpecifiers);

    /**
     * Recupera todas as entidades de forma ordenada.
     *
     * @param orderSpecifiers Ordenação
     * @return Lista de entidades ordenadas
     */
    @Override
    List<T> findAll(OrderSpecifier<?>... orderSpecifiers);

    /**
     * Recupera todas as entidades que correspondem a uma especificação.
     *
     * @param archbaseSpecification
     * @return lista de entidades
     */
    List<T> matching(ArchbaseSpecification<T> archbaseSpecification);

    /**
     * Conte quantas entidades correspondem a uma especificação.
     *
     * @param archbaseSpecification
     * @return quantidade de entidades
     */
    Long howMany(ArchbaseSpecification<T> archbaseSpecification);

    /**
     * Determine se alguma de nossas entidades corresponde a uma especificação.
     *
     * @param archbaseSpecification
     * @return true se alguma atende
     */
    Boolean containsAny(ArchbaseSpecification<T> archbaseSpecification);

    /**
     * Recupera todos os objetos que atendam ao filtro
     *
     * @param filter   Filtro
     * @param pageable Configuração de página
     * @return Página
     */
    Page<T> findAll(String filter, Pageable pageable);
}
