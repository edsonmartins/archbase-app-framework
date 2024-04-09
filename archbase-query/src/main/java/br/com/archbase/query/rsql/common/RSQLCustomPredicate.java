package br.com.archbase.query.rsql.common;

import br.com.archbase.query.rsql.parser.ast.ComparisonOperator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.criteria.Predicate;
import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RSQLCustomPredicate<T extends Comparable<?>> {

    private ComparisonOperator operator;
    private Class<T> type;
    private Function<RSQLCustomPredicateInput, Predicate> converter;

}
