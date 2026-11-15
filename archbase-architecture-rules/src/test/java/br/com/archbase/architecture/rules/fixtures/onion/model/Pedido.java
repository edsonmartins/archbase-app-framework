package br.com.archbase.architecture.rules.fixtures.onion.model;

import br.com.archbase.architecture.rules.fixtures.onion.infra.JpaPedidos;

/** Domain model -> Infrastructure: VIOLAÇÃO (anel interno olhando para fora). */
public class Pedido {
    JpaPedidos repositorio;
}
