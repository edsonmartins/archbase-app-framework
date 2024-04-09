package br.com.archbase.ddd.infraestructure.persistence.jpa.repository;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.contracts.Identifier;
import br.com.archbase.ddd.domain.contracts.Repository;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.FactoryExpression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.querydsl.jpa.sql.JPASQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contrato do Reposiório base JPA.
 *
 * @param <T>  tipo de entidade
 * @param <ID> tipo de identificador da entidade
 * @param <N>  Tipo numérico
 * @author edsonmartins
 */

@NoRepositoryBean
public interface ArchbaseJpaRepository<T extends AggregateRoot<T, ID>, ID extends Identifier, N extends Number & Comparable<N>> extends JpaSpecificationExecutor<T>, Repository<T, ID, N> {

    /**
     * @see JPQLQueryFactory#query()
     */
    <O> O query(Function<JPAQuery<?>, O> query);

    /**
     * @see JPQLQueryFactory#update(EntityPath)
     */
    void update(Consumer<JPAUpdateClause> update);

    /**
     * Exclui todas as entidades que correspondem ao {@link Predicate} fornecido.
     *
     * @param predicate para combinar
     * @return quantidade de linhas afetadas
     */
    long deleteWhere(Predicate predicate);

    <O> O jpaSqlQuery(Function<JPASQLQuery<T>, O> query);

    SubQueryExpression<T> jpaSqlSubQuery(Function<JPASQLQuery<T>, SubQueryExpression<T>> query);

    <O> O executeStoredProcedure(String name, Function<StoredProcedureQueryBuilder, O> query);

    <P> Optional<P> findOne(@NonNull JPQLQuery<P> query);

    <P> Optional<P> findOne(@NonNull FactoryExpression<P> factoryExpression, @NonNull Predicate predicate);

    <P> List<P> findAll(@NonNull JPQLQuery<P> query);

    <P> Page<P> findAll(@NonNull JPQLQuery<P> query, @NonNull Pageable pageable);

    <P> List<P> findAll(@NonNull FactoryExpression<P> factoryExpression, @NonNull Predicate predicate);

    <P> Page<P> findAll(@NonNull FactoryExpression<P> factoryExpression, @NonNull Predicate predicate, @NonNull Pageable pageable);
}