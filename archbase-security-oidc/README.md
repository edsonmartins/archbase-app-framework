# archbase-security-oidc

Aceita access tokens de um IdP OIDC externo — ArchGuard, Keycloak, Microsoft Entra ID, Auth0, Okta — **ao lado**
do login próprio do Archbase. O token do IdP prova quem é a pessoa; o que ela pode fazer continua decidido pelo
`archbase-security`, nos cinco portões, lendo só o banco.

Desenho e decisões: [`archbase-security/PROPOSTA_IDP_EXTERNO_OIDC.md`](../archbase-security/PROPOSTA_IDP_EXTERNO_OIDC.md).

## Ativação

**Opt-in em dois passos.** Nada acontece sem os dois:

1. Adicionar a dependência:

   ```xml
   <dependency>
       <groupId>br.com.archbase</groupId>
       <artifactId>archbase-security-oidc</artifactId>
       <version>${archbase.version}</version>
   </dependency>
   ```

2. Configurar ao menos um provedor em `archbase.security.oidc.providers.<id>`.

Com o jar e sem provedor, nenhum bean é criado, nenhuma tabela é tocada e sai um WARN de módulo inerte. Sem o jar,
o `archbase-security` funciona exatamente como antes.

**Configuração errada não sobe.** O `OidcStartupValidator` confere todos os provedores na subida e lista **todos**
os erros de uma vez. Não é excesso de zelo: cada regra fecha um token forjável (algoritmo simétrico, `aud` sem
conferência, emissor por HTTP) ou um tenant escolhido pelo cliente.

## Como o token é roteado

O `iss` do token (lido antes de verificar) decide quem cuida dele:

| `iss` do token | Quem valida |
|---|---|
| De um provedor configurado | Este módulo: assinatura (JWKS), algoritmo, emissor, `aud`, `exp`/`nbf`, `required-claims` |
| Qualquer outro | O login próprio do Archbase, como antes |

O módulo entra pelo SPI `ExternalBearerAuthenticator` do `archbase-security`. Convive com o `archbase-controlplane`:
o Control Plane tem ordem 100 e o OIDC 200.

Depois de validado o token, o usuário local é encontrado, no tenant da requisição:

1. pelo **vínculo** gravado (`provedor + tenant + sub` → usuário) — daí em diante o e-mail pode mudar no IdP;
2. sem vínculo, pelo **e-mail**, se `users.link-by-email` permitir — e o vínculo é gravado;
3. sem vínculo nem cadastro, com `users.provisioning=jit`, o usuário é **criado** sem grupo, sem perfil, sem senha e,
   por padrão, inativo.

Já existindo cadastro com aquele e-mail, nunca cria outro: vincula a ele ou não entra (`403 USER_NOT_LINKED`).

## Garantias

- Papel do token **nunca** vira permissão direta nem administrador. Com `role-mapping` ligado, só papéis listados
  explicitamente viram participação em grupo que já existe.
- O tenant do token vence o `X-TENANT-ID`; divergência é `403 TENANT_MISMATCH`.
- `tenant.header=true` só aceita quem já tem vínculo naquele tenant: o cabeçalho não escolhe tenant.
- Usuário inativo ou bloqueado é recusado (`403 USER_DISABLED`), mesmo com token válido.

## Propriedades

Por provedor, sob `archbase.security.oidc.providers.<id>`. O `<id>` vai para o vínculo gravado: só minúsculas,
dígitos e hífen (até 40) e **não deve ser trocado depois de em uso**.

