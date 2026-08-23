package br.com.archbase.security.config;

import br.com.archbase.security.access.AccessDecision;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;

/**
 * Apoio comum aos adaptadores que ligam as anotações de autorização ao core.
 *
 * <p>Os adaptadores existem para que a conversão para o core não mude ordem de execução nem
 * semântica de falha das aplicações em produção: cada anotação continua com seu pointcut e seu
 * interceptador. O que se unificou foi a <b>regra</b>, não a interceptação — a unificação do
 * interceptador é passo próprio, atrás de flag, depois do core em produção. Ver
 * {@code MODELO_CORE_AUTORIZACAO.md}.
 */
final class AuthorizationAdapters {

    private AuthorizationAdapters() {
    }

    static String declaringClass(MethodInvocation invocation) {
        return invocation.getMethod().getDeclaringClass().getName();
    }

    /** Assinatura do método, para diagnóstico e para o aviso único por método. */
    static String origin(MethodInvocation invocation) {
        return declaringClass(invocation) + "#" + invocation.getMethod().getName();
    }

    /**
     * Registra a decisão.
     *
     * <p>Negação sai em {@code debug} como antes — um 403 esperado não é incidente. O que mudou é
     * que agora existe o motivo: quem investiga um acesso negado lê o portão e o código em vez de
     * deduzir a partir de um booleano.
     */
    static void log(Logger log, AccessDecision decisao, String origem) {
        if (log.isDebugEnabled()) {
            log.debug("{} em {}", decisao.summary(), origem);
        }
    }
}
