package br.com.archbase.architecture.rules.fixtures.hexagonal.port;

import br.com.archbase.hexagonal.SecondaryPort;

@SecondaryPort
public interface Clientes {
    void salvar(String nome);
}
