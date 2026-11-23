# Proposta: IdP externo (OIDC) no archbase-security

> **Estado:** proposta, nada implementado. Base: análise do código em 2026-11-20 — archbase-security
> (`main` @ 6551982) e ArchGuard (`c7165c42`).

## 1. Objetivo

Permitir que uma aplicação Archbase aceite access tokens emitidos por um **IdP OIDC externo** — o
ArchGuard e também IdPs de mercado e open source (Keycloak, Entra ID, Auth0, Okta, Zitadel,
Authentik…) — **convivendo** com o login local (JWT HS256) e com o Control Plane.

Não-objetivos nesta fase: o Archbase virar IdP; SAML; federação de saída (SCIM out).

### Premissas (definidas em 2026-11-20)

- **P1 — Não quebrar o que funciona.** Aplicação que não adiciona o pacote novo não percebe
  diferença nenhuma: mesmo comportamento, mesmas tabelas, mesmas respostas, mesmo boot com
  `ddl-auto=validate`.
- **P2 — Só carrega se o dev adicionar o pacote.** A ativação é a dependência Maven
  `archbase-security-oidc`. Nada entra pelo `archbase-starter-security` nem por transitividade.

### Princípios

1. **Nenhum IdP tem código próprio.** Tudo que varia entre provedores é configuração (seção 5).
   O ArchGuard é um perfil de configuração como os outros.
2. **A autorização continua no Archbase.** Os cinco portões (MODELO_CORE_AUTORIZACAO.md) seguem lendo o
   banco local; o token prova *quem é*, não *o que pode*. Papéis do IdP entram por mapeamento
   explícito para grupos locais (seção 6).
3. **Ativado pela presença do pacote (P2).** Sem o jar, nenhuma classe, bean, rota ou tabela existe.
   Com o jar e sem provedor configurado, o módulo registra um WARN e fica inerte — não derruba a subida.
4. **Não escrever criptografia à mão.** Validação de JWT/JWKS via `spring-security-oauth2-jose`
   (Nimbus), que já cobre RS/ES/PS/EdDSA, cache de JWKS, rotação por `kid` e validação de claims.

## 2. Ponto de partida no archbase-security

O SPI que o Control Plane usa já é o encaixe certo, mas tem limites para mais de um IdP:

| Ponto | Situação hoje | Arquivo |
|---|---|---|
| `ExternalBearerAuthenticator` (`supports` / `check` / `authenticate`) | Serve. Primeiro `supports()=true` decide, sem fallback | `auth/external/ExternalBearerAuthenticator.java:22-49`, filtro `:266-306` |
| `ExternalAuthenticationPolicy` | **Bean único**: dois provedores → falha de injeção por ambiguidade | filtro `:88-91` |
| Discriminação do token | CP usa o `alg` (EdDSA). RS256 de vários emissores exige discriminar pelo **`iss`** | `ControlPlaneTokenValidator.java:56` |
| Autorização | Só existe sujeito se o principal for `UserEntity`; senão IDENTITY `PRINCIPAL_NOT_SUPPORTED` | `ArchbaseSecurityService.java:95-117`, `DefaultArchbaseAccessEvaluator.java:96-107` |
| Vínculo usuário externo ↔ local | `EXTERNAL_ID` único **global**, sem provedor, nunca consultado. CP tem tabela própria (`CONTROLPLANE_USUARIO`) | `UserEntity.java:104-105`, `cp/user/UserLinkStore.java` |
| Casts que quebram com `Authentication` externa | `(UsernamePasswordAuthenticationToken)` e `(UserEntity) getPrincipal()` | `ArchbaseUserService.java:30`, `SecurityAdapter.java:28` |
| JWKS | Só no controlplane, **só Ed25519** | `cp/token/JwksKeyStore.java:38,196-205` |
| Logout / revogação | Só tokens locais; token externo vira no-op silencioso | `ArchbaseLogoutService.java:51-56` |
| Testes | Fase 0 feita: `FiltroDeAutenticacaoComportamentoAtualTest` (48 cenários) fotografa o filtro, o SPI, a política e a troca de senha | `src/test/.../integration/FiltroDeAutenticacaoComportamentoAtualTest.java` |

### Achados da fase 0 (comportamento atual, registrado sem corrigir)

1. **`X-TENANT-ID` divergente do JWT dá 401, não o 403 que o código anuncia.** O header entra no
   contexto antes de o token ser conferido, e o token (`AccessTokenEntity`, `@TenantId`) é procurado
   no tenant do header, onde não existe. Sem autenticação, o bloco que responderia 403
   (`ArchbaseJwtAuthenticationFilter.java:227-236`) não roda — na prática é inalcançável. O acesso é
   negado do mesmo jeito. Token de API tem o mesmo efeito por outro caminho (`validateToken` filtra
   pelo tenant do contexto). Consequência para o OIDC: o caminho externo responde
   `403 TENANT_MISMATCH`; o próprio, 401. Unificar é mudança de comportamento — fica fora (P1).
2. **`PATCH /api/v1/user` (trocar a própria senha) sempre responde 500.** `ChangePasswordRequest`
   só tem `@Builder`, sem construtor vazio, e o Jackson não o desserializa
   (`Type definition error`). A falha é anterior ao cast que a fase 1 troca, então o cast é testado
   chamando o serviço com a `Authentication` real do filtro. Correção: `@NoArgsConstructor` +
   `@AllArgsConstructor` — um defeito, não mudança de comportamento; vai em PR próprio.

