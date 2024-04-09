package br.com.archbase.query.rsql.common;

import lombok.Value;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import java.util.List;

@Value(staticConstructor = "of")
public class RSQLCustomPredicateInput {

    private CriteriaBuilder criteriaBuilder;
    private Path<?> path;
    private List<Object> arguments;

}
