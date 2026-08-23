package br.com.archbase.multitenancy.interceptor;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class ArchbaseTenantRequestInterceptor implements HandlerInterceptor {

    public static final String X_TENANT_ID = "X-TENANT-ID";
    public static final String X_COMPANY_ID = "X-COMPANY-ID";

    /**
     * Aceita tenant e company vindos da query string, além do header.
     *
     * <p>Continua ligado por compatibilidade, mas tenant em query string acaba gravado no access
     * log, no histórico do navegador e no {@code Referer} enviado a terceiros — e transforma a troca
     * de tenant em algo que cabe num link. Prefira os headers e desligue com
     * {@code archbase.app.tenant.accept-query-param=false}.
     *
     * <p><b>Recebido pelo construtor, não por {@code @Value}.</b> Esta classe é instanciada com
     * {@code new} pelo {@code addInterceptors} da autoconfiguração — nunca passa pelo container,
     * então nenhuma anotação de injeção é processada nela. Um {@code @Value} aqui ficaria com o
     * default do Java ({@code false}), silenciosamente desligando o suporte a query string em vez
     * de mantê-lo ligado como a configuração promete.
     */
    private final boolean acceptQueryParam;

    /** Mantém o comportamento histórico (aceita query string) para quem instancia sem argumento. */
    public ArchbaseTenantRequestInterceptor() {
        this(true);
    }

    public ArchbaseTenantRequestInterceptor(boolean acceptQueryParam) {
        this.acceptQueryParam = acceptQueryParam;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = resolve(request, X_TENANT_ID);
        String companyId = resolve(request, X_COMPANY_ID);

        if (tenantId != null && !tenantId.isEmpty()) {
            ArchbaseTenantContext.setTenantId(tenantId);
        }

        if (companyId != null && !companyId.isEmpty()) {
            ArchbaseTenantContext.setCompanyId(companyId);
        }

        return true;
    }

    private String resolve(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if ((value == null || value.isEmpty()) && acceptQueryParam) {
            value = request.getParameter(name);
        }
        return value;
    }

    /**
     * Limpeza no {@code afterCompletion}, não no {@code postHandle}.
     *
     * <p>{@code postHandle} só roda quando o handler retorna normalmente: bastava o controller
     * lançar uma exceção para o tenant ficar no ThreadLocal e ser herdado pela próxima requisição
     * servida por aquela thread do pool — ou seja, um erro em qualquer endpoint podia fazer o
     * request seguinte ler os dados do tenant errado. {@code afterCompletion} roda sempre,
     * inclusive quando houve exceção.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ArchbaseTenantContext.clear();
    }
}
