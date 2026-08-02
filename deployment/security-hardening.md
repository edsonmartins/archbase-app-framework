# Endurecimento de segurança — migração para 3.0.11

A auditoria de segurança do framework corrigiu escalações de privilégio, bypasses de autenticação e
vazamentos de credencial. **Atualizar a dependência não muda o comportamento da sua aplicação**: as
correções que alterariam o resultado de requisições que hoje passam ficaram atrás de propriedades
com default compatível.

Isso é intencional — permite atualizar sem risco e virar as chaves depois, uma a uma, com o
frontend acompanhando. Mas significa que **subir a versão nova não realiza o ganho de segurança**.
Este documento é a lista do que ligar.

---

## Já ativo ao atualizar (sem configuração)

Estas correções valem imediatamente porque não quebram uso legítimo:

- **Bypass de MFA fechado** — o token de desafio não é mais aceito em `/auth/refresh-token`.
- **Refresh token revogável** — logout, troca e reset de senha agora encerram a renovação. O
  refresh rotaciona a cada uso.
- **Anotação de autorização na classe** — `@RequireProfile`/`@RequireRole`/`@RequirePersona` no
  nível da classe passam a ser aplicadas; antes liberavam tudo silenciosamente.
- **Não-admin não cria nem promove administrador**, e não edita conta de administrador.
- **Rate limiting** em login, verificação de MFA e reset (10 falhas / 15 min por padrão).
- **Token de API novo nasce como hash** — o valor em claro não é mais gravado.
- **Credenciais fora dos logs** — token de API e headers `Cookie`/`API-key` mascarados.
- **Tenant não vaza mais entre requisições** quando um endpoint lança exceção.
- **`companyId` e usuário autenticado propagam para tarefas `@Async`**.

### Mudanças de comportamento que continuam ativas por padrão

Estas não têm chave porque desligá-las seria reabrir a vulnerabilidade. Confira se alguma atinge
um fluxo seu **antes** de atualizar:

| Mudança | Quem pode sentir |
|---|---|
| `@RequireProfile`/`@RequireRole`/`@RequirePersona` **na classe** passam a ser aplicadas (antes liberavam tudo) | Quem anotou classe e "funcionava" — os usuários passam a ser avaliados de verdade. Levante os pontos com: `grep -rn "@Require" --include=*.java src/main` |
| Não-administrador não cria/promove administrador nem edita conta de administrador | Provisionamento automatizado rodando como conta de serviço não-admin. Escape: `archbase.security.admin-guard.enabled=false` |
| App com `UserDetailsService` próprio (principal ≠ `UserEntity`) é bloqueado na gestão de usuários | Escape: `archbase.security.admin-guard.allow-unverifiable-principal=true` |
| Access token deixa de ser aceito em `/auth/refresh-token` | Cliente que mandava o access token no refresh. Tokens antigos seguem aceitos |
| `CryptoUtil` passou a UTF-8 | Só onde o valor foi cifrado em JVM com `file.encoding` ≠ UTF-8: recifre com `ArchbaseColumnReencryptor` |

### Três mudanças de comportamento visíveis

0. **O logout passou a existir.** `ArchbaseLogoutService` nunca era registrado na cadeia de
   segurança: o `/logout` padrão do Spring rodava só o `SecurityContextLogoutHandler`, que com
   sessão STATELESS não faz nada — o cliente recebia sucesso e o token seguia válido até expirar.
   Agora há `POST /api/v1/auth/logout`, que revoga access e refresh da sessão e responde 200.
   Sobrescreva `getLogoutUrl()` se o caminho conflitar com uma rota existente.

1. **Token de API não pode mais ser recuperado depois de criado.** Se alguma tela lista tokens
   mostrando o valor, ela passará a mostrar vazio para os criados a partir de agora. Exiba o valor
   uma única vez, na resposta da criação.
2. **`@Async` agora enxerga o usuário autenticado.** Isso corrige a auditoria (`createdByUser`
   vazio) e checagens de permissão, mas é contexto que antes não existia ali. Desligue com
   `archbase.multitenancy.async.propagate-security-context=false` se alguma tarefa dependia da
   ausência.

