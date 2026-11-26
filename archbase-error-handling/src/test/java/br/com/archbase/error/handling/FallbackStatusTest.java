package br.com.archbase.error.handling;

import br.com.archbase.error.handling.handler.SimpleFallbackApiExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tratamento genérico: status que a exceção traz, mensagem de 5xx e substituição pela aplicação. Até a 3.5.0 só o
 * {@code @ResponseStatus} da própria classe contava — o resto virava 500.
 */
@DisplayName("Tratamento genérico de erro")
class FallbackStatusTest {

    @ResponseStatus(HttpStatus.CONFLICT)
    static class Conflito extends RuntimeException {
        Conflito(String m) {
            super(m);
        }
    }

    static class ConflitoEspecifico extends Conflito {
        ConflitoEspecifico() {
            super("já existe");
        }
    }

    private final ArchbaseErrorHandlingProperties properties = new ArchbaseErrorHandlingProperties();
    private final SimpleFallbackApiExceptionHandler handler = new SimpleFallbackApiExceptionHandler(properties);

    @Test
    @DisplayName("ResponseStatusException usa o próprio status e o motivo como mensagem")
    void responseStatusException() {
        var r = handler.handle(new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido 42 não encontrado"));
        assertThat(r.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getMessage()).isEqualTo("Pedido 42 não encontrado");
    }

    @Test
    @DisplayName("parâmetro obrigatório ausente é 400")
    void parametroAusente() {
        var r = handler.handle(new MissingServletRequestParameterException("pagina", "int"));
        assertThat(r.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("ArchbaseRuntimeException com status declarado usa esse status; o código continua o de antes")
    void archbaseRuntimeException() {
        var e = new ArchbaseRuntimeException(new StaticMessageSource(), HttpStatus.UNPROCESSABLE_CONTENT, "PEDIDO_FECHADO", "Pedido fechado");
        var r = handler.handle(e);
        assertThat(r.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(r.getCode()).isEqualTo(ArchbaseRuntimeException.class.getName());
    }

    @Test
    @DisplayName("@ResponseStatus herdado da superclasse vale")
    void responseStatusHerdado() {
        assertThat(handler.handle(new ConflitoEspecifico()).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("sem status conhecido continua 500 com a mensagem; com hide-server-error-messages, a mensagem some")
    void mensagemDe500() {
        var vazamento = new IllegalStateException("could not execute statement [insert into pedido ...]");
        assertThat(handler.handle(vazamento).getMessage()).contains("insert into pedido");

        properties.setHideServerErrorMessages(true);
        var r = handler.handle(vazamento);
        assertThat(r.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getMessage()).isEqualTo("Erro interno do servidor");
        assertThat(handler.handle(new ResponseStatusException(HttpStatus.NOT_FOUND, "não achei")).getMessage())
                .as("4xx mantém a mensagem").isEqualTo("não achei");
    }

    @Test
    @DisplayName("a aplicação troca o tratamento genérico declarando o seu, sem @Primary")
    void substituicao() {
        FallbackApiExceptionHandler proprio = e -> new ArchbaseApiErrorResponse(HttpStatus.I_AM_A_TEAPOT, "X", "x");
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ArchbaseErrorHandlingConfiguration.class))
                .withBean(FallbackApiExceptionHandler.class, () -> proprio)
                .run(ctx -> assertThat(ctx).getBean(FallbackApiExceptionHandler.class).isSameAs(proprio));
    }
}
