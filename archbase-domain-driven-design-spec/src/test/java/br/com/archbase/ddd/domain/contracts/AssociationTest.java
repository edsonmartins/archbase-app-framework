package br.com.archbase.ddd.domain.contracts;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssociationTest {

    record CustomerId(String value) implements Identifier {
    }

    record Customer(CustomerId id) implements AggregateRoot<Customer, CustomerId> {
        @Override
        public CustomerId getId() {
            return id;
        }
    }

    @Test
    void forAggregateEForIdApontamParaOMesmoAgregado() {
        Customer customer = new Customer(new CustomerId("c1"));

        Association<Customer, CustomerId> byAggregate = Association.forAggregate(customer);
        Association<Customer, CustomerId> byId = Association.forId(new CustomerId("c1"));

        assertEquals(new CustomerId("c1"), byAggregate.getId());
        assertEquals(byAggregate, byId);
        assertEquals(byAggregate.hashCode(), byId.hashCode());
        assertTrue(byAggregate.pointsTo(customer));
        assertTrue(byAggregate.pointsTo(new CustomerId("c1")));
        assertTrue(byAggregate.pointsToSameAggregateAs(byId));
        assertFalse(byAggregate.pointsTo(new CustomerId("c2")));
        assertNotEquals(byAggregate, Association.forId(new CustomerId("c2")));
        assertEquals("CustomerId[value=c1]", byAggregate.toString());
    }

    @Test
    void recusaNulo() {
        assertThrows(IllegalArgumentException.class, () -> Association.forAggregate(null));
        assertThrows(IllegalArgumentException.class, () -> Association.forId(null));
        assertThrows(IllegalArgumentException.class,
                () -> Association.<Customer, CustomerId>forId(new CustomerId("c1")).pointsTo((CustomerId) null));
    }

    @Test
    void resolverCarregaPeloIdentificador() {
        Customer customer = new Customer(new CustomerId("c1"));
        InsideAssociationResolver<Customer, CustomerId> resolver =
                id -> id.equals(customer.getId()) ? Optional.of(customer) : Optional.empty();

        assertSame(customer, resolver.resolveRequired(Association.forId(new CustomerId("c1"))));
        assertTrue(resolver.resolve(Association.<Customer, CustomerId>forId(new CustomerId("c2"))).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveRequired(Association.<Customer, CustomerId>forId(new CustomerId("c2"))));
    }
}
