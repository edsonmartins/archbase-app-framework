package br.com.archbase.event.driven.spec.annotations;


import br.com.archbase.event.driven.spec.query.contracts.Query;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(QueryMappings.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryMapping {
    Class<? extends Query> value();
}