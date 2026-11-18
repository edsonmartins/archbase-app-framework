package br.com.archbase.controlplane.command;

/**
 * Comando sem assinatura válida do Control Plane, ou que não é para esta instância.
 *
 * @since 3.4.6
 */
public class CommandRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CommandRejectedException(String message) {
        super(message);
    }

    public CommandRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
