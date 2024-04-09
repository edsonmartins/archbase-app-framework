package br.com.archbase.ddd.infraestructure.persistence.jdbc;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLUpdateClause;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contrato para Repositório com JDBC.
 *
 * @param <T>  Tipo de classe
 * @param <ID> Tipo de ID
 * @author edsonmartins
 */
@NoRepositoryBean
public interface ArchbaseJdbcRepository<T, ID> extends CrudRepository<T, ID> {

    List<T> save(T... iterable);

    @Override
    <S extends T> List<S> saveAll(Iterable<S> entities);

    Optional<T> findOne(Predicate predicate);

    @Override
    List<T> findAll();

    List<T> findAll(Predicate predicate);

    <O> O query(Function<SQLQuery<?>, O> query);

    void update(Consumer<SQLUpdateClause> update);

    /**
     * Exclui todas as entidades que correspondem ao dado {@link Predicate}.
     *
     * @param predicate para combinar
     * @return quantidade de linhas afetadas
     */
    long deleteWhere(Predicate predicate);

    /**
     * Retorna a projeção da entidade usada para mapear {@code QT} para {@code T}.
     *
     * @return projeção de entidade
     */
    Expression<T> entityProjection();
}