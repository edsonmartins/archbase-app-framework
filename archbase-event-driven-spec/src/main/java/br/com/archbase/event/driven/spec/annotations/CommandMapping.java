package br.com.archbase.event.driven.spec.annotations;


import br.com.archbase.event.driven.spec.command.contracts.Command;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(CommandMappings.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandMapping {
    Class<? extends Command> value();
}
