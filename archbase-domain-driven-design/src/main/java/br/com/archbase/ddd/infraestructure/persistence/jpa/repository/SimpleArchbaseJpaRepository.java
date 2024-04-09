package br.com.archbase.ddd.infraestructure.persistence.jpa.repository;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.contracts.Identifier;
import br.com.archbase.ddd.domain.contracts.InsideAssociation;
import br.com.archbase.ddd.domain.specification.ArchbaseSpecification;
import br.com.archbase.ddd.infraestructure.persistence.jpa.specification.SpecificationTranslator;
import br.com.archbase.query.rsql.jpa.ArchbaseRSQLJPASupport;
import com.querydsl.core.types.*;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.AbstractJPAQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.querydsl.jpa.sql.JPASQLQuery;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.SQLTemplatesRegistry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.*;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.order.AuditOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.envers.repository.support.DefaultRevisionMetadata;
import org.springframework.data.history.*;
import org.springframework.data.jpa.repository.support.*;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.data.history.RevisionMetadata.RevisionType.*;

/**
 * Implementação base do Repositório padrão a ser usada na camada de dominio do negócio para manipulação das entidades.
 *
 * @param <T>  tipo de entidade
 * @param <ID> tipo de identificador da entidade
 * @param <N>  Tipo numérico
 * @author edsonmartins
 */
