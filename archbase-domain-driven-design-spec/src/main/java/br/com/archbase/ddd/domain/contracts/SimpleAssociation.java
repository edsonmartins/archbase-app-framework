package br.com.archbase.ddd.domain.contracts;

import java.io.Serializable;

/**
 * Implementação de {@link Association} devolvida pelas fábricas {@link Association#forId(Identifier)} e
 * {@link Association#forAggregate(AggregateRoot)}. Imutável: guarda o identificador, não o agregado.
 *
 * @since 3.4.6
 */
final class SimpleAssociation<T extends AggregateRoot<T, ID>, ID extends Identifier>
        implements Association<T, ID>, Serializable {

    private static final long serialVersionUID = 1L;

    private final ID identifier;

    SimpleAssociation(ID identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("O identificador não pode ser nulo!");
        }
        this.identifier = identifier;
    }

    @Override
    public ID getId() {
        return identifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(getClass())) {
            return false;
        }
        return identifier.equals(((SimpleAssociation<?, ?>) obj).identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}
