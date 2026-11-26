# archbase-starter-messaging

Starter leve para workers e serviços de mensageria: módulos Archbase sem JPA de aplicação e sem Spring Security,
mais um `ObjectMapper` do Jackson 3. Alternativa ao `archbase-starter-core` para quem não precisa de repositório,
segurança nem MVC.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-messaging</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

| Dependência | Observação |
|---|---|
| `archbase-error-handling` | O `@ControllerAdvice` só age em aplicação web servlet (ver abaixo) |
| `archbase-validation` | Validação fluente e Bean Validation; traz `hibernate-validator` |
| `archbase-domain-driven-design-spec` | Contratos DDD; traz `hibernate-core` e `spring-data-commons`, mas nenhuma auto-configuração JPA |
| `archbase-event-driven-spec` | Só contratos e anotações CQRS. **Não** traz `CommandBus`/`EventBus`/`QueryBus`: esses estão em `archbase-event-driven` |
| `archbase-logging` | Logging estruturado, desligado por padrão |
| `spring-boot-starter`, `spring-boot-starter-validation`, `tools.jackson.core:jackson-databind` | |

`spring-boot-starter-activemq` e `spring-boot-starter-amqp` são `optional` no pom: **não** chegam à aplicação.
Declare o do seu broker (ou Kafka, que o starter não menciona).

## O que é preciso ligar

| Para ter | Faça |
|---|---|
| Broker (JMS, RabbitMQ, Kafka) | Adicione o starter do Spring Boot correspondente |
| Logging estruturado | `archbase.logging.enabled=true` (ver [archbase-logging](../archbase-logging/README.md)) |
| Tratamento de erro HTTP | Ser aplicação web servlet (`spring-boot-starter-web`); `archbase.error.handling.enabled` já vem `true` |
| Buses CQRS | Adicionar `archbase-event-driven` |

## Auto-configuração

Registrada: `ArchbaseMessagingAutoConfiguration`. Sem condição de classe ou propriedade: carrega sempre que o
starter está no classpath.

| O que | Condição | Efeito |
|---|---|---|
| Bean `archbaseMessagingObjectMapper` (`tools.jackson.databind.ObjectMapper`) | sem outro `ObjectMapper` do Jackson 3 | `JsonMapper` com `FAIL_ON_EMPTY_BEANS` e `FAIL_ON_UNKNOWN_PROPERTIES` desligados e `ACCEPT_EMPTY_STRING_AS_NULL_OBJECT` ligado |
| `@ComponentScan` de `br.com.archbase.error.handling`, `br.com.archbase.validation`, `br.com.archbase.logging` | — | ver abaixo |

O que a varredura efetivamente registra:

- `br.com.archbase.error.handling`: `ArchbaseErrorHandlingConfiguration` só vale em aplicação web servlet. Num
  worker sem `spring-boot-starter-web` (que o starter não traz) nada dali entra.
- `br.com.archbase.validation`: nenhum componente (o `ArchbaseValidationExceptionAdvice` é `@Aspect` sem
  `@Component`). A validação fluente é biblioteca: use as classes diretamente.
- `br.com.archbase.logging`: `ArchbaseLoggingAutoConfiguration`, que já viria pelo `.imports` do próprio módulo e
  exige `archbase.logging.enabled=true`.

O `ObjectMapper` registrado é o do Jackson 3. Código que injeta `com.fasterxml.jackson.databind.ObjectMapper`
(Jackson 2) não o recebe e precisa declarar o seu.

## Limitações

Em aplicação web, a varredura de `br.com.archbase.error.handling` registra `ErrorHandlingControllerAdvice` como
componente, além do bean que `ArchbaseErrorHandlingConfiguration` já declara: ficam duas instâncias do advice.

## Referência

- Logging: [archbase-logging](../archbase-logging/README.md)
- Validação: [archbase-validation](../archbase-validation/README.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
