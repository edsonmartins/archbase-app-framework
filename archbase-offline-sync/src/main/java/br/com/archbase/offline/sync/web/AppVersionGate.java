package br.com.archbase.offline.sync.web;

/**
 * Verifica a compatibilidade da versão do app (header {@code X-App-Version}).
 * O default é no-op; o app pode fornecer um bean que lança
 * {@link SyncAppVersionIncompatibleException} para versões antigas (→ HTTP 426).
 */
public interface AppVersionGate {
    void check(String appVersion);
}
