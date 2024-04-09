package br.com.archbase.event.driven.bus.exceptions;


import br.com.archbase.event.driven.spec.message.contracts.Message;

public class NoHandlerFoundException extends CommandBusException {
    public NoHandlerFoundException(Class<? extends Message> messageClass) {
        super(String.format("Nenhum manipulador encontrado para %s", messageClass.getName()));
    }

    public NoHandlerFoundException(String messageClassName) {
        super(String.format("Nenhum manipulador encontrado para %s", messageClassName));
    }
}