| Propriedade | Padrão | Regra |
|---|---|---|
| `issuer` | — | Emissor exato. Exatamente um entre `issuer` e `issuer-pattern` |
| `issuer-pattern` | — | Com `{tid}` uma vez (Entra multi-tenant). Exige `jwks-uri` e `allowed-tenants` |
| `allowed-tenants` | — | `tid` do IdP → tenant do Archbase. Só com `issuer-pattern`; `tid` fora do mapa é recusado |
| `jwks-uri` | descoberta | Vazio = `<issuer>/.well-known/openid-configuration` |
| `audiences` | — | **Obrigatório.** Ao menos um precisa estar no `aud` |
| `algorithms` | `RS256` | Só RS256/384/512, PS256/384/512, ES256/384/512 |
| `clock-skew` | `30s` | De 0 a 5 min |
| `required-claims` | — | Claim → valor exigido (ex.: `tokenType: access-token`) |
| `allow-insecure-http` | `false` | Sem ele, emissor, JWKS, introspecção e end-session precisam ser `https://`. Só para desenvolvimento |
| `claims.subject` | `sub` | Identificador estável. Entra: `oid`; Okta: `uid` |
| `claims.email` / `email-verified` / `name` | `email` / `email_verified` / `name` | Nome do claim (aceita URL) ou caminho com pontos |
| `claims.roles` | `[]` | Claims com papéis. Obrigatório com `role-mapping.sync` ligado |
| `tenant.claim` / `tenant.fixed` / `tenant.header` | — / — / `false` | **Exatamente uma fonte**. Com `issuer-pattern`, a fonte é `allowed-tenants` e nenhuma outra |
| `users.provisioning` | `existing-user` | `existing-user` \| `jit`. `jit` não combina com `tenant.header` |
| `users.jit.status` | `pending` | `pending` \| `active` (WARN na subida) |
| `users.link-by-email` | `verified-only` | `verified-only` \| `trusted-provider` (WARN) \| `never` |
| `users.local-password` | `keep` | `keep` \| `disable-on-link`: quem tem vínculo deixa de entrar, trocar e redefinir senha local |
| `role-mapping.sync` | `off` | `off` \| `add` (só inclui) \| `mirror` (também remove, só dos grupos mapeados) |
| `role-mapping.map` | — | Papel → nome do grupo local. Chave com `/`, `.` ou `:` precisa de colchetes |
| `introspection.enabled` | `false` | RFC 7662. Pega revogação antes do `exp` |
| `introspection.uri` / `client-id` / `client-secret` | — | Obrigatórios com introspecção ligada |
| `introspection.cache-ttl` | `30s` | De 0 a 10 min; nunca além do `exp` |
| `introspection.on-error` | `deny` | `deny` (`503 IDP_UNAVAILABLE`) \| `allow` (aceita só pela assinatura; WARN) |
| `logout.end-session-uri` | — | Devolvido pelo logout para o frontend redirecionar |
| `logout.backchannel.enabled` | `false` | Aceita o aviso de logout do IdP |
| `logout.backchannel.audiences` | `audiences` | `client_id` da aplicação no IdP, se diferente do `aud` do access token |
| `logout.backchannel.max-age` | `5m` | De 1 s a 15 min. Aviso mais velho é recusado |
| `logout.backchannel.token-lifetime` | `24h` | De 1 min a 30 dias. Precisa cobrir o access token mais longo do IdP |
| `logout.backchannel.revoke-local-sessions` | `true` | Também revoga as sessões locais. Só funciona com `claims.subject=sub` (WARN se não) |

Globais:

| Propriedade | Padrão | |
|---|---|---|
| `archbase.security.oidc.exchange.enabled` | `false` | Liga a troca de token |
| `archbase.security.oidc.exchange.local-token-ttl` | `15m` | Validade da sessão local emitida; nunca passa de `jwt.token-expiration` |

## Exemplos

Estes exemplos são carregados e validados por `ExemplosDocumentadosTest`
(`src/test/resources/docs/exemplos-provedores.yml`); se um deixar de subir, o build quebra. Os detalhes de cada IdP
— o que configurar do lado dele, armadilhas conhecidas — estão no §7 da proposta.

