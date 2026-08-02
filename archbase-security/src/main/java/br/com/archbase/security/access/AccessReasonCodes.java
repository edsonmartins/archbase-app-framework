package br.com.archbase.security.access;

/**
 * Códigos de motivo de uma {@link AccessDecision}.
 *
 * <p>São estáveis e destinados a consumo por máquina — log estruturado, corpo do 403, tela de
 * diagnóstico. A mensagem legível fica em {@link AccessDecision#message()} e pode mudar; o código,
 * não.
 */
public final class AccessReasonCodes {

    // ---- concedeu

    /** Concedido por permissão no catálogo. */
    public static final String GRANTED = "GRANTED";

    /** Concedido pela flag {@code isAdministrator}, sem consultar o catálogo. */
    public static final String GRANTED_ADMINISTRATOR = "GRANTED_ADMINISTRATOR";

    // ---- negou

    /**
     * O principal autenticado não é um {@code UserEntity} do Archbase — tipicamente uma aplicação
     * com {@code UserDetailsService} próprio, ou um acesso anônimo.
     */
    public static final String PRINCIPAL_NOT_SUPPORTED = "PRINCIPAL_NOT_SUPPORTED";

    /**
     * O principal é um usuário do Archbase, mas está sem dado obrigatório para decidir — hoje,
     * {@code isAdministrator} nulo, que a coluna aceita.
     */
    public static final String PRINCIPAL_INCOMPLETE = "PRINCIPAL_INCOMPLETE";

    /** Nenhuma permissão encontrada para o usuário, seus grupos ou seu perfil. */
    public static final String NO_GRANT = "NO_GRANT";

    /** Há permissão, mas nenhuma cujo escopo alcance o tenant, a empresa ou o projeto pedidos. */
    public static final String OUT_OF_SCOPE = "OUT_OF_SCOPE";

    private AccessReasonCodes() {
    }
}
