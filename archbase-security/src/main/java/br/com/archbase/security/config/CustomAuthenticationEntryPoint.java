package br.com.archbase.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
 *
 * <p><b>Sessão encerrada.</b> Quando o token tem assinatura válida mas foi revogado — logout, ou
 * outro login do mesmo usuário de sessão única ({@code allowMultipleLogins=false}) —, o
 * {@link ArchbaseJwtAuthenticationFilter} marca o motivo na requisição e o 401 sai com corpo JSON
 * {@code {"status":401,"error":"SESSAO_ENCERRADA","message":...}}. Sem isso o cliente só via
 * "não autorizado" e culpava a senha, enquanto a causa era um segundo login em outro lugar.</p>
 */
@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** Atributo da requisição em que o filtro de JWT registra por que o token foi recusado. */
    public static final String ATRIBUTO_MOTIVO = "archbase.security.motivoNaoAutenticado";

    /** Token com assinatura válida, mas revogado: a sessão foi encerrada (logout ou outro login). */
    public static final String SESSAO_ENCERRADA = "SESSAO_ENCERRADA";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("NÃO AUTENTICADO: {} {} — {}",
                request.getMethod(), request.getRequestURI(), authException.getMessage());

        if (SESSAO_ENCERRADA.equals(request.getAttribute(ATRIBUTO_MOTIVO))) {
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":401,\"error\":\"" + SESSAO_ENCERRADA + "\","
                    + "\"message\":\"Sessão encerrada: houve outro login com este usuário ou a sessão foi"
                    + " encerrada. Entre novamente.\"}");
            return;
        }

        response.setHeader("WWW-Authenticate", "Bearer");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Não autenticado: " + authException.getMessage());
    }
}
