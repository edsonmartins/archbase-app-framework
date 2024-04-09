package br.com.archbase.query.rsql.querydsl;

import com.querydsl.core.types.Path;
import lombok.Value;

import jakarta.persistence.metamodel.Attribute;

@Value(staticConstructor = "of")
class ArchbaseRSQLQueryDslContext {

    private String propertyPath;
    private Attribute<?, ?> attribute;
    private Path<?> entityClass;

}
