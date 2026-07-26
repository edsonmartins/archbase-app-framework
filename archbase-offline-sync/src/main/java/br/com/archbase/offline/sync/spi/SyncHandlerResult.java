package br.com.archbase.offline.sync.spi;

/** Resultado de sucesso de um handler. */
public class SyncHandlerResult {
    private final Long serverVersion;

    private SyncHandlerResult(Long serverVersion) {
        this.serverVersion = serverVersion;
    }

    public static SyncHandlerResult ok() {
        return new SyncHandlerResult(null);
    }

    public static SyncHandlerResult version(Long serverVersion) {
        return new SyncHandlerResult(serverVersion);
    }

    public Long getServerVersion() {
        return serverVersion;
    }
}