## 3. Arquitetura proposta

```
Authorization: Bearer <jwt>
        │
ArchbaseJwtAuthenticationFilter
        │  findExternal(token)
        ├── ControlPlaneBearerAuthenticator      (alg=EdDSA, já existe)
        ├── OidcBearerAuthenticator  ◄── NOVO     (iss ∈ provedores configurados)
        │       │ 1. lê iss SEM verificar (payload base64url canônico, tamanho limitado)
        │       │ 2. escolhe o provedor → JwtDecoder (Nimbus) daquele provedor
        │       │ 3. valida assinatura, iss, aud, exp/nbf, alg permitido, claims exigidas
        │       │ 4. ClaimMapper → ExternalIdentity {provider, subject, email, tenant, roles…}
        │       │ 5. IdentityLinker → UserEntity (vínculo / JIT)
        │       │ 6. RoleSync (opcional) → grupos locais
        │       └ OidcAuthentication(principal = UserEntity)
        └── (fluxo local HS256, se a política permitir)
```

### Empacotamento e ativação (P1, P2)

```
archbase-security            (core)  ← só mudanças compatíveis, listadas abaixo
archbase-security-oidc       (NOVO)  ← depende do core + spring-security-oauth2-jose
  └ META-INF/spring/…AutoConfiguration.imports → ArchbaseOidcAutoConfiguration
```

- O dev ativa adicionando `br.com.archbase:archbase-security-oidc`. Não existe
  `archbase.security.oidc.enabled`: a dependência **é** a decisão.
- `ArchbaseOidcAutoConfiguration` é `@ConditionalOnBean` do filtro de segurança do Archbase e
  `@ConditionalOnExpression` de haver ao menos um provedor em `archbase.security.oidc.providers`.
- **Tabela de vínculo fora do JPA do core.** A `SEGURANCA_IDENTIDADE_EXTERNA` é gravada por JDBC
  próprio do módulo e criada só por ele (mesmo padrão do `UserLinkStore` do Control Plane), seguindo
  `archbase.security.schema.mode` (`apply` cria, `report` só avisa, `off` não toca). **Não** entra na
  lista de 16 entidades do `ArchbaseSecuritySchemaInitializer`: se entrasse, todo projeto com
  `ddl-auto=validate` passaria a exigi-la sem ter adicionado o pacote — foi exatamente o que quebrou
  projetos na 3.1.x com `seguranca_evento`/`seguranca_revisao`.

### Mudanças no core (compatíveis)

Toda mudança no `archbase-security` tem de produzir **o mesmo resultado** para quem não usa o módulo:

| Mudança | Por que é compatível |
|---|---|
| Filtro recebe `List<ExternalAuthenticationPolicy>` e combina conservadoramente | Com 0 ou 1 bean (situação de hoje) o resultado é idêntico ao atual |
| `@Order` no autenticador do Control Plane | Hoje ele é o único; a ordem só importa quando o OIDC entra |
| `CurrentUser.resolve(Authentication)` no lugar dos dois casts | Para `UsernamePasswordAuthenticationToken` devolve o mesmo `UserEntity`; só deixa de lançar `ClassCastException` nos outros tipos |
| SPI `ArchbaseCredentialPolicy` (senha local permitida para este usuário?) consultado na troca/reset de senha | Sem bean registrado, a resposta é "permitida" — comportamento atual. O módulo OIDC registra a implementação que nega para usuário federado |
| Testes de regressão do filtro (JWT local, token de API, tenant) **antes** de mexer | Prova P1; hoje não existe nenhum teste do filtro no core |

### Componentes

| Componente | Responsabilidade |
|---|---|
| `ArchbaseOidcProperties` (`@ConfigurationProperties("archbase.security.oidc")`) | Lista de provedores (seção 5). Primeiro `@ConfigurationProperties` de verdade do módulo. |
| `OidcProviderRegistry` | Um `JwtDecoder` por provedor, criado por discovery ou `jwks-uri` explícito; resolve provedor por `iss` (exato ou por modelo). |
| `OidcBearerAuthenticator` | Implementa o SPI. `supports()` só olha `iss`; `check()` valida e mapeia sem banco; `authenticate()` vincula e monta o principal. |
| `ClaimMapper` | Lê claims por **caminho** (`realm_access.roles`), por nome-URL (`https://empresa.com/roles`), string ou array, com valor fixo como alternativa. |
| `IdentityLinker` + tabela `SEGURANCA_IDENTIDADE_EXTERNA` | `(provider_id, tenant_id, subject) → user_id`, por JDBC do próprio módulo (ver acima). Generaliza a `CONTROLPLANE_USUARIO`. |
| `RoleSync` | Papéis/grupos do token → grupos locais, por tabela de mapeamento na configuração. |
| `OidcAuthentication` | `AbstractAuthenticationToken` com principal `UserEntity` + claims originais + id do provedor. |
| `CurrentUser` (utilitário) | Resolve `UserEntity` de **qualquer** `Authentication`; substitui os casts de `SecurityAdapter:28` e `ArchbaseUserService:30`. |

### Mudanças no SPI existente

