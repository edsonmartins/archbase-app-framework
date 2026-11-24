# archbase-starter-multitenancy

Liga a parte web do multitenancy: o interceptor que põe o tenant e a empresa da requisição no
`ArchbaseTenantContext` e a propagação desse contexto para tarefas `@Async`.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-multitenancy</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

Só `archbase-multitenancy`. O que o módulo faz, e o modelo de isolamento (coluna `TENANT_ID` com `@TenantId` do
Hibernate), está no [README do módulo](../archbase-multitenancy/README.md).

## Auto-configuração

`ArchbaseMultitenancyAutoConfiguration`, ativa com `CurrentTenantIdentifierResolver` (Hibernate) no classpath e
`archbase.multitenancy.enabled` (padrão `true`). Também anota `@EnableAsync` e roda antes da auto-configuração de
tarefas do Spring Boot (ver `archbaseTenantAwareAsyncConfigurer` abaixo).

| Registra | Condição | O que é |
|---|---|---|
| Varredura de `br.com.archbase.multitenancy` | — | Resolvedor de tenant do Hibernate (`ArchbaseCurrentTenantIdentifierResolverImpl`), `TenantProperties` (`archbase.app.tenant.default.id`) e aspecto de empresa. O starter-core varre o mesmo pacote; os dois juntos não duplicam beans |
| `ArchbaseTenantRequestInterceptor` | — | Interceptor MVC: lê `X-TENANT-ID` e `X-COMPANY-ID`, limpa o contexto ao fim da requisição |
| `tenantAwareTaskDecorator` | sem bean `TaskDecorator` | `ArchbaseTenantAwareTaskDecorator`: copia tenant, empresa e (opcional) `SecurityContext` para a thread da tarefa |
| `archbaseTenantAwareAsyncConfigurer` | sem bean `AsyncConfigurer` | Não escolhe executor: o `@Async` usa o que o Spring escolheria (um único `TaskExecutor`, senão o bean `taskExecutor`). Só quando o Spring cairia no fallback — com o `archbase-starter` há dois executores e nenhum se chama `taskExecutor` — entra o mesmo `SimpleAsyncTaskExecutor` (uma thread por tarefa), agora com o decorator |

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.multitenancy.enabled` | `true` | Liga esta auto-configuração |
| `archbase.app.tenant.accept-query-param` | `true` | Interceptor aceita `X-TENANT-ID`/`X-COMPANY-ID` na query string quando o cabeçalho está ausente |
| `archbase.multitenancy.async.propagate-security-context` | `true` | Leva o usuário autenticado para dentro do `@Async` |

## Sozinho

Funciona sem o `archbase-starter-core` desde a 3.5.0: antes o resolvedor de tenant só entrava pela varredura do
core, e este starter punha o tenant no contexto sem que o Hibernate tivesse quem o lesse.

`archbase.multitenancy.enabled=false` desliga esta auto-configuração inteira. Com o starter-core presente, o
resolvedor e o aspecto continuam ativos pela varredura dele — as entidades com `@TenantId` não gravam sem resolvedor.

> A `AsyncConfig` (executor de 7 a 42 threads, fila 11) está deprecada: nunca entrou no contexto com o
> `archbase-starter`, porque a condição dela encontrava a própria classe e o Spring a descartava.

## Referência

- Comportamento completo, inclusive sem tenant no contexto: [archbase-multitenancy/README.md](../archbase-multitenancy/README.md)
- Propriedades de tenant e empresa: [archbase-security/docs/guides/configuration.md, §3](../archbase-security/docs/guides/configuration.md#3-tenant-e-empresa)
