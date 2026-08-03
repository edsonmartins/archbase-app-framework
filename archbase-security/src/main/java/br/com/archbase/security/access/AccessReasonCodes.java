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

    /** Concedido por satisfazer as restrições declaradas, sem capacidade a consultar no catálogo. */
    public static final String GRANTED_RESTRICTIONS_ONLY = "GRANTED_RESTRICTIONS_ONLY";

    /** O administrador foi isentado desta tranca por {@code allowSystemAdmin}. */
    public static final String RESTRICTION_WAIVED_ADMINISTRATOR = "RESTRICTION_WAIVED_ADMINISTRATOR";

    /** O usuário não tem o perfil exigido por {@code @RequireProfile}. */
    public static final String PROFILE_NOT_MATCHED = "PROFILE_NOT_MATCHED";

    /** O usuário não tem a persona exigida por {@code @RequirePersona}. */
    public static final String PERSONA_NOT_MATCHED = "PERSONA_NOT_MATCHED";

    /** O usuário não tem o papel exigido por {@code @RequireRole}. */
    public static final String ROLE_NOT_MATCHED = "ROLE_NOT_MATCHED";

    /** {@code @RequireRole} avaliada sem nenhum {@code ArchbaseRoleResolver}, com política deny. */
    public static final String ROLE_RESOLVER_MISSING = "ROLE_RESOLVER_MISSING";

    /** {@code requirePlatformAdmin} exigido de quem não é administrador. */
    public static final String PLATFORM_ADMIN_REQUIRED = "PLATFORM_ADMIN_REQUIRED";

    /** {@code ownerOnly} não confirmado pelo SPI. */
    public static final String NOT_OWNER = "NOT_OWNER";

    /** Conta desativada ou bloqueada, onde a tranca exige conta ativa. */
    public static final String ACCOUNT_NOT_ACTIVE = "ACCOUNT_NOT_ACTIVE";

    /**
     * O requisito não declara nem capacidade nem restrição — nada a avaliar. É erro de programação
     * do adaptador, e negar é a resposta segura.
     */
    public static final String EMPTY_REQUIREMENT = "EMPTY_REQUIREMENT";

    /**
     * O nível do sujeito não alcança o mínimo da capacidade.
     *
     * <p>É a negação que responde ao risco de alguém atribuir uma capacidade sensível a quem não
     * deveria: a concessão existe, e mesmo assim não vale.
     */
    public static final String LEVEL_TOO_LOW = "LEVEL_TOO_LOW";

    /** Uma permissão com {@code effect = DENY} alcançou o escopo pedido. */
    public static final String EXPLICIT_DENY = "EXPLICIT_DENY";

    /**
     * A tranca foi declarada, mas o avaliador correspondente não está no contexto.
     *
     * <p>É falha de <b>configuração da aplicação</b>, não de permissão — tipicamente os beans de
     * {@code br.com.archbase.security} não foram varridos. Tem código próprio justamente para não
     * ser confundido com falta de permissão por quem investiga.
     */
    public static final String RESTRICTION_EVALUATOR_MISSING = "RESTRICTION_EVALUATOR_MISSING";

    /** Há permissão, mas nenhuma cujo escopo alcance o tenant, a empresa ou o projeto pedidos. */
    public static final String OUT_OF_SCOPE = "OUT_OF_SCOPE";

    private AccessReasonCodes() {
    }
}
