// $Id:$
package br.com.archbase.validation.constraints;

/**
 * O Enum {@code CompositionType} que é usado como argumento para a anotação {@code ConstraintComposition}.
 */
public enum CompositionType {
    /**
     * Usado para indicar a disjunção de todas as restrições às quais se aplica.
     */
    OR,

    /**
     * Usado para indicar a conjunção de todas as restrições às quais é aplicado.
     */
    AND,

    /**
     * ALL_FALSE é uma generalização do operador NOT usual, que é aplicado a
     * uma lista de condições em vez de apenas um elemento.
     * Quando a anotação em que é usada é composta por uma única anotação de restrição, é equivalente a NOT.
     */
    ALL_FALSE
}


