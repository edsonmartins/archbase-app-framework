package br.com.archbase.logging;

import br.com.archbase.logging.StructuredLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Correlation id e MDC")
class CorrelacaoEMdcTest {

    @AfterEach
    void limpar() {
        MDC.clear();
    }

    private static String idVistoPelaAplicacao(CorrelationIdFilter filtro, MockHttpServletRequest pedido,
                                               MockHttpServletResponse resposta) throws Exception {
        AtomicReference<String> visto = new AtomicReference<>();
        filtro.doFilter(pedido, resposta, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest r, jakarta.servlet.ServletResponse s) {
                visto.set(MDC.get("correlationId"));
            }
        });
        return visto.get();
    }

    @Test
    @DisplayName("o cabeçalho configurado é lido no pedido (antes, só escrito na resposta)")
    void cabecalhoConfigurado() throws Exception {
        var filtro = new CorrelationIdFilter("X-Trace-Empresa");
        var pedido = new MockHttpServletRequest("GET", "/api/v1/pedidos");
        pedido.addHeader("X-Trace-Empresa", "abc-123");
        var resposta = new MockHttpServletResponse();

        assertThat(idVistoPelaAplicacao(filtro, pedido, resposta)).isEqualTo("abc-123");
        assertThat(resposta.getHeader("X-Trace-Empresa")).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("health check por segmento: /actuator/health e /readiness pulam; /api/healthcare não")
    void healthPorSegmento() throws Exception {
        var filtro = new CorrelationIdFilter();
        for (String pula : new String[]{"/actuator/health", "/actuator/health/liveness", "/readiness", "/api/v1/liveness"}) {
            assertThat(idVistoPelaAplicacao(filtro, new MockHttpServletRequest("GET", pula), new MockHttpServletResponse()))
                    .as(pula).isNull();
        }
        for (String filtra : new String[]{"/api/healthcare", "/pedidos/liveness-report", "/api/v1/clientes"}) {
            assertThat(idVistoPelaAplicacao(filtro, new MockHttpServletRequest("GET", filtra), new MockHttpServletResponse()))
                    .as(filtra).isNotBlank();
        }
    }

    @Test
    @DisplayName("log com pares chave-valor não apaga o que a aplicação pôs no MDC (antes, apagava tudo)")
    void mdcPreservado() {
        MDC.put("tenant", "t1");
        MDC.put("pedido", "p-anterior");

        StructuredLogger.info("processando", "pedido", "p-42", "etapa", "faturar");

        assertThat(MDC.get("tenant")).isEqualTo("t1");
        assertThat(MDC.get("pedido")).as("valor anterior restaurado").isEqualTo("p-anterior");
        assertThat(MDC.get("etapa")).as("chave só do log sai").isNull();
    }
}
