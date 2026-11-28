package br.com.archbase.event.driven.bus.handlers;

import br.com.archbase.event.driven.spec.command.contracts.Command;

public record Somar(int a, int b) implements Command<Integer> {
}
