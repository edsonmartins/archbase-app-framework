package br.com.archbase.controlplane.license;

/**
 * Licença que não pode ser aplicada: assinatura, emissor, instância ou produto não conferem.
 *
 * @since 3.4.6
 */
public class LicenseRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LicenseRejectedException(String message) {
        super(message);
    }

    public LicenseRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
