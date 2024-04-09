/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.archbase.ddd.infraestructure.persistence.jdbc;

import com.google.common.base.CaseFormat;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQueryFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ResolvableType;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Construtor de repositórios JDBC.
 *
 * @author edsonmartins
 */
@SuppressWarnings("all")
class ArchbaseJdbcRepositoryFactory extends org.springframework.data.jdbc.repository.support.JdbcRepositoryFactory {

    private final RelationalMappingContext context;
    private final JdbcConverter converter;
    private final ApplicationEventPublisher publisher;
    private final DataAccessStrategy accessStrategy;
    private final SQLQueryFactory sqlQueryFactory;
    private EntityCallbacks entityCallbacks;

    public ArchbaseJdbcRepositoryFactory(DataAccessStrategy dataAccessStrategy,
                                         RelationalMappingContext context,
                                         JdbcConverter converter,
                                         Dialect dialect, ApplicationEventPublisher publisher,
                                         NamedParameterJdbcOperations operations,
                                         SQLQueryFactory sqlQueryFactory) {
        super(dataAccessStrategy, context, converter, dialect, publisher, operations);
        this.publisher = publisher;
        this.context = context;
        this.converter = converter;
        this.accessStrategy = dataAccessStrategy;
        this.sqlQueryFactory = sqlQueryFactory;
    }

    @Override
    public void setEntityCallbacks(EntityCallbacks entityCallbacks) {
        super.setEntityCallbacks(entityCallbacks);
        this.entityCallbacks = entityCallbacks;
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        return SimpleArchbaseJdbcRepository.class;
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation repositoryInformation) {

        JdbcAggregateTemplate template = new JdbcAggregateTemplate(publisher, context, converter, accessStrategy);

        Class<?> type = repositoryInformation.getDomainType();
        RelationalPath<?> relationalPathBase = getRelationalPathBase(repositoryInformation);
        ConstructorExpression<?> constructor = getConstructorExpression(type, relationalPathBase);
        SimpleArchbaseJdbcRepository<?, ?> repository = new SimpleArchbaseJdbcRepository(template,
                context.getRequiredPersistentEntity(
                        type),
                sqlQueryFactory,
                constructor,
                relationalPathBase, converter);

        if (entityCallbacks != null) {
            template.setEntityCallbacks(entityCallbacks);
        }

        return repository;
    }

    private ConstructorExpression<?> getConstructorExpression(Class<?> type, RelationalPath<?> pathBase) {
        PreferredConstructor<?, ?> constructor = PreferredConstructorDiscoverer.discover(type);

        if (constructor == null) {
            throw new IllegalArgumentException(
                    "Não foi possível descobrir o construtor preferido para " + type);
        }

        Map<String, Path<?>> columnNameToColumn = pathBase.getColumns()
                .stream()
                .collect(Collectors.toMap(
                        column -> column.getMetadata().getName(),
                        Function.identity()));

        Path<?>[] paths = constructor.getParameters()
                .stream()
                .map(Parameter::getName)
                .map(columnNameToColumn::get)
                .toArray(Path[]::new);

        return Projections.constructor(type, paths);
    }

    private RelationalPathBase<?> getRelationalPathBase(RepositoryInformation repositoryInformation) {

        ResolvableType entityType = ResolvableType.forClass(repositoryInformation.getRepositoryInterface())
                .as(ArchbaseJdbcRepository.class)
                .getGeneric(0);
        if (entityType == null || entityType.getRawClass() == null) {
            throw new IllegalArgumentException("Não foi possível resolver a classe de consulta para " + repositoryInformation);
        }

        return getRelationalPathBase(getQueryClass(entityType.getRawClass()));
    }

    private Class<?> getQueryClass(Class<?> entityType) {
        String fullName = entityType.getPackage().getName() + ".Q" + entityType.getSimpleName();
        try {
            return entityType.getClassLoader().loadClass(fullName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Incapaz de carregar classe " + fullName);
        }
    }

    private RelationalPathBase<?> getRelationalPathBase(Class<?> queryClass) {
        String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, queryClass.getSimpleName().substring(1));
        Field field = ReflectionUtils.findField(queryClass, fieldName);

        if (field == null) {
            throw new IllegalArgumentException("Não encontrou um campo estático do mesmo tipo em " + queryClass);
        }

        return (RelationalPathBase<?>) ReflectionUtils.getField(field, null);
    }
}
