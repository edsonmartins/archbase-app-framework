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
}