package br.com.archbase.ddd.domain.contracts;

/**
 * Identifiable é uma interface de marcação para informar que determinados tipos como por exemplo Entidades {@link Entity} são identificáveis.
 * Com isso garantem que associações sejam feitas apenas com objetos identificáveis.
 *
 * @author edsonmartins
 */
public interface Identifiable<ID> {
    ID getId();
}
