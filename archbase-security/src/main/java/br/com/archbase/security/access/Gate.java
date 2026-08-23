package br.com.archbase.security.access;

/**
 * Os portões da decisão de acesso, na ordem em que são avaliados.
 *
 * <p><b>A regra que organiza o modelo: os portões {@link #IDENTITY} a {@link #LEVEL} só sabem
 * negar. Só o {@link #GRANT} concede.</b>
 *
 * <p>É isso que separa restrição de concessão. Uma tranca fechada não é contornável por concessão
 * nenhuma — ter a capacidade atribuída no catálogo não basta se um portão anterior barrou. E, do
 * outro lado, passar por todas as trancas não abre nada sozinho: sem concessão no catálogo, o
 * acesso é negado.
 *
 * @see ArchbaseAccessEvaluator
 */
public enum Gate {

    /** Conta ativa e principal resolvível. */
    IDENTITY,

    /** {@code tenantId}, {@code companyId} e {@code projectId} compatíveis. */
    SCOPE,

    /** {@code @RequireRole}, {@code @RequireProfile}, {@code @RequirePersona}. */
    RESTRICTION,

    /** Nível do sujeito ≥ nível mínimo da capacidade. */
    LEVEL,

    /** Catálogo: permissão concedida ao perfil, a um grupo ou diretamente ao usuário. */
    GRANT
}
