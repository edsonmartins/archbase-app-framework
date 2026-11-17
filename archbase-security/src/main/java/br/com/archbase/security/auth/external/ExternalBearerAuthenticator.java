package br.com.archbase.security.auth.external;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/**
 * Autentica um bearer token emitido <b>fora</b> do Archbase (por exemplo, o token de sessão do Control Plane).
 * <p>
 * O {@code ArchbaseJwtAuthenticationFilter} consulta os beans deste tipo antes do fluxo próprio. O primeiro que
 * {@link #supports(String) reconhecer} o token assume a requisição inteira, e o fluxo legado (JWT HS256 e token de
 * API) não roda. Sem nenhum bean, o filtro se comporta exatamente como antes.
 * <p>
 * A autenticação é feita em duas etapas para que o filtro aplique a regra de tenant de forma uniforme:
 * <ol>
 * <li>{@link #check(String, HttpServletRequest)} valida o token sem acessar banco e diz de qual tenant ele é;</li>
 * <li>o filtro confere o tenant com o {@code X-TENANT-ID}, define o {@code ArchbaseTenantContext} e só então chama
 * {@link #authenticate(ExternalTokenCheck, HttpServletRequest)}, que pode consultar o banco já no tenant certo.</li>
 * </ol>
 *
 * @since 3.4.6
 */
public interface ExternalBearerAuthenticator {

    /**
     * Se este autenticador é o responsável pelo token. Deve ser barato e não validar assinatura (ex.: olhar o
     * {@code alg} do cabeçalho). Um token reconhecido e inválido deve ser recusado em {@link #check}, não ignorado
     * aqui — senão ele cai no fluxo legado.
     */
    boolean supports(String token);

    /** Valida o token (assinatura, emissor, público, validade, tenant), sem acessar banco. */
    ExternalTokenCheck check(String token, HttpServletRequest request);

    /**
     * Monta a {@link Authentication} de um token já aceito em {@link #check}. Chamado com o
     * {@code ArchbaseTenantContext} já definido para {@link ExternalTokenCheck#tenantId()}.
     */
    Authentication authenticate(ExternalTokenCheck check, HttpServletRequest request);
}