1. **Política agregada.** O filtro passa a receber `List<ExternalAuthenticationPolicy>` e combina de
   forma conservadora: `acceptLegacyJwt = todas aceitam`; `allowDefaultTenant = todas permitem`.
   Uma política `reject` vence.
2. **Ordem explícita.** `ControlPlaneBearerAuthenticator` `@Order(100)`, `OidcBearerAuthenticator`
   `@Order(200)`. Os discriminadores não se sobrepõem (EdDSA+iss do CP vs. iss dos provedores), mas a
   ordem deixa de depender do classpath.
3. **`supports()` barato e seguro.** Lê o payload sem validar, **com o mesmo guarda de base64 canônico
   e tamanho máximo** adicionado no PR #44 (CVE-2026-114938), porque roda antes da autenticação.
4. Sem fallback depois de `supports()=true` — mantém o comportamento atual: token de um emissor
   conhecido que falha validação é **401**, nunca cai no fluxo local.

## 4. Fluxos

### 4.1 Archbase como resource server (destino; ver 10.2)

O frontend (archbase-react) faz Authorization Code + PKCE direto com o IdP e chama a API com o
access token do IdP. O Archbase só valida. Refresh, MFA, step-up e logout são do IdP.

### 4.2 Troca de token (primeira entrega no archbase-react; ver 10.2) — implementada na 1c

`POST /api/v1/auth/oidc/exchange` com `{"token": "<access token do IdP>"}` no corpo. O token passa pela
mesma validação e pela mesma regra de tenant que teria se fosse usado direto na API, e o retorno é uma
sessão local do Archbase. Desligado por padrão (`archbase.security.oidc.exchange.enabled=true` liga).

A sessão emitida é diferente da do login por senha, de propósito:

- **curta** — `exchange.local-token-ttl` (padrão 15 min), nunca acima de `token-expiration`;
- **sem refresh token** — quando vence, o frontend troca de novo com um token novo do IdP. Assim o IdP
  continua decidindo: usuário desligado lá não renova aqui, sem precisar de revogação cruzada;
- respeita `allowMultipleLogins` (sessão única encerra a anterior, como no login).

Emitida por `ArchbaseAuthenticationService.issueExternalSession`, método novo do core. Não reaproveita
`authenticateWithoutPassword` (do `/login-social`), que revoga **todas** as sessões do usuário a cada chamada.

### 4.3 Usuário federado nos endpoints locais

| Endpoint | Comportamento proposto |
|---|---|
| Troca de senha | 409 `FEDERATED_USER` — a senha é do IdP (e hoje é **ClassCastException**) |
| Reset de senha | Não envia e-mail para conta federada (resposta uniforme se `prevent-user-enumeration`) |
| Refresh local | Não se aplica (4.1) / normal (4.2) |
| Logout | Revoga o que for local; devolve `end_session_endpoint` do provedor para o frontend redirecionar (RP-initiated logout) |
| Token de API | Continua funcionando; o dono é o `UserEntity` vinculado |
| Admin-guard | Inalterado: opera sobre `UserEntity` |

### 4.4 Revogação

Access token JWT vale até o `exp`. Por provedor, opcionalmente:
- `introspection.enabled=true` com cache curto (ex.: 30 s) — ArchGuard, Keycloak e Okta têm; Auth0 e Entra não;
- **back-channel logout** (fase 3a, feita): o IdP avisa por `logout_token` e o Archbase grava um corte por
  `sub`/`sid`, válido por `token-lifetime`.

Recomendação geral: TTL de access token **≤ 15 min** no IdP.

## 5. Configuração

```yaml
archbase:
  security:
    oidc:                                  # só é lido se o pacote archbase-security-oidc estiver no classpath
      providers:
        <id>:
          issuer: https://...              # exato; ou issuer-pattern + allowed-tenants
          jwks-uri:                        # opcional; padrão = discovery do issuer
          audiences: [ ... ]               # obrigatório; vazio = não sobe
          algorithms: [RS256]              # allowlist; nunca 'none', nunca HS* com chave pública
          clock-skew: 30s
          required-claims: { }             # claim: valor exigido (ex.: tokenType: access-token)
          claims:
            subject: sub                   # caminho
            email: email
            email-verified: email_verified
            name: name
            roles: [ ... ]                 # caminhos; string, array ou objeto[].name
          tenant: { claim: ..., fixed: ..., header: false }   # no provedor, NÃO dentro de claims
          users:
            provisioning: existing-user    # existing-user | jit (ver seção 10.1)
            link-by-email: verified-only   # verified-only | trusted-provider | never
            jit: { status: pending }       # pending | active
            local-password: keep           # keep | disable-on-link
          role-mapping:                    # papel externo → grupo local
            sync: off                      # off | add | mirror
            map: { }
          introspection: { enabled: false }
          logout:
            end-session-uri:               # padrão = discovery
            backchannel:                   # aviso do IdP (fase 3a); desligado por padrão
              enabled: false
              audiences: []                # client_id no IdP; vazio = audiences
              max-age: 5m                  # idade máxima do logout_token
              token-lifetime: 24h          # cobre o access token mais longo do IdP
              revoke-local-sessions: true  # derruba também a sessão da troca de token
```

Referência completa e conferida das propriedades: [`archbase-security-oidc/README.md`](../archbase-security-oidc/README.md).