### Chaves que revertem comportamento, se você precisar

Nenhuma delas precisa de preparo — são interruptores diretos:

```properties
# Troca de senha autenticada NÃO desloga (padrão). Ligue quando o frontend estiver pronto
# para reautenticar; é a postura correta quando a troca é por suspeita de comprometimento.
archbase.security.password-change.revoke-sessions=false

# Token de API volta a ser gravado em claro, para telas que listam o valor.
# Custo: um dump do banco entrega acesso a toda integração.
archbase.security.api-token.hash-enabled=true

# Endpoint de logout do framework.
archbase.security.logout.enabled=true
archbase.security.logout.url=/api/v1/auth/logout
```

---

## Validação automática de pré-requisitos

Várias proteções dependem de algo que o framework não controla: um bean que você precisa registrar,
linhas que precisam existir no banco, uma migração que precisa ter rodado. Ligar a chave sem esse
preparo não dá erro na hora — dá comportamento errado depois, no meio de uma requisição.

Por isso, **a aplicação valida na subida e não sobe se uma proteção habilitada não puder funcionar**,
dizendo o que fazer:

```
═══ Configuração de segurança inconsistente ═══

Proteções foram habilitadas sem os pré-requisitos atendidos. ...

  1) archbase.security.api-token.purge-plaintext=true, mas 12 token(s) de API ainda
     não têm hash calculado. Apagar o valor em claro agora deixaria essas
     integrações sem meio de autenticar, de forma irreversível.
     O que fazer: suba uma vez SEM purge-plaintext (a migração de hash roda na
     inicialização), confirme no log a linha "hash calculado", e só então ligue.
```

Para diagnosticar sem impedir a subida (nunca como estado permanente):

```properties
archbase.security.hardening.validation=warn    # ou: off
```

O validador também registra em WARN as proteções que estão **inertes** — por exemplo
`admin-endpoints.policy=permit` —, para que o estado real apareça a cada deploy.

### O que é verificado

| Chave ligada | Pré-requisito verificado | Se faltar |
|---|---|---|
| `api-token.purge-plaintext=true` | Nenhum token de API sem hash; e `hash-enabled=true` | **Não sobe** |
| `require-role.no-resolver-policy=deny` | Existe ao menos um bean `ArchbaseRoleResolver` | **Não sobe** |
| `admin-endpoints.policy=permission` | `Resource` + `Action` MANAGE cadastrados para os 7 recursos | **Não sobe** |
| `jwt.strict-token-use=true` | Nenhuma sessão ativa emitida antes da atualização | **Não sobe** |
| `logout.enabled=true` | Nenhum endpoint da aplicação mapeado na URL do logout | **Não sobe** |

---

## Passo 1 — ligar assim que possível

Sem dependência de prazo ou de mudança no frontend.

```properties
# Fecha a escalação de privilégio nos endpoints administrativos de segurança.
# Exige que o usuário seja administrador. Use 'permission' se preferir controlar por
# Resource/Action cadastrados — nesse caso cadastre-os antes de virar.
archbase.security.admin-endpoints.policy=admin-only

# /actuator/** deixa de ser público. Confira antes se algum health check externo depende dele;
# se depender, exponha só /actuator/health via archbase.security.whitelist.
archbase.security.public-paths.actuator=false

# Desliga o auto-cadastro anônimo em POST /api/v1/auth/register.
archbase.security.public-paths.registration=false

# Remove da whitelist rotas de uma aplicação específica que vinham no default do framework.
# Se a sua aplicação usa /api/v1/assistente-virtual/webhook ou
# /api/v1/licenca/verificar-tenants/**, mova-as para archbase.security.whitelist.
archbase.security.public-paths.legacy-app-routes=false
```

Depois de subir, confira no log da inicialização a linha `Superfície anônima da API` — ela lista
exatamente o que ficou acessível sem autenticação.

---

