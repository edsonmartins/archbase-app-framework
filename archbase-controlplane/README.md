# archbase-controlplane

Liga uma aplicação Archbase ao Control Plane: aceita o token de sessão do tenant emitido por ele, valida
licença, assentos e entitlements, e recebe comandos de provisionamento. **Opt-in**: sem
`archbase.controlplane.enabled=true`, nenhum bean do módulo é criado e a aplicação se comporta como antes.

> **Estado:** Fases A (token de sessão) e B (licença, entitlements, assentos, modo restrito). MQTT e
> provisionamento (Fase C) ainda não estão implementados: por enquanto a licença entra por arquivo
> (`license.path`) e o estado dos tenants, por `ControlPlaneState.applyTenant`.
>
> **Implantação:** com o módulo ligado e **sem licença**, a instância entra em modo restrito (escrita recusada com
> `LICENSE_EXPIRED`). Comece com `archbase.controlplane.restricted-mode.enforcement=report`, confira nos logs
> (`controlplane.restricted.would_deny`) e só então passe para `enforce`.

## Exemplo mínimo

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-controlplane</artifactId>
    <version>${archbase.version}</version>
</dependency>
```

```properties
archbase.controlplane.enabled=true
archbase.controlplane.url=${CP_URL}
archbase.controlplane.public-url=${INSTANCE_PUBLIC_URL}
archbase.controlplane.product=powerview
archbase.controlplane.jwks.url=${CP_URL}/.well-known/jwks.json
archbase.controlplane.jwks.local-path=/var/lib/powerview/cp-jwks.json
archbase.controlplane.legacy-jwt=accept
archbase.controlplane.instance-id=${CP_INSTANCE_ID}
archbase.controlplane.license.path=/var/lib/powerview/cp-license.jws
archbase.controlplane.restricted-mode.enforcement=report   # enforce depois de validar

# Obrigatório com o módulo ligado: nenhum caminho pode cair no tenant padrão.
archbase.app.tenant.fail-on-missing=true
```

```java
@RestController
class RelatorioController {

    @GetMapping("/api/relatorios")
    public List<Relatorio> listar() {
        TenantSession sessao = ControlPlane.currentSession(); // userId, email, tenantId, adminRole, seatId
        ...
    }

    @PostMapping("/api/configuracoes")
    @RequiresAdmin                     // tenant_admin ou client_admin
    public void configurar(...) { ... }

    @DeleteMapping("/api/tenants/{id}")
    @RequiresAdmin(AdminScope.CLIENT)  // só client_admin
    public void remover(...) { ... }

    @GetMapping("/api/relatorios/excel")
    @RequiresEntitlement("powerview.export_excel")   // 403 NOT_ENTITLED sem o entitlement
    public byte[] excel() { ... }

    @GetMapping("/api/painel")
    @RequiresSeat                                     // 403 NO_SEAT sem assento
    public Painel painel() { ... }

    @PostMapping("/api/relatorios/exportar")
    @ExportOperation                                  // POST que só lê: liberado no modo restrito
    public byte[] exportar(@RequestBody Filtro filtro) { ... }

