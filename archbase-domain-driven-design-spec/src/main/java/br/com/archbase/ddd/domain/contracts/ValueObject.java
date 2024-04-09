package br.com.archbase.ddd.domain.contracts;

import java.io.Serializable;

/**
 * Alguns objetos descrevem ou calculam alguma característica de uma coisa. Muitos objetos não têm identidade conceitual.
 * Rastrear a identidade de entidades é essencial, mas anexar identidade a outros objetos pode prejudicar
 * desempenho do sistema, adicionar trabalho analítico e confundir o modelo fazendo com que todos os objetos pareçam
 * o mesmo. Por isso objetos de valor não devem ter identidade e ciclo de vida. As implementações devem ser imutáveis,
 * as operações não têm efeitos colaterais.
 *
 * @author edsonmartins
 */
public interface ValueObject extends Serializable {
}