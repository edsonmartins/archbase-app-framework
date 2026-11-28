# archbase-starter-analytics

Auto-configuração do [archbase-analytics](../archbase-analytics/README.md): o proxy auditado até o Cube e o
backend das consultas salvas.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-analytics</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase.

## O que traz

Só `archbase-analytics` (e, por ele, `spring-boot-starter-web`, Jackson 3, `spring-security-core`).

## O que é preciso ligar

Adicionar o starter não registra nada. Para o analytics funcionar, a aplicação precisa:

| O quê | Por quê |
|---|---|
| `archbase.analytics.enabled=true` | Sem isso, nenhum bean é criado |
| `archbase.analytics.secret` com 32 bytes ou mais, igual ao `CUBEJS_API_SECRET` do Cube | Assina o token de escopo |
| `archbase.analytics.secret-validation=fail` | Recomendado: o padrão `warn` deixa subir com segredo vazio |
| Beans `DataScopeProvider` e `AnalyticsAuditPort` | Sem os dois, o proxy não é registrado |
| Bean `SavedQueryStorePort` | Sem ele, as consultas salvas não são registradas |
| Autenticação no `SecurityContextHolder` (por exemplo, `archbase-security`) | Os controllers respondem 401 a quem não está autenticado |

## Auto-configuração

Registrada: `ArchbaseAnalyticsAutoConfiguration`, condicionada a `archbase.analytics.enabled=true` (sem
`matchIfMissing`: desligada por padrão). Habilita `AnalyticsProperties`.

| Bean | Condição |
|---|---|
| `CubeTokenMinter` | `@ConditionalOnMissingBean`. Antes de criá-lo, roda `CubeSecretValidator.validar` (`warn`/`fail`/`off`) |
| `AnalyticsProxyController` | Beans `DataScopeProvider` **e** `AnalyticsAuditPort` presentes; `@ConditionalOnMissingBean` |
| `SavedQueryController` | Bean `SavedQueryStorePort` presente; `@ConditionalOnMissingBean` |

Qualquer um dos três pode ser substituído declarando um bean do mesmo tipo. Faltar uma porta não impede a subida:
o controller correspondente simplesmente não existe e as rotas respondem 404.

Propriedades, rotas e comportamento do proxy estão no [README do módulo](../archbase-analytics/README.md).

## Referência

- Módulo: [archbase-analytics](../archbase-analytics/README.md)
- Hardening do segredo: [deployment/security-hardening.md](../deployment/security-hardening.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
