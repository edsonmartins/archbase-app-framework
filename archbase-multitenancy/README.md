# archbase-multitenancy

Isolamento de dados por tenant **numa coluna** das tabelas, com o `@TenantId` do Hibernate 7, e filtro opcional por
empresa dentro do tenant. Não há suporte a banco ou esquema separado por tenant.

| Peça | Classe | Papel |
|---|---|---|
| Contexto | `ArchbaseTenantContext` (em `archbase-domain-driven-design-spec`) | Tenant e empresa da thread atual |
| Interceptor | `ArchbaseTenantRequestInterceptor` | Lê `X-TENANT-ID` / `X-COMPANY-ID` da requisição para o contexto |
| Resolvedor do Hibernate | `ArchbaseCurrentTenantIdentifierResolverImpl` | Diz ao Hibernate qual é o tenant atual |
| Aspecto de empresa | `ArchbaseTenantServiceAspect` | Liga o filtro `companyFilter` em chamadas de repositório |
| Tarefas assíncronas | `ArchbaseTenantAwareTaskDecorator`, `AsyncConfig` | Levam o contexto para a thread do `@Async` |
| Tenant padrão | `TenantProperties` | `archbase.app.tenant.default.id` |

## Como as peças entram no contexto

O módulo não tem auto-configuração própria. São dois caminhos, e uma aplicação normalmente precisa dos dois:

