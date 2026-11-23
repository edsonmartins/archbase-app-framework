[← Voltar ao Índice](../README.md) | [Próximo: Anotações e Uso →](annotations-and-usage.md)

---

# Configuração do Módulo archbase-security

Referência das propriedades lidas pelo `archbase-security`, pelo `archbase-starter-security` e pela parte de
tenant do `archbase-multitenancy`. Cada tabela traz o nome, o padrão e o que a propriedade faz.

Duas regras para ler esta página:

- **Os padrões preservam o comportamento antigo.** Várias proteções vêm desligadas para que atualizar o
  framework não quebre quem já está em produção. O valor recomendado para produção está na
  [referência rápida do guia de endurecimento](../../../deployment/security-hardening.md#referência-rápida-para-produção)
  e no [checklist](#14-checklist-de-produção) ao fim desta página.
- **Propriedade que não está aqui não existe.** `archbase.security.method.enabled`,
  `archbase.security.permission.cache.*` (não há cache de permissões), `archbase.security.jwt.expiration` e
  `archbase.multitenancy.scan-packages` apareciam em versões antigas desta documentação e nunca foram lidas pelo código.

Módulos opt-in têm referência própria: [IdP externo (`archbase-security-oidc`)](../../../archbase-security-oidc/README.md)
e [Control Plane (`archbase-controlplane`)](../../../archbase-controlplane/README.md).

---

## 1. Obrigatórias

Não têm valor padrão. Sem qualquer uma delas o placeholder não resolve e **a aplicação não sobe**.

| Propriedade | O que faz |
|---|---|
| `archbase.security.jwt.secret-key` | Chave HS256 dos tokens. Decodificada como **Base64 padrão** (`Decoders.BASE64`): alfabeto URL-safe (`-`, `_`) e texto livre são recusados. Depois de decodificada, mínimo de 32 bytes. |
| `archbase.security.jwt.token-expiration` | Validade do access token, em **milissegundos**. |
| `archbase.security.jwt.refresh-expiration` | Validade do refresh token, em milissegundos. |
| `archbase.security.whitelist` | Rotas liberadas sem autenticação, além das do framework. Separadas por vírgula, **sem espaços** (o valor é dividido em `,` sem `trim`). Vazio (`archbase.security.whitelist=`) é válido e significa nenhuma rota extra. |
| `archbase.security.cors.allowed-origins` | Origens aceitas, separadas por vírgula. |
| `archbase.security.cors.allowed-methods` | Métodos aceitos, separados por vírgula. |
| `archbase.security.cors.allowed-headers` | Cabeçalhos aceitos, separados por vírgula (`*` é aceito). |
| `archbase.security.cors.allow-credentials` | `true`/`false`. Com `true`, a origem não pode ser `*`. |

A whitelist e o CORS são lidos pela `DefaultArchbaseSecurityConfiguration`. Uma aplicação que registra a
própria configuração (ver [Exemplos, §3](code-examples.md#3-configuração-customizada-de-segurança)) não
usa essas chaves — nem as `public-paths.*` da seção 2.

```properties
archbase.security.jwt.secret-key=${JWT_SECRET}
archbase.security.jwt.token-expiration=3600000        # 1 hora
archbase.security.jwt.refresh-expiration=604800000    # 7 dias
archbase.security.whitelist=/actuator/health
archbase.security.cors.allowed-origins=https://app.example.com
archbase.security.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
archbase.security.cors.allowed-headers=*
archbase.security.cors.allow-credentials=true
```

### Gerando a chave JWT

```bash
# OpenSSL (48 bytes aleatórios)
openssl rand -base64 48

# Python (Base64 padrão; NÃO use secrets.token_urlsafe)
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(48)).decode())"
```

A chave de produção vem de variável de ambiente ou cofre de segredos, nunca do repositório. Um valor fixo
só no profile de desenvolvimento, e também em Base64 válido:

```properties
# application-dev.properties
archbase.security.jwt.secret-key=${JWT_SECRET:ZGV2LW9ubHkta2V5LW5vdC1mb3ItcHJvZHVjdGlvbi11c2UtMTIzNDU2Nzg5MA==}
```

### Escolhendo as validades

O refresh precisa ser bem maior que o access token. Valores de referência:

| Validade | Milissegundos |
|---|---|
| 15 min | `900000` |
| 1 hora | `3600000` |
| 4 horas | `14400000` |
| 24 horas | `86400000` |
| 7 dias | `604800000` |
| 30 dias | `2592000000` |

`refresh-expiration` também define o prazo para ligar `jwt.strict-token-use` (seção 7).

---

## 2. Liga/desliga do starter e caminhos públicos

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.enabled` | `true` | Liga a auto-configuração do starter (`ArchbaseSecurityApplicationConfig`, `DefaultArchbaseSecurityConfiguration`, `MethodSecurityConfig`) e a do esquema de segurança. |
| `archbase.security.public-paths.actuator` | `true` | Libera `/actuator/**`. Com `management.endpoints.web.exposure.include=*`, expõe `env`, `configprops` e `heapdump`. Em produção, desligue e libere só `/actuator/health` pela `whitelist`. |
| `archbase.security.public-paths.registration` | `true` | Auto-cadastro anônimo em `POST /api/v1/auth/register`. Com `false`, essa rota passa a exigir autenticação; o resto de `/api/v1/auth/**` continua anônimo. |
| `archbase.security.public-paths.swagger` | `true` | Libera `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/swagger-resources/**`, `/webjars/**` e as rotas do Swagger v2. |
| `archbase.security.public-paths.static-files` | `true` | Libera `/api/files/**`, `/static/**`, `/*.html`, `/*.png`, `/*.jpg`, `/*.jpeg`. **Se a aplicação serve arquivos de usuário em `/api/files/**`, eles ficam públicos.** |
| `archbase.security.public-paths.legacy-app-routes` | `true` | Deprecado. Libera `/api/v1/assistente-virtual/webhook` e `/api/v1/licenca/verificar-tenants/**`, rotas de uma aplicação específica. Quem depende delas deve movê-las para `whitelist` e desligar. |
| `archbase.security.logout.enabled` | `true` | Registra o logout que revoga os tokens da sessão. Desligue se a aplicação já tem fluxo próprio. |
| `archbase.security.logout.url` | `/api/v1/auth/logout` | Caminho do logout. O `LogoutFilter` intercepta antes do controller: se a aplicação mapeia um endpoint no mesmo caminho, a subida falha (seção 12). |

Sempre públicos, independentemente das chaves acima: `/api/v1/auth/**`, `/api/v1/apiToken/activate`,
`/error` e requisições `OPTIONS`. Na subida, o log mostra a lista somada em
`Superfície anônima da API (...)`.

---

## 3. Tenant e empresa

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.multitenancy.enabled` | `true` | Liga a auto-configuração do `archbase-starter-multitenancy`: interceptor que lê `X-TENANT-ID`/`X-COMPANY-ID` e propagação do contexto para `@Async`. |
| `archbase.multitenancy.async.propagate-security-context` | `true` | Leva o usuário autenticado para dentro do `@Async`, além do tenant. |
| `archbase.app.tenant.default.id` | — | Tenant usado quando não há tenant no contexto (tarefas de sistema, varredura de `@HasPermission` na subida). Sem ele, o resolver cai em `archbase` com WARN no log. |
| `archbase.app.tenant.fail-on-missing` | `false` | `true`: recusa a operação sem tenant no contexto, em vez de cair no tenant padrão. Todo acesso legítimo precisa carregar tenant. Exigido pelo Control Plane. |
| `archbase.app.tenant.accept-query-param` | `true` | Aceita `X-TENANT-ID`/`X-COMPANY-ID` na query string, além do cabeçalho. |
| `archbase.security.company.validation` | `off` | Confere o `X-COMPANY-ID` contra o usuário autenticado: `off`, `warn` (só registra) ou `enforce` (403 `COMPANY_NOT_ALLOWED`; empresa descartada em rota anônima). Exige um bean `ArchbaseCompanyAccessResolver`; com `enforce` e sem o bean, a aplicação não sobe. Ligue `warn` antes. |

Detalhes e exemplo do resolver em
[security-hardening.md — Empresa no cabeçalho](../../../deployment/security-hardening.md#empresa-no-cabeçalho-x-company-id).

---

## 4. Endurecimento de autorização

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.admin-endpoints.policy` | `permit` | Quem chama os endpoints administrativos do módulo (usuário, grupo, recurso, ação, perfil, tokens). `permit`: qualquer autenticado; `admin-only`: exige `isAdministrator`; `permission`: exige a permissão declarada no endpoint (administrador continua passando). Valor desconhecido nega. Ver a ressalva sobre o auto-registro de telas do archbase-react no guia de endurecimento. |
| `archbase.security.admin-guard.enabled` | `true` | Impede que um não-administrador crie ou promova administrador, ou edite a conta de um. |
| `archbase.security.admin-guard.allow-unverifiable-principal` | `false` | `true` aceita, nessa trava, um principal que não é `UserEntity` (aplicação com `UserDetailsService` próprio). Reabre a escalação. |
| `archbase.security.require-role.no-resolver-policy` | `permit` | O que `@RequireRole` faz sem bean `ArchbaseRoleResolver`: `permit` libera, `deny` nega. `deny` sem o bean impede a subida. |
| `archbase.security.prevent-user-enumeration` | `false` | O pedido de e-mail de redefinição de senha responde igual para e-mail cadastrado, não cadastrado, usuário sem permissão de troca e falha de envio. O frontend precisa exibir uma mensagem neutra. |

---

## 5. Senha

Toda a política vem desligada.

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.password.min-length` | `0` | Comprimento mínimo. `0` = sem mínimo. |
| `archbase.security.password.require-digit` | `false` | Exige dígito. |
| `archbase.security.password.require-uppercase` | `false` | Exige maiúscula. |
| `archbase.security.password.require-lowercase` | `false` | Exige minúscula. |
| `archbase.security.password.require-special` | `false` | Exige caractere especial. |
| `archbase.security.password.block-common` | `false` | Recusa senhas comuns. |
| `archbase.security.password.expiration-days` | `0` | Validade da senha em dias. `0` = sem expiração periódica. |
| `archbase.security.password-change.revoke-sessions` | `false` | `true`: trocar a senha revoga as outras sessões do usuário. |

A política de composição é aplicada pelo `UserService` (criação e edição de usuário) e pela troca de senha.
Gravar usuário direto no repositório não passa por ela.

---

## 6. Limite de tentativas e IP de origem

Contagem em memória, por instância: num cluster de N instâncias o limite efetivo é N vezes o configurado.

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.rate-limit.enabled` | `true` | Liga a contagem nos fluxos de credencial: login, verificação de MFA e reset de senha. |
| `archbase.security.rate-limit.max-attempts` | `10` | Tentativas falhas toleradas na janela, por chave. |
| `archbase.security.rate-limit.window-seconds` | `900` | Janela de contagem. |
| `archbase.security.rate-limit.block-seconds` | `900` | Duração do bloqueio. |
| `archbase.security.rate-limit.discovery.max-attempts` | `200` | Limite próprio do `GET /api/v1/auth/tenants`, chamado pela tela a cada digitação do e-mail. |
| `archbase.security.rate-limit.discovery.block-seconds` | `300` | Bloqueio desse endpoint. |
| `archbase.security.client-ip.trust-forwarded-for` | `false` | Usa `X-Forwarded-For` como origem na chave por IP do `/auth/tenants`. Ligue **só** atrás de proxy que sobrescreve o cabeçalho; sem ele, o atacante troca de identidade a cada requisição e bloqueia terceiros forjando o endereço deles. |

---

## 7. Tokens

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.jwt.strict-token-use` | `false` | Recusa token sem o claim `token_use`, que distingue access de refresh. Com `false`, um token legado sem o claim é aceito em qualquer uso. Só ligue depois de passado um `refresh-expiration` desde o deploy da versão que emite o claim; com sessões antigas ativas, a subida falha. |
| `archbase.security.jwt.accept-token-query-param` | `true` | Aceita a credencial em `?token=`. Token em URL vai para log de acesso, histórico e `Referer`. Downloads e SSE costumam depender disso. |
| `archbase.security.api-token.hash-enabled` | `true` | Grava só o hash SHA-256 do token de API. `false` grava também o valor em claro. |
| `archbase.security.api-token.hash-migration-enabled` | `true` | Na subida, calcula o hash dos tokens antigos que não o têm. |
| `archbase.security.api-token.purge-plaintext` | `false` | **Irreversível**: apaga o valor em claro dos tokens. A subida falha se ainda houver token sem hash ou se `hash-enabled=false`. Ligue num segundo deploy. |
| `archbase.security.token.cleanup.enabled` | `false` | Liga o job que marca como expirados os tokens de acesso vencidos e apaga os antigos. Exige `@EnableScheduling` na aplicação. |
| `archbase.security.token.cleanup.cron` | `0 0 1 * * ?` | Quando o job roda. |
| `archbase.security.token.cleanup.retention-days` | `30` | Tokens expirados há mais que isso são apagados. |

---

## 8. MFA e cifragem

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.crypto.key` | vazio | Chave AES-GCM do segredo TOTP em repouso. Base64 de 16, 24 ou 32 bytes (`openssl rand -base64 32`); outro valor é usado como bytes crus. Vazia, cifrar o segredo falha e ativar MFA não funciona. |
| `archbase.security.mfa.issuer` | `Archbase` | Nome do emissor exibido no aplicativo autenticador. |

---

## 9. Auditoria

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.audit.enabled` | `false` | Liga a trilha: eventos em `SEGURANCA_EVENTO`, histórico Envers nas tabelas `_AUD`, `GET /api/v1/security/audit/events` e a purga. Desligada, o Envers fica inativo e nenhuma tabela `_AUD` é exigida. Crie as tabelas antes de ligar (`deployment/sql/auditoria-seguranca-*.sql`). |
| `archbase.security.audit.retention-months` | `12` | Retenção da trilha. `0` desliga a purga. |
| `archbase.security.audit.retention-cron` | `0 30 3 * * *` | Quando a purga roda. |
| `archbase.security.audit.tenant-scoped` | `false` | Restringe a leitura da trilha ao tenant de quem consulta. Sem ele, o administrador de um tenant lê os eventos de todos. |

---

## 10. Core de autorização

Ver [`MODELO_CORE_AUTORIZACAO.md`](../../MODELO_CORE_AUTORIZACAO.md). Os padrões reproduzem o comportamento
anterior ao core.

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.scan-packages` | vazio | Pacotes varridos na subida atrás de `@HasPermission`/`@ArchbaseResource`, separados por vírgula. Cada capacidade encontrada vira recurso + ação no catálogo, gravados no tenant `archbase.app.tenant.default.id`. Vazio: nada é sincronizado (WARN no log). |
| `archbase.security.sync.mode` | `apply` | `apply` grava o catálogo; `report` só registra no log o que faria; `refresh` grava e **reescreve** descrição, rótulo e categoria das capacidades existentes a partir do código, descartando o que o admin ajustou — use uma vez e volte para `apply`. O nível mínimo nunca é reescrito. |
| `archbase.security.permission.require-active` | `false` | `true`: concessões sobre ação ou recurso inativo deixam de valer, como a tela já mostrava. Leia o relatório de concessões a ações inativas antes de ligar. |
| `archbase.security.access-level.enabled` | `false` | Liga o portão LEVEL: o `minimumLevel` da capacidade passa a ser um piso. Não concede nada. |
| `archbase.security.access-level.default` | `READER` | Nível de quem não tem perfil, ou tem perfil sem nível. Valores: `NONE`, `READER`, `OPERATOR`, `SUPERVISOR`, `TENANT_ADMIN`. Valor inválido impede a subida. |
| `archbase.security.diagnostics.enabled` | `false` | Expõe `/api/v1/security/diagnostics/*` (panorama, efetivo de um usuário, simulação). Exige `isAdministrator` mesmo ligado, independentemente de `admin-endpoints.policy`. |

---

## 11. Esquema de segurança

Ver [`deployment/esquema-de-seguranca.md`](../../../deployment/esquema-de-seguranca.md).

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.schema.mode` | `apply` | `apply`: compara o mapeamento das entidades de segurança com o banco e cria o que falta, só com comandos aditivos. `report`: escreve no log o DDL que falta, sem executar. `off`: não olha o banco. |
| `archbase.security.schema.fail-on-error` | `false` | `true`: um erro nessa rotina (ex.: usuário sem permissão de DDL) impede a subida. Útil em homologação. |

---

## 12. Validação de pré-requisitos

| Propriedade | Padrão | O que faz |
|---|---|---|
| `archbase.security.hardening.validation` | `fail` | Na subida, confere se cada proteção ligada pode funcionar e não sobe se não puder: `company.validation=enforce` sem resolver, `require-role.no-resolver-policy=deny` sem resolver, `admin-endpoints.policy=permission` sem os recursos cadastrados, `strict-token-use` com sessões antigas, `purge-plaintext` com tokens sem hash, `access-level.default` inválido, endpoint da aplicação no caminho do logout. `warn` só registra; `off` desliga. Rebaixe apenas para diagnóstico. |

---

## 13. Variáveis de ambiente

```bash
export JWT_SECRET="$(openssl rand -base64 48)"          # gere uma vez e guarde no cofre de segredos
export ARCHBASE_CRYPTO_KEY="$(openssl rand -base64 32)" # se usar MFA; trocar a chave invalida os segredos já cifrados
```

```properties
archbase.security.jwt.secret-key=${JWT_SECRET}
archbase.security.crypto.key=${ARCHBASE_CRYPTO_KEY}
```

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - ARCHBASE_CRYPTO_KEY=${ARCHBASE_CRYPTO_KEY}
```

---

## 14. Checklist de produção

Coerente com a [referência rápida](../../../deployment/security-hardening.md#referência-rápida-para-produção);
os itens marcados com ⚠ exigem preparo, descrito lá.

**Segredos e tokens**
- [ ] `jwt.secret-key` vem de variável de ambiente, é Base64 padrão e decodifica para ≥ 32 bytes
- [ ] `token-expiration` curto (≤ 4 h) e `refresh-expiration` bem maior (7–30 dias)
- [ ] `crypto.key` definida, se MFA for usado
- [ ] `jwt.accept-token-query-param=false` ⚠ (downloads/SSE)
- [ ] `jwt.strict-token-use=true` ⚠ (depois de um `refresh-expiration` desde o deploy)
- [ ] `api-token.hash-enabled=true`; `api-token.purge-plaintext=true` num segundo deploy ⚠ (irreversível)
- [ ] `token.cleanup.enabled=true`, com `@EnableScheduling`

**Superfície pública**
- [ ] `cors.allowed-origins` com origens explícitas, nunca `*` com `allow-credentials=true`
- [ ] `public-paths.actuator=false`, com `/actuator/health` na `whitelist` se necessário
- [ ] `public-paths.registration=false`, `public-paths.swagger=false`, `public-paths.legacy-app-routes=false`
- [ ] `public-paths.static-files` revisto — com `true`, `/api/files/**` é público

**Autorização**
- [ ] `admin-endpoints.policy=admin-only` ⚠ (incompatível com o auto-registro de telas do archbase-react)
- [ ] `require-role.no-resolver-policy=deny` ⚠ (bean `ArchbaseRoleResolver`)
- [ ] `permission.require-active=true` ⚠
- [ ] `diagnostics.enabled=false` fora da janela de migração
- [ ] `hardening.validation=fail`

**Tenant e empresa**
- [ ] `app.tenant.fail-on-missing=true` ⚠ e `app.tenant.accept-query-param=false`
- [ ] `company.validation=enforce` ⚠ (bean `ArchbaseCompanyAccessResolver`; `warn` antes)

**Credenciais**
- [ ] `prevent-user-enumeration=true` ⚠ (mensagem neutra no frontend)
- [ ] `password.*` conforme a política da empresa; `password-change.revoke-sessions=true`
- [ ] `rate-limit.enabled=true`; `client-ip.trust-forwarded-for=true` só atrás de proxy que sobrescreve o cabeçalho

**Auditoria**
- [ ] `audit.enabled=true` (tabelas criadas antes) e `audit.tenant-scoped=true`

**Fora do módulo**
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (nunca `update` em produção)
- [ ] `spring.jpa.show-sql=false` e log de segurança em INFO ou WARN

---

**Ver também**:
- [Anotações e Uso](annotations-and-usage.md) — `@HasPermission` e companhia
- [Exemplos de Código](code-examples.md) — configuração customizada de segurança
- [Guia de endurecimento](../../../deployment/security-hardening.md) — ordem para virar cada proteção
- [Troubleshooting](troubleshooting.md)

---

[← Voltar ao Índice](../README.md) | [Próximo: Anotações e Uso →](annotations-and-usage.md)