@SuppressWarnings("all")
public class SimpleArchbaseJpaRepository<T extends AggregateRoot<T, ID>, ID extends Serializable & Identifier, N extends Number & Comparable<N>> extends SimpleJpaRepository<T, ID>
        implements ArchbaseJpaRepository<T, ID, N>, QuerydslPredicateExecutor<T> {

    private final EntityPath<T> path;
    private final JPAQueryFactory jpaQueryFactory;
    private final Supplier<JPASQLQuery<T>> jpaSqlFactory;
    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ID> entityInformation;
    private final QuerydslPredicateExecutor<T> querydslPredicateExecutor;
    private final Querydsl querydsl;
    private SpecificationTranslator translator;


    public SimpleArchbaseJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager, EntityPath<T> path, JPAQueryFactory jpaQueryFactory, Supplier<JPASQLQuery<T>> jpaSqlFactory, EntityManager entityManager1, JpaEntityInformation<T, ID> entityInformation1, QuerydslPredicateExecutor<T> querydslPredicateExecutor, Querydsl querydsl) {
        super(entityInformation, entityManager);
        this.path = path;
        this.jpaQueryFactory = jpaQueryFactory;
        this.jpaSqlFactory = jpaSqlFactory;
        this.entityManager = entityManager1;
        this.entityInformation = entityInformation1;
        this.querydslPredicateExecutor = querydslPredicateExecutor;
        this.querydsl = querydsl;
    }

    public SimpleArchbaseJpaRepository(Class<T> domainClass, EntityManager em, EntityPath<T> path, JPAQueryFactory jpaQueryFactory, Supplier<JPASQLQuery<T>> jpaSqlFactory, EntityManager entityManager, JpaEntityInformation<T, ID> entityInformation, QuerydslPredicateExecutor<T> querydslPredicateExecutor, Querydsl querydsl) {
        super(domainClass, em);
        this.path = path;
        this.jpaQueryFactory = jpaQueryFactory;
        this.jpaSqlFactory = jpaSqlFactory;
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
        this.querydslPredicateExecutor = querydslPredicateExecutor;
        this.querydsl = querydsl;
    }

    @SafeVarargs
    @Override
    public final List<T> save(T... iterable) {
        return saveAll(Arrays.asList(iterable));
    }

    @Override
    public List<T> findAll(Predicate predicate) {
        return querydslPredicateExecutor.findAll(predicate);
    }

    @Override
    public List<T> findAll(Predicate predicate, Sort sort) {
        return querydslPredicateExecutor.findAll(predicate, sort);
    }

    @Override
    public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orderSpecifiers) {
        return querydslPredicateExecutor.findAll(predicate, orderSpecifiers);
    }

    @Override
    public List<T> findAll(OrderSpecifier<?>... orderSpecifiers) {
        return querydslPredicateExecutor.findAll(orderSpecifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> matching(ArchbaseSpecification<T> archbaseSpecification) {
        return buildQuery(archbaseSpecification).getResultList();
    }

    /**
     * Construa uma nova consulta digitada, que seleciona todas as entidades que
     * satisfazem uma archbaseSpecification.
     *
     * @param archbaseSpecification descreve o que nossas entidades retornadas devem corresponder
     * @return consulta que é capaz de recuperar todas as entidades correspondentes
     */
    private TypedQuery<T> buildQuery(ArchbaseSpecification<T> archbaseSpecification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityInformation.getJavaType());
        Root<T> root = cq.from(entityInformation.getJavaType());
        cq.where(translator.translateToPredicate(archbaseSpecification, root, cq, cb));
        return entityManager.createQuery(cq);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean containsAny(ArchbaseSpecification<T> archbaseSpecification) {
        return !buildQuery(archbaseSpecification).setMaxResults(1).getResultList().isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<T> findAll(String filter, Pageable pageable) {
        return this.findAll(ArchbaseRSQLJPASupport.rsql(filter), pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long howMany(ArchbaseSpecification<T> archbaseSpecification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<T> root = cq.from(entityInformation.getJavaType());
        cq.select(cb.count(root));
        cq.where(translator.translateToPredicate(archbaseSpecification, root, cq, cb));
        return entityManager.createQuery(cq).getSingleResult();
    }

    @Override
    public <O> O query(Function<JPAQuery<?>, O> query) {
        return query.apply(jpaQueryFactory.query());
    }

    @Override
    public void update(Consumer<JPAUpdateClause> update) {
        update.accept(jpaQueryFactory.update(path));
    }

    @Override
    public long deleteWhere(Predicate predicate) {
        return jpaQueryFactory.delete(path).where(predicate).execute();
    }

    @Override
    public <O> O jpaSqlQuery(Function<JPASQLQuery<T>, O> query) {
        return query.apply(jpaSqlFactory.get());
    }

    @Override
    public SubQueryExpression<T> jpaSqlSubQuery(Function<JPASQLQuery<T>, SubQueryExpression<T>> query) {
        return jpaSqlQuery(query);
    }


    @Override
    public <O> O executeStoredProcedure(String name, Function<StoredProcedureQueryBuilder, O> query) {
        return query.apply(new StoredProcedureQueryBuilder(name, entityManager));
    }

    private SQLTemplates getSQLServerTemplates(EntityManagerFactory entityManagerFactory) throws SQLException {
        DatabaseMetaData databaseMetaData = getDatabaseMetaData(entityManagerFactory.createEntityManager());
        return new SQLTemplatesRegistry().getTemplates(databaseMetaData);
    }

    private DatabaseMetaData getDatabaseMetaData(EntityManager entityManager) throws SQLException {
        SessionImplementor sessionImplementor = entityManager.unwrap(SessionImplementor.class);
        DatabaseMetaData metaData = sessionImplementor.unwrap(Connection.class).getMetaData();
        entityManager.close();
        return metaData;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.history.RevisionRepository#findLastChangeRevision(java.io.Serializable)
     */
    @SuppressWarnings("unchecked")
    public Optional<Revision<N, T>> findLastChangeRevision(ID id) {

        List<Object[]> singleResult = createBaseQuery(id) //
                .addOrder(AuditEntity.revisionProperty("timestamp").desc()) //
                .setMaxResults(1) //
                .getResultList();

        Assert.state(singleResult.size() <= 1, "Esperamos no máximo um resultado.");

        if (singleResult.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(createRevision(new QueryResult<>(singleResult.get(0))));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.envers.repository.support.EnversRevisionRepository#findRevision(java.io.Serializable, java.lang.Number)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<Revision<N, T>> findRevision(ID id, N revisionNumber) {

        Assert.notNull(id, "O identificador não deve ser nulo!");
        Assert.notNull(revisionNumber, "O número da revisão não deve ser nulo!");

        List<Object[]> singleResult = createBaseQuery(id) //
                .add(AuditEntity.revisionNumber().eq(revisionNumber)) //
                .getResultList();

        Assert.state(singleResult.size() <= 1, "Esperamos no máximo um resultado.");

        if (singleResult.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(createRevision(new QueryResult<>(singleResult.get(0))));
    }

    @SuppressWarnings("unchecked")
    public Revisions<N, T> findRevisions(ID id) {

        List<Object[]> resultList = createBaseQuery(id).getResultList();
        List<Revision<N, T>> revisionList = new ArrayList<>(resultList.size());

        for (Object[] objects : resultList) {
            revisionList.add(createRevision(new QueryResult<>(objects)));
        }

        return Revisions.of(revisionList);
    }

    @SuppressWarnings("unchecked")
    public Page<Revision<N, T>> findRevisions(ID id, Pageable pageable) {

        AuditOrder sorting = RevisionSort.getRevisionDirection(pageable.getSort()).isDescending() //
                ? AuditEntity.revisionNumber().desc() //
                : AuditEntity.revisionNumber().asc();

        List<Object[]> resultList = createBaseQuery(id) //
                .addOrder(sorting) //
                .setFirstResult((int) pageable.getOffset()) //
                .setMaxResults(pageable.getPageSize()) //
                .getResultList();

        Long count = (Long) createBaseQuery(id) //
                .addProjection(AuditEntity.revisionNumber().count()).getSingleResult();

        List<Revision<N, T>> revisions = new ArrayList<>();

        for (Object[] singleResult : resultList) {
            revisions.add(createRevision(new QueryResult<>(singleResult)));
        }

        return new PageImpl<>(revisions, pageable, count);
    }

    private AuditQuery createBaseQuery(ID id) {

        Class<T> type = entityInformation.getJavaType();
        AuditReader reader = AuditReaderFactory.get(entityManager);

        return reader.createQuery() //
                .forRevisionsOfEntity(type, false, true) //
                .add(AuditEntity.id().eq(id));
    }

    @SuppressWarnings("unchecked")
    private Revision<N, T> createRevision(QueryResult<T> queryResult) {
        return Revision.of((RevisionMetadata<N>) queryResult.createRevisionMetadata(), queryResult.entity);
    }

    @Override
    public Optional findById(Identifier identifier) {
        return Optional.empty();
    }

    @Override
    @NonNull
    public <P> Optional<P> findOne(@NonNull JPQLQuery<P> query) {
        return Optional.ofNullable(query.fetchFirst());
    }

    @Override
    @NonNull
    public <P> Optional<P> findOne(@NonNull FactoryExpression<P> factoryExpression, @NonNull Predicate predicate) {
        JPQLQuery<P> query = createQuery(factoryExpression, predicate);
        return findOne(query);
    }

    @Override
    @NonNull
    public <P> List<P> findAll(@NonNull JPQLQuery<P> query) {
        return query.fetch();
    }

    @Override
    @NonNull
    public <P> Page<P> findAll(@NonNull JPQLQuery<P> query, @NonNull Pageable pageable) {
        return getPage(query, query, pageable);
    }

    @Override
    @NonNull
    public <P> List<P> findAll(@NonNull FactoryExpression<P> factoryExpression, @NonNull Predicate predicate) {
        JPQLQuery<P> query = createQuery(factoryExpression, predicate);
        return findAll(query);
    }

    @Override
    @NonNull
    public <P> Page<P> findAll(@NonNull FactoryExpression<P> factoryExpression, @NonNull Predicate predicate, @NonNull Pageable pageable) {
        JPQLQuery<P> query = createQuery(factoryExpression, predicate);
        JPQLQuery<?> countQuery = querydslPredicateExecutor.createCountQuery(predicate);
        return getPage(query, countQuery, pageable);
    }

    @Override
    @NonNull
    public Optional<T> findOne(@NonNull Predicate predicate) {
        return querydslPredicateExecutor.findOne(predicate);
    }

    @Override
    @NonNull
    public Page<T> findAll(@NonNull Predicate predicate, @NonNull Pageable pageable) {
        return querydslPredicateExecutor.findAll(predicate, pageable);
    }

    @Override
    public long count(@NonNull Predicate predicate) {
        return querydslPredicateExecutor.count(predicate);
    }

    @Override
    public boolean exists(@NonNull Predicate predicate) {
        return querydslPredicateExecutor.exists(predicate);
    }

    @Override
    public <S extends T, R> R findBy(Predicate predicate, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    private <P> JPQLQuery<P> createQuery(FactoryExpression<P> factoryExpression, Predicate predicate) {
        return querydslPredicateExecutor
                .createQuery(predicate)
                .select(factoryExpression);
    }

    private <P> Page<P> getPage(JPQLQuery<P> query, JPQLQuery<?> countQuery, Pageable pageable) {
        JPQLQuery<P> paginatedQuery = querydsl.applyPagination(pageable, query);
        return PageableExecutionUtils.getPage(paginatedQuery.fetch(), pageable, countQuery::fetchCount);
    }

    public void setTranslator(SpecificationTranslator translator) {
        this.translator = translator;
    }

    @Override
    public Optional resolve(InsideAssociation insideAssociation) {
        return ArchbaseJpaRepository.super.resolve(insideAssociation);
    }

    @Override
    public AggregateRoot resolveRequired(InsideAssociation insideAssociation) {
        return ArchbaseJpaRepository.super.resolveRequired(insideAssociation);
    }

    @Override
    public void deleteInBatch(Iterable<T> entities) {
        super.deleteInBatch(entities);
    }

    @SuppressWarnings("unchecked")
    static class QueryResult<T> {

        private final T entity;
        private final Object metadata;
        private final RevisionMetadata.RevisionType revisionType;

        QueryResult(Object[] data) {

            Assert.notNull(data, "Os dados não devem ser nulos");
            Assert.isTrue( //
                    data.length == 3, //
                    () -> String.format("Os dados devem ter tamanho 3, mas têm tamanho %d.", data.length));
            Assert.isTrue( //
                    data[2] instanceof RevisionType, //
                    () -> String.format("O terceiro elemento da matriz deve ser do tipo Revisão, mas é do tipo %s",
                            data[2].getClass()));

            entity = (T) data[0];
            metadata = data[1];
            revisionType = convertRevisionType((RevisionType) data[2]);
        }

        private static RevisionMetadata.RevisionType convertRevisionType(RevisionType datum) {

            switch (datum) {

                case ADD:
                    return INSERT;
                case MOD:
                    return UPDATE;
                case DEL:
                    return DELETE;
                default:
                    return UNKNOWN;
            }
        }

        RevisionMetadata createRevisionMetadata() {

            return metadata instanceof DefaultRevisionEntity //
                    ? new DefaultRevisionMetadata((DefaultRevisionEntity) metadata, revisionType) //
                    : new AnnotationRevisionMetadata<>(metadata, RevisionNumber.class, RevisionTimestamp.class, revisionType);
        }
    }

    private static class QuerydslPredicateExecutor<T> extends QuerydslJpaPredicateExecutor<T> {
        QuerydslPredicateExecutor(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager, EntityPathResolver resolver, CrudMethodMetadata metadata) {
            super(entityInformation, entityManager, resolver, metadata);
        }

        @Override
        @NonNull
        public JPQLQuery<?> createCountQuery(Predicate... predicate) {
            return super.createCountQuery(predicate);
        }

        @Override
        @NonNull
        public AbstractJPAQuery<?, ?> createQuery(Predicate... predicate) {
            return super.createQuery(predicate);
        }
    }

}