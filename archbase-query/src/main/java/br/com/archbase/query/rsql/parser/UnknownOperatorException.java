package br.com.archbase.query.rsql.parser;

/**
 * Essa exceção é lançada quando o operador de comparação desconhecido / sem suporte é analisado.
 */
public class UnknownOperatorException extends RuntimeException {

    private final String operator;


    public UnknownOperatorException(String operator) {
        this(operator, "Operador desconhecido: " + operator);
    }

    public UnknownOperatorException(String operator, String message) {
        super(message);
        this.operator = operator;
    }


    public String getOperator() {
        return operator;
    }
}
