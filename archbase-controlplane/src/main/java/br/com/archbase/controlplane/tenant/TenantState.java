package br.com.archbase.controlplane.tenant;

/**
 * Estado comercial do tenant no Control Plane.
 * <p>
 * <b>Contrato assumido.</b> O pedido cita "ACTIVE, DELINQUENT, SUSPENDED..."; valores desconhecidos recebidos do
 * Control Plane viram {@link #UNKNOWN}, que não restringe nada.
 *
 * @since 3.4.6
 */
public enum TenantState {
    /** Normal. */
    ACTIVE,
    /** Inadimplente: informativo, sem restrição nesta instância. */
    DELINQUENT,
    /** Suspenso: login novo e escrita recusados com {@code TENANT_SUSPENDED}; leitura e exportação liberadas. */
    SUSPENDED,
    /** Sem informação do Control Plane para este tenant. */
    UNKNOWN;

    public static TenantState parse(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