    @PostMapping("/api/copiloto")
    public Resposta perguntar(...) {
        Usage uso = ControlPlane.entitlements().consume("copilot.ia_creditos", 120);
        if (uso.exceeded()) { /* estouro é tolerado: o produto decide o que fazer */ }
        ...
    }
}
```

```java
boolean can   = ControlPlane.entitlements().can("powerview.export_excel");
long    limit = ControlPlane.entitlements().value("powerview.assentos");
boolean seat  = ControlPlane.seats().check(userId);
TenantState state = ControlPlane.tenantState();        // ACTIVE, DELINQUENT, SUSPENDED, UNKNOWN
LicenseStatus lic = ControlPlane.license().status();   // VALID, GRACE, EXPIRED
```

A aplicação não sobe se faltar `url` (ou `issuer`), `public-url`, `product`, uma fonte de chaves
(`jwks.url` e/ou `jwks.local-path`) ou `archbase.app.tenant.fail-on-missing=true`. Todos os problemas são
listados de uma vez.

## Configuração

| Propriedade | Padrão | Para que serve |
|---|---|---|
| `archbase.controlplane.enabled` | `false` | Liga o módulo |
| `archbase.controlplane.url` | — | Endereço do Control Plane; também o `iss` esperado, se `issuer` não for definido |
| `archbase.controlplane.issuer` | `url` | `iss` esperado no token |
| `archbase.controlplane.product` | — | Código do produto no catálogo |
| `archbase.controlplane.public-url` | — | URL pública da instância; o `aud` do token tem de contê-la |
| `archbase.controlplane.jwks.url` | — | JWKS do Control Plane |
| `archbase.controlplane.jwks.local-path` | — | Cópia local do JWKS: lida na subida, regravada a cada busca |
| `archbase.controlplane.jwks.refresh-min-interval` | `60s` | Intervalo mínimo entre buscas por `kid` desconhecido |
| `archbase.controlplane.jwks.timeout` | `5s` | Timeout da busca do JWKS |
| `archbase.controlplane.legacy-jwt` | `accept` | `accept`: aceita os dois tokens; `reject`: só o do Control Plane |
| `archbase.controlplane.clock-skew` | `30s` | Tolerância de relógio para `exp` |
| `archbase.controlplane.seat-required` | `false` | Recusa token de sessão sem `seat` (`NO_SEAT`) |
| `archbase.controlplane.schema.mode` | `apply` | `apply`: cria na subida a tabela `CONTROLPLANE_USUARIO`; `off`: a aplicação cria |
| `archbase.controlplane.instance-id` | — | Id da instância; a licença tem de ser dela (`sub`) |
| `archbase.controlplane.license.path` | — | Arquivo com a licença (JWS), aplicado na subida se for mais nova que a do banco |
| `archbase.controlplane.grace-period` | `7d` | Carência depois do `exp` da licença (status `GRACE`); depois, modo restrito |
| `archbase.controlplane.restricted-mode.enforcement` | `enforce` | `enforce` recusa; `report` só registra em log |
| `archbase.controlplane.restricted-mode.allowed-paths` | `/api/v1/auth/**` | Caminhos liberados no modo restrito (padrão Ant) |
| `archbase.controlplane.instance-credential` / `mqtt.*` | — | Fase C |

## Token

JWS assinado com Ed25519. Validado a cada requisição só com as chaves em memória, sem consultar o banco:

| Claim | Regra |
|---|---|
| `kid` (cabeçalho) | Chave do JWKS. `kid` desconhecido dispara uma busca do JWKS; sem a chave depois disso, `UNKNOWN_KEY` |
| `iss` | Igual ao configurado, senão `WRONG_ISSUER` |
| `aud` | Contém `public-url`, senão `WRONG_AUDIENCE` |
| `exp` | Obrigatório; vencido dá `TOKEN_EXPIRED` |
| `sub` | Id do usuário no Control Plane (não o e-mail) |
| `email` | E-mail verificado; usado para achar o usuário local no primeiro acesso |
| `tnt` | Obrigatório no token de sessão (`TENANT_REQUIRED`, 403). `X-TENANT-ID` diferente dá `TENANT_MISMATCH` |
| `adm` | `member`, `tenant_admin` ou `client_admin`; ausente vira `member`. Authority `ROLE_CP_<PAPEL>` |
| `seat` | Opcional; exigido com `seat-required=true` |
| `token_use` | `session` (ou ausente) ou `instance` |

**Token de instância** (`token_use=instance`): chamada serviço a serviço. O `sub` é a instância; o tenant vem de
`tnt` ou, na falta dele, do `X-TENANT-ID`, que passa a ser obrigatório. Recebe a authority `ROLE_CP_INSTANCE` e
nunca passa por `@RequiresAdmin`.

**Usuário local.** O núcleo de autorização do Archbase (`@HasPermission`) só reconhece `UserEntity`, então o
módulo resolve o usuário do produto, no tenant do token, nesta ordem: vínculo gravado em `CONTROLPLANE_USUARIO`; id
local igual ao `sub`; e-mail do token, gravando o vínculo. Usuário encontrado e desativado ou bloqueado é recusado
com `USER_DISABLED`. Sem usuário local, a requisição entra só com a sessão: `@HasPermission` nega e
`@RequiresAdmin` continua valendo.

**Recusas** têm corpo `{"status": ..., "error": "<CÓDIGO>", "code": "<CÓDIGO>", "message": "..."}`. Cada código
tem um status só: 401 para token inválido (`INVALID_TOKEN`, `INVALID_SIGNATURE`, `UNKNOWN_KEY`, `TOKEN_EXPIRED`,
`WRONG_ISSUER`, `WRONG_AUDIENCE`) e 403 para o resto (`TENANT_REQUIRED`, `TENANT_MISMATCH`, `NO_SEAT`,
`USER_DISABLED`, `NOT_ADMIN`). 401 é o sinal para o cliente renovar a sessão; 403 não se resolve renovando.

## Licença, entitlements e assentos

**Contratos assumidos — confirmar com as ADRs 0005, 0010 e 0014.** Cada um está isolado numa classe:

- **Licença** (`LicenseVerifier`): JWS Ed25519 do Control Plane, com `iss`, `sub` = `instance-id`, `prd` = produto,
  `token_use=license`, `iat` e `exp`. Verificada offline com o mesmo JWKS do token de sessão. Licença vencida com
  assinatura boa é aceita; quem decide é o status: `VALID` até o `exp`, `GRACE` até `exp + grace-period`, `EXPIRED`
  depois (ou sem licença). Licença reentregue com `exp` menor que a atual é ignorada.
- **Estado do tenant** (`TenantSnapshot`): estado (`ACTIVE`, `DELINQUENT`, `SUSPENDED`; desconhecido vira
  `UNKNOWN`), instante da suspensão, edição, entitlements (booleano = 1/0, limites e créditos numéricos), consumo já
  contabilizado pelo Control Plane e quem tem assento. Versão crescente: snapshot com versão menor ou igual à atual é
  ignorado.

Tudo fica em memória e no banco (`CONTROLPLANE_LICENCA`, `CONTROLPLANE_TENANT`, `CONTROLPLANE_CONSUMO`); nenhuma
consulta por requisição vai ao Control Plane. Com ele fora do ar, o último estado vale até o fim da carência.

| Situação | O que acontece |
|---|---|
| Licença `GRACE` | Tudo segue funcionando |
| Licença `EXPIRED` (ou ausente) | Modo restrito: `GET`/`HEAD`/`OPTIONS`, `@ExportOperation` e `allowed-paths` passam; o resto recebe 403 `LICENSE_EXPIRED`. Nada é apagado |
| Tenant `SUSPENDED` | Token emitido depois da suspensão (login novo) recebe 403 `TENANT_SUSPENDED`; a sessão anterior lê e exporta, mas não escreve |
| `@RequiresEntitlement` sem o entitlement | 403 `NOT_ENTITLED` |
| `@RequiresSeat` | Exige `seat` no token e, se o Control Plane informou os assentos, que o usuário ainda esteja entre eles; senão 403 `NO_SEAT` |
| `consume` | Sempre registra localmente; o estouro volta em `Usage.exceeded()` e no log, sem recusar |

## Convivência com o JWT atual

- `legacy-jwt=accept`: o token do Control Plane (EdDSA) e o JWT atual (HS256, conferido em
  `SEGURANCA_TOKEN_ACESSO`) funcionam no mesmo deploy. O do Control Plane tem prioridade. Sem `X-TENANT-ID`, o
  JWT atual usa o tenant do próprio claim, nunca o tenant padrão.
- `legacy-jwt=reject`: o JWT atual deixa de autenticar e as rotas de login local respondem 404
  `LOCAL_LOGIN_DISABLED`: `/api/v1/auth/authenticate`, `/login`, `/login-flexible`, `/login-social`, `/mfa/verify`,
  `/refresh-token`, `/register`, `/resetPassword`, `/sendResetPasswordEmail/**` e `/tenants`.
- O token de API do Archbase (UUID) não é afetado por nenhum dos dois modos.

## Tabela própria

`CONTROLPLANE_USUARIO` (vínculo `sub` → usuário local, por tenant), acessada por JDBC e não por JPA: projetos com
`ddl-auto=validate` não passam a exigi-la por ter o jar no classpath. Com `schema.mode=off`, crie-a com:

```sql
CREATE TABLE CONTROLPLANE_USUARIO (
    TENANT_ID  VARCHAR(100) NOT NULL,
    CP_USER_ID VARCHAR(100) NOT NULL,
    USER_ID    VARCHAR(40)  NOT NULL,
    DH_VINCULO TIMESTAMP    NOT NULL,
    PRIMARY KEY (TENANT_ID, CP_USER_ID)
);
```

## Integração com o archbase-security

O módulo entra pelo ponto de extensão `br.com.archbase.security.auth.external` do `ArchbaseJwtAuthenticationFilter`
(`ExternalBearerAuthenticator`, `ExternalAuthenticationPolicy`). Sem bean desses tipos, o filtro segue exatamente
o caminho anterior.