```yaml
archbase:
  security:
    oidc:
      providers:

        # ArchGuard (superfície legada Casdoor). Aplicação com tokenFormat: JWT-Standard.
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

        # Keycloak: um realm por cliente, tenant fixo.
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

        # Microsoft Entra ID multi-tenant: o tenant vem de allowed-tenants, e de nenhuma outra fonte.
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

        # Auth0 com Organizations.
        auth0:
          issuer: https://empresa.us.auth0.com/   # com barra final
          audiences: [ https://api.empresa.com ]
          tenant:
            claim: org_id
          claims:
            email: https://empresa.com/email  # claim customizado exige namespace
            roles: [ permissions ]

        # Okta (custom authorization server), sem tenant no token.
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

`tenant` fica **direto no provedor**, não dentro de `claims`. Fora do lugar, o Spring ignora a chave e a subida
falha com "informe exatamente uma fonte".

## Endpoints

Todos sob `/api/v1/auth/**` são públicos na configuração padrão do Archbase. Quem usa `CustomSecurityConfiguration`
precisa manter esses caminhos fora da autenticação.

| Método e caminho | Para quê |
|---|---|
| `POST /api/v1/auth/oidc/exchange` | Só com `exchange.enabled`. Corpo `{"token": "<access token do IdP>"}`; devolve uma sessão local curta, **sem refresh token** — quando vence, o cliente troca de novo |
| `POST /api/v1/auth/oidc/logout` | Com o token do IdP no `Authorization`: revoga-o **nesta aplicação** (todas as instâncias) e devolve `end_session_endpoint`, se configurado, para o navegador encerrar a sessão no IdP |
| `POST /api/v1/auth/oidc/backchannel-logout` | OIDC Back-Channel Logout 1.0, `application/x-www-form-urlencoded` com `logout_token`. Cadastre esta URL no IdP |

Administrativos — exigem `isAdministrator` (sem ele, `403` sem corpo) e um tenant no contexto:

| Método e caminho | Para quê |
|---|---|
| `GET /api/v1/security/external-identities?userId=` | Vínculos de um usuário |
| `POST /api/v1/security/external-identities` | Vincula: `{"providerId", "subject", "userId"}` |
| `DELETE /api/v1/security/external-identities/{providerId}/{subject}` | Desvincula |
| `GET /api/v1/security/identity-migration/export?providerId=` | Usuários do tenant, para importar no IdP de destino |
| `POST /api/v1/security/identity-migration/plan` | Dry-run: relatório e `planHash` |
| `POST /api/v1/security/identity-migration/execute` | Grava, se o `planHash` ainda for o do relatório |
| `GET /api/v1/security/identity-migration/history` | Execuções e dry-runs do tenant |

## Migração de identidades

Para trocar o login próprio por um IdP (por exemplo, o OAuth2 do Archbase pelo ArchGuard) sem recadastrar ninguém:

1. `export` — lista os usuários do tenant para criar no IdP de destino.
2. Cria os usuários no IdP e monta as entradas `{subject, email}` ou `{subject, userId}` com o `sub` que o IdP deu.
3. `plan` — dry-run. Cada linha sai como `LINK`, `ALREADY_LINKED` ou `SKIP` com o motivo (`USER_NOT_FOUND`,
   `AMBIGUOUS_EMAIL`, `DUPLICATE_SUBJECT`, `SUBJECT_LINKED_TO_OTHER_USER`, `ADMINISTRATOR`…). Administradores
   ficam de fora salvo `includeAdministrators: true`. Até 10.000 linhas por plano.
4. `execute` com o `planHash` do relatório conferido. Se algo mudou desde o dry-run, volta `409 PLAN_CHANGED` com o
   plano novo, e nada é gravado.
5. Depois de conferir o acesso, `users.local-password: disable-on-link` desliga a senha local de quem migrou.

Dry-run e execução ficam registrados em `SEGURANCA_MIGRACAO_IDENTIDADE`.

## Tabelas

Criadas por JDBC, fora do mapeamento JPA — projeto com `ddl-auto=validate` não passa a exigi-las. Seguem
`archbase.security.schema.mode`: `apply` cria o que falta, `report` só loga o DDL, `off` não toca.

| Tabela | Conteúdo |
|---|---|
| `SEGURANCA_IDENTIDADE_EXTERNA` | Vínculo `provedor + tenant + sub` → usuário (índice `IX_SEG_IDENT_EXT_USUARIO`) |
| `SEGURANCA_TOKEN_EXTERNO_REVOGADO` | Hash dos tokens do IdP revogados pelo logout, até o `exp` |
| `SEGURANCA_LOGOUT_EXTERNO` | Cortes do back-channel logout. Só criada se algum provedor ligar o back-channel |
| `SEGURANCA_MIGRACAO_IDENTIDADE` | Histórico da migração de identidades |

## Códigos de erro

| Status | Código | Quando |
|---|---|---|
| 401 | `INVALID_TOKEN` | Assinatura, emissor, `aud`, algoritmo ou claim exigido não conferem |
| 401 | `TOKEN_EXPIRED` | `exp` vencido |
| 401 | `TOKEN_REVOKED` | Revogado pelo logout, pela introspecção ou pelo back-channel |
| 503 | `IDP_UNAVAILABLE` | Introspecção sem resposta com `on-error=deny` |
| 403 | `TENANT_REQUIRED` | A requisição não identifica o tenant |
| 403 | `TENANT_MISMATCH` | `X-TENANT-ID` diferente do tenant do token |
| 403 | `USER_NOT_LINKED` | Sem vínculo, sem cadastro vinculável e sem JIT |
| 403 | `USER_DISABLED` | Usuário inativo ou bloqueado (inclusive o JIT pendente) |
| 400 | `NOT_AN_OIDC_SESSION` | `/oidc/logout` chamado sem token de IdP |
| 400/404/409 | `INVALID_REQUEST`, `UNKNOWN_PROVIDER`, `USER_NOT_FOUND`, `IDENTITY_ALREADY_LINKED`, `USER_ALREADY_LINKED` | API de vínculo |
| 400/409 | `TENANT_REQUIRED`, `UNKNOWN_PROVIDER`, `INVALID_REQUEST`, `TOO_MANY_ENTRIES`, `PLAN_REQUIRED`, `PLAN_CHANGED` | API de migração |
| 400 | `invalid_request` | Back-channel: formato da especificação (`error`/`error_description`), sempre `Cache-Control: no-store` |
