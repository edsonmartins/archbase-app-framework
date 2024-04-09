package br.com.archbase.plugin.manager;


public enum PluginState {

    /**
     * O tempo de execução sabe que o plug-in está lá. Ele conhece o caminho do archbasePlugin, o descritor do archbasePlugin.
     */
    CREATED("CREATED"),

    /**
     * O archbasePlugin não pode ser usado.
     */
    DISABLED("DISABLED"),

    /**
     * O archbasePlugin é criado. Todas as dependências são criadas e resolvidas.
     * O archbasePlugin está pronto para ser iniciado.
     */
    RESOLVED("RESOLVED"),

    /**
     * O {@link ArchbasePlugin # start ()} foi executado. Um archbasePlugin iniciado pode contribuir com extensões.
     */
    STARTED("STARTED"),

    /**
     * O {@link ArchbasePlugin # stop ()} foi executado.
     */
    STOPPED("STOPPED"),

    /**
     * ArchbasePlugin falhou ao iniciar.
     */
    FAILED("FAILED");

    private String status;

    private PluginState(String status) {
        this.status = status;
    }

    public static PluginState parse(String string) {
        for (PluginState status : values()) {
            if (status.equalsState(string)) {
                return status;
            }
        }

        return null;
    }

    public boolean equalsState(String status) {
        return (status == null ? Boolean.FALSE : this.status.equalsIgnoreCase(status));
    }

    @Override
    public String toString() {
        return status;
    }
}
