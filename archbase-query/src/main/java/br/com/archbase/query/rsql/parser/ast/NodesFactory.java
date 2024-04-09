package br.com.archbase.query.rsql.parser.ast;


import br.com.archbase.query.rsql.parser.UnknownOperatorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fábrica que cria instâncias de {@link Node} para o analisador.
 */
public class NodesFactory {

    private final Map<String, ComparisonOperator> comparisonOperators;


    public NodesFactory(Set<ComparisonOperator> operators) {

        comparisonOperators = new HashMap<>(operators.size());
        for (ComparisonOperator op : operators) {
            for (String sym : op.getSymbols()) {
                comparisonOperators.put(sym, op);
            }
        }
    }

    /**
     * Cria uma instância {@link LogicalNode} específica para o operador especificado e com o
     * determinados nós filhos.
     *
     * @param operator O operador lógico para o qual criar um nó.
     * @param children Nós filhos, ou seja, operandos.
     * @return Uma subclasse de {@link LogicalNode} de acordo com o operador especificado.
     */
    public LogicalNode createLogicalNode(LogicalOperator operator, List<Node> children) {
        switch (operator) {
            case AND:
                return new AndNode(children);
            case OR:
                return new OrNode(children);

            // this normally can't happen
            default:
                throw new IllegalStateException("Operador desconhecido: " + operator);
        }
    }

    /**
     * Cria uma instância {@link ComparisonNode} com os parâmetros fornecidos.
     *
     * @param operatorToken Uma representação textual do operador de comparação a ser encontrado no
     *                      conjunto de {@linkplain ComparisonOperator operator} compatível.
     * @param selector      O seletor que especifica o lado esquerdo da comparação.
     * @param arguments     Uma lista de argumentos que especifica o lado direito da comparação.
     * @throws UnknownOperatorException Se nenhum operador para o token de operador especificado existir.
     */
    public ComparisonNode createComparisonNode(
            String operatorToken, String selector, List<String> arguments) {

        ComparisonOperator op = comparisonOperators.get(operatorToken);
        if (op != null) {
            return new ComparisonNode(op, selector, arguments);
        } else {
            throw new UnknownOperatorException(operatorToken);
        }
    }
}
