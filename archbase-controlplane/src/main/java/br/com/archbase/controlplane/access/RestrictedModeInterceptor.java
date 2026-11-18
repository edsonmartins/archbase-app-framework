package br.com.archbase.controlplane.access;

import br.com.archbase.controlplane.ControlPlane;
import br.com.archbase.controlplane.config.ControlPlaneProperties;
import br.com.archbase.controlplane.license.LicenseStatus;
import br.com.archbase.controlplane.state.ControlPlaneState;
import br.com.archbase.controlplane.tenant.TenantState;
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Modo restrito: com a licença vencida além da carência ({@code LICENSE_EXPIRED}) ou o tenant suspenso
 * ({@code TENANT_SUSPENDED}), leitura e exportação continuam e a escrita é recusada com 403. Nunca apaga dados.
 * <p>
 * Escrita = método que não seja {@code GET}, {@code HEAD} ou {@code OPTIONS}, exceto endpoints com
 * {@link ExportOperation} e os caminhos de {@code archbase.controlplane.restricted-mode.allowed-paths}. Com
 * {@code enforcement=report}, só registra em log.
 *
 * @since 3.4.6
 */
public class RestrictedModeInterceptor implements HandlerInterceptor {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private static final Logger log = LoggerFactory.getLogger(RestrictedModeInterceptor.class);

    private final ControlPlaneState state;
    private final ControlPlaneProperties.RestrictedMode config;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RestrictedModeInterceptor(ControlPlaneState state, ControlPlaneProperties.RestrictedMode config) {
        this.state = state;
        this.config = config;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        ControlPlane.bindRequest(state);
        if (SAFE_METHODS.contains(request.getMethod()) || isExport(handler) || isAllowedPath(request)) {
            return true;
        }
        String code = null;
        String message = null;
        if (state.licenseStatus() == LicenseStatus.EXPIRED) {
            code = ControlPlaneAccessDeniedException.LICENSE_EXPIRED;
            message = "Licença vencida: a instância está em modo somente leitura";
        } else {
            String tenant = ArchbaseTenantContext.getTenantId();
            if (state.tenant(tenant).map(s -> s.state() == TenantState.SUSPENDED).orElse(false)) {
                code = ControlPlaneAccessDeniedException.TENANT_SUSPENDED;
                message = "Tenant suspenso: escrita bloqueada, leitura e exportação liberadas";
            }
        }
        if (code == null) {
            return true;
        }
        state.metrics().restrictedDenied(code, config.getEnforcement().name().toLowerCase(java.util.Locale.ROOT));
        if (config.getEnforcement() == ControlPlaneProperties.Enforcement.REPORT) {
            log.warn("controlplane.restricted.would_deny code={} tnt={} {} {} (enforcement=report)", code,
                    ArchbaseTenantContext.getTenantId(), request.getMethod(), request.getRequestURI());
            return true;
        }
        log.info("controlplane.restricted.denied code={} tnt={} {} {}", code, ArchbaseTenantContext.getTenantId(),
                request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":403,\"error\":\"" + code + "\",\"code\":\"" + code
                + "\",\"message\":\"" + message + "\"}");
        // Com preHandle devolvendo false, o Spring não chama afterCompletion: limpa aqui.
        ControlPlane.unbindRequest();
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        ControlPlane.unbindRequest();
    }

    private static boolean isExport(Object handler) {
        if (handler instanceof HandlerMethod method) {
            return AnnotatedElementUtils.hasAnnotation(method.getMethod(), ExportOperation.class)
                    || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), ExportOperation.class);
        }
        return false;
    }

    private boolean isAllowedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String path = context != null && !context.isEmpty() && uri.startsWith(context)
                ? uri.substring(context.length()) : uri;
        List<String> allowed = config.getAllowedPaths();
        return allowed != null && allowed.stream().anyMatch(pattern -> matcher.match(pattern, path));
    }
}
