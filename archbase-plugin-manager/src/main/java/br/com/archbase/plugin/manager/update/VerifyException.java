package br.com.archbase.plugin.manager.update;


import br.com.archbase.plugin.manager.PluginRuntimeException;

/**
 * Exceção de marcador para falha de verificação do plugin
 */
public class VerifyException extends PluginRuntimeException {

    public VerifyException(String message) {
        super(message);
    }

    public VerifyException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    public VerifyException(String message, Object... args) {
        super(message, args);
    }

}
