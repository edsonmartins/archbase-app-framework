package br.com.archbase.ddd.domain.contracts;

/**
 * Associação explícita a outro {@link AggregateRoot}: guarda só o {@link Identifier} do agregado alvo, sem carregá-lo.
 * É a forma recomendada de um agregado referenciar outro (ver
 * {@code ArchbaseAggregateRules.aggregateReferencesShouldBeViaIdOrAssociation()}).
 * <p>
 * Diferente de {@link InsideAssociation}, não expõe {@code load()}: o domínio não depende de mecanismo de carga.
 * Quem precisa do agregado resolve a associação no repositório, via
 * {@link InsideAssociationResolver#resolve(Association)}.
 * <pre>{@code
 * class Order implements AggregateRoot<Order, OrderId> {
 *     Association<Customer, CustomerId> customer;
 *
 *     Order(Customer customer) {
 *         this.customer = Association.forAggregate(customer);
 *     }
 * }
 * }</pre>
 * Portado do jMolecules ({@code org.jmolecules.ddd.types.Association}).
 *
 * @param <T>  tipo do agregado alvo
 * @param <ID> tipo do identificador do agregado alvo
 * @since 3.4.6
 * @see <a href="https://scabl.blogspot.com/2015/04/aeddd-9.html">John Sullivan - Advancing Enterprise DDD -
 * Reinstating the Aggregate</a>
 */
public interface Association<T extends AggregateRoot<T, ID>, ID extends Identifier> extends Identifiable<ID> {

    /**
     * Associação que aponta para o identificador do agregado informado. O identificador é lido no momento da
     * criação.
     *
     * @param aggregate não pode ser {@literal null}
     */
    static <T extends AggregateRoot<T, ID>, ID extends Identifier> Association<T, ID> forAggregate(T aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("O agregado não pode ser nulo!");
        }
        return forId(aggregate.getId());
    }

    /**
     * Associação que aponta para o identificador informado.
     *
     * @param identifier não pode ser {@literal null}
     */
    static <T extends AggregateRoot<T, ID>, ID extends Identifier> Association<T, ID> forId(ID identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("O identificador não pode ser nulo!");
        }
        return new SimpleAssociation<>(identifier);
    }

    /**
     * Se esta associação aponta para o mesmo agregado que a outra. Compara só os identificadores, ao contrário de
     * {@link #equals(Object)}, que também compara o tipo da associação.
     *
     * @param other não pode ser {@literal null}
     */
    default boolean pointsToSameAggregateAs(Association<?, ID> other) {
        if (other == null) {
            throw new IllegalArgumentException("A outra associação não pode ser nula!");
        }
        return getId().equals(other.getId());
    }

    /**
     * Se esta associação aponta para o agregado com o identificador informado.
     *
     * @param identifier não pode ser {@literal null}
     */
    default boolean pointsTo(ID identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("O identificador não pode ser nulo!");
        }
        return getId().equals(identifier);
    }

    /**
     * Se esta associação aponta para o agregado informado.
     *
     * @param aggregate não pode ser {@literal null}
     */
    default boolean pointsTo(T aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("O agregado não pode ser nulo!");
        }
        return pointsTo(aggregate.getId());
    }
}
