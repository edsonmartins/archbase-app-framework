package br.com.archbase.analytics;

import br.com.archbase.analytics.proxy.AnalyticsProxyController;
import br.com.archbase.analytics.savedquery.SavedQueryController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As rotas do analytics existem — o Spring reconhece as duas classes como handler.
 *
 * <p>Regressão real (3.2.2 → 3.3.1): trocar {@code @RestController} por
 * {@code @ResponseBody} tirou as classes do component scan, mas também as tirou do
 * mapeamento. Desde o Spring Framework 6.2, {@code isHandler()} é apenas
 * {@code hasAnnotation(beanType, Controller.class)}: sem {@code @Controller} o bean é
 * criado, a autoconfiguração reporta sucesso, e cada endpoint responde 404 — falha
 * silenciosa que só aparece no navegador de quem usa o explorador.
 *
 * <p>O teste pergunta ao próprio {@code RequestMappingHandlerMapping}, e não a uma cópia
 * da regra: se o Spring mudar o critério de novo, é aqui que se descobre.
 */
class ControllersSaoHandlersTest {

    private final MappingSonda mapping = new MappingSonda();

    @Test
    @DisplayName("o proxy do Cube é reconhecido como handler")
    void proxyEHandler() {
        assertThat(mapping.ehHandler(AnalyticsProxyController.class)).isTrue();
    }

    @Test
    @DisplayName("o controller de consultas salvas é reconhecido como handler")
    void savedQueryEHandler() {
        assertThat(mapping.ehHandler(SavedQueryController.class)).isTrue();
    }

    /** {@code isHandler} é protected; a sonda só o torna alcançável do teste. */
    private static class MappingSonda extends RequestMappingHandlerMapping {
        boolean ehHandler(Class<?> tipo) {
            return isHandler(tipo);
        }
    }
}
