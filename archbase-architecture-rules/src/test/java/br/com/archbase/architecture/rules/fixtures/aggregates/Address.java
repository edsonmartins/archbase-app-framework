package br.com.archbase.architecture.rules.fixtures.aggregates;

import br.com.archbase.ddd.domain.contracts.Entity;

public class Address implements Entity<Customer, String> {
    @Override
    public String getId() {
        return null;
    }
}
