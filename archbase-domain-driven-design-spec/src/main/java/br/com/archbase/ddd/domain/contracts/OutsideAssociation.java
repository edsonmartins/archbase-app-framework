package br.com.archbase.ddd.domain.contracts;

import java.io.Serializable;

/**
 * {@link OutsideAssociation} é basicamente uma indireção em direção a um identificador de agregado relacionado em um contexto externo que serve puramente
 * à expressividade dentro do modelo.
 *
 * @author edsonmartins
 */
public interface OutsideAssociation<T, ID extends Identifier> extends Serializable {

    public T load();

    public ID getId();

}
