package br.com.archbase.ddd.domain.contracts;

import java.io.Serializable;

/**
 * Identifier é apenas uma interface de marcação para equipar os tipos de identificadores. Isso encoraja tipos dedicados
 * a descrever identificadores. A intenção principal disso é evitar que cada entidade seja identificada por um tipo comum
 * (como Long ou UUID). Embora possa parecer uma boa ideia do ponto de vista da persistência, é fácil misturar um
 * identificador de uma Entidade com o identificador de outra. Os tipos de identificadores explícitos evitam esse problema.
 *
 * @author edsonmartins
 */
public interface Identifier extends Serializable {

}
