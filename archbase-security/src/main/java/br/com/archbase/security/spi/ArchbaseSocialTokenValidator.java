package br.com.archbase.security.spi;

import java.util.Map;

/**
 * Valida um token de login social contra o provedor (Google, Facebook, …) e devolve os dados do
 * usuário.
 *
 * <p><b>Por que existe.</b> {@code POST /api/v1/auth/login-social} é anônimo e ficava com uma
 * implementação de mentira: montava um mapa com o provedor e o token recebidos, sem consultar
 * ninguém, e devolvia comentários no lugar dos dados
 * ({@code // data.put("email", decodedToken.getEmail())}). Na prática o endpoint sempre falhava por
 * e-mail nulo — o que salvou o sistema por acidente, não por desenho: bastaria alguém "completar" o
 * mapa com o e-mail vindo da requisição para transformar o endpoint em login sem senha para
 * qualquer conta.
 *
 * <p>Sem um bean desta interface, o endpoint responde {@code 501 Not Implemented} — recusa
 * explícita, em vez de um erro de validação que parece um bug do cliente.
 *
 * <p>A implementação deve <b>verificar a assinatura do token no provedor</b> (SDK oficial ou
 * endpoint de tokeninfo) e conferir o {@code audience} contra o client id da aplicação. Aceitar o
 * payload decodificado sem verificar assinatura permite forjar qualquer identidade.
 */
public interface ArchbaseSocialTokenValidator {

    /**
     * @param provider identificador do provedor, como veio na requisição
     * @return {@code true} se esta implementação responde por esse provedor
     */
    boolean supports(String provider);

    /**
     * Valida o token junto ao provedor.
     *
     * @return dados do usuário; a chave {@code email} é obrigatória, e {@code name}/{@code picture}
     *         são usadas no cadastro automático quando presentes
     * @throws RuntimeException se o token for inválido, expirado ou de outra aplicação
     */
    Map<String, Object> validate(String provider, String token);
}
