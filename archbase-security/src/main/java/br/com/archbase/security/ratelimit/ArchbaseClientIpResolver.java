package br.com.archbase.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * O endereço de origem de uma requisição, para efeito de contagem de tentativas.
 *
 * <p><b>Por que não basta {@code getRemoteAddr()}.</b> Atrás de proxy reverso, ingress ou
 * balanceador — o deploy normal — {@code getRemoteAddr()} devolve o endereço do <i>proxy</i>,
 * igual para todo mundo. Um limitador chaveado nisso não distingue usuários: ele conta o tráfego
 * inteiro numa chave só e, ao estourar, recusa o serviço para todos. Foi assim que a proteção do
 * {@code /auth/tenants} virou negação de serviço.
 *
 * <p><b>Por que confiar em {@code X-Forwarded-For} é opt-in.</b> O cabeçalho é escrito pelo
 * cliente e só é confiável quando existe um proxy à frente que o sobrescreve. Ligá-lo sem esse
 * proxy dá ao atacante o poder de trocar de identidade a cada requisição — ou seja, de anular o
 * limitador e ainda de bloquear terceiros forjando o endereço deles. Por isso o padrão é
 * {@code false}: quem tem proxy liga e sabe por quê.
 *
 * <pre>archbase.security.client-ip.trust-forwarded-for=true</pre>
 */
@Component
public class ArchbaseClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Value("${archbase.security.client-ip.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    /**
     * @return o endereço do cliente, ou {@code "desconhecido"} quando não há nenhum — nunca nulo,
     *         para que a chave de contagem seja sempre bem formada
     */
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "desconhecido";
        }
        if (trustForwardedFor) {
            String encaminhado = request.getHeader(X_FORWARDED_FOR);
            if (encaminhado != null && !encaminhado.isBlank()) {
                // O cabeçalho é uma lista: cliente, proxy1, proxy2... O primeiro é a origem.
                String primeiro = encaminhado.split(",")[0].trim();
                if (!primeiro.isEmpty()) {
                    return primeiro;
                }
            }
        }
        String remoto = request.getRemoteAddr();
        return remoto != null && !remoto.isBlank() ? remoto : "desconhecido";
    }
}