Regra de validação na subida (`OidcStartupValidator`, independente de `archbase.security.hardening.validation`):
provedor sem `audiences`, com `algorithms` vazio ou simétrico, com fonte de tenant ausente ou duplicada, ou com
emissor `http://` sem `allow-insecure-http` **não sobe**, e todos os erros saem listados de uma vez.

### Resolução de tenant — opções

| Estratégia | Quando | Proteção |
|---|---|---|
| `tenant.claim` | O IdP emite o tenant (ArchGuard `owner`/`org_id`, Auth0 `org_id`) | Claim vence o header; divergência = 403 `TENANT_MISMATCH` (já é assim) |
| `tenant.fixed` | Um provedor por tenant (ex.: um realm Keycloak por cliente) | — |
| Mapa `tid → tenant` | Entra ID multi-tenant | Tenant Entra fora do mapa = 401 |
| `tenant.header: true` | IdP não sabe de tenant | **Exige vínculo** `(provider, tenant, subject)` existente: o header não pode escolher um tenant a que o usuário não pertence |

## 6. Autorização

- **Padrão:** permissões vêm do banco local (perfil ∪ grupos ∪ usuário), exatamente como hoje. O
  usuário federado precisa estar vinculado a um `UserEntity`.
- **`role-mapping.sync=add|mirror`:** a cada autenticação (com cache), os papéis do token viram
  participação em grupos locais. `mirror` remove de grupos mapeados que sumiram do token. Os cinco
  portões continuam sem ler token.
- **Claim de papéis ausente não é "nenhum papel"** (implementado na 2b): o Entra omite `groups` quando o
  usuário tem grupos demais (*overage*), e alguns IdPs só emitem o claim sob um escopo. Tratar ausência como
  lista vazia faria o `mirror` apagar os grupos da pessoa a cada login. Só uma lista presente — inclusive
  vazia — decide.
- Papel com `/`, `:` ou `.` no nome precisa de colchetes na chave: `role-mapping.map.[cliente-a/vendas]=Vendas`
  (YAML: `"[cliente-a/vendas]": Vendas`); sem eles o Spring descarta esses caracteres.
- Papéis do token **nunca** concedem diretamente (GRANT só vem do banco). Isso evita que um IdP mal
  configurado — ou um IdP de cliente no modo multi-tenant — conceda acesso no Archbase.
- Administrador (`isAdministrator`) **não** é concedido por mapeamento; continua manual, sob o admin-guard.

## 7. Exemplos de provedor

Os blocos abaixo são os mesmos do README do módulo, validados por `ExemplosDocumentadosTest` (bind e
`OidcStartupValidator`). A primeira versão desta seção punha `tenant` dentro de `claims` e tinha
indentação inválida no Entra e no Okta — nenhum dos cinco subia.

### 7.1 ArchGuard (superfície legada Casdoor, a que está no ar)

```yaml
archguard:
  issuer: https://idp.empresa.com
  jwks-uri: https://idp.empresa.com/.well-known/archbase-app/jwks
  audiences: [ archbase-app-client-id ]
  algorithms: [ RS256 ]
  required-claims:
    tokenType: access-token
  tenant:
    claim: owner                      # organização do usuário
  introspection:
    enabled: true
    uri: https://idp.empresa.com/api/login/oauth/introspect
    client-id: archbase-app-client-id
    client-secret: ${ARCHGUARD_CLIENT_SECRET:troque}
  logout:
    backchannel:
      enabled: true
```
Aplicação no ArchGuard: **`tokenFormat: JWT-Standard`**, grants só `authorization_code` +
`refresh_token` (+ `client_credentials` para serviço), `expireInHours` curto.

Por que `JWT-Standard` (conferido em `object/token_standard_jwt.go:25-86` e `token_jwt.go:54-64`):
emite só `owner`, `preferred_username`, `id`/`sub`, `name`, `picture`, `email`, `email_verified`,
`phone` mais os registrados — **nenhum segredo** (B1) — e marca o refresh corretamente como
`refresh-token` (sem o defeito B3). O custo é **não trazer `roles`/`groups`**: com o ArchGuard, as
permissões ficam 100% no banco do Archbase (o padrão desta proposta). Se um dia for preciso papel do
IdP, a saída é `JWT-Custom` com `tokenFields` restritos — só depois de corrigido o B3.
Quando o contrato v1 (ADR-0023) for montado, a migração é só configuração: `issuer`/`jwks-uri` de
`/api/v1/oidc`, `tenant.claim: org`, e `acr`/`amr` passam a ficar disponíveis.

### 7.2 Keycloak

```yaml
keycloak:
  issuer: https://sso.empresa.com/realms/empresa
  audiences: [ archbase-api ]         # exige "Audience mapper"; o padrão do Keycloak é "account"
  tenant:
    fixed: empresa
  claims:
    roles: [ realm_access.roles, resource_access.archbase-api.roles ]
  role-mapping:
    sync: add
    map:
      financeiro: Financeiro
      "[empresa/vendas]": Vendas      # chave com / . : precisa de colchetes
```
Introspecção disponível em `/protocol/openid-connect/token/introspect`. O `aud` padrão do Keycloak é
`account`: sem um *Audience mapper* para `archbase-api`, todo token é recusado.

### 7.3 Microsoft Entra ID

