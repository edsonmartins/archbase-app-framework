# Atualizar de 3.x para 3.5

Toda a linha 3.x já roda em **Java 25, Spring Boot 4.1 e Jackson 3** (`tools.jackson`). Subir de uma 3.x
anterior para a 3.5.0 não muda a plataforma: o trabalho é de banco, de configuração e de conferir as poucas
mudanças de comportamento listadas abaixo. Quem vem da 2.x começa por
[upgrade-2.x-para-3.x.md](upgrade-2.x-para-3.x.md).

## 1. Banco

O que entrou no esquema de segurança ao longo da 3.x:

| Versão | Tabela | Mudança |
|---|---|---|
| 3.0.11 | `seguranca_token_acesso` | `tp_uso_token` |
| 3.0.11 | `seguranca_token_api` | `token_hash`; `token` passa a ser nulável |
| 3.1.10 | `seguranca_evento`, `seguranca_revisao` | Tabelas novas (trilha e revisões) |
| 3.1.20 | `seguranca` | `employee_id` |
| 3.4.0 | `seguranca_acao` | `categoria` (a entidade já mapeava; a migration passou a criar) |
| 3.4.0 | `seguranca_acao` | `rotulo` |
| 3.4.0 | `seguranca_acao_dependencia` | Tabela nova |

Como aplicar depende de como o projeto controla o esquema:

| Configuração | O que fazer |
|---|---|
| `ddl-auto=update` | Nada. O Hibernate cria o que falta |
| Flyway com PostgreSQL e `archbase-starter-flyway` | Nada. `R__archbase_security_schema.sql` é aplicado sozinho |
| `ddl-auto=none`, sem Flyway | Nada, com `archbase.security.schema.mode=apply` (padrão): o inicializador cria o que falta no dialeto do banco |
| `ddl-auto=validate` | **Rode os scripts antes de subir.** A validação do Hibernate roda antes do inicializador e a aplicação não sobe |
| MySQL com Flyway | Desligue `archbase.flyway.include-archbase-locations` (o `R__` é PostgreSQL) e rode os scripts |

Scripts, todos idempotentes, em [`sql/`](sql/):

- `upgrade-2.x-para-3.x-{postgresql,mysql}.sql` — todas as colunas da tabela acima e a tabela de dependências.
  Serve para qualquer 3.x: o que já existe é pulado.
- `auditoria-seguranca-{postgresql,mysql}.sql` — `seguranca_evento`, `seguranca_revisao` e as tabelas `_aud`.
  Com `validate`, as duas primeiras são exigidas **mesmo com a trilha desligada**.

Para ver o DDL exato que falta no seu banco sem aplicar nada: `archbase.security.schema.mode=report`.

## 2. Mudanças de comportamento

Estas valem ao atualizar, sem configuração. Confira se alguma atinge um fluxo seu.

