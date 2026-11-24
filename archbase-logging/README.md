# archbase-logging

Correlation id por requisição HTTP no MDC do SLF4J (filtro servlet) e `StructuredLogger`, um utilitário estático que
põe pares chave-valor no MDC durante uma chamada de log. Não configura formato de saída: para os campos aparecerem
no log, o padrão do Logback/Log4j2 da aplicação precisa incluí-los (por exemplo `%X{correlationId}`) ou usar um
encoder que exporte o MDC.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-logging</artifactId>
    <version>3.5.0</version>
</dependency>
```

Também chega pelo `archbase-starter-messaging`.

## Auto-configuração

`ArchbaseLoggingAutoConfiguration`, registrada no `AutoConfiguration.imports`. Desligada por padrão.

| Condição | O que registra |
|---|---|
| `archbase.logging.enabled=true` | `ArchbaseLoggingProperties` |
| o acima, aplicação web, `jakarta.servlet.Filter` no classpath e `archbase.logging.correlation-id-filter-enabled` (padrão `true`) | `FilterRegistrationBean<CorrelationIdFilter>` em `/*`, ordem `Ordered.HIGHEST_PRECEDENCE`, nome `CorrelationIdFilter` |

| Propriedade | Padrão | Efeito |
|---|---|---|
| `archbase.logging.enabled` | `false` | Liga a auto-configuração |
| `archbase.logging.correlation-id-filter-enabled` | `true` | Registra o filtro |
| `archbase.logging.correlation-header` | `X-Correlation-ID` | Cabeçalho da **resposta** onde o id é devolvido (ver abaixo) |

`ArchbaseLoggingProperties` declara ainda `http-logging-enabled`, `application-name` e `http-logging.*`
(`log-request-body`, `log-response-body`, `max-payload-size`, `obscured-headers`, `ignored-uris`). **Nenhuma delas
tem efeito**: o módulo não tem código que as leia. Não há log de requisição/resposta HTTP neste módulo.

## CorrelationIdFilter

Para cada requisição:

1. Procura o id nos cabeçalhos `X-Correlation-ID`, `Correlation-ID`, `x-request-id` e `x-vcap-request-id`, nessa
   ordem; usa o primeiro não vazio.
2. Sem nenhum, gera um UUID sem hífens (`StructuredLogger.generateCorrelationId()`).
3. Põe o valor no MDC com a chave `correlationId` e no cabeçalho de resposta configurado.
4. Remove `correlationId` do MDC ao fim da requisição.

A lista de cabeçalhos lidos é fixa: `archbase.logging.correlation-header` muda só o cabeçalho de resposta, não o de
entrada. O valor recebido do cliente é usado como veio, sem validação de tamanho ou formato.

O filtro não age quando o URI **contém** `/actuator/health`, `/health`, `/readiness` ou `/liveness` — qualquer
rota com esses trechos no caminho fica sem correlation id.

Fora da auto-configuração, o filtro pode ser instanciado à mão: `new CorrelationIdFilter()` ou
`new CorrelationIdFilter("X-Meu-Header")`.

## StructuredLogger

Classe final com métodos estáticos. Todas as mensagens saem pelo logger de nome
`br.com.archbase.logging.StructuredLogger`, não pelo da classe que chama.

```java
StructuredLogger.info("Exportação iniciada", "userId", "123", "operation", "export");
StructuredLogger.error("Falha na exportação", ex, "userId", "123");

// mensagem só é montada se o nível estiver ligado
StructuredLogger.log("debug", () -> "Dado: " + calculoCaro());

StructuredLogger.withCorrelationId("job-42")
        .withField("lote", "7")
        .info("Lote processado");
```

| Método | Comportamento |
|---|---|
| `info`, `warn`, `error`, `debug`, `trace(String, Object...)` | Põe os pares no MDC (valor `null` é ignorado), registra a mensagem e limpa o MDC |
| `error(String, Throwable, Object...)` | Igual, com a exceção |
| `log(String level, Supplier<String>)` | Nível por nome (`info`, `warn`, `error`, `debug`, `trace`); nome desconhecido não registra nada |
| `generateCorrelationId()` | UUID sem hífens |
| `setCorrelationId`, `getCorrelationId` | Lê e grava `correlationId` no MDC |
| `setTracingContext(traceId, spanId)` | Grava `traceId` e `spanId` no MDC (valores `null` ignorados) |
| `withCorrelationId(id)`, `withNewCorrelationId()` | `LoggerContext` encadeável (`withField`, `info`, `warn`, `error`, `debug`, `clear`) |
| `clear()` | `MDC.clear()` |

Pontos que surpreendem:

- Número ímpar de argumentos chave-valor lança `IllegalArgumentException`.
- Depois de cada chamada com pares, o MDC é **apagado inteiro** e só `correlationId`, `traceId` e `spanId` são
  repostos. Qualquer outra chave que a aplicação ou outra biblioteca tenha posto no MDC da thread se perde.
- `withCorrelationId` e `withField` gravam no MDC e não removem: os valores ficam na thread até um `clear()`. Em
  pool de threads, chame `clear()` (do `LoggerContext` ou do `StructuredLogger`) ao terminar.
- `micrometer-tracing` é dependência opcional do pom, mas nada no módulo a usa: `traceId`/`spanId` só entram no
  MDC por `setTracingContext`.

## Referência

- Módulos do framework: [CLAUDE.md](../CLAUDE.md)
