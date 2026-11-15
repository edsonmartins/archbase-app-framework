package br.com.archbase.architecture.rules.fixtures.aggregates;

import br.com.archbase.ddd.domain.contracts.Identifier;

public record OrderId(String value) implements Identifier {
}