## Passo 2 — exige ação prévia

```properties
# Só depois de registrar um bean ArchbaseRoleResolver na aplicação.
# Sem ele, @RequireRole não valida nada e esta chave apenas nega tudo.
archbase.security.require-role.no-resolver-policy=deny

# Só depois de ajustar o frontend para "se o e-mail estiver cadastrado, você receberá as
# instruções" — o endpoint deixa de responder 400 "usuário não encontrado".
archbase.security.prevent-user-enumeration=true

# Só depois de confirmar que nenhum cliente manda credencial em ?token= (downloads e SSE
# costumam ser os casos).
archbase.security.jwt.accept-token-query-param=false

# Política de força de senha. Não invalida senhas já gravadas; passa a recusar trocas fracas.
archbase.security.password.min-length=12
archbase.security.password.require-digit=true
archbase.security.password.require-uppercase=true
archbase.security.password.require-lowercase=true
archbase.security.password.require-special=true

# Só em aplicação onde todo acesso legítimo sempre carrega tenant. Recusa a operação em vez de
# recair no tenant padrão — que é como dados de um tenant acabam respondendo por outro.
archbase.app.tenant.fail-on-missing=true
archbase.app.tenant.accept-query-param=false
```

---

## Passo 3 — depende de prazo, e um deles é irreversível

```properties
# Recusa token JWT sem o claim token_use, ou seja, emitido antes desta versão.
# Ligue apenas depois que o maior archbase.security.jwt.refresh-expiration configurado tiver
# passado desde o deploy — antes disso, derruba sessões em curso.
archbase.security.jwt.strict-token-use=true
```

```properties
# IRREVERSÍVEL. Apaga o valor em claro dos tokens de API já existentes no banco.
# Depois disto, voltar para uma versão anterior do framework deixa TODA integração por token de
# API sem meio de autenticar, e não há como recuperar os valores.
#
# Ligue num SEGUNDO deploy, depois de validar que a versão nova está estável em produção.
# Enquanto não ligar, o log avisa a cada subida quantos tokens seguem em claro.
archbase.security.api-token.purge-plaintext=true
```

---

## Migração de schema

O arquivo `db/migration/archbase/R__archbase_security_schema.sql` acompanha o framework e é
idempotente (`if not exists` em tudo). Aplica-se sozinho em quem usa `archbase-starter-flyway`.

> **⚠ O script é específico de PostgreSQL** e o starter o injeta nas locations de **todo** projeto
> por padrão (`archbase.flyway.include-archbase-locations=true`). Numa aplicação MySQL ou Oracle o
> Flyway falha na sintaxe e **a aplicação não sobe**. O mesmo acontece se o Flyway estiver
> habilitado mas as tabelas `seguranca*` vierem de `ddl-auto`, porque o Flyway roda antes do
> Hibernate. Nesses casos:
>
> ```properties
> archbase.flyway.include-archbase-locations=false
> ```
>
> e aplique o DDL equivalente pelo versionamento do próprio projeto.

Colunas adicionadas nesta versão:

| Tabela | Coluna | Para quê |
|---|---|---|
| `seguranca_token_acesso` | `tp_uso_token` | Separa access de refresh na mesma tabela |
| `seguranca_token_api` | `token_hash` | SHA-256 do token; passa a ser a chave de busca |
| `seguranca_token_api` | `token` | Passa a ser nulável |

Quem **não** usa Flyway precisa garantir que as colunas existam **antes** de subir a versão nova:

- com `spring.jpa.hibernate.ddl-auto=update`, o Hibernate as cria sozinho;
- com `validate` ou `none`, aplique o DDL do arquivo acima manualmente. **Sem a coluna
  `token_hash`, toda autenticação por token de API para** — a consulta passa a referenciá-la
  sempre, e não há degradação graciosa aqui.

O cálculo dos hashes das linhas existentes roda na subida (`ArchbaseApiTokenHashMigrator`).
Enquanto ele não rodar, os tokens antigos continuam autenticando pelo valor em claro.
