package br.com.archbase.ddd.domain.contracts;

import java.util.Optional;


public interface AggregateLookup<T extends AggregateRoot<T, ID>, ID extends Identifier> {

    Optional<T> findById(ID id);
}
