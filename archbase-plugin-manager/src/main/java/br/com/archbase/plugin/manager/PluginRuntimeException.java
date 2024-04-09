package br.com.archbase.plugin.manager;

import br.com.archbase.plugin.manager.util.StringUtils;

/**
 * Uma exceção usada para indicar que ocorreu um problema de plug-in.
 * É uma classe de exceção de plug-in genérica a ser lançada quando nenhuma classe mais específica for aplicável.
 */
public class PluginRuntimeException extends RuntimeException {

    public PluginRuntimeException() {
        super();
    }

    public PluginRuntimeException(String message) {
        super(message);
    }

    public PluginRuntimeException(Throwable cause) {
        super(cause);
    }

    public PluginRuntimeException(Throwable cause, String message, Object... args) {
        super(StringUtils.format(message, args), cause);
    }

    public PluginRuntimeException(String message, Object... args) {
        super(StringUtils.format(message, args));
    }

}
