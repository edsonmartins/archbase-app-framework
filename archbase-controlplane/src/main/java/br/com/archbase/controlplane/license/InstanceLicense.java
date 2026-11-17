package br.com.archbase.controlplane.license;

import java.time.Duration;
import java.time.Instant;

/**
 * Licença da instância, já com a assinatura verificada.
 *
 * @param instanceId id da instância a que a licença pertence
 * @param product    código do produto
 * @param issuedAt   emissão
 * @param expiresAt  fim da validade
 * @param token      o JWS original, guardado para reverificação offline
 * @since 3.4.6
 */
public record InstanceLicense(String instanceId, String product, Instant issuedAt, Instant expiresAt, String token) {

    /** Situação no instante informado, com a carência dada. */
    public LicenseStatus statusAt(Instant now, Duration gracePeriod) {
        if (!now.isAfter(expiresAt)) {
            return LicenseStatus.VALID;
        }
        if (!now.isAfter(expiresAt.plus(gracePeriod))) {
            return LicenseStatus.GRACE;
        }
        return LicenseStatus.EXPIRED;
    }
}