```yaml
entra:
  issuer-pattern: https://login.microsoftonline.com/{tid}/v2.0
  jwks-uri: https://login.microsoftonline.com/common/discovery/v2.0/keys
  allowed-tenants:
    "[11111111-2222-3333-4444-555555555555]": cliente-a
    "[66666666-7777-8888-9999-000000000000]": cliente-b
  audiences: [ "api://archbase-api" ]
  required-claims:
    ver: "2.0"                        # manifest: accessTokenAcceptedVersion = 2
  claims:
    subject: oid                      # "sub" é pairwise por aplicação; o estável é oid
    roles: [ roles ]
  users:
    link-by-email: never              # Entra não emite email_verified
```
Use *app roles* (`roles`), não `groups`: grupos vêm como GUID e, acima de 200, o token traz só um
indicador de *overage*. Entra não oferece introspecção.
Token emitido para o Microsoft Graph não é validável: o frontend precisa pedir token para o escopo
da API (`api://archbase-api/.default`).

### 7.4 Auth0

```yaml
auth0:
  issuer: https://empresa.us.auth0.com/   # com barra final
  audiences: [ https://api.empresa.com ]
  tenant:
    claim: org_id
  claims:
    email: https://empresa.com/email  # claim customizado exige namespace
    roles: [ permissions ]
```
Sem `audience` no pedido de login, o Auth0 emite access token opaco, não validável aqui. `permissions`
exige RBAC ligado na API. Auth0 não oferece introspecção.

### 7.5 Okta

```yaml
okta:
  issuer: https://empresa.okta.com/oauth2/default
  audiences: [ "api://default" ]
  tenant:
    header: true                      # só entra quem já tem vínculo naquele tenant
  claims:
    subject: uid                      # "sub" costuma ser o login e pode mudar
    roles: [ groups ]
  users:
    link-by-email: never
```
Exige *custom authorization server* (o org AS não serve). O claim `groups` precisa ser criado no AS,
com filtro. Introspecção em `/oauth2/default/v1/introspect`.

> Detalhes de provedores de mercado mudam; os exemplos refletem a documentação conhecida em 2026 e
> devem ser conferidos no ambiente real antes de virar documentação oficial.

## 8. Bloqueios e riscos no ArchGuard

Encontrados na análise de `/Volumes/DevCache/desenvolvimento/archguard`. **B1 é independente desta
proposta e afeta quem já consome o ArchGuard hoje.**

| # | Achado | Evidência | Impacto | Ação |
|---|---|---|---|---|
| **B1** | Formato `tokenFormat: JWT` (padrão, e o que os consumidores usam segundo a ADR-0023) serializa o usuário inteiro no token, incluindo **`totpSecret`, `recoveryCodes`, `passwordSalt`**. `refineUser` só zera `password`. | `object/token_jwt.go:229,287-288,486-506`; `docs/adr/ADR-0023…md:18-34` | Quem vê um access token (frontend, log, proxy, qualquer relying party) gera códigos TOTP e usa códigos de recuperação do usuário: **MFA anulado**. | Corrigir no ArchGuard (remover os campos dos claims) e trocar consumidores para `JWT-Standard`; avaliar rotação de TOTP/recovery dos usuários. |
| B2 | `origin` vazio em `conf/app.conf:30` → `iss` montado a partir do cabeçalho **Host**. | `object/wellknown_oidc_discovery.go:82-104` | Issuer instável/forjável por Host; discovery e token podem divergir atrás de proxy. | Configurar `origin`; no Archbase o issuer é sempre fixo na configuração. |
| B3 | Em `JWT-Custom`, o refresh token sai com `tokenType: access-token`, mesma chave e mesmo `aud`. | `object/token_jwt.go:395-396,597-600` | Refresh token aceito como access token. | Usar `JWT-Standard`; manter `required-claims.tokenType`. |
| B4 | TTL padrão de 168 h; sem endpoint de revogação (RFC 7009). | `object/init.go:243`; `routers/router.go` | Token roubado vale 7 dias. | `expireInHours` curto; introspecção ligada. |
| B5 | Back-channel logout lê `cert.PrivateKey` direto e falha em silêncio com a chave no OpenBao. | `object/token_logout.go:73-79,115-118` | Logout não propaga. | **Corrigido em archguard#2** (ROADMAP 0.5), com mais 4 falhas do mesmo envio. |
| B6 | JWKS global publica certificados de todas as organizações. | `object/wellknown_oidc_discovery.go:203-237` | Aceitar token assinado por certificado de outra organização. | Usar a JWKS **por aplicação**. |
| B7 | PKCE não é obrigatório; `password`/`implicit` ativos se a aplicação habilitar. | `object/token_oauth_util.go` (~`:192-214`); `token_oauth.go:74-79` | — | Configurar a aplicação Archbase só com os grants da seção 7.1. |
| B8 | Contrato v1 (`org`, `mid`, `acr`, `amr`, `sid`) existe mas não está montado; a API de contexto de sessão usa `identity.subject`, diferente do `sub` legado. | ADR-0023; `internal/http/service_context.go`; `internal/domain/identity.go:150` | Step-up/MFA (`acr`) indisponível; API de contexto inutilizável com token legado. | Mapeamento configurável agora; migrar quando o v1 for montado. |


## 9. Fases

