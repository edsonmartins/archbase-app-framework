package br.com.archbase.security.access;

/**
 * O que uma linha de permissão faz: soma ou subtrai.
 *
 * <p>Até aqui o modelo só sabia somar — perfil ∪ grupos ∪ direto, união pura. Excluir uma pessoa de
 * algo que o time inteiro tem exigia criar um grupo paralelo só para ela, que é como catálogos de
 * permissão viram um emaranhado.
 *
 * <p>A regra é uma frase, explicável para analista de negócio sem diagrama: <b>o perfil libera para
 * o time inteiro, e a negação no usuário tira daquela pessoa</b>. {@link #DENY} vence em qualquer
 * nível — não há precedência entre usuário, grupo e perfil, e não faz falta.
 *
 * <p>Escopo: uma negação vale <b>dentro do escopo em que foi declarada</b>. {@code DENY} com
 * {@code tenantId = A} não afeta o tenant B; sem escopo, vence em todos. Mesma semântica que a
 * concessão sempre teve.
 */
public enum PermissionEffect {

    /** Concede. É o valor de toda linha existente, e o padrão de toda linha nova. */
    GRANT,

    /** Nega, vencendo qualquer concessão de qualquer origem dentro do mesmo escopo. */
    DENY;

    /**
     * Converte o texto gravado na coluna.
     *
     * <p>Nulo é {@link #GRANT}: é o que as 2.229 concessões existentes de um sistema em produção
     * significam, e a coluna nasce vazia para todas elas.
     *
     * <p>Valor <b>desconhecido</b> também é {@code GRANT}, e não {@code DENY}. Parece contraintuitivo
     * num módulo de segurança, mas o contrário é pior: um erro de digitação em uma linha
     * transformaria uma concessão legítima numa negação que ninguém pediu, e o suporte procuraria o
     * problema no lugar errado. Quem quer negar declara {@code DENY} corretamente.
     */
    public static PermissionEffect parse(String valor) {
        if (valor == null || valor.isBlank()) {
            return GRANT;
        }
        return DENY.name().equalsIgnoreCase(valor.trim()) ? DENY : GRANT;
    }
}
