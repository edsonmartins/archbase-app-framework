package br.com.archbase.architecture.rules.fixtures.hexagonal.core;

import br.com.archbase.architecture.rules.fixtures.hexagonal.port.CadastrarCliente;
import br.com.archbase.architecture.rules.fixtures.hexagonal.port.Clientes;

/** Aplicação -> portas: válido. */
public class CadastroDeClientes implements CadastrarCliente {
    Clientes clientes;

    @Override
    public void cadastrar(String nome) {
        clientes.salvar(nome);
    }
}
