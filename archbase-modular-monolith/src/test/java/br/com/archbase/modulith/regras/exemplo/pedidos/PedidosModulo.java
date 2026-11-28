package br.com.archbase.modulith.regras.exemplo.pedidos;

import br.com.archbase.modulith.annotations.Module;
import br.com.archbase.modulith.regras.exemplo.pedidos.internal.CalculoDeFrete;

/** Só com @Module, e usando o interno do próprio módulo: as duas regras devem aceitar. */
@Module(name = "pedidos")
public class PedidosModulo {
    final CalculoDeFrete frete = new CalculoDeFrete();
}
