package br.com.archbase.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde <b>401 Unauthorized</b> quando a requisição chega sem autenticação válida — token
 * ausente, expirado ou inválido.
 *
 * <p>Sem este entry point o Spring cai no {@link CustomAccessDeniedHandler} e devolve <b>403</b>
 * para os dois casos, que são distintos: 401 significa "não sei quem é você, autentique-se de
 * novo"; 403, "sei quem é você e você não pode". Clientes usam essa diferença para decidir entre
 * renovar a sessão e mostrar "sem permissão" — o interceptor do archbase-flutter, por exemplo, só
 * dispara o refresh do token em 401. Com 403 em token expirado, o app nunca renovava: o usuário
 * via erro de rede e uma tela vazia, sem entender que bastava entrar de novo.</p>
 */
@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("NÃO AUTENTICADO: {} {} — {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        response.setHeader("WWW-Authenticate", "Bearer");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Não autenticado: " + authException.getMessage());
    }
}
