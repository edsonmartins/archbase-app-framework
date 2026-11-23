# archbase-controlplane

Liga uma aplicação Archbase ao Control Plane: aceita o token de sessão do tenant emitido por ele, valida
licença, assentos e entitlements, e recebe comandos de provisionamento. **Opt-in**: sem
`archbase.controlplane.enabled=true`, nenhum bean do módulo é criado e a aplicação se comporta como antes.

> **Estado:** Fases A (token de sessão), B (licença, entitlements, assentos, modo restrito), C (comandos de
> provisionamento, status e heartbeat por MQTT) e D (métricas e regras de alerta).
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
archbase.controlplane.instance-credential=${CP_INSTANCE_CREDENTIAL}
archbase.controlplane.product-version=${APP_VERSION}
archbase.controlplane.mqtt.uri=ssl://broker.controlplane:8883   # ou wss://broker.controlplane:443/mqtt

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
| `archbase.controlplane.schema.mode` | `apply` | `apply`: cria na subida as seis tabelas do módulo que faltarem (ver [Tabelas próprias](#tabelas-próprias)); `off`: a aplicação cria |
| `archbase.controlplane.instance-id` | — | Id da instância; a licença tem de ser dela (`sub`) |
| `archbase.controlplane.license.path` | — | Arquivo com a licença (JWS), aplicado na subida se for mais nova que a do banco |
| `archbase.controlplane.grace-period` | `7d` | Carência depois do `exp` da licença (status `GRACE`); depois, modo restrito |
| `archbase.controlplane.restricted-mode.enforcement` | `enforce` | `enforce` recusa; `report` só registra em log |
| `archbase.controlplane.restricted-mode.allowed-paths` | `/api/v1/auth/**` | Caminhos liberados no modo restrito (padrão Ant) |
| `archbase.controlplane.instance-credential` | — | Credencial da instância: senha no broker e Bearer do heartbeat HTTPS |
| `archbase.controlplane.product-version` | `desconhecida` | Versão do produto; mudou, presets e manifesto são publicados de novo |
| `archbase.controlplane.mqtt.uri` | — | Broker (`ssl://...:8883` ou `wss://...:443/...`). Vazio: canal MQTT desligado |
| `archbase.controlplane.mqtt.username` / `password` / `client-id` | `instance-id` / `instance-credential` / `instance-id` | Identidade no broker |
| `archbase.controlplane.mqtt.session-expiry` | `7d` | Quanto tempo o broker guarda a sessão e os comandos pendentes |
| `archbase.controlplane.mqtt.reconnect-min-delay` / `reconnect-max-delay` | `1s` / `2m` | Espera crescente entre reconexões |
| `archbase.controlplane.mqtt.keep-alive` | `60s` | Keep-alive MQTT |
| `archbase.controlplane.heartbeat.interval` | `5m` | Intervalo do heartbeat |
| `archbase.controlplane.heartbeat.url` | `{url}/api/v1/instances/{instance-id}/heartbeat` | Fallback HTTPS do heartbeat |

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

## Provisionamento por MQTT

O módulo cuida do transporte; o produto implementa só o que cada comando significa para o seu banco:

```java
@Component
class PowerViewProvisioner implements TenantProvisioner {
    public ProvisionResult provision(TenantSpec spec) { ... }   // cria o tenant a partir do preset
    public ProvisionResult update(TenantSpec spec)    { ... }   // edição, entitlements, assentos
    public ProvisionResult suspend(String tenantId)   { ... }
    public ProvisionResult resume(String tenantId)    { ... }
    public ProvisionResult deprovision(String tenantId) { ... } // exporta e apaga
    public ProvisionResult reset(String tenantId)     { ... }   // demo: volta ao preset
    public List<PresetDescriptor> presets()           { ... }
}
```

Cada chamada roda numa transação, com `ArchbaseTenantContext` no tenant do comando. Devolver
`ProvisionResult.failed(motivo)` ou lançar exceção desfaz tudo e publica `failed`; o Control Plane reenvia. Um
`FeatureManifestProvider` opcional publica as chaves de entitlement que o produto entende.

| Tópico | Uso |
|---|---|
| `cp/v1/inst/{id}/cmd` | Comandos do Control Plane (assinados), QoS 1 |
| `cp/v1/inst/{id}/evt` | Resultado de cada comando (`applied`, `failed`, `rejected`), heartbeat e manifesto |
| `cp/v1/inst/{id}/status` | Retido: `online` ao conectar, `offline` ao parar, e `offline` como Last Will se a instância cair |

**Garantias:**

- **Assinatura.** Comando sem assinatura válida do Control Plane, ou endereçado a outra instância, não chama nada.
  É registrado no logger `br.com.archbase.controlplane.security` e publicado como `rejected`.
- **Idempotência.** O id do comando (`jti`) vai para `CONTROLPLANE_COMANDO` na mesma transação do produto.
  Repetição não chama o produto e é publicada como `applied` com `duplicate: true`, para o Control Plane parar de
  reenviar. Se o produto falha, o registro também é desfeito e o reenvio é aplicado.
- **Entrega.** A sessão é persistente (`cleanStart=false`) e o ack ao broker só sai depois do processamento: comando
  publicado com a instância fora chega quando ela volta, e uma queda no meio do processamento faz o broker
  reentregar.
- **Comandos do próprio módulo.** `license.renewed`, `entitlements.changed` e `membership.changed` atualizam os caches
  sem chamar o produto. O cache do tenant só muda depois do commit do produto.
- **Heartbeat.** Sai a cada `heartbeat.interval` com versão, tenants atendidos, saúde, status da licença e consumo
  metered ainda não reportado (agregado por tenant e chave). Com o broker fora do ar, vai por HTTPS. O consumo só é
  marcado como reportado depois da confirmação.
- **Presets e manifesto.** Publicados no primeiro registro e a cada `product-version` nova.
- **Broker fora do ar na subida.** Não impede a aplicação de subir: a conexão é tentada em segundo plano, com espera
  crescente.

**Contrato assumido — confirmar com a ADR-0014** (isolado em `CommandVerifier`). O comando é um JWS Ed25519 do
Control Plane com `iss`, `sub` = `instance-id`, `token_use=command`, `jti` = id do comando, `cmd` = tipo, `iat` e o
objeto `data`, com `tenantId` e os campos do tipo. `data.version`, se vier, ordena as mudanças de estado do tenant:
comando atrasado com versão menor é ignorado. Sem ela, vale a ordem de chegada.

## Métricas e alertas

Cada caso de falha tem log estruturado (`controlplane.*`) e métrica no Micrometer. O módulo só registra no
`MeterRegistry` da aplicação. Para o VictoriaMetrics coletar, a aplicação expõe em formato Prometheus:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
management.endpoints.web.exposure.include=health,prometheus
```

Sem registry, as métricas não vão a lugar nenhum e nada quebra. Nenhuma métrica tem o tenant como tag: o número de
tenants não tem teto, e o tenant está no log de cada caso.

| Métrica (Prometheus) | Tipo | Tags | Caso |
|---|---|---|---|
| `controlplane_license_status` | gauge | — | 0 `VALID`, 1 `GRACE`, 2 `EXPIRED` ou ausente |
| `controlplane_license_expiry_seconds` | gauge | — | Segundos até o `exp` (negativo depois) |
| `controlplane_license_grace_end_seconds` | gauge | — | Segundos até o modo restrito |
| `controlplane_restricted_denied_total` | counter | `code`, `enforcement` | Escrita barrada (`LICENSE_EXPIRED`, `TENANT_SUSPENDED`); `report` também conta |
| `controlplane_tenants` | gauge | `state` | Tenants por estado |
| `controlplane_jwks_keys` | gauge | — | Chaves carregadas; 0 = nenhum token aceito |
| `controlplane_jwks_fetch_total` | counter | `result` | Buscas do JWKS (`success`, `failure`) |
| `controlplane_mqtt_connected` | gauge | — | 1 = conectado ao broker |
| `controlplane_mqtt_connect_failures_total` | counter | — | Falhas da primeira conexão |
| `controlplane_mqtt_disconnects_total` | counter | — | Quedas da conexão |
| `controlplane_heartbeat_total` | counter | `channel` | `mqtt`, `https` (broker fora) ou `none` (não entregue) |
| `controlplane_usage_unreported` | gauge | — | Consumo metered ainda não reportado |
| `controlplane_usage_exceeded_total` | counter | `key` | Consumo acima do limite (tolerado) |
| `controlplane_commands_total` | counter | `type`, `status` | `applied`, `duplicate`, `failed` (produto falhou), `rejected` (assinatura inválida) |
| `controlplane_token_rejected_total` | counter | `code` | Token recusado, inclusive `TENANT_MISMATCH` decidido no filtro |
| `controlplane_access_denied_total` | counter | `code` | `NOT_ADMIN`, `NO_SEAT`, `NOT_ENTITLED` |

As regras de alerta prontas estão em [`alerts/controlplane-alerts.yml`](alerts/controlplane-alerts.yml), no formato do
vmalert, também importável no Grafana Alerting. Há uma regra por caso da tabela de falhas: JWKS inacessível, sem
chaves, carência, carência acabando, modo restrito, escrita barrada, tenant suspenso, broker desconectado, heartbeat
por HTTPS ou não entregue, consumo sem reconciliar ou acima do limite, provisionamento que falhou, comando forjado e
tokens recusados em volume. O teste `PrometheusNamesTest` quebra o build se uma regra citar uma métrica que o
módulo não exporta.

`controlplane_usage_unreported` faz um `COUNT` no banco a cada coleta. É barato, mas se o intervalo de coleta for
muito curto, considere aumentá-lo.

## Convivência com o JWT atual

- `legacy-jwt=accept`: o token do Control Plane (EdDSA) e o JWT atual (HS256, conferido em
  `SEGURANCA_TOKEN_ACESSO`) funcionam no mesmo deploy. O do Control Plane tem prioridade. Sem `X-TENANT-ID`, o
  JWT atual usa o tenant do próprio claim, nunca o tenant padrão.
- `legacy-jwt=reject`: o JWT atual deixa de autenticar e as rotas de login local respondem 404
  `LOCAL_LOGIN_DISABLED`: `/api/v1/auth/authenticate`, `/login`, `/login-flexible`, `/login-social`, `/mfa/verify`,
  `/refresh-token`, `/register`, `/resetPassword`, `/sendResetPasswordEmail/**` e `/tenants`.
- O token de API do Archbase (UUID) não é afetado por nenhum dos dois modos.

## Tabelas próprias

Seis tabelas, todas acessadas por JDBC e não por JPA: projetos com `ddl-auto=validate` não passam a exigi-las por
ter o jar no classpath. Ficam no banco, e não em arquivo, para que o consumo e o registro de comando aplicado
entrem na mesma transação do produto.

| Tabela | Criada por | Para que serve |
|---|---|---|
| `CONTROLPLANE_USUARIO` | `UserLinkStore` | Vínculo `sub` do Control Plane → usuário local, por tenant |
| `CONTROLPLANE_LICENCA` | `ControlPlaneStateStore` | A licença da instância (JWS), uma linha só |
| `CONTROLPLANE_TENANT` | `ControlPlaneStateStore` | O último snapshot de estado de cada tenant, em JSON, com a versão |
| `CONTROLPLANE_CONSUMO` | `ControlPlaneStateStore` | Consumo metered registrado localmente, até ser reportado ao Control Plane |
| `CONTROLPLANE_COMANDO` | `ControlPlaneStateStore` | Id (`jti`) de cada comando aplicado — a idempotência do provisionamento |
| `CONTROLPLANE_META` | `ControlPlaneStateStore` | Chave/valor interno; hoje guarda a última `product-version` cujo manifesto foi publicado |

Com `archbase.controlplane.schema.mode=apply` (padrão), as tabelas que faltam são criadas na subida, depois de
todos os beans existirem; a existência é conferida pelos metadados do JDBC, e tabela que já existe não é alterada.
É uma propriedade do módulo, separada de `archbase.security.schema.mode`, e só tem dois valores: `apply` e `off` —
não há `report`.

Com `schema.mode=off`, crie as seis antes da subida. O tipo `TEXT` abaixo é o do PostgreSQL; o módulo usa
`LONGTEXT` em MySQL/MariaDB, `NVARCHAR(MAX)` em SQL Server e `CLOB` nos demais.

```sql
CREATE TABLE CONTROLPLANE_USUARIO (
    TENANT_ID  VARCHAR(100) NOT NULL,
    CP_USER_ID VARCHAR(100) NOT NULL,
    USER_ID    VARCHAR(40)  NOT NULL,
    DH_VINCULO TIMESTAMP    NOT NULL,
    PRIMARY KEY (TENANT_ID, CP_USER_ID)
);

CREATE TABLE CONTROLPLANE_LICENCA (
    ID             VARCHAR(20) NOT NULL PRIMARY KEY,
    TOKEN          TEXT        NOT NULL,
    DH_ATUALIZACAO TIMESTAMP   NOT NULL
);

CREATE TABLE CONTROLPLANE_TENANT (
    TENANT_ID      VARCHAR(100) NOT NULL PRIMARY KEY,
    VERSAO         BIGINT       NOT NULL,
    SNAPSHOT       TEXT         NOT NULL,
    DH_ATUALIZACAO TIMESTAMP    NOT NULL
);

CREATE TABLE CONTROLPLANE_CONSUMO (
    ID         VARCHAR(40)  NOT NULL PRIMARY KEY,
    TENANT_ID  VARCHAR(100) NOT NULL,
    CHAVE      VARCHAR(200) NOT NULL,
    QUANTIDADE BIGINT       NOT NULL,
    DH_CONSUMO TIMESTAMP    NOT NULL,
    REPORTADO  INTEGER      NOT NULL
);

CREATE TABLE CONTROLPLANE_COMANDO (
    ID           VARCHAR(100) NOT NULL PRIMARY KEY,
    TIPO         VARCHAR(100) NOT NULL,
    DH_APLICACAO TIMESTAMP    NOT NULL
);

CREATE TABLE CONTROLPLANE_META (
    CHAVE          VARCHAR(100) NOT NULL PRIMARY KEY,
    VALOR          VARCHAR(500) NOT NULL,
    DH_ATUALIZACAO TIMESTAMP    NOT NULL
);
```

## Integração com o archbase-security

O módulo entra pelo ponto de extensão `br.com.archbase.security.auth.external` do `ArchbaseJwtAuthenticationFilter`
(`ExternalBearerAuthenticator`, `ExternalAuthenticationPolicy`). Sem bean desses tipos, o filtro segue exatamente
o caminho anterior.

### Convivência com o archbase-security-oidc

Os dois módulos podem estar ligados no mesmo deploy. O filtro consulta os `ExternalBearerAuthenticator` na ordem do
`@Order`, e **o primeiro cujo `supports()` aceita o token assume a requisição inteira** — os seguintes não são
consultados, e o fluxo do JWT próprio também não roda:

| Ordem | Autenticador | `supports()` aceita quando |
|---|---|---|
| `ORDER_CONTROL_PLANE = 100` | `ControlPlaneBearerAuthenticator` | o cabeçalho do JWS declara `alg` EdDSA |
| `ORDER_OIDC = 200` | `OidcBearerAuthenticator` | o `iss` do token é um dos provedores configurados |

Na prática: o token do Control Plane é reconhecido antes, e um token de IdP externo, que não é EdDSA, segue para o
OIDC. Um IdP configurado no OIDC que assine com EdDSA teria os tokens tomados pelo Control Plane e recusados por
ele. A recusa vem de quem assumiu o token: um token reconhecido e inválido é recusado, não repassado ao próximo.

A `ExternalAuthenticationPolicy` é só do Control Plane (o OIDC não registra uma), então `legacy-jwt=reject` vale
para o deploy inteiro: com ela, o JWT próprio deixa de autenticar mesmo com o OIDC ligado. Havendo mais de uma
política, o filtro as combina e basta uma recusar.
