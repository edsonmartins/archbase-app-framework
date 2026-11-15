package br.com.archbase.architecture.rules.fixtures.aggregates;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.archbase.ddd.domain.contracts.AggregateRoot;

public class Order implements AggregateRoot<Order, OrderId> {
    // válidos
    List<LineItem> lineItems;
    CustomerId customerId;
    static Customer CACHE_ESTATICO;

    // violações: entidade de outro agregado
    Address shippingAddress;
    Map<String, Address> addressesByType;

    // violações: referência direta a outra raiz
    Customer customer;
    Optional<Customer> maybeCustomer;
    Invoice[] invoices;

    @Override
    public OrderId getId() {
        return null;
    }
}
