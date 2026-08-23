package br.com.archbase.multitenancy.interceptor;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ArchbaseTenantRequestInterceptor")
class ArchbaseTenantRequestInterceptorTest {

    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void tearDown() {
        ArchbaseTenantContext.clear();
    }

    @Test
    @DisplayName("resolve tenant e company pelo header")
    void resolvePorHeader() throws Exception {
        HttpServletRequest request = requestWith(
                Map.of(ArchbaseTenantRequestInterceptor.X_TENANT_ID, "tenant-a",
                        ArchbaseTenantRequestInterceptor.X_COMPANY_ID, "company-1"),
                Map.of());

        new ArchbaseTenantRequestInterceptor().preHandle(request, response, new Object());

        assertThat(ArchbaseTenantContext.getTenantId()).isEqualTo("tenant-a");
        assertThat(ArchbaseTenantContext.getCompanyId()).isEqualTo("company-1");
    }

    @Test
    @DisplayName("o construtor sem argumento aceita query string — o default documentado é ligado")
    void defaultAceitaQueryString() throws Exception {
        // Regressão: a versão anterior lia esta opção de um @Value num objeto criado com `new`,
        // que o container nunca processa. O campo ficava com o default do Java (false) e o suporte
        // a query string sumia silenciosamente, ao contrário do que a configuração prometia.
        HttpServletRequest request = requestWith(
                Map.of(),
                Map.of(ArchbaseTenantRequestInterceptor.X_TENANT_ID, "tenant-a"));

        new ArchbaseTenantRequestInterceptor().preHandle(request, response, new Object());

        assertThat(ArchbaseTenantContext.getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    @DisplayName("desligado, ignora a query string mas continua honrando o header")
    void desligadoIgnoraQueryString() throws Exception {
        HttpServletRequest apenasQuery = requestWith(
                Map.of(),
                Map.of(ArchbaseTenantRequestInterceptor.X_TENANT_ID, "tenant-a"));

        new ArchbaseTenantRequestInterceptor(false).preHandle(apenasQuery, response, new Object());
        assertThat(ArchbaseTenantContext.getTenantId()).isNull();

        HttpServletRequest comHeader = requestWith(
                Map.of(ArchbaseTenantRequestInterceptor.X_TENANT_ID, "tenant-b"),
                Map.of());

        new ArchbaseTenantRequestInterceptor(false).preHandle(comHeader, response, new Object());
        assertThat(ArchbaseTenantContext.getTenantId()).isEqualTo("tenant-b");
    }

    @Test
    @DisplayName("afterCompletion limpa o contexto mesmo com exceção — postHandle não rodaria")
    void afterCompletionLimpaComExcecao() {
        ArchbaseTenantContext.setTenantId("tenant-a");
        ArchbaseTenantContext.setCompanyId("company-1");

        new ArchbaseTenantRequestInterceptor().afterCompletion(
                mock(HttpServletRequest.class), response, new Object(), new IllegalStateException("falhou"));

        assertThat(ArchbaseTenantContext.getTenantId()).isNull();
        assertThat(ArchbaseTenantContext.getCompanyId()).isNull();
    }

    private HttpServletRequest requestWith(Map<String, String> headers, Map<String, String> params) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, String> h = new HashMap<>(headers);
        Map<String, String> p = new HashMap<>(params);
        when(request.getHeader(ArchbaseTenantRequestInterceptor.X_TENANT_ID))
                .thenReturn(h.get(ArchbaseTenantRequestInterceptor.X_TENANT_ID));
        when(request.getHeader(ArchbaseTenantRequestInterceptor.X_COMPANY_ID))
                .thenReturn(h.get(ArchbaseTenantRequestInterceptor.X_COMPANY_ID));
        when(request.getParameter(ArchbaseTenantRequestInterceptor.X_TENANT_ID))
                .thenReturn(p.get(ArchbaseTenantRequestInterceptor.X_TENANT_ID));
        when(request.getParameter(ArchbaseTenantRequestInterceptor.X_COMPANY_ID))
                .thenReturn(p.get(ArchbaseTenantRequestInterceptor.X_COMPANY_ID));
        return request;
    }
}
