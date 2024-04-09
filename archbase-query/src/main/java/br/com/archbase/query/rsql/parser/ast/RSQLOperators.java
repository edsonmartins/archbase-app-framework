package br.com.archbase.query.rsql.parser.ast;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

@SuppressWarnings("all")
public abstract class RSQLOperators {

    public static final AtomicReference<ComparisonOperator>
            EQUAL = new AtomicReference<>(new ComparisonOperator("=="));
    public static final ComparisonOperator NOT_EQUAL = new ComparisonOperator("!=");
    public static final ComparisonOperator GREATER_THAN = new ComparisonOperator("=gt=", ">");
    public static final ComparisonOperator GREATER_THAN_OR_EQUAL = new ComparisonOperator("=ge=", ">=");
    public static final ComparisonOperator LESS_THAN = new ComparisonOperator("=lt=", "<");
    public static final ComparisonOperator LESS_THAN_OR_EQUAL = new ComparisonOperator("=le=", "<=");
    public static final ComparisonOperator IN = new ComparisonOperator("=in=", true);
    public static final ComparisonOperator NOT_IN = new ComparisonOperator("=out=", true);
    private RSQLOperators() {
    }

    public static Set<ComparisonOperator> defaultOperators() {
        return new HashSet(asList(EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL,
                LESS_THAN, LESS_THAN_OR_EQUAL, IN, NOT_IN));
    }
}