| Fase | Entrega | Testes |
|---|---|---|
| **0** | Testes de regressão do filtro no core (JWT local, token de API, tenant por header/claim, Control Plane) **sem mudar código**. É a régua de P1. | Os testes passam antes e depois de cada fase |
| **1** | **1a — feita (2026-11-21):** mudanças compatíveis no core (seção 3) — política agregada, `ArchbaseCurrentUser`, `ArchbaseLocalCredentialPolicy`, `@Order`. **1b — feita (2026-11-21):** módulo `archbase-security-oidc`: propriedades + validação na subida, `OidcProviderRegistry` (Nimbus, JWKS explícito ou por descoberta, emissor por modelo `{tid}`), `OidcBearerAuthenticator`, `ClaimReader` (nome ou caminho), vínculo (`existing-user` + `link-by-email`, proteção contra segunda identidade com o mesmo e-mail), `local-password: disable-on-link`. **1c — feita (2026-11-21):** `POST /api/v1/auth/oidc/exchange` (desligado por padrão) + `ArchbaseAuthenticationService.issueExternalSession` no core (só acréscimo; o login por senha e o `authenticateWithoutPassword`, que revoga todas as sessões, não mudam): sessão local curta (`exchange.local-token-ttl`, padrão 15 min), **sem refresh token**, respeitando `allowMultipleLogins` | IdP falso RS256 (nos moldes do `FakeControlPlane`): iss/aud/exp/alg/kid, rotação, dois provedores + CP + JWT local no mesmo contexto, tenant por claim/header/mapa; **`@SpringBootTest` de boot com e sem o pacote**, com `ddl-auto=validate` |
| **2** | **2a — feita (2026-11-21):** `users.provisioning=jit` (`jit.status` `pending` padrão / `active`; só com e-mail confiável; nunca duplica cadastro; usuário nasce sem senha local, sem poder redefinir, sem grupo e não admin; proibido com tenant pelo header) + API administrativa de vínculo `/api/v1/security/external-identities` (exige `isAdministrator`). **2b — feita (2026-11-21):** `role-mapping` (`add`/`mirror`) sobre `claims.roles`; claim ausente não muda nada. **2c — feita (2026-11-21):** introspecção RFC 7662 por provedor, depois da assinatura, cache por SHA-256 do token (`cache-ttl` padrão 30 s, nunca além do `exp`), `on-error` `deny` (503 `IDP_UNAVAILABLE`, padrão) / `allow`. **2d — feita (2026-11-21):** `POST /api/v1/auth/oidc/logout` revoga o token nesta aplicação (lista `SEGURANCA_TOKEN_EXTERNO_REVOGADO`, por hash, todas as instâncias) e devolve o `end_session_endpoint` (`logout.end-session-uri`) para o frontend fazer o RP-Initiated Logout no IdP | Sincronização de grupos; token revogado via introspecção |
| **3** | **3a — feita no Archbase (2026-11-21):** `POST /api/v1/auth/oidc/backchannel-logout` (OIDC Back-Channel Logout 1.0), ligado por provedor com `logout.backchannel.enabled`. O `logout_token` é validado com as chaves do provedor: `iss`, `aud` (`logout.backchannel.audiences`, padrão `audiences`), `events`, sem `nonce`, `sub` ou `sid`, `iat` dentro de `max-age` (5 min); o `exp` é conferido quando vem. O aviso grava um corte em `SEGURANCA_LOGOUT_EXTERNO` (`sub` → tokens emitidos até o `iat` do aviso; `sid` → aquela sessão, e tokens sem `sid` do mesmo `sub` caem junto) e, com `revoke-local-sessions` (padrão), revoga as sessões locais dos usuários vinculados — é o que faz o logout do IdP alcançar a troca de token (4.2). **3a — ArchGuard (B5), feita (2026-11-22, archguard#2):** chave pelo custodiante, algoritmo do access token, `exp` e `typ: logout+jwt`, aviso antes de expirar o token no logout por access token, timeout de 5 s; depois do deploy, cadastrar `backchannelLogoutUri` nas aplicações. **3b:** `acr`/step-up quando o ArchGuard montar o v1. **3c:** suporte OIDC nativo no archbase-react (10.2) | Logout token assinado; aviso velho, sem `events`, com `nonce` ou de outro `aud` recusado |
| **4** | **Feita (2026-11-22):** migração entre IdPs — export, dry-run com `planHash`, execução tudo-ou-nada condicionada ao plano conferido e histórico por tenant (ver 9.1) | Local→provedor e vínculo pré-existente conferidos nos testes; plano que muda entre dry-run e execução não grava |

### 9.1 Migração entre IdPs (fase 4)

Dois casos: **login local do Archbase → IdP externo** (ex.: hoje JWT próprio, amanhã ArchGuard) e
**IdP externo → outro** (ex.: Keycloak → ArchGuard). Nos dois, o que muda é quem emite o token; o
`UserEntity`, os perfis, grupos e permissões ficam onde estão.

Etapas, cada uma reversível até a última:

1. **Exportar identidades para o IdP de destino** — usuários locais (nome, e-mail, tenant) para o
   ArchGuard via SCIM de entrada (`/scim/*`, que o ArchGuard já tem) ou API de importação do IdP.
   Senha **não** migra: o IdP de destino força definição no primeiro acesso, ou federa com o de origem.
2. **Pré-vincular** — gravar `(provedor, tenant, sub)` → `user_id` na tabela de vínculo **antes** do
   primeiro login, a partir de um mapeamento: e-mail verificado (padrão), id externo, ou arquivo CSV
   fornecido pelo operador. Sem isso, o primeiro acesso dependeria de `link-by-email` e de e-mail verificado.
3. **Dry-run obrigatório** — relatório por tenant: vinculados, sem correspondência, ambíguos (mesmo
   e-mail em dois usuários), administradores. Nada é gravado sem o relatório conferido.
4. **Convivência** — os dois emissores aceitos ao mesmo tempo (já é o comportamento: JWT local +
   provedor externo; para IdP→IdP, dois provedores configurados). O frontend migra no seu ritmo.
5. **Corte** — por tenant: `local-password: disable-on-link` (a `ArchbaseLocalCredentialPolicy` passa a
   negar a senha local de quem está vinculado) e, por fim, recusar o JWT local (`acceptLegacyJwt=false`)
   ou remover o provedor de origem. Único passo que tira acesso; reverter é reconfigurar.

**Entregue (fase 4, 2026-11-22):** `IdentityMigrationService` no `archbase-security-oidc`, exposto em
`/api/v1/security/identity-migration` (exige `isAdministrator`, sempre no tenant da requisição):

| Endpoint | O que faz |
|---|---|
| `GET /export?providerId=` | Usuários do tenant (id, nome, e-mail, login, admin, inativo, `sub` já vinculado) — etapa 1, para importar no IdP de destino |
| `POST /plan` | Dry-run — etapas 2 e 3. Entrada: `providerId`, `entries[]` de `{subject, email ou userId}` vindos do IdP de destino, `includeAdministrators`. Saída: ação por linha (`LINK`, `ALREADY_LINKED`, `SKIP` + motivo), resumo, usuários ativos sem correspondência, impacto do corte e `planHash` |
| `POST /execute` | Refaz o plano e grava só se o `planHash` for o do relatório conferido (`409 PLAN_CHANGED` com o plano novo, se não for). Tudo ou nada, numa transação |
| `GET /history` | Dry-runs e execuções do tenant (`SEGURANCA_MIGRACAO_IDENTIDADE`) |

Regras do plano:
- e-mail comparado sem caixa e sem espaços;
- `userId` e e-mail na mesma linha têm de concordar;
- `subject` ou usuário repetido no arquivo derruba **todas** as linhas envolvidas — não há desempate;
- nunca sobrescreve vínculo existente (`SUBJECT_LINKED_TO_OTHER_USER`, `USER_LINKED_TO_OTHER_SUBJECT`);
- administradores ficam de fora salvo `includeAdministrators=true`: com `disable-on-link`, o admin vinculado
  perde a senha local, que é o acesso de emergência se o IdP cair. O plano conta à parte os administradores
  que ficariam sem vínculo.

Nada aqui tira acesso; o corte (etapa 5) continua sendo configuração, e o plano mostra quem ficaria sem
entrar se ele fosse feito agora.

O histórico é tabela própria, e não a trilha de segurança do core: o tipo de evento daquela trilha é enum
com `CHECK` (PostgreSQL) / `ENUM` (MySQL) nos scripts de `deployment/sql`, e um tipo novo seria recusado — em
silêncio, porque o logger engole a falha — em toda base já criada.

Ficou de fora: empurrar os usuários para o IdP por SCIM (o export é a entrada para a importação de cada IdP)
e um executável de linha de comando separado (a API é chamável por `curl`; o arquivo do IdP precisa virar o
JSON de `entries`).

Fora do plano por P1: migrar o Control Plane para o `OidcProviderRegistry`. Ele funciona e tem
testes; mexer nele só se houver ganho concreto, em PR separado.

## 10. Decisões

### 10.0 Onde fica o código — **decidido**

Módulo separado `archbase-security-oidc`, ativado pela dependência (P2). Seção 3.

### 10.1 Usuário que existe só no IdP

O problema: todo `@HasPermission` exige um `UserEntity` local (seção 2). Então a pergunta real é
**quando e como o `UserEntity` passa a existir e fica ligado ao `sub` do IdP**.

| Opção | Como funciona | A favor | Contra |
|---|---|---|---|
| **A. `existing-user`** | Só entra quem já tem cadastro no Archbase. No 1º acesso o vínculo é feito por e-mail; depois, sempre por `(provedor, tenant, sub)` | Nenhum acesso nasce sem um admin decidir; o fluxo de cadastro atual não muda | Admin cadastra todo mundo; sem e-mail confiável o vínculo precisa ser manual |
| **B. `jit` + `pending`** | 1º acesso cria o `UserEntity` **inativo** e sem permissão; admin aprova | Sem cadastro prévio; continua sem acesso automático | Fila de aprovação; risco de "lixo" de cadastros |
| **C. `jit` + ativo + `role-mapping`** | 1º acesso cria ativo; grupos vêm dos papéis do IdP | Zero trabalho manual; ideal para IdP corporativo de um único cliente | O IdP passa a decidir quem entra; erro de configuração no IdP vira acesso |

**O ponto crítico em todas: vincular por e-mail.** Se o IdP deixa o usuário pôr qualquer e-mail sem
verificar, ele se vincula à conta de outra pessoa no Archbase (ataque conhecido com Entra ID multi-tenant,
o "nOAuth"). Por isso `link-by-email` tem três valores:

- `verified-only` (padrão): só com `email_verified=true` no token. Serve para ArchGuard (JWT-Standard),
  Keycloak, Auth0, Okta-OIDC com o claim.
- `trusted-provider`: o admin declara que aquele IdP só tem e-mails da empresa (ex.: Entra de
  tenant único). Gera WARN na subida.
- `never`: vínculo só manual, pelo admin, informando o `sub` — ou pela tela de vínculo (fase 2).

**Sugestões:**
1. **Padrão do módulo: A com `verified-only`.** É o único que não dá acesso a ninguém que já não
   tivesse — coerente com P1.
2. **ArchGuard:** A. O ArchGuard é o cadastro mestre; o Archbase só precisa do vínculo.
3. **Cliente que traz o próprio Entra/Okta:** C, com `role-mapping` restrito a grupos com prefixo
   (`archbase-*`) e **sem** mapeamento para administrador (já é regra da seção 6).
4. **Autocadastro (B2C, Google/Auth0 social):** B.
5. **Senha local de quem foi vinculado:** `local-password: keep` por padrão (P1 — quem já entrava
   com senha continua entrando). `disable-on-link` quando o cliente quiser forçar o IdP.
6. **Desligamento:** usuário desativado no IdP para de receber token, mas o `UserEntity` continua
   ativo. Sugiro gravar `ultimo_acesso_externo` no vínculo e um relatório de "vinculados sem acesso
   há N dias" na fase 2; SCIM de entrada fica para depois.

### 10.2 Como o frontend (archbase-react) faz login

| Opção | Como funciona | A favor | Contra |
|---|---|---|---|
| **A. Resource server** | O archbase-react faz Authorization Code + PKCE direto no IdP (ex.: `oidc-client-ts`) e chama a API com o token do IdP | Um só token; logout, MFA, step-up e revogação ficam no IdP; padrão de mercado | Exige mudar o archbase-react: login, renovação silenciosa, logout, seleção de tenant; telas de senha somem para federado |
| **B. Troca de token** | O archbase-react faz o login OIDC só para obter o token do IdP e chama `POST /auth/oidc/exchange`; recebe o JWT local e **todo o resto segue igual** | Mudança mínima no frontend (um botão "Entrar com…"); refresh, logout, tenant e auto-registro de recursos inalterados | Revogação no IdP só vale quando o JWT local expira; dois tokens no ciclo |
| **C. BFF** | O backend faz o login OIDC (`spring-security-oauth2-client`) e o navegador só tem cookie de sessão | O mais seguro para navegador (nenhum token no JS) | Muda o modelo stateless do Archbase inteiro — viola P1 |

**Sugestões:**
1. **Fase 1: B no archbase-react, A já pronto no backend.** O mesmo validador atende os dois, então
   o backend sai completo; o frontend muda o mínimo. Clientes de API, integrações e serviços (que
   não usam o archbase-react) já usam A desde o primeiro dia.
2. **Mitigar o contra de B:** JWT local emitido pela troca com vida curta (15 min,
   `exchange.local-token-ttl`) e **sem refresh token** — renovar exige um token novo do IdP, então
   usuário desligado no IdP não renova. *(Implementado assim na 1c; a ideia inicial de revalidar o
   vínculo no refresh local foi trocada por não emitir refresh.)*
3. **Fase 3: A no archbase-react**, como opção por aplicação, quando o ArchGuard tiver o v1
   (`acr`, `sid`, rotação de refresh) — é quando A passa a entregar mais que B.
4. **C:** descartado enquanto P1 valer.

Serviço a serviço (`client_credentials`) não tem usuário: sugiro mapear cada `client_id` para um
usuário técnico local na configuração do provedor (`service-clients: { <clientId>: <e-mail> }`),
para que os cinco portões continuem funcionando sem exceção.

### 10.3 Vazamento de segredos no token do ArchGuard (B1)

Não é item desta proposta, mas bloqueia usar o ArchGuard com segurança — e afeta os consumidores
atuais. Sugestão de plano para o time do ArchGuard:

| Quando | Ação | Mudança |
|---|---|---|
| **Imediato** | Trocar as aplicações consumidoras para `tokenFormat: JWT-Standard` | Só configuração; quem lê `roles`/`groups`/`properties` do token precisa ser revisto antes (alcada/squadx, segundo a ADR-0023) |
| **Imediato** | Reduzir `expireInHours` | Configuração |
| **Curto prazo** | Remover `PasswordSalt`, `TotpSecret`, `RecoveryCodes` (e revisar `LastSigninIp` e afins) de `UserWithoutThirdIdp`, ou zerá-los em `refineUser` | Código, `object/token_jwt.go`; teste que decodifica um token de cada formato e falha se algum desses claims aparecer |
| **Curto prazo** | Verificar se o Casdoor upstream já corrigiu e trazer por cherry-pick (ADR-0003) | — |
| **Avaliação** | Para usuários com MFA ativo cujo token possa ter circulado em log/proxy: regenerar TOTP e códigos de recuperação | Operacional; depende de onde os tokens foram registrados |

O dono natural é o time do ArchGuard; a correção de código é pequena e a troca de formato é só
configuração.
