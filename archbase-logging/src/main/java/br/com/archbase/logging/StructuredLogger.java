package br.com.archbase.logging;

import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Logger estruturado com suporte a correlation ID e campos personalizados.
 * Usa MDC (Mapped Diagnostic Context) do SLF4J para propagar contexto.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * // Com correlation ID automático
 * StructuredLogger.info("Processamento iniciado", "userId", "123", "operation", "export");
 *
 * // Com correlation ID específico
 * StructuredLogger.withCorrelationId("custom-id").info("Mensagem");
 *
 * // Com supplier lazy evaluation
 * StructuredLogger.debug(() -> "Dado: " + expensiveOperation());
 *
 * // Limpar contexto
 * StructuredLogger.clear();
 * }
 * </pre>
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class StructuredLogger {

    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String TRACE_ID_KEY = "traceId";

    /**
     * Gera um novo correlation ID.
     *
     * @return Novo correlation ID UUID
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Define o correlation ID no MDC.
     *
     * @param correlationId Correlation ID a ser definido
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    /**
     * Obtém o correlation ID atual do MDC.
     *
     * @return Correlation ID atual ou null se não definido
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Define o trace ID e span ID no MDC (para integração com tracing distribuído).
     *
     * @param traceId Trace ID
     * @param spanId  Span ID
     */
    public static void setTracingContext(String traceId, String spanId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
        if (spanId != null) {
            MDC.put(SPAN_ID_KEY, spanId);
        }
    }

    /**
     * Cria um contexto com correlation ID específico.
     *
     * @param correlationId Correlation ID a ser usado
     * @return LoggerContext para encadeamento
     */
    public static LoggerContext withCorrelationId(String correlationId) {
        return new LoggerContext(correlationId);
    }

    /**
     * Cria um contexto com novo correlation ID gerado automaticamente.
     *
     * @return LoggerContext para encadeamento
     */
    public static LoggerContext withNewCorrelationId() {
        return new LoggerContext(generateCorrelationId());
    }

    /**
     * Registra mensagem no nível INFO com campos estruturados.
     *
     * @param message Mensagem a ser logada
     * @param keysValues Pares chave-valor para incluir no contexto
     */
    public static void info(String message, Object... keysValues) {
        logWithFields("info", message, keysValues);
    }

    /**
     * Registra mensagem no nível ERROR com campos estruturados.
     *
     * @param message Mensagem a ser logada
     * @param keysValues Pares chave-valor para incluir no contexto
     */
    public static void error(String message, Object... keysValues) {
        logWithFields("error", message, keysValues);
    }

    /**
     * Registra mensagem no nível ERROR com exceção.
     *
     * @param message Mensagem a ser logada
     * @param throwable Exceção a ser logada
     * @param keysValues Pares chave-valor para incluir no contexto
     */
    public static void error(String message, Throwable throwable, Object... keysValues) {
        withFields(keysValues);
        org.slf4j.LoggerFactory.getLogger(StructuredLogger.class).error(message, throwable);
        clearFields();
    }

    /**
     * Registra mensagem no nível WARN com campos estruturados.
     *
     * @param message Mensagem a ser logada
     * @param keysValues Pares chave-valor para incluir no contexto
     */
    public static void warn(String message, Object... keysValues) {
        logWithFields("warn", message, keysValues);
    }

    /**
     * Registra mensagem no nível DEBUG com campos estruturados.
     *
     * @param message Mensagem a ser logada
     * @param keysValues Pares chave-valor para incluir no contexto
     */
    public static void debug(String message, Object... keysValues) {
        logWithFields("debug", message, keysValues);
    }

    /**
     * Registra mensagem no nível TRACE com campos estruturados.
     *
     * @param message Mensagem a ser logada
     * @param keysValues Pares chave-valor para incluir no contexto
     */
    public static void trace(String message, Object... keysValues) {
        logWithFields("trace", message, keysValues);
    }

    /**
     * Registra mensagem lazy (só executa supplier se nível estiver habilitado).
     *
     * @param level   Nível de log ("info", "warn", "error", "debug", "trace")
     * @param message Supplier da mensagem
     */
    public static void log(String level, Supplier<String> message) {
        var logger = org.slf4j.LoggerFactory.getLogger(StructuredLogger.class);
        switch (level.toLowerCase()) {
            case "info" -> { if (logger.isInfoEnabled()) logger.info(message.get()); }
            case "warn" -> { if (logger.isWarnEnabled()) logger.warn(message.get()); }
            case "error" -> { if (logger.isErrorEnabled()) logger.error(message.get()); }
            case "debug" -> { if (logger.isDebugEnabled()) logger.debug(message.get()); }
            case "trace" -> { if (logger.isTraceEnabled()) logger.trace(message.get()); }
        }
    }

    /**
     * Limpa todos os campos do MDC.
     */
    public static void clear() {
        MDC.clear();
    }

    private static void logWithFields(String level, String message, Object... keysValues) {
        withFields(keysValues);
        var logger = org.slf4j.LoggerFactory.getLogger(StructuredLogger.class);
        switch (level.toLowerCase()) {
            case "info" -> logger.info(message);
            case "warn" -> logger.warn(message);
            case "error" -> logger.error(message);
            case "debug" -> logger.debug(message);
            case "trace" -> logger.trace(message);
        }
        clearFields();
    }

    private static void withFields(Object... keysValues) {
        if (keysValues == null || keysValues.length % 2 != 0) {
            throw new IllegalArgumentException("Número par de argumentos requerido (chave, valor)");
        }
        for (int i = 0; i < keysValues.length; i += 2) {
            if (keysValues[i + 1] != null) {
                MDC.put(keysValues[i].toString(), keysValues[i + 1].toString());
            }
        }
    }

    private static void clearFields() {
        // Preserva correlationId, traceId, spanId
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        String traceId = MDC.get(TRACE_ID_KEY);
        String spanId = MDC.get(SPAN_ID_KEY);
        MDC.clear();
        if (correlationId != null) MDC.put(CORRELATION_ID_KEY, correlationId);
        if (traceId != null) MDC.put(TRACE_ID_KEY, traceId);
        if (spanId != null) MDC.put(SPAN_ID_KEY, spanId);
    }

    /**
     * Contexto de logger com correlation ID específico.
     */
    public static class LoggerContext {
        private final String correlationId;
        private final Map<String, String> additionalFields;

        private LoggerContext(String correlationId) {
            this.correlationId = correlationId;
            this.additionalFields = Collections.emptyMap();
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }

        private LoggerContext(String correlationId, Map<String, String> fields) {
            this.correlationId = correlationId;
            this.additionalFields = fields;
            MDC.put(CORRELATION_ID_KEY, correlationId);
            fields.forEach(MDC::put);
        }

        /**
         * Adiciona campos ao contexto.
         *
         * @param key   Chave
         * @param value Valor
         * @return Novo contexto com campos adicionais
         */
        public LoggerContext withField(String key, String value) {
            Map<String, String> newFields = new java.util.HashMap<>(additionalFields);
            newFields.put(key, value);
            MDC.put(key, value);
            return new LoggerContext(correlationId, newFields);
        }

        /**
         * Registra mensagem no nível INFO.
         *
         * @param message Mensagem a ser logada
         * @return Este contexto
         */
        public LoggerContext info(String message) {
            org.slf4j.LoggerFactory.getLogger(StructuredLogger.class).info(message);
            return this;
        }

        /**
         * Registra mensagem no nível WARN.
         *
         * @param message Mensagem a ser logada
         * @return Este contexto
         */
        public LoggerContext warn(String message) {
            org.slf4j.LoggerFactory.getLogger(StructuredLogger.class).warn(message);
            return this;
        }

        /**
         * Registra mensagem no nível ERROR.
         *
         * @param message Mensagem a ser logada
         * @return Este contexto
         */
        public LoggerContext error(String message) {
            org.slf4j.LoggerFactory.getLogger(StructuredLogger.class).error(message);
            return this;
        }

        /**
         * Registra mensagem no nível ERROR com exceção.
         *
         * @param message   Mensagem a ser logada
         * @param throwable Exceção a ser logada
         * @return Este contexto
         */
        public LoggerContext error(String message, Throwable throwable) {
            org.slf4j.LoggerFactory.getLogger(StructuredLogger.class).error(message, throwable);
            return this;
        }

        /**
         * Registra mensagem no nível DEBUG.
         *
         * @param message Mensagem a ser logada
         * @return Este contexto
         */
        public LoggerContext debug(String message) {
            org.slf4j.LoggerFactory.getLogger(StructuredLogger.class).debug(message);
            return this;
        }

        /**
         * Limpa o contexto.
         */
        public void clear() {
            StructuredLogger.clear();
        }
    }

    /**
     * Propriedades de configuração do logging.
     */
    public static class Properties {
        public static final String CORRELATION_HEADER_DEFAULT = "X-Correlation-ID";
        public static final String CORRELATION_HEADER_PROPERTY = "archbase.logging.correlation-header";
    }
}
