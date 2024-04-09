package br.com.archbase.event.driven.bus.middleware.logging;


import br.com.archbase.event.driven.spec.command.contracts.Command;
import br.com.archbase.event.driven.spec.message.contracts.Message;
import br.com.archbase.event.driven.spec.middleware.contracts.Middleware;
import br.com.archbase.event.driven.spec.middleware.contracts.NextMiddlewareFunction;
import br.com.archbase.event.driven.spec.query.contracts.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoggingMiddleware implements Middleware {
    private static final Log log = LogFactory.getLog(LoggingMiddleware.class);

    @Override
    public <R> R handle(Message<R> message, NextMiddlewareFunction<Message<R>, R> next) {
        log.info(String.format("Recebido %s (%s), %s foi despachado para o barramento %s",
                message.getClass().getName(), message.toString(), getMessageType(message),
                getMessageType(message)));
        try {
            R result = next.call(message);

            String serializedResult = result != null ? result.toString() : null;
            log.info(String.format("O %s %s (%s) foi tratado com sucesso com o resultado: %s",
                    getMessageType(message), message.getClass().getName(), message.toString(), serializedResult));

            return result;
        } catch (Exception ex) {
            log.error(String.format("Falha ao lidar %s (%s)", message.getClass().getName(), message.toString()), ex);
            throw ex;
        }
    }

    private String getMessageType(Message<?> message) {
        if (message instanceof Command<?>) {
            return "command";
        } else if (message instanceof Query<?>) {
            return "query";
        } else {
            return "message";
        }
    }
}
