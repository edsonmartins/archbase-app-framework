package br.com.archbase.architecture.rules.fixtures.aggregates;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;
import br.com.archbase.ddd.domain.contracts.Entity;

/** Base genérica intermediária: o agregado dono só se resolve descendo a hierarquia. */
public abstract class BaseEntity<T extends AggregateRoot<T, ?>> implements Entity<T, String> {
    @Override
    public String getId() {
        return null;
    }
}
