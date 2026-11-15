package br.com.archbase.architecture.rules.fixtures.onion.infra;

import br.com.archbase.architecture.rules.fixtures.onion.domainservice.Pedidos;
import br.com.archbase.architecture.rules.fixtures.onion.model.Pedido;

/** Infrastructure -> anéis internos: válido. */
public class JpaPedidos implements Pedidos {
    @Override
    public Pedido buscar(String id) {
        return null;
    }
}
