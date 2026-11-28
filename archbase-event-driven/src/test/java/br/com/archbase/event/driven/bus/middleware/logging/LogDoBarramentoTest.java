package br.com.archbase.event.driven.bus.middleware.logging;

import br.com.archbase.event.driven.spec.command.contracts.Command;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoggingMiddleware: conteúdo só em DEBUG")
class LogDoBarramentoTest {

    record CadastrarCliente(String cpf) implements Command<String> {
    }

    private final Logger logger = (Logger) LoggerFactory.getLogger(LoggingMiddleware.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void ligar() {
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void desligar() {
        logger.detachAppender(appender);
        logger.setLevel(null);
    }

    @Test
    @DisplayName("em INFO, nem a mensagem, nem o resultado, nem o erro levam o conteúdo")
    void semConteudoEmInfo() {
        var middleware = new LoggingMiddleware();
        middleware.handle(new CadastrarCliente("529.982.247-25"), m -> "token-secreto");
        assertThatThrownBy(() -> middleware.handle(new CadastrarCliente("111.444.777-35"), m -> {
            throw new IllegalArgumentException("recusado");
        })).isInstanceOf(IllegalArgumentException.class);

        assertThat(appender.list).isNotEmpty().noneMatch(e -> e.getFormattedMessage().contains("529.982")
                || e.getFormattedMessage().contains("111.444") || e.getFormattedMessage().contains("token-secreto"));
    }
}
