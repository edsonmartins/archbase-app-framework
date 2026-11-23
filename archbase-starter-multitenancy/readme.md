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
`archbase.multitenancy.enabled` (padrão `true`). Também anota `@EnableAsync`.

| Registra | Condição | O que é |
|---|---|---|
| `ArchbaseTenantRequestInterceptor` | — | Interceptor MVC: lê `X-TENANT-ID` e `X-COMPANY-ID`, limpa o contexto ao fim da requisição |
| `tenantAwareTaskDecorator` | sem bean `TaskDecorator` | `ArchbaseTenantAwareTaskDecorator`: copia tenant, empresa e (opcional) `SecurityContext` para a thread da tarefa |
| `asyncConfig` | sem bean `AsyncConfigurer` | `AsyncConfig`: executor do `@Async` (7 a 42 threads, fila 11, prefixo `TenantAwareTaskExecutor-`) com o decorator acima |

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.multitenancy.enabled` | `true` | Liga esta auto-configuração |
| `archbase.app.tenant.accept-query-param` | `true` | Interceptor aceita `X-TENANT-ID`/`X-COMPANY-ID` na query string quando o cabeçalho está ausente |
| `archbase.multitenancy.async.propagate-security-context` | `true` | Leva o usuário autenticado para dentro do `@Async` |

## O que não vem deste starter

O resolvedor de tenant do Hibernate (`ArchbaseCurrentTenantIdentifierResolverImpl`), o `TenantProperties`
(`archbase.app.tenant.default.id`) e o aspecto que liga o filtro de empresa são `@Component` do
`archbase-multitenancy`. **Nenhuma auto-configuração os registra**: eles entram pela varredura do
[`archbase-starter-core`](../archbase-starter-core/readme.md#a-varredura-de-pacotes), que inclui
`br.com.archbase.multitenancy`.

Consequências:

- Este starter sozinho, sem o starter-core (ou sem a aplicação varrer `br.com.archbase.multitenancy`), põe o tenant
  no contexto mas o Hibernate não tem quem o leia.
- `archbase.multitenancy.enabled=false` desliga só o interceptor e a propagação para `@Async`. Com o starter-core
  presente, o resolvedor e o aspecto continuam ativos.

Use o [`archbase-starter`](../archbase-starter/readme.md), que traz os dois.

## Referência

- Comportamento completo, inclusive sem tenant no contexto: [archbase-multitenancy/README.md](../archbase-multitenancy/README.md)
- Propriedades de tenant e empresa: [archbase-security/docs/guides/configuration.md, §3](../archbase-security/docs/guides/configuration.md#3-tenant-e-empresa)