| Peças | Registradas por |
|---|---|
| Resolvedor, aspecto de empresa, `TenantProperties`, `AsyncConfig` | Varredura de `br.com.archbase.multitenancy` feita pelo [`archbase-starter-core`](../archbase-starter-core/readme.md#a-varredura-de-pacotes) (com `archbase.web.mvc.enabled`, padrão `true`) |
| Interceptor, decorator de tarefa, `AsyncConfig` | [`archbase-starter-multitenancy`](../archbase-starter-multitenancy/readme.md) (com `archbase.multitenancy.enabled`, padrão `true`) |

O [`archbase-starter`](../archbase-starter/readme.md) traz os dois starters. `archbase.multitenancy.enabled=false`
não desliga o resolvedor nem o aspecto.

## Contexto

`ArchbaseTenantContext` guarda dois valores por thread, em `InheritableThreadLocal`: `tenantId` e `companyId`.
`clear()` remove os dois.

## Isolamento por tenant

Entidades que estendem `TenantPersistenceEntityBase` herdam a coluna `TENANT_ID` (até 40 caracteres) anotada com
`@TenantId` (`org.hibernate.annotations`). O Hibernate preenche a coluna ao inserir e restringe as consultas dessas
entidades ao tenant atual, que pergunta ao resolvedor.

```java
@Entity
@Table(name = "PEDIDO")
public class Pedido extends TenantPersistenceEntityBase {
    // id String (UUID gerado no construtor), colunas de auditoria e TENANT_ID herdados
}
```

Entidades que estendem `PersistenceEntityBase` não são isoladas.

O resolvedor (`ArchbaseCurrentTenantIdentifierResolverImpl`) se registra no Hibernate pela propriedade
`hibernate.tenant_identifier_resolver`, via `HibernatePropertiesCustomizer` do Spring Boot.

### Sem tenant no contexto

O resolvedor decide nesta ordem:

1. Há tenant no `ArchbaseTenantContext`: usa-o.
2. `archbase.app.tenant.fail-on-missing=true`: lança `IllegalStateException` e a operação não acontece.
3. `archbase.app.tenant.default.id` configurado: usa-o.
4. Nada configurado: usa `archbase` e registra WARN.

O padrão (`fail-on-missing=false`) faz uma operação sem tenant ler e gravar no tenant padrão, sem erro. É
conveniente para tarefas de sistema e perigoso em requisição de usuário. O Control Plane exige `true`.

## Isolamento por empresa

Opcional, dentro do tenant. Entidades que estendem `CompanyPersistenceEntityBase` têm `TENANT_ID` (com `@TenantId`)
e `COMPANY_ID`, e declaram o filtro Hibernate `companyFilter` (`COMPANY_ID = :companyId`).

- **Gravação:** o `CompanyEntityListener` copia a empresa do contexto para `companyId` em `@PrePersist`,
  `@PreUpdate` e `@PreRemove`.
- **Leitura:** o `ArchbaseTenantServiceAspect` envolve todo método público de `Repository`. Se há empresa no
  contexto, liga o `companyFilter` na sessão com esse valor.

O filtro não é desligado ao fim da chamada: vale para o resto da sessão, inclusive JPQL direto pelo
`EntityManager`. Se nenhuma entidade estende `CompanyPersistenceEntityBase`, o filtro não existe e a empresa do
contexto é ignorada (log em DEBUG).

## Requisição HTTP

`ArchbaseTenantRequestInterceptor`, registrado pelo `archbase-starter-multitenancy`:

- **`preHandle`:** lê `X-TENANT-ID` e `X-COMPANY-ID` do cabeçalho; se o cabeçalho estiver vazio e
  `archbase.app.tenant.accept-query-param=true`, da query string. Valor vazio não altera o contexto.
- A empresa **não** é posta no contexto se o filtro de segurança marcou a requisição com o atributo
  `ArchbaseTenantContext.COMPANY_DISCARDED_ATTRIBUTE` (`archbase.company.discarded`) — ver abaixo.
- **`afterCompletion`:** `ArchbaseTenantContext.clear()`. Roda também quando o controller lança exceção.

O interceptor não aplica o tenant padrão: sem cabeçalho, o contexto fica vazio e quem decide é o resolvedor.

Tenant em query string vai parar em access log, histórico do navegador e `Referer`. Em produção, prefira
`archbase.app.tenant.accept-query-param=false`.

### Com o `archbase-security`

O `ArchbaseJwtAuthenticationFilter` roda antes do interceptor e trata o tenant primeiro:

- lê os mesmos cabeçalhos, obedecendo a mesma `accept-query-param`;
- sem `X-TENANT-ID`, põe no contexto o `archbase.app.tenant.default.id`, se houver;
- com token autenticado que traz tenant (claim do JWT, linha do token de API ou token externo), o tenant do token
  vence: `X-TENANT-ID` diferente é recusado com 403;
- com `archbase.security.company.validation` (`off` | `warn` | `enforce`, padrão `off`) confere o `X-COMPANY-ID`
  contra o usuário. Exige um bean `ArchbaseCompanyAccessResolver`, porque o vínculo usuário–empresa é da aplicação.
  Em `enforce`: empresa não permitida é `403 COMPANY_NOT_ALLOWED`, e em requisição anônima a empresa é descartada e
  marcada com `COMPANY_DISCARDED_ATTRIBUTE`, o que impede o interceptor de recolocá-la. Em `warn`, só registra.

Detalhes em [archbase-security/docs/guides/configuration.md, §3](../archbase-security/docs/guides/configuration.md#3-tenant-e-empresa)
e no [guia de endurecimento](../deployment/security-hardening.md).

## Tarefas assíncronas

O `ArchbaseTenantAwareTaskDecorator` captura, na thread que submete a tarefa, o tenant, a empresa e — com
`archbase.multitenancy.async.propagate-security-context=true` (padrão) — o `SecurityContext`. Instala-os na thread
que executa e, no fim, **restaura** o que a thread tinha antes (ou limpa, se não tinha nada).

Onde ele é aplicado:

| Executor | Usa o decorator? |
|---|---|
| `AsyncConfig` — executor do `@Async` (7 a 42 threads, fila 11), quando a aplicação não declara `AsyncConfigurer` | Sim |
| `threadPoolTaskExecutor` do `archbase-starter-core` (bean `TaskExecutor`) | Não: é montado à mão, sem decorator |

O `archbase-starter-multitenancy` também registra o decorator como bean `TaskDecorator` (`tenantAwareTaskDecorator`),
mas nenhum executor do Archbase o consome por injeção.

Quem declara o próprio `AsyncConfigurer` ou `TaskExecutor` precisa pôr o decorator nele:

```java
executor.setTaskDecorator(new ArchbaseTenantAwareTaskDecorator(true));
```

## Propriedades

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.multitenancy.enabled` | `true` | Liga o `archbase-starter-multitenancy` (interceptor, decorator, `AsyncConfig`) |
| `archbase.multitenancy.async.propagate-security-context` | `true` | Propaga o usuário autenticado para o `@Async` |
| `archbase.app.tenant.default.id` | — | Tenant usado sem tenant no contexto |
| `archbase.app.tenant.fail-on-missing` | `false` | Recusa a operação sem tenant no contexto |
| `archbase.app.tenant.accept-query-param` | `true` | Aceita `X-TENANT-ID`/`X-COMPANY-ID` na query string |
| `archbase.security.company.validation` | `off` | Do `archbase-security`; ver acima |

Não existe `archbase.multitenancy.scan-packages`.

## Referência

- Propriedades de tenant e empresa: [archbase-security/docs/guides/configuration.md](../archbase-security/docs/guides/configuration.md#3-tenant-e-empresa)
- Tenant no login e no token: [archbase-security/readme-security.md](../archbase-security/readme-security.md)
- Visão geral do framework: [CLAUDE.md](../CLAUDE.md)
