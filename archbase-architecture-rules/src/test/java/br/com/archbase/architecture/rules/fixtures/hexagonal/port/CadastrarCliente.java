package br.com.archbase.architecture.rules.fixtures.hexagonal.port;

import br.com.archbase.hexagonal.PrimaryPort;

/** Anotação no tipo vence a do pacote. */
@PrimaryPort
public interface CadastrarCliente {
    void cadastrar(String nome);
}
