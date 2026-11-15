package br.com.archbase.architecture.rules.fixtures.hexagonal.out;

import br.com.archbase.architecture.rules.fixtures.hexagonal.port.Clientes;

/** Adaptador secundário -> porta secundária: válido. */
public class ClientesJdbc implements Clientes {
    @Override
    public void salvar(String nome) {
    }
}
