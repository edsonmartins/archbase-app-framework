package br.com.archbase.architecture.rules.fixtures.onion.domainservice;

import br.com.archbase.architecture.rules.fixtures.onion.model.Pedido;

/** Domain service -> Domain model: válido. */
public interface Pedidos {
    Pedido buscar(String id);
}
