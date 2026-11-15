package br.com.archbase.architecture.rules.fixtures.aggregates;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;

public class Customer implements AggregateRoot<Customer, CustomerId> {
    Address address;

    @Override
    public CustomerId getId() {
        return null;
    }
}
