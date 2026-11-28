# archbase-error-handling

`@ControllerAdvice` que converte toda exceção lançada por um `@RestController` numa resposta JSON padronizada
(`code`, `message`, erros de campo e propriedades extras), com códigos configuráveis por propriedade.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-error-handling</artifactId>
    <version>3.5.0</version>
</dependency>
```

Chega pelo `archbase-domain-driven-design` (portanto por `archbase-starter-core` e `archbase-starter`) e pelo
`archbase-starter-messaging`.

## O que traz

Dependências de compilação: `spring-orm`, `spring-security-core` e `jackson-annotations`. `spring-boot-starter-web`
é `provided`: a aplicação precisa ter Spring MVC.

## Auto-configuração

`ArchbaseErrorHandlingConfiguration`, registrada no `AutoConfiguration.imports`. Condições: aplicação web
**servlet** e `archbase.error.handling.enabled` diferente de `false` (padrão `true`). Registra:

| Bean | Tipo | Condição extra |
|---|---|---|
| `errorHandlingControllerAdvice` | `ErrorHandlingControllerAdvice` | — |
| `defaultHandler` | `SimpleFallbackApiExceptionHandler` (`FallbackApiExceptionHandler`) | sem outro `FallbackApiExceptionHandler` |
| `typeMismatchApiExceptionHandler` | `TypeMismatchArchbaseApiExceptionHandler` | — |
| `springValidationApiExceptionHandler` | `SpringValidationArchbaseApiExceptionHandler` | — |
| `beanValidationApiExceptionHandler` | `BeanValidationArchbaseApiExceptionHandler` | classe `jakarta.validation.ConstraintViolationException` presente |
| `springSecurityApiExceptionHandler` | `SpringSecurityArchbaseApiExceptionHandler` | classe `AccessDeniedException` presente |
| `objectOptimisticLockingFailureApiExceptionHandler` | `ObjectOptimisticLockingFailureArchbaseApiExceptionHandler` | classe `ObjectOptimisticLockingFailureException` presente |

Só o `defaultHandler` tem `@ConditionalOnMissingBean`: declare um `FallbackApiExceptionHandler` para trocá-lo (até a 3.5.0 era preciso `@Primary`). A configuração também carrega, via `@PropertySource`, o arquivo
`error-handling-defaults.properties` com os códigos padrão (tabela abaixo); valores do `application.properties`
têm precedência.

| Propriedade | Padrão | Efeito |
|---|---|---|
| `archbase.error.handling.enabled` | `true` | `false` desliga todo o módulo |
| `archbase.error.handling.exception-logging` | `MESSAGE_ONLY` | `NO_LOGGING`, `MESSAGE_ONLY` (só a mensagem) ou `WITH_STACKTRACE`. Resposta 5xx em ERROR; 4xx em WARN, com a pilha só em DEBUG |
| `archbase.error.handling.hide-server-error-messages` | `false` | `true` troca a mensagem das respostas 5xx do tratamento genérico por `Erro interno do servidor`; o log continua completo |
| `archbase.error.handling.codes.<chave>` | ver abaixo | Troca o `code` da resposta. Chave: nome completo da classe da exceção, código da restrição de validação (`NotNull`) ou `campo.Restrição` (`email.NotBlank`) |
| `archbase.error.handling.messages.<chave>` | — | Troca a `message` de um erro de campo. Chave: `campo.Restrição` ou `Restrição` |

## Como uma exceção vira resposta

`ErrorHandlingControllerAdvice` tem um único `@ExceptionHandler` para `Throwable` e se aplica só a classes
anotadas com `@RestController` (`@ControllerAdvice(annotations = RestController.class)`). Para cada exceção:

1. percorre os beans `ArchbaseApiExceptionHandler` em ordem (`@Order`/`Ordered`) e usa o primeiro cujo
   `canHandle` devolve `true`;
2. se nenhum aceita, usa o `FallbackApiExceptionHandler`;
3. registra em log conforme `exception-logging` e o status da resposta.

O `SimpleFallbackApiExceptionHandler` usa, na ordem: o status que a exceção traz (`ErrorResponse` do Spring —
`ResponseStatusException`, `MissingServletRequestParameterException`, `HandlerMethodValidationException` — ou o
`getStatusCode()` de `ArchbaseRuntimeException`); o `@ResponseStatus` da classe ou de uma superclasse; senão 500.
Para `ErrorResponse`, a mensagem é o `detail`.

| Handler | Exceções | Status | Extras na resposta |
|---|---|---|---|
| Validação | `MethodArgumentNotValidException` | 400 | `fieldErrors`, `globalErrors` |
| Validação | `HttpMessageNotReadableException` | 400 | — |
| Bean Validation | `ConstraintViolationException`, também quando é a causa (até 10 níveis) de outra exceção, como a `TransactionSystemException` do commit | 400 | `fieldErrors` (o valor recusado só quando é simples: texto, número, booleano, enum, data) |
| Tipo | `TypeMismatchException` e subclasses (`MethodArgumentTypeMismatchException`) | 400 | `property`, `rejectedValue`, `expectedType` |
| Segurança | `AccessDeniedException` e subclasses, como a `AuthorizationDeniedException` da negação de `@HasPermission` (código `ACCESS_DENIED`) | 403 | — |
| Segurança | `AuthenticationCredentialsNotFoundException`, `InsufficientAuthenticationException` (e subclasses, como em toda esta tabela de segurança) | 401 | — |
| Segurança | `AuthenticationServiceException` | 500 | — |
| Segurança | `AccountExpiredException`, `BadCredentialsException`, `UsernameNotFoundException`, `LockedException`, `DisabledException` | 400 | — |
| Lock otimista | `ObjectOptimisticLockingFailureException` | 409 | `identifier`, `persistentClassName` |
| Fallback | qualquer outra | `@ResponseStatus` da classe, senão 500 | campos/métodos com `@ArchbaseResponseErrorProperty` |

O `code` é o nome completo da classe da exceção, trocado pelo valor de `archbase.error.handling.codes.<classe>` se
houver. No fallback, `@ArchbaseResponseErrorCode("MEU_CODIGO")` na classe da exceção tem prioridade sobre o nome da
classe (e também pode ser trocado por propriedade). A `message` é `exception.getMessage()`, exceto em
`MethodArgumentNotValidException` (texto fixo com o nome do objeto e a contagem de erros).

Códigos padrão de `error-handling-defaults.properties`: `VALIDATION_FAILED`, `MESSAGE_NOT_READABLE`,
`TYPE_MISMATCH`, `ARGUMENT_TYPE_MISMATCH`, `ACCESS_DENIED`, `ACCOUNT_EXPIRED`, `AUTH_CREDENTIALS_NOT_FOUND`,
`AUTH_SERVICE_ERROR`, `BAD_CREDENTIALS`, `USERNAME_NOT_FOUND`, `INSUFFICIENT_AUTHENTICATION`, `ACCOUNT_LOCKED`,
`ACCOUNT_DISABLED`, `OPTIMISTIC_LOCKING_ERROR`, e para restrições de Bean Validation (`NotNull` →
`REQUIRED_NOT_NULL`, `Size` → `INVALID_SIZE`, `Email` → `INVALID_EMAIL` etc.; lista completa no arquivo).

### Formato

`ArchbaseApiErrorResponse` é serializado com `@JsonInclude(NON_EMPTY)`: listas vazias e nulos somem; o status HTTP
não aparece no corpo; propriedades extras ficam no nível raiz (`@JsonAnyGetter`).

```json
{
  "code": "VALIDATION_FAILED",
  "message": "A validação falhou para o objeto='clienteDto'. Contagem de erros: 1",
  "fieldErrors": [
    { "code": "REQUIRED_NOT_BLANK", "property": "nome", "message": "must not be blank", "rejectedValue": "" }
  ]
}
```

## Uso

### Exceção própria

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
@ArchbaseResponseErrorCode("CLIENTE_NAO_ENCONTRADO")
public class ClienteNaoEncontradoException extends RuntimeException {

    @ArchbaseResponseErrorProperty
    private final String clienteId;

    public ClienteNaoEncontradoException(String clienteId) {
        super("Cliente não encontrado: " + clienteId);
        this.clienteId = clienteId;
    }
}
```

