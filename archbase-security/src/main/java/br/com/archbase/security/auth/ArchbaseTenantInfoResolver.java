package br.com.archbase.security.auth;

/**
 * Fornece o rótulo de apresentação de um tenant — nome e descrição da organização.
 *
 * <p><b>Por que isto existe.</b> O {@code archbase-security} não tem entidade de tenant: o tenant é
 * uma coluna ({@code TENANT_ID}) na linha de cada registro, e não há tabela alguma que guarde o nome
 * da organização. Antes desta interface, {@code GET /api/v1/auth/tenants} preenchia {@code nome} e
 * {@code descricao} com as colunas {@code NOME}/{@code DESCRICAO} da linha de <b>usuário</b> — ou
 * seja, devolvia o nome da pessoa, sem autenticação, para quem soubesse o e-mail. Além do vazamento,
 * era errado na tela: o seletor de tenant exibia o próprio nome do usuário no lugar da organização.
 *
 * <p>Como o framework não tem essa informação, ele deixou de inventá-la. Quem tem — a aplicação, que
 * conhece o seu cadastro de organizações — registra um bean desta interface e passa a preencher os
 * rótulos. Sem bean registrado, só o {@code tenantId} é devolvido, e o cliente exibe o id.
 *
 * <p>Também é usado para enriquecer o tenant devolvido na resposta de login
 * ({@link AuthenticationResponse#getTenant()}).
 *
 * <p><b>Contrato:</b> é consultado em fluxos anônimos (pré-login). Não deve lançar exceção nem
 * devolver informação que só um usuário autenticado poderia ver.
 */
@FunctionalInterface
public interface ArchbaseTenantInfoResolver {

    /**
     * Devolve os dados de apresentação do tenant, ou {@code null} quando não houver rótulo para ele
     * (nesse caso apenas o {@code tenantId} chega ao cliente).
     *
     * @param tenantId identificador do tenant
     * @return dados de apresentação, ou {@code null}
     */
    TenantLoginOption resolve(String tenantId);
}
