package br.com.archbase.controlplane.api;

import br.com.archbase.controlplane.license.LicenseStatus;
import br.com.archbase.controlplane.state.ControlPlaneState;

import java.time.Instant;
import java.util.Optional;

/**
 * Licença da instância.
 *
 * @since 3.4.6
 */
public class LicenseInfo {

    private final ControlPlaneState state;

    public LicenseInfo(ControlPlaneState state) {
        this.state = state;
    }

    public LicenseStatus status() {
        return state.licenseStatus();
    }

    public Optional<Instant> expiresAt() {
        return state.license().map(l -> l.expiresAt());
    }

    /** Fim da carência: depois dele, modo restrito. */
    public Optional<Instant> graceEndsAt() {
        return state.graceEndsAt();
    }
}
