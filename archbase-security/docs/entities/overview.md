[← Voltar ao Índice](../README.md) | [Próximo: Entidades Core →](core-entities.md)

---

# Visão Geral das Entidades de Segurança

> **Conferido contra o código.** Tabelas, colunas, tipos e nulabilidade desta pasta foram
> conferidos contra `archbase-security/src/main/java/br/com/archbase/security/persistence/*.java`
> e contra `PersistenceEntityBase` / `TenantPersistenceEntityBase`
> (`archbase-domain-driven-design-spec`) na versão **3.5.0**. Se o código mudar, o código vale.

## As 16 entidades

A lista oficial é a constante `ENTIDADES` de `ArchbaseSecuritySchemaInitializer`: é o que a rotina
de esquema (`archbase.security.schema.mode`) pode criar, e nada fora dela.

| Classe | Tabela | `@TenantId` | Auditada (Envers) | Documento |
|--------|--------|:-----------:|:-----------------:|-----------|
| `SecurityEntity` (abstrata) | `SEGURANCA` | sim | sim | [core](core-entities.md#securityentity) |
| `UserEntity` | `SEGURANCA` (`TP_SEGURANCA='USUARIO'`) | sim | sim | [core](core-entities.md#userentity) |
| `GroupEntity` | `SEGURANCA` (`TP_SEGURANCA='SEGURANCA_GRUPO'`) | sim | sim | [core](core-entities.md#groupentity) |
| `ProfileEntity` | `SEGURANCA` (`TP_SEGURANCA='SEGURANCA_PERFIL'`) | sim | sim | [core](core-entities.md#profileentity) |
| `UserGroupEntity` | `SEGURANCA_GRUPO_USUARIO` | sim | sim | [core](core-entities.md#usergroupentity) |
| `AccessScheduleEntity` | `SEGURANCA_HORARIO_ACESSO` | sim | não | [core](core-entities.md#accessscheduleentity) |
| `AccessIntervalEntity` | `SEGURANCA_INTERVALO_ACESSO` | sim | não | [core](core-entities.md#accessintervalentity) |
| `ResourceEntity` | `SEGURANCA_RECURSO` | sim | sim | [permissões](permission-entities.md#resourceentity) |
| `ActionEntity` | `SEGURANCA_ACAO` | sim | sim | [permissões](permission-entities.md#actionentity) |
| `ActionDependencyEntity` | `SEGURANCA_ACAO_DEPENDENCIA` | sim | não | [permissões](permission-entities.md#actiondependencyentity) |
| `PermissionEntity` | `SEGURANCA_PERMISSAO` | sim | sim | [permissões](permission-entities.md#permissionentity) |
| `AccessTokenEntity` | `SEGURANCA_TOKEN_ACESSO` | sim | não | [tokens](token-entities.md#accesstokenentity) |
| `ApiTokenEntity` | `SEGURANCA_TOKEN_API` | sim | não | [tokens](token-entities.md#apitokenentity) |
| `PasswordResetTokenEntity` | `SEGURANCA_TOKEN_REDEFINICAO_SENHA` | sim | não | [tokens](token-entities.md#passwordresettokenentity) |
| `SecurityEventEntity` | `SEGURANCA_EVENTO` | **não** | não | [abaixo](#securityevententity--seguranca_evento) |
| `ArchbaseSecurityRevision` | `SEGURANCA_REVISAO` | **não** | é a revisão | [abaixo](#archbasesecurityrevision--seguranca_revisao) |

São 13 tabelas físicas: as quatro primeiras classes dividem `SEGURANCA`.

**Nome físico.** As anotações usam maiúsculas; o nome que chega ao banco depende da naming
strategy da aplicação. Com o padrão do Spring Boot ele fica em minúsculas (`seguranca_token_api`),
como nos scripts de `deployment/sql/`.

## Colunas herdadas

Todas as entidades, exceto `SecurityEventEntity` e `ArchbaseSecurityRevision`, estendem
`TenantPersistenceEntityBase`, que estende `PersistenceEntityBase`.

| Campo | Coluna base | Tipo Java | Nulável | Observação |
|-------|-------------|-----------|:-------:|------------|
| `id` | `ID` (40) | `String` | não (PK) | **Sempre renomeada** por `@AttributeOverride`: `ID_SEGURANCA`, `ID_PERMISSAO`, `ID_TOKEN`… Não é UUID no tipo: é `String`. |
| `code` | `CODIGO` (40) | `String` | sim | Também renomeada em todas as entidades do módulo (`CD_SEGURANCA`, `CD_PERMISSAO`…). Nenhuma tabela de segurança tem `CODIGO`. |
| `version` | `VERSAO` | `Long` | sim | `@Version` — bloqueio otimista. |
| `createEntityDate` | `DH_CRIACAO` | `LocalDateTime` | sim | **Não** é preenchida pelo listener (sem `@CreatedDate`, de propósito): fica com o que o código atribuir. |
| `createdByUser` | `USUARIO_CRIOU` | `String` | sim | `@CreatedBy`, preenchida pelo `AuditingEntityListener` via `AuditorAware`. |
| `updateEntityDate` | `DH_ATUALIZACAO` | `LocalDateTime` | sim | Idem `DH_CRIACAO`: sem `@LastModifiedDate`. |
| `lastModifiedByUser` | `ULTIMO_USUARIO_ALTEROU` | `String` | sim | `@LastModifiedBy`. |
| `tenantId` | `TENANT_ID` (40) | `String` | sim | `@TenantId` do Hibernate: preenchida na gravação e filtrada na leitura pelo tenant corrente. |

Tabela de ids e códigos por entidade:

| Entidade | Id | Código |
|----------|----|--------|
| `SecurityEntity` (e subclasses) | `ID_SEGURANCA` | `CD_SEGURANCA` |
| `UserGroupEntity` | `ID_USUARIO_GRUPO` | `CD_USUARIO_GRUPO` |
| `AccessScheduleEntity` | `ID_HORARIO_ACESSO` | `CD_HORARIO_ACESSO` |
| `AccessIntervalEntity` | `ID_INTERVALO_ACESSO` | `CD_INTERVALO_ACESSO` |
| `ResourceEntity` | `ID_RECURSO` | `CD_RECURSO` |
| `ActionEntity` | `ID_ACAO` | `CD_ACAO` |
| `ActionDependencyEntity` | `ID_DEPENDENCIA` | `CD_DEPENDENCIA` |
| `PermissionEntity` | `ID_PERMISSAO` | `CD_PERMISSAO` |
| `AccessTokenEntity` | `ID_TOKEN` | `CD_TOKEN` |
| `ApiTokenEntity` | `ID_TOKEN_API` | `CD_TOKEN_API` |
| `PasswordResetTokenEntity` | `ID_TOKEN` | `CD_TOKEN` |

## Convenções de mapeamento

- **Booleanos `BO_*`** (e `CONTA_BLOQUEADA`, `TOKEN_REVOGADO`, `TOKEN_EXPIRADO`) usam
  `BooleanToSNConverter`: gravam `'S'`/`'N'` em `VARCHAR(1)`. O conversor grava `'N'` para `null` e
  lê qualquer coisa diferente de `'S'` como `false`. A exceção é `SEGURANCA_EVENTO.BO_SUCESSO`, que
  não tem conversor e usa o booleano nativo do banco.
- **Níveis** (`ProfileEntity.ACCESS_LEVEL`, `ActionEntity.MINIMUM_LEVEL`) usam `AccessLevelConverter`
  e gravam o nome do enum `AccessLevel`: `NONE`, `READER`, `OPERATOR`, `SUPERVISOR`, `TENANT_ADMIN`.
- **Efeito** da permissão (`EFFECT`) usa `PermissionEffectConverter`: `GRANT` ou `DENY`.
- **Herança SINGLE_TABLE** em `SEGURANCA`, discriminador `TP_SEGURANCA`. Colunas que só existem
  numa subclasse (ex.: `SENHA`, `ACCESS_LEVEL`) ficam nulas nas linhas das outras.

## Mapa de relacionamentos

```
SecurityEntity (SEGURANCA, abstrata)
├── UserEntity
│   ├── profile         → ProfileEntity        (PERFIL_ID)
│   ├── accessSchedule  → AccessScheduleEntity (HORARIO_ACESSO_ID)
│   ├── groups          ← UserGroupEntity      (mappedBy user)
│   └── tokens          ← AccessTokenEntity    (mappedBy user)
├── GroupEntity
└── ProfileEntity

UserGroupEntity      → UserEntity (ID_USUARIO), → GroupEntity (ID_GRUPO)
PermissionEntity     → SecurityEntity (ID_SEGURANCA: usuário, grupo ou perfil), → ActionEntity (ID_ACAO)
ActionEntity         → ResourceEntity (ID_RECURSO)
ActionDependencyEntity → ActionEntity (ID_ACAO), → ActionEntity (ID_ACAO_REQUERIDA, nulável)
AccessScheduleEntity ← AccessIntervalEntity (ID_HORARIO_ACESSO)
AccessTokenEntity    → UserEntity (ID_USUARIO)
ApiTokenEntity       → UserEntity (ID_SEGURANCA)
PasswordResetTokenEntity → UserEntity (ID_USUARIO)
```

`UserEntity` não tem coleção de tokens de API nem de tokens de redefinição: essas relações só
existem do lado do token.

## Trilha de auditoria (Envers)

Estão anotadas com `@Audited`: `SecurityEntity` e as três subclasses, `UserGroupEntity`,
`PermissionEntity`, `ActionEntity` e `ResourceEntity`. A trilha **só é gravada com
`archbase.security.audit.enabled=true`**. Com `false` (o padrão), `ArchbaseSecurityAuditConfiguration`
desliga a integração do Envers inteira: nada é gravado e nenhuma tabela `_AUD` é mapeada ou exigida.

Ligada, ela define o sufixo `_AUD`, a coluna de revisão `ID_REVISAO` e a de tipo `TP_REVISAO`. As
tabelas são `SEGURANCA_AUD`, `SEGURANCA_GRUPO_USUARIO_AUD`, `SEGURANCA_PERMISSAO_AUD`,
`SEGURANCA_ACAO_AUD` e `SEGURANCA_RECURSO_AUD`, mais `SEGURANCA_REVISAO`. Crie-as **antes** de virar a
chave, pelos scripts `deployment/sql/auditoria-seguranca-postgresql.sql` ou `-mysql.sql`. Na ordem
inversa, a primeira escrita derruba a aplicação.

Ficam fora da trilha, de propósito: `UserEntity.tokens` (`@NotAudited`: token é sessão, não
permissão) e o conteúdo do horário de acesso (`accessSchedule` com `targetAuditMode = NOT_AUDITED`:
a troca de horário entra, o horário não). `ActionDependencyEntity` também não é auditada: a aresta
muda por deploy, não por gente.

### ArchbaseSecurityRevision — `SEGURANCA_REVISAO`

Entidade de revisão do Envers (`@RevisionEntity(ArchbaseSecurityRevisionListener.class)`). Não estende
a base. Índices `IDX_SEGURANCA_REVISAO_TENANT` (`TENANT_ID`) e `IDX_SEGURANCA_REVISAO_DH` (`DH_REVISAO`).

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_REVISAO` | `long` | não (PK) | `@RevisionNumber`, sequência `SEGURANCA_REVISAO_SEQ` (allocationSize 50). |
| `timestamp` | `DH_REVISAO_MILIS` | `long` | sim | `@RevisionTimestamp`, epoch em ms (contrato do Envers). |
| `dataHora` | `DH_REVISAO` | `LocalDateTime` | sim | O mesmo instante, legível. |
| `usuario` | `USUARIO` (255) | `String` | sim | Nulo quando a alteração não veio de requisição autenticada. |
| `tenantId` | `TENANT_ID` (255) | `String` | sim | Coluna comum, **sem** `@TenantId`. |
| `origem` | `ORIGEM` (100) | `String` | sim | Endereço de origem; atrás de proxy, ver `archbase.security.client-ip.trust-forwarded-for`. |

### SecurityEventEntity — `SEGURANCA_EVENTO`

O que a trilha do Envers não cobre porque não muda estado: login, falha de login, logout, acesso
negado, simulação. Não estende a base. Só é gravada com `archbase.security.audit.enabled=true`
(`ArchbaseSecurityEventLogger` fica inerte com `false`). Índices `IDX_SEGURANCA_EVENTO_DH`,
`IDX_SEGURANCA_EVENTO_TENANT` e `IDX_SEGURANCA_EVENTO_USUARIO`.

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_EVENTO` (40) | `String` | não (PK) | |
| `tipo` | `TP_EVENTO` (40) | `SecurityEventType` | **não** | `@Enumerated(STRING)`: `LOGIN`, `LOGIN_FALHOU`, `LOGOUT`, `ACESSO_NEGADO`, `SIMULACAO`. |
| `dataHora` | `DH_EVENTO` | `LocalDateTime` | **não** | |
| `usuario` | `USUARIO` (255) | `String` | sim | Pode ser um e-mail inexistente (tentativa de login). |
| `tenantId` | `TENANT_ID` (255) | `String` | sim | Coluna comum, **sem** `@TenantId`. |
| `origem` | `ORIGEM` (100) | `String` | sim | |
| `recurso` | `RECURSO` (255) | `String` | sim | Quando o evento é sobre uma capacidade. |
| `acao` | `ACAO` (255) | `String` | sim | |
| `detalhe` | `DETALHE` (500) | `String` | sim | Numa negação, o portão que recusou. |
| `sucesso` | `BO_SUCESSO` | `boolean` | **não** | Booleano nativo, sem conversor `S`/`N`. |

**Por que não tem `@TenantId`** (javadoc de `SecurityEventJpaRepository.apagarAnterioresA`): a purga
da retenção, `ArchbaseAuditRetentionJob`, é `@Scheduled` e roda sem tenant no contexto. Com
`@TenantId`, o `DELETE` alcançaria só o tenant padrão e a tabela cresceria para sempre nos demais.
O isolamento fica na consulta de leitura, ligado por `archbase.security.audit.tenant-scoped=true`;
com o padrão `false`, administrador de um tenant lê os eventos de todos.

O mesmo vale, por outro motivo, para `SEGURANCA_REVISAO`: as duas são as únicas entidades do módulo
sem `@TenantId`.

**Cuidado com o enum.** O DDL gerado para PostgreSQL cria um `CHECK` com os valores de `TP_EVENTO`
(veja o script). Um tipo novo em `SecurityEventType` é recusado pelo banco nas bases já existentes
até o `CHECK` ser refeito.

## Próximos passos

- [Entidades Core](core-entities.md): usuário, grupo, perfil, vínculo usuário-grupo, horário de acesso
- [Sistema de Permissões](permission-entities.md): recurso, ação, dependência de ação, permissão
- [Gerenciamento de Tokens](token-entities.md): token de acesso/refresh, token de API, redefinição de senha

---

[← Voltar ao Índice](../README.md) | [Próximo: Entidades Core →](core-entities.md)
