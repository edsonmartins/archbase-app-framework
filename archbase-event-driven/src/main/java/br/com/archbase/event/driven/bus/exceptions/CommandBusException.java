package br.com.archbase.event.driven.bus.exceptions;

public class CommandBusException extends RuntimeException {

    public CommandBusException(String message) {
        super(message);
    }
}