| Versão | Mudança | Quem sente |
|---|---|---|
| 3.0.11 | `@RequireProfile`/`@RequireRole`/`@RequirePersona` na **classe** passam a valer | Quem anotou classe e "funcionava" |
| 3.0.11 | Logout passa a revogar de fato (`POST /api/v1/auth/logout`) | Rota da aplicação no mesmo caminho: a subida falha com o conflito |
| 3.0.11 | Token de API novo é gravado só como hash | Tela que lista o valor do token |
| 3.0.11 | `@Async` passa a ver o usuário autenticado | Tarefa que dependia da ausência; desligue com `archbase.multitenancy.async.propagate-security-context=false` |
| 3.3.0 | `GET /api/v1/resource/my-permissions` devolve todas as capacidades do usuário | Aditivo |
| 3.4.x | `X-COMPANY-ID` não derruba mais a requisição quando a entidade não tem `companyFilter` | Aditivo |
| 3.4.x | Resposta de login não traz mais o hash da senha | Cliente que lia o campo (não deveria) |
| 3.4.x | 401 de token revogado diz que a sessão foi encerrada | Mensagem, não status |
| 3.4.x | Regras de arquitetura (`archbase-architecture-rules`) passam a examinar classes de verdade | Build com regras ligadas pode acusar violações que antes passavam em silêncio |
| 3.5.0 | `PATCH /api/v1/user` (troca de senha autenticada) deixou de responder 500 | Correção |
| 3.5.0 | Data URL de campo de armazenamento só é aceita em Base64 canônico e até `archbase.storage.data-url.max-bytes` (padrão 20 MiB); o token do Control Plane também é conferido antes de decodificar (mitigação do CVE-2026-114938 — a correção é o JDK atualizado) | Cliente que envia Base64 com quebra de linha, espaço ou alfabeto URL-safe, ou arquivo acima do limite: passa a receber erro |
| 3.5.0 | **`archbase.security.enabled=false` passa a valer.** Antes, com o `archbase-starter-core`, a varredura dele registrava a segurança do Archbase assim mesmo. Agora nada do módulo é registrado e, com o Spring Security no classpath, vale a cadeia padrão do Spring Boot (autenticação exigida em todas as rotas); a subida registra um WARN | Quem tinha a propriedade em `false` e contava com a segurança do Archbase: remova a linha |
| 3.5.0 | `@Async` passa a receber o usuário autenticado também com o `archbase-starter`. A promessa da 3.0.11 só valia sem o starter-core: com ele, o `@Async` rodava num executor sem o decorator. O executor continua o mesmo (`SimpleAsyncTaskExecutor`, uma thread por tarefa); se a aplicação declara `taskExecutor` ou um único `TaskExecutor`, nada muda | Tarefa que dependia da ausência; desligue com `archbase.multitenancy.async.propagate-security-context=false` |
| 3.5.0 | O `archbase-starter-security` registra sozinho os componentes de segurança e traz o `archbase-starter-multitenancy` e o `spring-boot-starter-validation`; o `archbase-starter-multitenancy` registra sozinho o resolvedor de tenant. Antes, os dois só funcionavam com o `archbase-starter-core` | Aditivo. Quem varre `br.com.archbase` e declara os repositórios de segurança continua igual: o starter não os declara de novo |
| 3.5.0 | `archbase.web.mvc.enabled=false` não leva mais a segurança junto (antes o contexto não subia) e o `@EnableJpaAuditing` do starter-core não exige mais o `auditorAware` da segurança quando ela está ausente ou desligada | Correção |
| 3.5.0 | `CPF.of`/`CNPJ.of` do `archbase-value-objects` passam a aceitar documentos válidos (antes recusavam todos, por um erro no primeiro dígito verificador) e o `CNPJ` passa a aceitar o formato alfanumérico | Correção. Quem contornava com o construtor pode voltar ao `of` |
| 3.5.0 | `CPF.instance(s)` do `archbase-shared-kernel` guarda o número sem máscara, como o `setInternalCpf` já fazia; `CNPJ.instance(s)` aceita CNPJ formatado e alfanumérico. Os dois ganham `of(s)` e `isValido()`, que conferem os dígitos; `instance` continua sem conferir | Quem comparava `getInternalCpf()` com o texto formatado |
| 3.5.0 | `save`, `validate` e `validateGroup` dos services genéricos (`CommonArchbaseService`, `SimpleArchbaseService` e as versões `Command`) passam a recusar objeto inválido com `ConstraintViolationException`, sem gravar. Antes o resultado da validação era descartado. A injeção do validator também deixa de exigir o bean `validator`, que no Spring Boot 4 costuma não existir (o service não subia) | Quem gravava objetos que violam as próprias restrições: corrija o objeto, ou sobrescreva `verificarRestricoes` |
| 3.5.0 | `ConstraintViolationException` (direta ou como causa) responde 400 com `fieldErrors` no `archbase-error-handling`; antes caía no tratamento genérico e respondia 500 | Cliente que tratava esse 500 |
| 3.5.0 | **Negação de `@HasPermission` volta a responder 403** (código `ACCESS_DENIED`). Com o Spring Security 7 ela lança `AuthorizationDeniedException`, subclasse de `AccessDeniedException`; o `archbase-error-handling` comparava a classe exata e respondia 500. As demais exceções de segurança mapeadas também passam a valer para subclasses | Cliente que tratava esse 500 como falta de permissão |
| 3.5.0 | **`archbase-starter-offline-sync` não tira mais da aplicação as entidades e os repositórios dela.** O starter declarava `@EntityScan` e `@EnableJpaRepositories`: o Boot parava de procurar entidades no pacote da aplicação e recuava na criação de repositórios. Agora o pacote do módulo soma aos da aplicação | Quem contornava declarando `@EntityScan`/`@EnableJpaRepositories` na aplicação pode manter |

Proteções novas que entraram **desligadas** — o padrão reproduz o comportamento anterior — estão em
[security-hardening.md](security-hardening.md), com a referência rápida do que ligar em produção. As da 3.3.0
mais relevantes são `archbase.security.audit.tenant-scoped` e `archbase.analytics.secret-validation`. A 3.5.0
acrescenta `archbase.security.company.validation`, que confere o `X-COMPANY-ID` contra o usuário — aplicações que
usam empresa como fronteira de dados devem ligá-la.

## 3. Módulos novos (opt-in)

Nenhum é carregado sem a dependência **e** a configuração:

| Versão | Módulo | Para quê |
|---|---|---|
| 3.2.0 | `archbase-analytics` / `archbase-starter-analytics` | Proxy auditado para a camada semântica Cube |
| 3.4.x | `archbase-controlplane` | Licença, assentos, provisionamento e métricas do Control Plane |
| 3.5.0 | `archbase-security-oidc` | Tokens de IdP externo (ArchGuard, Keycloak, Entra ID, Auth0, Okta), troca de token, logout, back-channel e migração de identidades — ver [README](../archbase-security-oidc/README.md) |

## 4. Configuração

Nenhuma propriedade obrigatória nova na 3.x (`jwt.*`, `whitelist` e `cors.*` já eram obrigatórias). Nova e opcional:
`archbase.storage.data-url.max-bytes`. Confira, porém, que a aplicação usa os nomes que o código lê — a
documentação antiga trazia nomes que nunca existiram:

| Errado (ignorado) | Certo |
|---|---|
| `archbase.security.jwt.secret` | `archbase.security.jwt.secret-key` |
| `archbase.security.jwt.expiration` | `archbase.security.jwt.token-expiration` (e `refresh-expiration`) |
| `archbase.security.method.enabled`, `archbase.security.permission.cache.*` | Não existem; não há cache de permissões |
| `archbase.multitenancy.scan-packages` | `archbase.security.scan-packages` (varredura de `@HasPermission`) |

## 5. Conferência depois de subir

1. O log da subida não tem `Schema validation` nem `oidc.schema`/`archbase.security.schema` com erro.
2. O WARN do validador de hardening lista as proteções inertes — compare com a referência rápida de
   [security-hardening.md](security-hardening.md).
3. Login, refresh, logout e uma chamada com token de API funcionam.
4. Com `archbase.security.diagnostics.enabled=true` temporariamente, `GET /api/v1/security/diagnostics/overview`
   mostra o catálogo de capacidades esperado.
