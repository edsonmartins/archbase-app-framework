package br.com.archbase.query.rsql.jpa;

import lombok.Value;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Attribute;

@Value(staticConstructor = "of")
class ArchbaseRSQLJPAContext {

    private Path<?> path;
    private Attribute<?, ?> attribute;

}
