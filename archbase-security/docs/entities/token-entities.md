[← Anterior: Sistema de Permissões](permission-entities.md) | [Voltar ao Índice](../README.md) | [Próximo: Sistema de Autenticação →](../architecture/authentication-system.md)

---

# Gerenciamento de Tokens

Colunas conferidas contra `persistence/*.java` na versão 3.5.0. As colunas herdadas da base
(`VERSAO`, `DH_CRIACAO`, `USUARIO_CRIOU`, `DH_ATUALIZACAO`, `ULTIMO_USUARIO_ALTEROU`, `TENANT_ID`)
estão em [Visão Geral](overview.md#colunas-herdadas). "Nulável" é o que o mapeamento declara.
Nenhuma das três entidades é auditada pelo Envers.

| Classe | Tabela | Para quê |
|--------|--------|----------|
| `AccessTokenEntity` | `SEGURANCA_TOKEN_ACESSO` | JWT de acesso **e** de refresh emitidos pelo próprio Archbase |
| `ApiTokenEntity` | `SEGURANCA_TOKEN_API` | Token de API (UUID) para integração |
| `PasswordResetTokenEntity` | `SEGURANCA_TOKEN_REDEFINICAO_SENHA` | Código numérico de redefinição de senha |

---

## AccessTokenEntity

**Classe**: `br.com.archbase.security.persistence.AccessTokenEntity`
**Tabela**: `SEGURANCA_TOKEN_ACESSO`

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_TOKEN` (40) | `String` | não (PK) | |
| `code` | `CD_TOKEN` (40) | `String` | sim | |
| `token` | `TOKEN` (5000) | `String` | sim | `unique = true`. O JWT inteiro. |
| `tokenType` | `TP_TOKEN` (50) | `TokenType` | sim | `@Enumerated(STRING)`. Único valor: `BEARER`. |
| `tokenUse` | `TP_USO_TOKEN` (20) | `TokenUse` | sim | `@Enumerated(STRING)`: `ACCESS` ou `REFRESH`. Nulo em linhas anteriores à coluna, que eram todas de access. |
| `revoked` | `TOKEN_REVOGADO` (1) | `boolean` | sim | `S`/`N`. |
| `expired` | `TOKEN_EXPIRADO` (1) | `boolean` | sim | `S`/`N`. |
| `expirationTime` | `TEMPO_EXPIRACAO` | `Long` | sim | Instante de expiração em **epoch ms** (`TokenResult.expiresIn()`). |
| `expirationDate` | `DH_EXPIRACAO` | `LocalDateTime` | sim | O mesmo instante, como data. |
| `user` | `ID_USUARIO` | `UserEntity` | sim | `@ManyToOne(LAZY)`. |

### Como é usado

- **Login** grava duas linhas: o access (`TP_USO_TOKEN = 'ACCESS'`) e o refresh
  (`TP_USO_TOKEN = 'REFRESH'`). O refresh **é persistido**: é o que permite revogá-lo no logout, na
  troca de senha e na desativação da conta.
- **Cada requisição** com JWT próprio confere a assinatura e a validade (`ArchbaseJwtService`) **e**
  a linha em banco: o token precisa existir, sem `TOKEN_REVOGADO` nem `TOKEN_EXPIRADO`. Como a
  entidade tem `@TenantId`, a linha é procurada no tenant do contexto.
- **Sessão única.** Com `BO_PERMITE_MULTIPLICOS_LOGINS` diferente de `'S'`, um novo login revoga
  todos os tokens anteriores do usuário (access e refresh). Com `'S'`, o login reaproveita o access
  que ainda vale e emite só um refresh novo.
- **Refresh** (`POST /api/v1/auth/refresh-token`) revoga o refresh apresentado (ou todos, em sessão
  única) e emite um par novo.
- **Revogação em lote**: `revokeAllUserTokens` faz um `UPDATE` em lote, não carrega-e-salva.

### Claims do JWT

`ArchbaseJwtService` emite `sub` (o `userName`), `iat`, `exp`, `jti`, `token_use` (`access`,
`refresh` ou `mfa_challenge`) e `tenantId` quando o usuário tem tenant. O token de desafio de MFA
leva ainda `mfa = challenge` e não é persistido. Não há claim de id de usuário, empresa ou papéis.

Validade, em **milissegundos**:

```properties
archbase.security.jwt.secret-key=<chave em Base64, >= 256 bits>
archbase.security.jwt.token-expiration=3600000
archbase.security.jwt.refresh-expiration=604800000
```

As três não têm valor padrão: sem elas a aplicação não sobe (placeholder não resolvido).

---

## ApiTokenEntity

**Classe**: `br.com.archbase.security.persistence.ApiTokenEntity`
**Tabela**: `SEGURANCA_TOKEN_API`

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_TOKEN_API` (40) | `String` | não (PK) | |
| `code` | `CD_TOKEN_API` (40) | `String` | sim | |
| `name` | `NOME` | `String` | **não** | |
| `description` | `DESCRICAO` | `String` | **não** | |
| `user` | `ID_SEGURANCA` | `UserEntity` | **não** | `@ManyToOne`. As permissões do token são as deste usuário. |
| `token` | `TOKEN` | `String` | **sim** | Valor em claro. **Nulo** nas linhas criadas a partir da 3.0.11 com `archbase.security.api-token.hash-enabled=true` (padrão). Sem restrição única. |
| `tokenHash` | `TOKEN_HASH` (64) | `String` | sim | SHA-256 do token, hex minúsculo (`ApiTokenHasher`). **É por aqui que a autenticação busca.** Nulo só em linha antiga ainda não migrada. |
| `revoked` | `BO_REVOGADO` (1) | `Boolean` | **não** | `S`/`N`. |
| `expirationDate` | `DH_EXPIRACAO` | `LocalDateTime` | **não** | Obrigatória: não existe token de API sem expiração. |
| `activated` | `BO_ATIVADO` (1) | `Boolean` | **não** | `S`/`N`. Nasce `N`. |

### Como é usado

- **Criação** (`ApiTokenService.createToken`): o token é um `UUID.randomUUID()`. Grava-se sempre
  `TOKEN_HASH`; `TOKEN` só com `hash-enabled=false`. O valor em claro aparece uma única vez, na
  resposta da criação (e no e-mail de ativação). O token nasce com `BO_ATIVADO = 'N'`.
- **Ativação**: `GET /api/v1/apiToken/activate`.
- **Autenticação**: o filtro reconhece um UUID no `Authorization: Bearer` e procura
  `token_hash = sha256(valor)` (ou, para linha não migrada, `token_hash IS NULL AND token = valor`),
  com `BO_REVOGADO = 'N'`, `BO_ATIVADO = 'S'` e `DH_EXPIRACAO > agora`. Havendo tenant no contexto, a
  linha também tem de ser desse tenant.
- **Migração**: `ArchbaseApiTokenHashMigrator` calcula o hash das linhas antigas na subida.
  `archbase.security.api-token.purge-plaintext=true` apaga o valor em claro — **irreversível**, e o
  validador de hardening recusa a subida se ainda houver linha sem hash.

---

## PasswordResetTokenEntity

**Classe**: `br.com.archbase.security.persistence.PasswordResetTokenEntity`
**Tabela**: `SEGURANCA_TOKEN_REDEFINICAO_SENHA`

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_TOKEN` (40) | `String` | não (PK) | Mesmo nome de coluna que em `SEGURANCA_TOKEN_ACESSO`; não existe `ID_TOKEN_REDEFINICAO`. |
| `code` | `CD_TOKEN` (40) | `String` | sim | |
| `token` | `TOKEN` (13) | `String` | sim | Sem restrição única. |
| `revoked` | `TOKEN_REVOGADO` (1) | `boolean` | sim | `S`/`N`. |
| `expired` | `TOKEN_EXPIRADO` (1) | `boolean` | sim | `S`/`N`. |
| `expirationTime` | `TEMPO_EXPIRACAO` | `Long` | sim | **Duração em segundos** (`PasswordResetToken.EXPIRATION_TIME_UNIT = SECONDS`), não instante. Padrão `3600`. |
| `user` | `ID_USUARIO` | `UserEntity` | sim | `@ManyToOne(LAZY)`. |

### Como é usado

- `POST /api/v1/auth/sendResetPasswordEmail/{email}` gera o código com
  `TokenGeneratorUtil.generateNumericToken()`: **8 dígitos** de `SecureRandom` (a coluna comporta 13).
  Códigos anteriores ainda vigentes do usuário são revogados.
- A expiração é calculada: `DH_CRIACAO + TEMPO_EXPIRACAO segundos` antes de agora.
- `POST /api/v1/auth/resetPassword` confere o código contra o usuário, troca a senha e revoga o
  código e as sessões do usuário.
- Com `archbase.security.prevent-user-enumeration=true`, a resposta da solicitação é uniforme, exista
  ou não o e-mail.

---

## Resumo comparativo

| | `AccessTokenEntity` | `ApiTokenEntity` | `PasswordResetTokenEntity` |
|---|---|---|---|
| Id | `ID_TOKEN` | `ID_TOKEN_API` | `ID_TOKEN` |
| Valor | JWT em `TOKEN` (único) | UUID; busca por `TOKEN_HASH` | 8 dígitos em `TOKEN` |
| Usuário | `ID_USUARIO` | `ID_SEGURANCA` (obrigatório) | `ID_USUARIO` |
| Expiração | `TEMPO_EXPIRACAO` (epoch ms) e `DH_EXPIRACAO` | `DH_EXPIRACAO` (obrigatória) | `TEMPO_EXPIRACAO` (duração em s) |
| Revogação | `TOKEN_REVOGADO` | `BO_REVOGADO` | `TOKEN_REVOGADO` |
| Específico | `TP_USO_TOKEN` (`ACCESS`/`REFRESH`) | `BO_ATIVADO` | — |

---

[← Anterior: Sistema de Permissões](permission-entities.md) | [Voltar ao Índice](../README.md) | [Próximo: Sistema de Autenticação →](../architecture/authentication-system.md)
