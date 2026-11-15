package br.com.archbase.architecture.rules.fixtures.hexagonal.in;

import br.com.archbase.architecture.rules.fixtures.hexagonal.out.ClientesJdbc;

/** Adaptador primário -> adaptador secundário: VIOLAÇÃO. */
public class ControllerQueVaiAoBanco {
    ClientesJdbc jdbc;
}
