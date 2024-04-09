package br.com.archbase.query.rsql.parser.ast;

public enum LogicalOperator {

    AND(";"),
    OR(",");

    private final String symbol;

    private LogicalOperator(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
