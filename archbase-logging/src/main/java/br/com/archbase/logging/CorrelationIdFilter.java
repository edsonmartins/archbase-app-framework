package br.com.archbase.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter para extrair e propagar correlation ID em requisições HTTP.
 * <p>
 * Este filter:
 * <ul>
 *   <li>Extrai correlation ID do header da requisição</li>
 *   <li>Gera novo correlation ID se não existir</li>
 *   <li>Adiciona correlation ID ao MDC para logging</li>
 *   <li>Adiciona correlation ID ao header da resposta</li>
 * </ul>
 * <p>
 * Headers configuráveis (padrão):
 * <ul>
 *   <li>X-Correlation-ID</li>
 *   <li>Correlation-ID</li>
 *   <li>x-request-id</li>
 * </ul>
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final List<String> POSSIBLE_HEADERS = List.of(
            "X-Correlation-ID",
            "Correlation-ID",
            "x-request-id",
            "x-vcap-request-id"
    );

    private final String headerName;

    public CorrelationIdFilter() {
        this(CORRELATION_ID_HEADER);
    }

    public CorrelationIdFilter(String headerName) {
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = extractCorrelationId(request);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = StructuredLogger.generateCorrelationId();
        }

        MDC.put("correlationId", correlationId);
        response.setHeader(headerName, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String extractCorrelationId(HttpServletRequest request) {
        for (String header : POSSIBLE_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Não filtrar requisições de health check
        String path = request.getRequestURI();
        return path != null && (path.contains("/actuator/health") ||
                path.contains("/health") ||
                path.contains("/readiness") ||
                path.contains("/liveness"));
    }
}
