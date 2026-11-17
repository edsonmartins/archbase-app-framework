package br.com.archbase.ddd.domain.contracts;

import java.util.Optional;

public interface InsideAssociationResolver<T extends AggregateRoot<T, ID>, ID extends Identifier>
        extends AggregateLookup<T, ID> {

    default Optional<T> resolve(InsideAssociation<T, ID> insideAssociation) {
        return findById(insideAssociation.getId());
    }

    default T resolveRequired(InsideAssociation<T, ID> insideAssociation) {
        return resolve(insideAssociation).orElseThrow(
                () -> new IllegalArgumentException(String.format("Não foi possível resolver a associação %s!", insideAssociation)));
    }

    /**
     * Carrega o agregado para o qual a {@link Association} aponta.
     *
     * @since 3.4.6
     */
    default Optional<T> resolve(Association<T, ID> association) {
        return findById(association.getId());
    }

    /**
     * Carrega o agregado para o qual a {@link Association} aponta, ou falha se ele não existir.
     *
     * @since 3.4.6
     */
    default T resolveRequired(Association<T, ID> association) {
        return resolve(association).orElseThrow(
                () -> new IllegalArgumentException(String.format("Não foi possível resolver a associação %s!", association)));
    }
}