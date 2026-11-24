# archbase-analytics

Lado servidor da camada semântica: um proxy auditado até o [Cube](https://cube.dev) e o backend das consultas
salvas. O navegador nunca fala com o Cube; fala com este proxy, que confere o usuário, cunha um token curto com o
escopo de dados que a aplicação define e repassa a consulta. O módulo não sabe nada do domínio: escopo, auditoria
e persistência vêm de três portas que a aplicação implementa.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-analytics</artifactId>
    <version>3.5.0</version>
</dependency>
```

Não há BOM do Archbase: declare a versão explícita e use a mesma em todos os módulos Archbase. Este módulo é só
biblioteca; quem registra os beans é o [archbase-starter-analytics](../archbase-starter-analytics/README.md).

## O que traz

`spring-boot-starter-web`, `com.fasterxml.jackson.core:jackson-databind` (Jackson 2), `spring-jdbc`,
`spring-security-core` e `micrometer-core`.

## Portas que a aplicação implementa

| Porta (`br.com.archbase.analytics.port`) | Para quê | Obrigatória para |
|---|---|---|
| `DataScopeProvider` | `Map<String, Object> claimsForUser(String username)`: as claims de escopo que vão no token do Cube (filial, região...). Mapa vazio = token sem escopo; quem nega é a policy da view no Cube | proxy |
| `AnalyticsAuditPort` | `void record(AuditedQuery query)`: chamado a cada `/load`. Recebe usuário, hash do contexto, JSON da consulta, origem, duração, linhas, truncamento, resultado e código de erro — nunca as linhas | proxy |
| `SavedQueryStorePort` | Persistência das consultas salvas: `listVisible`, `find`, `findVisibleTo`, `save`, `remove`. O payload (`queryJson`, `vizJson`) é opaco | consultas salvas |

`findVisibleTo(id, requesterId)` tem implementação padrão que delega a `find(id)`. **Em aplicação multi-tenant,
sobrescreva-o**: escopo `team`/`org` não tem dimensão de tenant no contrato, e com o padrão quem souber o id de uma
consulta compartilhada de outro tenant consegue lê-la.

```java
@Component
class EscopoPorFilial implements DataScopeProvider {
    private final UsuarioRepository usuarios;

    EscopoPorFilial(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    public Map<String, Object> claimsForUser(String username) {
        return usuarios.findFilial(username)
                .<Map<String, Object>>map(f -> Map.of("filialId", f))
                .orElse(Map.of());
    }
}
```

## Endpoints

Base: `${archbase.analytics.base-path:/api/analytics}` (o placeholder é lido direto pelo `@RequestMapping`; não há
campo para ele em `AnalyticsProperties`).

| Método e caminho | Classe | O que faz |
|---|---|---|
| `GET {base}/v1/meta` | `AnalyticsProxyController` | Repassa `/v1/meta` do Cube |
| `POST {base}/v1/load` | `AnalyticsProxyController` | Repassa a consulta. Corpo: `{"query": {...}}` ou só o objeto da consulta |
| `GET {base}/v1/load?query=<json>` | `AnalyticsProxyController` | Mesma coisa, para URL compartilhável |
| `GET {base}/saved-queries[?scope=private\|team\|org]` | `SavedQueryController` | As próprias e as `team`/`org` de qualquer dono |
| `GET {base}/saved-queries/{id}` | `SavedQueryController` | 404 se não visível |
| `POST {base}/saved-queries` | `SavedQueryController` | Cria ou atualiza; o dono é sempre o usuário autenticado (403 se o id é de outro) |
| `DELETE {base}/saved-queries/{id}` | `SavedQueryController` | Sempre 204, removida ou não |

Comportamento do proxy (`/v1/load`):

- **Autenticação**: lê o `SecurityContextHolder`; sem usuário (ou anônimo) responde 401 `UNAUTHENTICATED`. O
  módulo não autentica ninguém — depende do filtro de segurança da aplicação.
- **Token do Cube**: HS256 assinado com `archbase.analytics.secret`, com `sub`, `iat`, `exp` e as claims do
  `DataScopeProvider`. O token da aplicação nunca chega ao Cube.
- **Teto de linhas**: `limit` da consulta é forçado para o intervalo `1..row-limit`. Se a resposta atinge o
  limite, vêm os cabeçalhos `X-Analytics-Truncated: true` e `X-Analytics-Row-Limit`.
- **Concorrência**: no máximo `concurrency-per-user` consultas simultâneas por usuário; acima disso, 429
  `CONCURRENCY_LIMIT`.
- **Timeout**: `timeout-seconds` no total, incluindo as repetições enquanto o Cube responde `Continue wait`.
- **Origem**: o cabeçalho `X-Analytics-Origin: widget` é registrado como `widget`; qualquer outro valor, como
  `explorer`.
- **Erros**: o texto do Cube nunca chega ao cliente; só `{"error": {"code": ..., "retryable": ...}}`, com os
  códigos de `AnalyticsErrorCode`: `QUERY_TIMEOUT` (504), `CONCURRENCY_LIMIT` (429), `FORBIDDEN_MEMBER` (403),
  `UPSTREAM_ERROR` (502), `UNAUTHENTICATED` (401), `INVALID_QUERY` (400, `query` do GET que não é JSON).
  Corpo de POST que não é JSON responde 502 `UPSTREAM_ERROR`.

`/v1/meta` não passa pela auditoria nem pelo limite de concorrência.

## Propriedades

Prefixo `archbase.analytics` (`AnalyticsProperties`):

| Propriedade | Padrão | Efeito |
|---|---|---|
| `enabled` | `false` | Liga a auto-configuração e os dois controllers |
| `cube-url` | `http://cube-api:4000/cubejs-api` | URL interna do Cube |
| `secret` | `""` | Segredo HS256 compartilhado com o Cube (o mesmo de `CUBEJS_API_SECRET`) |
| `secret-validation` | `warn` | Conferência do segredo na subida: `warn` registra aviso, `fail` impede a subida, `off` não confere |
| `secret-min-bytes` | `32` | Tamanho mínimo do segredo, em bytes UTF-8 |
| `token-ttl-seconds` | `120` | Validade do token cunhado |
| `timeout-seconds` | `30` | Teto de tempo da consulta no proxy (deve ser maior que o do Cube) |
| `row-limit` | `50000` | Teto de linhas |
| `concurrency-per-user` | `2` | Consultas simultâneas por usuário |
| `base-path` | `/api/analytics` | Prefixo das rotas (só placeholder) |

`CubeSecretValidator` reprova segredo vazio ou com menos de `secret-min-bytes`. Com o padrão `warn`, o analytics
sobe assinando com segredo vazio — e um token assinado assim é forjável, o que anula o `DataScopeProvider`. Em
ambiente real use `secret-validation=fail`.

## Limitações

- **Exige um `ObjectMapper` do Jackson 2 como bean.** O minter e os dois controllers recebem
  `com.fasterxml.jackson.databind.ObjectMapper`, e o Spring Boot 4 só registra o do Jackson 3
  (`tools.jackson`). Nada no Archbase declara o do Jackson 2. Sem ele, ligar `enabled=true` impede a subida
  (`No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper'`). Declare-o:

  ```java
  @Bean
  com.fasterxml.jackson.databind.ObjectMapper analyticsObjectMapper() {
      return new com.fasterxml.jackson.databind.ObjectMapper();
  }
  ```

- Os controllers são `@Controller` condicionados a `archbase.analytics.enabled=true`. Uma aplicação que faz
  component scan de `br.com.archbase.analytics` com o analytics ligado os registra pela varredura, mesmo sem as
  portas — e então não sobe. Deixe o registro para o starter.
- Não há streaming: a resposta do Cube é bufferizada inteira; o teto de linhas é o que a limita.
- O semáforo de concorrência é por instância da aplicação, não distribuído.

## Referência

- Auto-configuração: [archbase-starter-analytics](../archbase-starter-analytics/README.md)
- Hardening do segredo: [deployment/security-hardening.md](../deployment/security-hardening.md)
- Módulos e padrões do framework: [CLAUDE.md](../CLAUDE.md)
