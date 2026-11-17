package br.com.archbase.controlplane.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Com {@code archbase.controlplane.legacy-jwt=reject}, desliga as rotas de login local do Archbase: o login passa a
 * acontecer no app ou no Admin, contra o ArchGuard ou o IdP do tenant, e a instância só recebe o token pronto.
 * Responde 404 com {@code {"code":"LOCAL_LOGIN_DISABLED"}}.
 * <p>
 * Roda antes da cadeia do Spring Security, para a rota sumir mesmo quando está na lista pública.
 *
 * @since 3.4.6
 */
public class LocalLoginRoutesFilter extends OncePerRequestFilter implements Ordered {

    public static final String CODE = "LOCAL_LOGIN_DISABLED";

    private static final String PREFIX = "/api/v1/auth/";

    /** Rotas exatas sob {@code /api/v1/auth/}. */
    private static final Set<String> ROUTES = Set.of(
            "authenticate", "mfa/verify", "login", "login-flexible", "login-social", "refresh-token",
            "register", "resetPassword", "tenants");

    /** Rotas com parâmetro no caminho. */
    private static final List<String> ROUTE_PREFIXES = List.of("sendResetPasswordEmail/");

    private static final Logger log = LoggerFactory.getLogger(LocalLoginRoutesFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isLocalLoginRoute(pathWithinApplication(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        log.info("controlplane.local_login.blocked {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":404,\"error\":\"" + CODE + "\",\"code\":\"" + CODE + "\","
                + "\"message\":\"Login local desativado: autentique pelo Control Plane\"}");
    }

    static boolean isLocalLoginRoute(String path) {
        if (path == null || !path.startsWith(PREFIX)) {
            return false;
        }
        String route = path.substring(PREFIX.length());
        if (route.endsWith("/")) {
            route = route.substring(0, route.length() - 1);
        }
        if (ROUTES.contains(route)) {
            return true;
        }
        for (String prefix : ROUTE_PREFIXES) {
            if (route.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context != null && !context.isEmpty() && uri.startsWith(context) ? uri.substring(context.length()) : uri;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
