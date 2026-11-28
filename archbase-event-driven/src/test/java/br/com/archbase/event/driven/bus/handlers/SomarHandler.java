package br.com.archbase.event.driven.bus.handlers;

import br.com.archbase.event.driven.spec.annotations.CommandMapping;
import br.com.archbase.event.driven.spec.command.contracts.CommandHandler;

@CommandMapping(Somar.class)
public class SomarHandler implements CommandHandler<Somar, Integer> {
    @Override
    public Integer handle(Somar command) {
        if (command.a() < 0) {
            throw new IllegalArgumentException("negativo");
        }
        return command.a() + command.b();
    }
}