Resposta: 404, `{"code":"CLIENTE_NAO_ENCONTRADO","message":"Cliente não encontrado: 42","clienteId":"42"}`.

`@ArchbaseResponseErrorProperty` aceita `value` (nome da propriedade no JSON; vazio usa o nome do campo ou da
propriedade do getter) e `includeIfNull` (padrão `false`). Em campos, só os declarados na própria classe da exceção
são lidos (`getDeclaredFields`), não os herdados; em métodos, só os públicos sem parâmetros.

### Handler próprio

Declare um bean que implemente `ArchbaseApiExceptionHandler` (ou estenda `AbstractArchbaseApiExceptionHandler`, que
aplica `archbase.error.handling.codes`). Os handlers do módulo não têm `@Order`; dê `@Order` ao seu para garantir que
ele seja consultado antes.

```java
@Component
@Order(0)
public class ConflitoHandler extends AbstractArchbaseApiExceptionHandler {

    public ConflitoHandler(ArchbaseErrorHandlingProperties properties) {
        super(properties);
    }

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof DataIntegrityViolationException;
    }

    @Override
    public ArchbaseApiErrorResponse handle(Throwable exception) {
        return new ArchbaseApiErrorResponse(HttpStatus.CONFLICT, getErrorCode(exception), "Registro em conflito");
    }
}
```

Para trocar o fallback, declare outro `FallbackApiExceptionHandler` com `@Primary`; sem isso, há dois candidatos e o
contexto não sobe.

## Limitações

- Sem `hide-server-error-messages=true`, a `message` de um 500 é a `getMessage()` da exceção: mensagens internas
  (SQL, nomes de classes, caminhos) vão para o cliente. Ligue em produção.
- O `code` de uma `ArchbaseRuntimeException` continua sendo o nome da classe, não o `getCode()` dela: trocar mudaria
  o campo para quem já o compara. Para um código próprio, use `archbase.error.handling.codes.<classe>` ou
  `@ArchbaseResponseErrorCode`.
- Até a 3.5.0, só o `@ResponseStatus` da própria classe contava (todos os casos acima respondiam 500), todo erro saía
  em ERROR, e o arquivo `META-INF/spring.factories` (chaves que o Boot 4 ignora) duplicava o registro.
- Só cobre `@RestController`. Exceções em `@Controller` com `@ResponseBody`, em filtros (inclusive os do Spring
  Security) e em aplicações reativas não passam por aqui.

## Referência

- [archbase-starter-core](../archbase-starter-core/README.md)
- Módulos do framework: [CLAUDE.md](../CLAUDE.md)
