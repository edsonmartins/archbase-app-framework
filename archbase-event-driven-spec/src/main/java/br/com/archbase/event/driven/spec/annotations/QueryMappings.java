package br.com.archbase.event.driven.spec.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface QueryMappings {
    QueryMapping[] value();
}