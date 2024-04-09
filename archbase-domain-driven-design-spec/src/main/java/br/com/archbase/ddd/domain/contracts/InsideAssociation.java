package br.com.archbase.ddd.domain.contracts;

/**
 * {@link InsideAssociation} é basicamente uma indireção em direção a um identificador de agregado relacionado que serve puramente
 * à expressividade dentro do modelo local limitado ao contexto.
 *
 * @author edsonmartins
 */
public interface InsideAssociation<T extends AggregateRoot<T, ID>, ID extends Identifier> extends Identifiable<ID> {

    public T load();

}
