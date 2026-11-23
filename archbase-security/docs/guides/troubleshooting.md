[← Anterior: Melhores Práticas](best-practices.md) | [Voltar ao Índice](../README.md)

---

# Troubleshooting - Solução de Problemas

## Introdução

Problemas comuns do módulo `archbase-security`, com sintoma, causa e o que fazer. Nomes de
propriedade, coluna e classe conferidos contra o código da versão 3.5.0. Tabelas e colunas estão em
[Entidades](../entities/overview.md).

---

## 1. Problemas Comuns

### 1.1 Erro: "Token inválido" mesmo com token correto

**Sintomas**: 401 em toda requisição; no log, `Erro ao processar token JWT` com
`SignatureException`, `DecodingException` ou `WeakKeyException`.

**Causas**:

1. **Chave diferente entre quem emitiu e quem valida** — outra instância, outro ambiente, ou variável
   de ambiente ausente depois de um reinício.
2. **Chave que não é Base64.** `ArchbaseJwtService` decodifica `archbase.security.jwt.secret-key` com
   `Decoders.BASE64` e assina com HS256. Uma chave com caracteres fora do alfabeto Base64 (como
   `minha-chave-de-teste`) falha com `DecodingException`; uma chave que decodifica para menos de
   256 bits falha com `WeakKeyException`.
3. **Espaço ou quebra de linha** colados na chave.

O erro de chave **não aparece na subida**: `initialize()` só avisa se a chave estiver vazia. Ele
aparece na primeira emissão de token (o login falha) e na leitura de cada JWT.

**Solução**:

```bash
# Gerar uma chave válida: 32 bytes aleatórios em Base64
openssl rand -base64 32
```

```properties
archbase.security.jwt.secret-key=${JWT_SECRET}
```

Todas as instâncias que validam o token precisam da **mesma** chave. Não registre a chave em log,
nem parcialmente.

---

### 1.2 Erro: "Access Denied" para administrador

**Sintomas**: usuário com `BO_ADMINISTRADOR = 'S'` recebe 403.

**Causas** (todas conferidas em `DefaultArchbaseAccessEvaluator`):

1. **Conta desativada ou bloqueada.** O atalho de administrador exige `isEnabled()`, que é falso com
   `BO_CONTA_DESATIVADA = 'S'` ou `CONTA_BLOQUEADA = 'S'`.
2. **`BO_ADMINISTRADOR` nulo.** É tratado como não-administrador, com `WARN` no log
   (`Usuário ... está com isAdministrator nulo`).
3. **Negação explícita.** Uma permissão com `EFFECT = 'DENY'` que alcance a capacidade vence também
   para o administrador.
4. **Restrição por perfil, papel ou persona.** `@RequireProfile`, `@RequireRole` e `@RequirePersona`
   com `allowSystemAdmin = false` negam o administrador que não atende à restrição.

