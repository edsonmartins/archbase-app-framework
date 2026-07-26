package br.com.archbase.offline.sync.spi;

/**
 * Fornece o tenant corrente para a idempotência durável. O app implementa
 * (normalmente delegando ao contexto de multitenancy do Archbase).
 */
public interface SyncTenantProvider {
    String currentTenantId();
}
