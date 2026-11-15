package br.com.archbase.architecture.rules.fixtures.hexagonal.core;

import br.com.archbase.architecture.rules.fixtures.hexagonal.out.ClientesJdbc;

/** Aplicação -> adaptador secundário: VIOLAÇÃO. */
public class AtalhoProibido {
    ClientesJdbc jdbc;
}
