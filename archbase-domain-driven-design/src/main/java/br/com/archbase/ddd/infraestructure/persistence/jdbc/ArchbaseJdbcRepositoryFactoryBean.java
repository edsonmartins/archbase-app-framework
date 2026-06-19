package br.com.archbase.ddd.infraestructure.persistence.jdbc;

import com.querydsl.sql.SQLQueryFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jdbc.core.convert.*;
import org.springframework.data.jdbc.core.convert.QueryMappingConfiguration;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

import java.io.Serializable;


/**
 * Construtor de repositórios JDBC.
 *
 * @author edsonmartins
 */
class ArchbaseJdbcRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends JdbcRepositoryFactoryBean<T, S, ID> {

    private ApplicationEventPublisher publisher;
    private BeanFactory beanFactory;
    private RelationalMappingContext mappingContext;
    private JdbcConverter converter;
    private DataAccessStrategy dataAccessStrategy;
    private QueryMappingConfiguration queryMappingConfiguration = QueryMappingConfiguration.EMPTY;
    private NamedParameterJdbcOperations operations;
    private EntityCallbacks entityCallbacks;
    private SQLQueryFactory sqlQueryFactory;
    private Dialect dialect;

    protected ArchbaseJdbcRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {

        super.setApplicationEventPublisher(publisher);
        this.publisher = publisher;
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {

        ArchbaseJdbcRepositoryFactory jdbcRepositoryFactory = new ArchbaseJdbcRepositoryFactory(dataAccessStrategy,
                mappingContext,
                converter,
                dialect,
                publisher,
                operations,
                sqlQueryFactory
        );
        jdbcRepositoryFactory.setQueryMappingConfiguration(queryMappingConfiguration);
        jdbcRepositoryFactory.setEntityCallbacks(entityCallbacks);

        return jdbcRepositoryFactory;
    }

    @Autowired
    @Override
    public void setMappingContext(RelationalMappingContext mappingContext) {

        super.setMappingContext(mappingContext);
        this.mappingContext = mappingContext;
    }

    @Autowired
    @Override
    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public void setDataAccessStrategy(DataAccessStrategy dataAccessStrategy) {
        super.setDataAccessStrategy(dataAccessStrategy);
        this.dataAccessStrategy = dataAccessStrategy;
    }

    /**
     * @param queryMappingConfiguration can be {@literal null}. {@link #afterPropertiesSet()} defaults to
     *                                  {@link QueryMappingConfiguration#EMPTY} if {@literal null}.
     */
    @Autowired(required = false)
    @Override
    public void setQueryMappingConfiguration(QueryMappingConfiguration queryMappingConfiguration) {
        super.setQueryMappingConfiguration(queryMappingConfiguration);
        this.queryMappingConfiguration = queryMappingConfiguration;
    }

    @Autowired
    public void setSQLQueryFactory(SQLQueryFactory sqlQueryFactory) {
        this.sqlQueryFactory = sqlQueryFactory;
    }

    @Override
    public void setJdbcOperations(NamedParameterJdbcOperations operations) {
        this.operations = operations;
    }

    @Autowired
    @Override
    public void setConverter(JdbcConverter converter) {
        super.setConverter(converter);
        this.converter = converter;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        super.setBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.state(this.mappingContext != null, "MappingContext é obrigatório e não deve ser nulo!");
        Assert.state(this.converter != null, "RelationalConverter é obrigatório e não deve ser nulo!");
        if (this.operations == null) {
            Assert.state(beanFactory != null, "Se nenhuma JdbcOperations for definida, um BeanFactory deve estar disponível.");
            this.operations = (NamedParameterJdbcOperations)this.beanFactory.getBean(NamedParameterJdbcOperations.class);
        }

        if (this.dataAccessStrategy == null) {
            Assert.state(beanFactory != null, "Se nenhuma DataAccessStrategy for definida, um BeanFactory deve estar disponível.");
            this.dataAccessStrategy = (DataAccessStrategy)this.beanFactory.getBeanProvider(DataAccessStrategy.class).getIfAvailable(() -> {
                Assert.state(this.dialect != null, "Dialeto é requerido não deve ser nulo");
                SqlGeneratorSource sqlGeneratorSource = new SqlGeneratorSource(this.mappingContext, this.converter, this.dialect);
                SqlParametersFactory sqlParametersFactory = new SqlParametersFactory(this.mappingContext, this.converter);
                InsertStrategyFactory insertStrategyFactory = new InsertStrategyFactory(this.operations, this.dialect);
                return new DefaultDataAccessStrategy(sqlGeneratorSource, this.mappingContext, this.converter, this.operations, sqlParametersFactory, insertStrategyFactory, this.queryMappingConfiguration);
            });
        }

        if (this.queryMappingConfiguration == null) {
            this.queryMappingConfiguration = QueryMappingConfiguration.EMPTY;
        }

        if (this.beanFactory != null) {
            this.entityCallbacks = EntityCallbacks.create(this.beanFactory);
        }

        super.afterPropertiesSet();

    }
}



