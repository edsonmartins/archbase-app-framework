package br.com.archbase.analytics.port;

import java.util.Map;

/**
 * A ÚNICA costura entre o framework e o domínio do produto.
 *
 * <p>O framework cunha e transporta o token de acesso ao Cube; o produto decide
 * <b>quais claims de escopo</b> vão nesse token — filial, vendedor, região, o
 * que a camada semântica do produto usar nas suas policies. O framework não
 * interpreta o conteúdo: apenas o assina e o envia.
 *
 * <p>É esta interface que preserva o invariante de "zero conhecimento de
 * domínio": tudo que é específico de negócio fica na implementação do produto,
 * nunca no framework.
 *
 * <p>Deny-by-default vive na policy da <i>view</i> do Cube: um mapa vazio
 * (usuário sem escopo) resulta num token sem claims de escopo, e a view nega —
 * comportamento correto, decidido no data model, não aqui.
 */
public interface DataScopeProvider {

    /**
     * As claims de escopo do usuário, injetadas no token cunhado.
     *
     * @param username identidade autenticada (do security context)
     * @return mapa de claims; vazio quando o usuário não tem escopo atribuído
     */
    Map<String, Object> claimsForUser(String username);
}
