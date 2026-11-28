package br.com.archbase.resource.logger.aspect;

import br.com.archbase.resource.logger.annotation.Logging;
import br.com.archbase.resource.logger.annotation.NoLogging;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("SimpleArchbaseResourceAspect")
class AspectoDeRecursoTest {

    @RestController
    @Logging
    public static class ComLog {
        @GetMapping(value = "/pedido", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> pedido() {
            return Map.of("numero", 42);
        }
    }

    @RestController
    @Logging
    @NoLogging
    public static class Silenciado {
        @GetMapping("/a")
        public String silencioso() {
            return "a";
        }

        @Logging
        @GetMapping("/b")
        public String reativado() {
            return "b";
        }
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    private static <T> T comAspecto(T alvo, Logger log) {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(alvo);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new SimpleArchbaseResourceAspect(log));
        return (T) fabrica.getProxy();
    }

    @Test
    @DisplayName("@GetMapping com produces JSON: o retorno é serializado (antes, toString do Map) e sem autenticação não há erro")
    void getMappingSerializaEPassaSemAutenticacao() {
        Logger log = mock(Logger.class);
        comAspecto(new ComLog(), log).pedido();

        ArgumentCaptor<String> mensagens = ArgumentCaptor.forClass(String.class);
        verify(log, atLeastOnce()).info(mensagens.capture());
        assertThat(mensagens.getAllValues()).anyMatch(m -> m.contains("pedido() retornou: [{\"numero\":42}]"));
        assertThat(mensagens.getAllValues()).as("linha de entrada, que a NPE sem autenticação suprimia")
                .anyMatch(m -> m.startsWith("pedido() chamado via"));
        verify(log, never()).error(anyString(), org.mockito.ArgumentMatchers.<Throwable>any());
    }

    @Test
    @DisplayName("@NoLogging na classe silencia os métodos, salvo os com @Logging")
    void noLoggingNaClasse() {
        Logger log = mock(Logger.class);
        Silenciado proxy = comAspecto(new Silenciado(), log);

        proxy.silencioso();
        verify(log, never()).info(anyString());

        proxy.reativado();
        verify(log, atLeastOnce()).info(anyString());
    }

    @Test
    @DisplayName("o construtor padrão usa a categoria do próprio aspecto (antes, java.lang.String)")
    void categoriaDoLogger() throws Exception {
        var campo = SimpleArchbaseResourceAspect.class.getDeclaredField("log");
        campo.setAccessible(true);
        assertThat(((Logger) campo.get(new SimpleArchbaseResourceAspect())).getName())
                .isEqualTo(SimpleArchbaseResourceAspect.class.getName());
    }
}