O **horário de acesso não é causa**: `AccessScheduleEntity` e `BO_HORARIO_LIVRE` são gravados mas
**não são aplicados** pelo framework, para ninguém — ver
[Entidades Core](../entities/core-entities.md#horário-de-acesso-não-é-aplicado).

**Solução**:

```sql
-- 1. Estado da conta (o id é texto, coluna ID_SEGURANCA)
SELECT id_seguranca, user_name, email,
       bo_administrador, bo_conta_desativada, conta_bloqueada,
       bo_alterar_senha_proximo_login, bo_senha_nunca_expira, dt_ultima_troca_senha
FROM seguranca
WHERE tp_seguranca = 'USUARIO' AND id_seguranca = 'id-do-usuario';

-- 2. Reativar, se for o caso
UPDATE seguranca
SET bo_administrador = 'S', bo_conta_desativada = 'N', conta_bloqueada = 'N'
WHERE tp_seguranca = 'USUARIO' AND id_seguranca = 'id-do-usuario';

-- 3. Negações que alcançam o usuário diretamente (repita com os ids dos grupos e do perfil)
SELECT p.id_permissao, r.nome AS recurso, a.nome AS acao, p.company_id, p.project_id
FROM seguranca_permissao p
JOIN seguranca_acao a   ON a.id_acao = p.id_acao
JOIN seguranca_recurso r ON r.id_recurso = a.id_recurso
WHERE p.id_seguranca = 'id-do-usuario' AND upper(trim(p.effect)) = 'DENY';
```

Não existe coluna de "senha expirada": a expiração é calculada por `isCredentialsNonExpired()` a
partir de `BO_ALTERAR_SENHA_PROXIMO_LOGIN`, `BO_SENHA_NUNCA_EXPIRA`, `DT_ULTIMA_TROCA_SENHA` e
`archbase.security.password.expiration-days`. Senha expirada não aparece como 403 de permissão:
ela recusa o login e a renovação do token (`CredentialsExpiredException`).

**Debug programático**: em vez de um endpoint próprio, use a simulação do diagnóstico
(`POST /api/v1/security/diagnostics/simulate`, ver 1.5), que diz em que portão a decisão parou. Se
precisar de um endpoint, proteja-o pela flag de administrador — **não** com `hasRole(...)`:
`UserEntity.getAuthorities()` devolve lista vazia, então `hasRole`/`hasAuthority` sempre negam.

```java
@GetMapping("/debug/admin-status/{userId}")
public ResponseEntity<Map<String, Object>> debugAdminStatus(@PathVariable String userId,
                                                            Authentication authentication) {
    boolean chamadorAdmin = ArchbaseCurrentUser.of(authentication)
            .map(u -> Boolean.TRUE.equals(u.getIsAdministrator()) && u.isEnabled())
            .orElse(false);
    if (!chamadorAdmin) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    UserEntity user = userJpaRepository.findById(userId)   // id é String
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

    Map<String, Object> status = new LinkedHashMap<>();
    status.put("isAdministrator", user.getIsAdministrator());
    status.put("isEnabled", user.isEnabled());
    status.put("accountNonLocked", user.isAccountNonLocked());
    status.put("credentialsNonExpired", user.isCredentialsNonExpired());
    status.put("changePasswordOnNextLogin", user.getChangePasswordOnNextLogin());
    status.put("passwordChangedAt", user.getPasswordChangedAt());
    return ResponseEntity.ok(status);
}
```

`ArchbaseCurrentUser` está em `br.com.archbase.security.auth`; `UserJpaRepository` em
`br.com.archbase.security.repository`.

---

### 1.3 Erro: Contexto de tenant null em operações assíncronas

**Sintomas**: o código síncrono funciona; em `@Async` ou em tarefa de executor o tenant é nulo e as
consultas voltam vazias ou falham.

**Causa**: `ArchbaseTenantContext` é ThreadLocal. A thread do executor não herda o contexto.

**Solução**: o `archbase-starter-multitenancy` já registra um `TaskDecorator`
(`ArchbaseTenantAwareTaskDecorator`) e um `AsyncConfig` quando a aplicação não declara o seu
`AsyncConfigurer`. O decorator copia tenant, empresa e, por padrão, o `SecurityContext`
(`archbase.multitenancy.async.propagate-security-context=true`). O contexto do Archbase tem tenant e
empresa — não há projeto no `ArchbaseTenantContext`.

Se a aplicação cria o próprio executor, aplique o decorator nele:

```java
@Bean
public ThreadPoolTaskExecutor relatoriosExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("relatorios-");
    executor.setTaskDecorator(new ArchbaseTenantAwareTaskDecorator());
    executor.initialize();
    return executor;
}
```

`@Scheduled` não tem requisição de origem: não há tenant para propagar, e a tarefa tem de definir
(`ArchbaseTenantContext.setTenantId(...)`) e limpar (`clear()`) o tenant ela mesma.

---

### 1.4 Erro: "Token expired" imediatamente após login

**Causa mais comum**: validade configurada em segundos. As duas propriedades são em
**milissegundos**, e nenhuma tem valor padrão:

```properties
# ERRADO — 3,6 segundos
archbase.security.jwt.token-expiration=3600

# 1 hora
archbase.security.jwt.token-expiration=3600000
# 7 dias
archbase.security.jwt.refresh-expiration=604800000
```

Outras causas: relógio do servidor dessincronizado (o `exp` é gerado em UTC a partir do relógio
local) ou várias instâncias com relógios diferentes. Confira com `date -u` e NTP.

Para ver a expiração de um token, decodifique o payload (2.3): o campo `exp` está em segundos desde
a época.

---

### 1.5 A tela de permissões não mostra a capacidade que eu quero conceder

Três causas distintas, e a diferença entre elas importa.

**A tela nunca foi aberta por um administrador.** O catálogo de um recurso de tela é criado pelo
`POST /api/v1/resource/register`, que **escreve** o catálogo e por isso é endpoint administrativo.
Desde a correção do 403 em toda tela, usuário comum apenas lê — quem registra é o administrador. Na
prática: **a capacidade de uma tela só existe depois que um administrador abriu aquela tela ao menos
uma vez.**

É o comportamento correto, e tem um efeito colateral que ninguém conta: a lista de disponíveis
*parece* completa quando não é. Não há como o servidor distinguir uma tela que ninguém abriu de uma
tela que não existe.

> Se o produto esconde telas do administrador por papel, ele está escondendo também o registro
> delas. Abra cada tela uma vez com uma conta administrativa antes de sair concedendo.

**A ação foi desativada.** A varredura de `@HasPermission` desativa capacidades de tipo `API` que o
código não declara mais, e a tela filtra `action.active`. Rode com
`archbase.security.sync.mode=report` e leia o log: ele lista o que seria desativado antes de
escrever qualquer coisa.

**O `@HasPermission` não pôde ser catalogado.** Método anotado cujo recurso não resolve — sem
`resource` na anotação e sem `@ArchbaseResource` na classe que **declara** o método — não vira linha
de catálogo. A varredura registra `ERROR` apontando a classe e o método na subida. A capacidade é
exigida em runtime e não existe para ser concedida, então todo não-administrador leva 403 permanente.

> **Os endpoints de diagnóstico citados aqui e em 1.6** (`/api/v1/security/diagnostics/*`) só
> existem com `archbase.security.diagnostics.enabled=true` (padrão `false`: sem ela o controller nem
> é registrado). E, mesmo
> ligados, respondem 403 a quem não tiver `isAdministrator` com a conta habilitada — independente de
> `archbase.security.admin-endpoints.policy`.

### 1.6 A permissão está concedida e o usuário continua sem conseguir

**Recurso desativado.** Uma concessão sobre ação ativa de recurso **inativo** funciona hoje — o
`@HasPermission` não consulta `active` — e deixa de funcionar no dia em que
`archbase.security.permission.require-active` for ligado. A tela de permissões mostra essas linhas
marcadas como *recurso desativado* em vez de escondê-las, justamente para dar a chance de arrumar
antes. O panorama do diagnóstico conta quantas são.

**Falta uma dependência.** Conceder `aprovar_custo` sem `view` produz alguém que passa na autorização
do endpoint e não chega até ele pela interface — e nada aparece errado em lugar nenhum. Se o código
declara `@HasPermission(requires = ...)`, o relatório de efetivo
(`/api/v1/security/diagnostics/users/{id}/effective`) traz `unmetDependencies` por capacidade, e a
tela de concessão oferece as dependências junto.

**Uma negação explícita.** Uma linha `DENY` em qualquer origem — usuário, grupo ou perfil — vence a
concessão dentro do escopo em que foi declarada. O relatório de efetivo lista a capacidade com
situação `DENIED`, e a simulação (`POST /diagnostics/simulate`) diz em que portão parou.

### 1.7 401 com `X-TENANT-ID` diferente do tenant do token

**Sintoma**: o mesmo token funciona sem o cabeçalho (ou com o tenant certo) e dá 401 quando o
cliente manda outro `X-TENANT-ID` — ou `?X-TENANT-ID=` na query, aceito enquanto
`archbase.app.tenant.accept-query-param=true`.

**Causa**: o cabeçalho entra no contexto **antes** de o token ser conferido. O JWT próprio é procurado
em `SEGURANCA_TOKEN_ACESSO` (`@TenantId`) e o token de API em `SEGURANCA_TOKEN_API`, ambos no tenant
do cabeçalho — onde a linha não existe. Sem autenticação, o resultado é **401** para o **JWT próprio
e para o token de API**. O bloco do filtro que responderia 403 nesse caso nem chega a rodar (ver
`FiltroDeAutenticacaoComportamentoAtualTest`, `tenantDivergenteNaoAutentica` e
`tenantDivergenteDa401`).

Só o **token de IdP externo** (Control Plane, OIDC) recebe **403 `TENANT_MISMATCH`**: nesse caminho o
tenant vem do token e a divergência é conferida explicitamente.

**Solução**: o cliente deve mandar o tenant do próprio token, ou não mandar o cabeçalho — o tenant
do token (`tenantId` no JWT, `TENANT_ID` na linha do token de API) é a fonte de verdade.

### 1.8 401 com `SESSAO_ENCERRADA`

Corpo `{"status":401,"error":"SESSAO_ENCERRADA",...}`: o JWT tem assinatura válida, mas a linha foi
revogada — logout, ou outro login do mesmo usuário com `BO_PERMITE_MULTIPLICOS_LOGINS` diferente de
`'S'` (sessão única: o login novo revoga todos os tokens anteriores). Se o usuário precisa de várias
sessões, ligue `allowMultipleLogins`.

### 1.9 403 `COMPANY_NOT_ALLOWED`

Com `archbase.security.company.validation=enforce`, o filtro confere o `X-COMPANY-ID` contra o
usuário autenticado perguntando aos beans `ArchbaseCompanyAccessResolver`
(`br.com.archbase.security.spi`). Recusa com 403 `COMPANY_NOT_ALLOWED` quando nenhum resolver
permite, quando o resolver lança exceção, ou quando o principal não é um `UserEntity` local. Em
requisição anônima, com `enforce`, a empresa é descartada do contexto (sem 403).

- `enforce` sem nenhum resolver registrado **não sobe** (validador de hardening, 1.11).
- Para medir antes de recusar, use `warn`: registra no log o que seria recusado e deixa passar.
- O padrão é `off`.

### 1.10 Aplicação não sobe: `Could not resolve placeholder`

Estas propriedades são lidas sem valor padrão e têm de estar definidas:

| Propriedade | Lida em |
|-------------|---------|
| `archbase.security.jwt.secret-key` | `ArchbaseJwtService` |
| `archbase.security.jwt.token-expiration` | `ArchbaseJwtService` |
| `archbase.security.jwt.refresh-expiration` | `ArchbaseJwtService` |
| `archbase.security.whitelist` | `DefaultArchbaseSecurityConfiguration` |
| `archbase.security.cors.allowed-origins` | idem |
| `archbase.security.cors.allowed-methods` | idem |
| `archbase.security.cors.allowed-headers` | idem |
| `archbase.security.cors.allow-credentials` | idem |

As de `whitelist` e `cors.*` só são exigidas quando vale a configuração padrão, isto é, quando a
aplicação não registra uma `CustomSecurityConfiguration`.

### 1.11 Aplicação não sobe: "Proteções foram habilitadas sem os pré-requisitos atendidos"

É o `ArchbaseSecurityHardeningValidator`, que roda no `ApplicationReadyEvent` e lança
`IllegalStateException` quando uma proteção ligada não tem como funcionar. A mensagem numera cada
item e diz o que fazer. Os casos atuais:

- `api-token.purge-plaintext=true` com `hash-enabled=false`, ou com token de API ainda sem hash;
- `require-role.no-resolver-policy=deny` sem bean `ArchbaseRoleResolver`;
- `company.validation=enforce` sem bean `ArchbaseCompanyAccessResolver`, ou valor desconhecido;
- `admin-endpoints.policy=permission` sem recurso + ação `MANAGE` cadastrados para `USER`, `GROUP`,
  `RESOURCE`, `ACTION`, `USER_PROFILE`, `API_TOKEN`, `ACCESS_TOKEN`;
- `jwt.strict-token-use=true` com sessões ativas sem `TP_USO_TOKEN`;
- um endpoint da aplicação mapeado na URL do logout (`archbase.security.logout.url`).

Para diagnóstico, `archbase.security.hardening.validation=warn` troca a falha por `ERROR` no log
(`off` desliga). Não deixe assim: a proteção continua sem funcionar. Detalhes em
`deployment/security-hardening.md`.

---

## 2. Dicas de Debug

### 2.1 Habilitar Logging Detalhado

```properties
# Módulo de segurança
logging.level.br.com.archbase.security=DEBUG

# Uma linha por decisão de @HasPermission: "PERMITE (...) via ..." ou "NEGA em <portão> (...)"
logging.level.br.com.archbase.security.config.AuthorizationAdapters=DEBUG

# Spring Security
logging.level.org.springframework.security=DEBUG

# SQL e parâmetros (Hibernate 6/7)
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE

# Filtros HTTP
logging.level.org.springframework.web.filter=DEBUG
```

`org.hibernate.type.descriptor.sql.BasicBinder` é o logger do Hibernate 5 e não mostra nada no
Hibernate 6/7. Em produção, volte `br.com.archbase.security` para `INFO`: em `DEBUG` o filtro
registra tenant e usuário de cada requisição.

### 2.2 Debugar Avaliação de Permissões

Não reimplemente o avaliador. A decisão, com o motivo, está disponível em
`br.com.archbase.security.service.ArchbaseSecurityService` — classe que já existe, não a declare de
novo:

```java
@Autowired
private ArchbaseSecurityService securityService;

AccessDecision d = securityService.decide(authentication, "VIEW", "SALES_REPORT_API",
        tenantId, companyId, projectId);

log.info("{}", d.summary());       // "NEGA em SCOPE (OUT_OF_SCOPE)" / "PERMITE (...) via Perfil X"
d.chain().forEach(o -> log.info("  {}", o)); // portão a portão
```

`AccessDecision` é um record com `allowed`, `deniedAt` (o portão), `reasonCode`, `message`,
`grantedBy`, `grantedByName` e `chain`. Uma negação vinda de `decide(...)` é gravada como
`ACESSO_NEGADO` em `SEGURANCA_EVENTO` (com `archbase.security.audit.enabled=true`).

Para avaliar **outra pessoa** sem escrever código e sem poluir a trilha, use a simulação (exige o que
está na nota de 1.5):

```http
POST /api/v1/security/diagnostics/simulate
Content-Type: application/json

{ "userId": "id-do-usuario", "resource": "SALES_REPORT_API", "action": "VIEW",
  "tenantId": "t1", "companyId": null, "projectId": null }
```

`userId` ou `email`, mais `resource` e `action`, são obrigatórios. A resposta é o mesmo
`AccessDecision`.

### 2.3 Inspecionar Token JWT

O que o `ArchbaseJwtService` põe no token: `sub` (`userName`), `iat`, `exp`, `jti`, `token_use`
(`access`, `refresh` ou `mfa_challenge`) e, se o usuário tiver tenant, `tenantId`. **Não** há claims
`userId`, `companyId` nem `roles` — a empresa vem do `X-COMPANY-ID`, e os papéis não estão no token.

Para ler o payload, não é preciso endpoint nem site externo (nunca cole token de produção em site
público):

```bash
#!/bin/bash
# decode-jwt.sh — mostra header e payload (não confere assinatura)
TOKEN=${1#"Bearer "}
b64url() { local s=$(echo -n "$1" | tr '_-' '/+'); while [ $(( ${#s} % 4 )) -ne 0 ]; do s="$s="; done; echo -n "$s" | base64 -d; }
echo "=== HEADER ==="; b64url "$(echo "$TOKEN" | cut -d. -f1)" | jq .
echo "=== PAYLOAD ==="; b64url "$(echo "$TOKEN" | cut -d. -f2)" | jq .
```

Se quiser um endpoint de desenvolvimento, use só os métodos **públicos** do serviço:
`extractUsername`, `extractTenantId`, `extractTokenUse`, `extractExpiration`, `isTokenExpired`,
`isAccessToken`, `isRefreshToken`, `isTokenValid(token, userDetails)` e
`extractClaim(token, claims -> ...)`. `extractAllClaims` é privado. Lembre que `extractExpiration` e
`extractClaim` validam a assinatura e lançam `ExpiredJwtException` em token vencido.

```java
@RestController
@RequestMapping("/api/debug")
@Profile("dev") // APENAS EM DESENVOLVIMENTO
public class TokenDebugController {

    private final ArchbaseJwtService jwtService;

    public TokenDebugController(ArchbaseJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/token-info")
    public Map<String, Object> tokenInfo(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("isExpired", jwtService.isTokenExpired(token));   // não lança
        try {
            info.put("username", jwtService.extractUsername(token));
            info.put("tenantId", jwtService.extractTenantId(token));
            info.put("tokenUse", jwtService.extractTokenUse(token));
            info.put("expiration", jwtService.extractExpiration(token));
            info.put("jti", jwtService.extractClaim(token, Claims::getId));
        } catch (JwtException e) {
            info.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return info;
    }
}
```

Um token decodificável e não vencido ainda pode ser recusado: o filtro exige também a linha em
`SEGURANCA_TOKEN_ACESSO` sem `TOKEN_REVOGADO`/`TOKEN_EXPIRADO`, no tenant do contexto (ver 1.7 e 1.8).

---

## 3. Monitoramento

**O módulo de segurança não publica métricas.** Não há `MeterRegistry`, contador nem timer em
`archbase-security`; nomes como `security.authentication.failures` ou `security.tokens.active` não
existem, e alertas Prometheus escritos contra eles nunca disparam. (O `archbase-controlplane` tem as
suas próprias métricas, opt-in — ver o README dele.)

O que existe para observar segurança:

- **`SEGURANCA_EVENTO`** — login, falha de login, logout, acesso negado e simulação, com usuário,
  origem e o portão que recusou. Gravada só com `archbase.security.audit.enabled=true`. Consulta por
  `GET /api/v1/security/audit/events` (administrador). Em multi-tenant, ligue
  `archbase.security.audit.tenant-scoped=true`; sem ela, administrador de um tenant lê os eventos de
  todos.
- **Trilha do Envers** (`*_AUD` + `SEGURANCA_REVISAO`) — quem mudou usuário, grupo, perfil,
  permissão, ação e recurso. Mesma chave `audit.enabled`, e as tabelas têm de existir antes.
- **Logs**: `NÃO AUTENTICADO` (entry point, 401), `Acesso cross-tenant NEGADO`, `Acesso à empresa
  NEGADO`, `[segurança] ...` na subida (proteções inertes e pré-requisitos).

Se a aplicação quer métricas, derive-as desses eventos, ou instrumente os pontos que ela mesma
controla.

---

## 4. Checklist de Troubleshooting

### Problemas de Autenticação

- [ ] `archbase.security.jwt.secret-key` em Base64, ≥ 256 bits, igual em todas as instâncias
- [ ] `token-expiration` e `refresh-expiration` em milissegundos
- [ ] Relógio do servidor sincronizado
- [ ] Conta sem `BO_CONTA_DESATIVADA`/`CONTA_BLOQUEADA` e credenciais não expiradas
- [ ] `X-TENANT-ID` igual ao tenant do token, ou ausente (1.7)
- [ ] Token não revogado por outro login de sessão única (1.8)

### Problemas de Autorização

- [ ] A capacidade existe no catálogo e está ativa (1.5)
- [ ] Há concessão no usuário, num grupo dele ou no perfil
- [ ] Nenhum `DENY` alcança a capacidade (vale também para administrador)
- [ ] `COMPANY_ID`/`PROJECT_ID` da concessão compatíveis com a requisição
- [ ] `@HasPermission` com `action`, `description` e recurso resolvível
- [ ] Simulação do diagnóstico consultada (2.2)

### Problemas de Multi-Tenancy

- [ ] Tenant chega pelo cabeçalho ou pelo token
- [ ] Executores próprios com `ArchbaseTenantAwareTaskDecorator` (1.3)
- [ ] `@Scheduled` define e limpa o tenant por conta própria
- [ ] Entidades da aplicação estendem `TenantPersistenceEntityBase`

---

## 5. Recursos Adicionais

- [Configuração](configuration.md)
- [Entidades](../entities/overview.md)
- [Arquitetura de autorização](../../ARQUITETURA.md)
- [Multi-Tenancy](../../../archbase-multitenancy/README.md)
- Endurecimento: `deployment/security-hardening.md`
- Documentação principal: [readme-security.md](../../readme-security.md)

---

[← Anterior: Melhores Práticas](best-practices.md) | [Voltar ao Índice](../README.md)
