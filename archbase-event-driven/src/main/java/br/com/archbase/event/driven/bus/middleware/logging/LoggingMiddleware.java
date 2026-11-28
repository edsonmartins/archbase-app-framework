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

    /**
     * Em INFO, só o tipo da mensagem; o conteúdo ({@code toString()} da mensagem e do resultado) sai em DEBUG. Até a
     * 3.5.0 o conteúdo ia para o log em INFO em todo despacho — inclusive o do erro —, e com ele CPF, senha ou o que
     * mais a mensagem carregasse.
     */
    @Override
    public <R> R handle(Message<R> message, NextMiddlewareFunction<Message<R>, R> next) {
        String tipo = getMessageType(message);
        String classe = message.getClass().getName();
        log.info(String.format("Recebido %s %s", tipo, classe));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Conteúdo do %s %s: %s", tipo, classe, message));
        }
        try {
            R result = next.call(message);
            log.info(String.format("O %s %s foi tratado com sucesso", tipo, classe));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Resultado do %s %s: %s", tipo, classe, result));
            }
            return result;
        } catch (Exception ex) {
            log.error(String.format("Falha ao lidar %s %s", tipo, classe), ex);
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
