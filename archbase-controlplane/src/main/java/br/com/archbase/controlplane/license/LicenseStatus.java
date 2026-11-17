package br.com.archbase.controlplane.license;

/**
 * Situação da licença da instância.
 *
 * @since 3.4.6
 */
public enum LicenseStatus {
    /** Dentro da validade. */
    VALID,
    /** Vencida, mas dentro da carência ({@code archbase.controlplane.grace-period}): tudo segue funcionando. */
    GRACE,
    /** Vencida além da carência, ou ausente: modo restrito (leitura e exportação; escrita recusada). */
    EXPIRED
}
