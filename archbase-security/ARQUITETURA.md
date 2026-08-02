# Arquitetura do archbase-security

Referência de como o módulo funciona e como aplicá-lo. Escrito a partir do código, não da
documentação anterior — onde os dois divergem, este documento aponta a divergência
explicitamente na seção [Documentação × código](#documentação--código).

---

## Por que gera confusão

O módulo tem **quatro sistemas de autorização independentes** que coexistem, e nada no código
diz qual usar quando. Uma requisição pode ser barrada em dois lugares diferentes, por motivos
diferentes, com respostas diferentes:

| Camada | Onde decide | Granularidade | Configurada por |
|---|---|---|---|
| **1. Cadeia HTTP** | `SecurityFilterChain` | Caminho de URL | `archbase.security.whitelist` + `configureAuthorizationRules` |
| **2. Anotações de método** | 5 `AuthorizationManager` | Método Java | `@HasPermission`, `@RequireProfile`, `@RequireRole`, `@RequirePersona` |
| **3. Bypass de administrador** | `ArchbaseSecurityService` | Global | Campo `isAdministrator` do usuário |
| **4. Isolamento de tenant** | Filtro JWT + `@TenantId` | Linha do banco | Claim do token vs. header |

Some disso três fontes concretas de erro, todas verificadas no código:

1. **`hasRole()` e `hasAuthority()` do Spring nunca funcionam.** `UserEntity.getAuthorities()`
   devolve lista vazia. Um `@PreAuthorize("hasRole('ADMIN')")` **nega sempre**, silenciosamente.
   Autorização aqui é só pelas anotações do Archbase.
2. **`@HasPermission` exige `description`**, que não tem valor padrão. Todo exemplo da
   documentação antiga omite — e **não compila**.
3. **Oito propriedades são obrigatórias** e sem elas a aplicação não sobe, com erro de
   placeholder que não indica segurança. Ver [Configuração mínima](#configuração-mínima).

---

## Caminho de uma requisição

```mermaid
flowchart TD
    REQ([Requisição HTTP]) --> F1[ArchbaseJwtAuthenticationFilter]

    F1 --> CRED{Que credencial<br/>foi apresentada?}
    CRED -->|Bearer JWT| JWT[Valida assinatura<br/>+ linha viva em<br/>SEGURANCA_TOKEN_ACESSO<br/>+ token_use = access]
    CRED -->|UUID<br/>Bearer ou cru| API[Valida em<br/>SEGURANCA_TOKEN_API<br/>hash, ativo, não expirado]
    CRED -->|?token= na URL| QP[Mesma validação<br/>desligável por<br/>accept-token-query-param]
    CRED -->|nenhuma| ANON[Segue anônimo]

    JWT --> AUTH[SecurityContext preenchido<br/>principal = UserEntity]
    API --> AUTH
    QP --> AUTH

    AUTH --> TEN{Tenant do token<br/>= X-TENANT-ID?}
    ANON --> TEN
    TEN -->|diverge| F403([403])
    TEN -->|confere ou ausente| CHAIN[Regras da cadeia HTTP]

    CHAIN --> WL{Caminho está<br/>na whitelist?}
    WL -->|sim| MVC[DispatcherServlet]
    WL -->|não| AUTHED{Autenticado?}
    AUTHED -->|não| F401([401])
    AUTHED -->|sim| MVC

    MVC --> ANOT{Método tem<br/>anotação Archbase?}
    ANOT -->|não| EXEC([Executa])
    ANOT -->|sim| MGR[AuthorizationManager<br/>correspondente]
    MGR -->|nega| F403B([403])
    MGR -->|permite| EXEC

    style F403 fill:#c62828,color:#fff
    style F403B fill:#c62828,color:#fff
    style F401 fill:#ef6c00,color:#fff
    style EXEC fill:#2e7d32,color:#fff
```

**O ponto que mais confunde:** a whitelist (camada 1) libera o *caminho*, mas **não desliga** as
anotações de método (camada 2). Um endpoint na whitelist com `@HasPermission` continua exigindo
permissão — e como não há usuário autenticado, nega. As duas camadas se somam, nunca se
substituem.

---

## Modelo de permissão

Usuário, grupo e perfil são **a mesma tabela** (`SEGURANCA`), separados por discriminador. É isso
que permite conceder permissão a qualquer um dos três de forma uniforme.

```mermaid
erDiagram
    SEGURANCA {
        string ID_SEGURANCA PK
        string TP_SEGURANCA "USUARIO | SEGURANCA_GRUPO | SEGURANCA_PERFIL"
        string TENANT_ID
    }
    SEGURANCA_PERMISSAO {
        string ID_SEGURANCA FK "quem recebe"
        string ID_ACAO FK "o que pode"
        string tenantId "escopo opcional"
        string companyId "escopo opcional"
        string projectId "escopo opcional"
    }
    SEGURANCA_ACAO {
        string ID_ACAO PK
        string NOME "CREATE, READ, MANAGE..."
        string ID_RECURSO FK
    }
    SEGURANCA_RECURSO {
        string ID_RECURSO PK
        string NOME "USER, PRODUCT..."
    }
    SEGURANCA_GRUPO_USUARIO {
        string ID_USUARIO FK
        string ID_GRUPO FK
    }

    SEGURANCA ||--o{ SEGURANCA_PERMISSAO : "recebe"
    SEGURANCA_ACAO ||--o{ SEGURANCA_PERMISSAO : "concede"
    SEGURANCA_RECURSO ||--o{ SEGURANCA_ACAO : "expõe"
    SEGURANCA ||--o{ SEGURANCA_GRUPO_USUARIO : "usuário ↔ grupo"
    SEGURANCA ||--o| SEGURANCA : "usuário → perfil"
```

Ao avaliar `@HasPermission`, o framework monta o conjunto de identidades do usuário —
**id do próprio usuário + ids dos seus grupos + id do seu perfil** — e procura uma permissão
para qualquer uma delas. Herança é por união, não por hierarquia.

Duas regras que surpreendem:

- **Administrador ignora tudo.** `isAdministrator = true` faz `hasPermission` devolver `true`
  sem consultar o banco. Não existe recurso que um administrador não alcance.
- **Escopo nulo é curinga.** Permissão com `tenantId` nulo vale para qualquer tenant; o mesmo
  para company e project. O isolamento real entre tenants vem do `@TenantId` do Hibernate, não
  desses campos — eles são estreitamento *dentro* do tenant.

---

## O catálogo: Resource e Action

Esta é a parte mais granular do módulo — e a que mais confunde, porque **`@HasPermission` não é
só uma checagem: é também uma declaração**. A anotação alimenta um catálogo no banco, e é desse
catálogo que as permissões são concedidas.

Existem **dois catálogos**, separados pelo campo `TIPO` do recurso, com ciclos de vida opostos:

```mermaid
flowchart TD
    subgraph API["Recursos TIPO = API — automático"]
        A1["@HasPermission<br/>resource, action, description"] --> A2[ArchbaseActionSynchronizationService]
        A2 -->|"@PostConstruct, na subida"| A3{scan-packages<br/>configurado?}
        A3 -->|não| A4([Nada acontece.<br/>Só um WARN no log])
        A3 -->|sim| A5[Varre os pacotes com Reflections]
        A5 --> A6[Cria Resource e Action<br/>que não existem]
        A5 --> A7[DESATIVA Action e Resource<br/>sem anotação correspondente]
    end

    subgraph VIEW["Recursos TIPO = VIEW — manual"]
        V1[POST /api/v1/resource/register] --> V2[Cria Resource + Actions<br/>do corpo da requisição]
        V2 --> V3([Para permissões de tela,<br/>botão, menu — não de endpoint])
    end

    A6 --> CAT[(Catálogo)]
    A7 --> CAT
    V2 --> CAT
    CAT --> G["POST /api/v1/resource/permissions<br/>concede Action a User, Group ou Profile"]
    G --> CHK[Avaliação de @HasPermission]

    style A4 fill:#c62828,color:#fff
    style A7 fill:#ef6c00,color:#fff
```

### Por que `description` é obrigatório

Não é burocracia: o texto vira a **descrição da Action** no catálogo, exibida na tela de
concessão de permissões. Como a Action é criada a partir da anotação, o framework exige a
descrição no momento em que ela é declarada. Por isso não tem valor padrão — e por isso todo
exemplo da documentação antiga, que omite o campo, não compila.

### As cinco armadilhas

**1. Sem `archbase.security.scan-packages`, nada é sincronizado.** A propriedade tem padrão
vazio; quando vazia, o serviço registra um `WARN` e retorna. O sintoma é desconcertante: os
métodos estão anotados, o catálogo está vazio, e `@HasPermission` **nega todo mundo** — porque
sem Action não existe permissão que possa apontar para ela.

```properties
archbase.security.scan-packages=com.suaempresa.seuapp
```

**2. A sincronização roda apenas no tenant padrão.** `Resource` e `Action` são entidades
`@TenantId`, e o `@PostConstruct` acontece fora de qualquer requisição — o resolvedor de tenant
cai no `archbase.app.tenant.default.id`. Numa aplicação multi-tenant, os demais tenants ficam
**sem catálogo API**, e `@HasPermission` nega para todos eles. Nesse cenário, popule o catálogo
por tenant (migration ou chamada a `/resource/register`) em vez de contar com a varredura.

**3. Desativar no admin não tira o acesso.** A consulta que decide a autorização casa
**apenas por nome de ação e nome de recurso**:

```sql
WHERE u.id IN :securityIds AND a.name = :actionName AND r.name = :resourceName
```

Não há filtro por `active` nem por `type`. Ou seja: uma Action marcada como inativa — pela
sincronização ou pelo admin — **continua concedendo acesso**. O campo `active` serve à
apresentação no admin, não à decisão. Quem desativar um recurso esperando cortar o acesso não
vai cortar.

**4. Renomear a anotação deixa a permissão órfã.** Trocar `action = "CREATE"` por
`action = "CRIAR"` cria uma Action nova, **sem nenhuma permissão concedida** — o método passa a
negar todo mundo. A Action antiga é desativada, mas suas permissões continuam no banco e, pelo
item anterior, continuariam valendo se algum método voltasse a usar aquele nome. Renomear
`resource` ou `action` é, na prática, revogar o acesso ao método e deixar lixo para trás.

**5. Nome colide entre os dois catálogos.** `ensureResourceExists` busca o recurso **só pelo
nome**, sem filtrar tipo: um recurso `VIEW` inativo criado pelo admin com o mesmo nome de um
usado em `@HasPermission` é reativado e convertido para `API`. E como a consulta de autorização
também ignora o tipo, uma permissão concedida sobre um recurso `VIEW` satisfaz um
`@HasPermission` de endpoint com o mesmo par de nomes. **Trate o espaço de nomes de recursos
como único**, independentemente do tipo.

### Concessão

Conceder é sempre pelo **id da Action**, nunca pelo nome:

```
GET  /api/v1/resource/permissions                     lista o catálogo com os ids
POST /api/v1/resource/permissions                     { actionId, securityId, type }
                                                      type = USER | GROUP | PROFILE
GET  /api/v1/resource/permissions/{resourceName}      o que o usuário logado pode neste recurso
```

Como `User`, `Group` e `Profile` são a mesma tabela, `securityId` aceita qualquer um dos três — é
o campo `type` que diz ao serviço onde procurar o id.

---

## Qual anotação usar

```mermaid
flowchart TD
    Q0{O acesso depende de quê?} 
    Q0 -->|Recurso + ação<br/>cadastrados no banco| HP["@HasPermission<br/>resource + action + description"]
    Q0 -->|Perfil do usuário<br/>no Archbase| RP["@RequireProfile"]
    Q0 -->|Papel do domínio<br/>da aplicação| RR["@RequireRole"]
    Q0 -->|Persona de negócio| RPE["@RequirePersona"]

    HP --> HPN["Catálogo alimentado na subida<br/>da API (precisa de scan-packages)<br/>ou pelo admin. Sem catálogo,<br/>ninguém passa"]
    RP --> RPN[Usa apenas o perfil ÚNICO<br/>do usuário. Sem perfil, nega]
    RR --> RRN[Exige um bean<br/>ArchbaseRoleResolver.<br/>Sem ele, não valida nada]
    RPE --> RPEN[Mapeia perfil → persona<br/>por nome. context e<br/>contextData são IGNORADOS]

    style RRN fill:#ef6c00,color:#fff
    style RPEN fill:#ef6c00,color:#fff
```

Recomendação prática, em ordem de preferência:

1. **`@HasPermission`** — é a única com modelo de dados completo por trás e escopo multi-tenant.
   Use como padrão. Antes, garanta que o catálogo está sendo alimentado — ver
   [O catálogo: Resource e Action](#o-catálogo-resource-e-action).
2. **`@RequireProfile`** — atalho útil quando a regra é mesmo "só o perfil X". Lembre que o
   usuário tem **um** perfil, não vários: `requireAll = true` com dois perfis nunca passa.
3. **`@RequireRole`** — só faz sentido se a aplicação registrar `ArchbaseRoleResolver`. Sem isso
   o comportamento é decidido por `archbase.security.require-role.no-resolver-policy`.
4. **`@RequirePersona`** — o mapeamento embutido é um `switch` sobre nomes de perfil
   (`PLATFORM_ADMIN`, `STORE_ADMIN`, `CUSTOMER`, `DRIVER`); fora desses, compara o nome da
   persona com o nome do perfil. Os parâmetros `context` e `contextData` **não são lidos**.

**Anotações combinadas somam (AND).** Cada anotação tem seu próprio interceptador; todos precisam
permitir. Não existe OR entre elas.

**Nível de classe funciona** em `@RequireProfile`, `@RequireRole` e `@RequirePersona` — a
anotação no método vence a da classe. **`@HasPermission` é `@Target(METHOD)`**: na classe, não
compila.

---

## Ciclo de vida das credenciais

```mermaid
sequenceDiagram
    participant C as Cliente
    participant A as /api/v1/auth
    participant DB as SEGURANCA_TOKEN_ACESSO

    C->>A: POST /authenticate (email, senha)
    A->>A: rate limit por e-mail normalizado
    alt usuário tem MFA
        A-->>C: mfa_required + challenge_token (5 min)
        C->>A: POST /mfa/verify (challenge + código)
    end
    A->>DB: grava access (token_use=access)
    A->>DB: grava refresh (token_use=refresh)
    A-->>C: access_token + refresh_token

    Note over C,DB: o desafio de MFA nunca vira credencial:<br/>não tem linha em banco e é recusado no refresh

    C->>A: POST /refresh-token (refresh)
    A->>DB: confere linha viva + token_use=refresh
    A->>DB: revoga o par anterior, grava novo
    A-->>C: novo par

    C->>A: POST /auth/logout (Bearer access)
    A->>DB: UPDATE em lote: revoga TODOS do usuário
    A-->>C: 200
```

O access token é **stateful**: a assinatura sozinha não basta, a linha precisa existir e estar
viva. É isso que torna logout e revogação efetivos — e é por isso que apagar a tabela desloga
todo mundo.

---

## Fiação: o que entra com qual dependência

```mermaid
flowchart LR
    APP[Sua aplicação] --> ST[archbase-starter]
    ST --> STC[archbase-starter-core]
    ST --> STS[archbase-starter-security]
    ST --> STM[archbase-starter-multitenancy]

    STC --> MVC["ArchbaseServerMvcConfiguration"]
    MVC -->|"@ConditionalOnProperty<br/>archbase.web.mvc.enabled"| SCAN["ArchbaseComponentScanConfiguration<br/>ComponentScan de br.com.archbase.security<br/>+ EntityScan + JpaRepositories"]
    STS --> CFG["ArchbaseSecurityApplicationConfig<br/>DefaultArchbaseSecurityConfiguration<br/>MethodSecurityConfig"]
    STM --> TEN["Interceptor de tenant<br/>+ TaskDecorator"]

    SCAN --> BEANS[Serviços, controllers,<br/>filtro JWT, managers]
    CFG --> BEANS

    style SCAN fill:#ef6c00,color:#fff
```

Dois detalhes que enganam, e ambos produzem o mesmo sintoma — a aplicação sobe, mas metade da
segurança não existe:

- **Quem varre os componentes é o `starter-core`, não o `starter-security`.** Usar só o
  `archbase-starter-security` registra as configurações e nenhum serviço. Na prática, use
  `archbase-starter`.
- **`archbase.web.mvc.enabled=false` desliga o scan inteiro.** A propriedade parece ser sobre
  MVC, mas a cadeia é
  `ArchbaseCoreAutoConfiguration → ArchbaseServerMvcConfiguration → ArchbaseComponentScanConfiguration`,
  e é essa última que registra os serviços de segurança, as entidades e os repositórios.

Para substituir a configuração de segurança, implemente `CustomSecurityConfiguration` e estenda
`BaseArchbaseSecurityConfiguration`. A `DefaultArchbaseSecurityConfiguration` é
`@ConditionalOnMissingBean(CustomSecurityConfiguration.class)` e sai de cena sozinha.

---

## Configuração mínima

Estas oito **não têm valor padrão**. Faltando qualquer uma, a aplicação não sobe:

```properties
archbase.security.jwt.secret-key=<Base64 de 32 bytes>
archbase.security.jwt.token-expiration=3600000
archbase.security.jwt.refresh-expiration=86400000
archbase.security.whitelist=
archbase.security.cors.allowed-origins=*
archbase.security.cors.allowed-methods=*
archbase.security.cors.allowed-headers=*
archbase.security.cors.allow-credentials=false
```

Além dessas, uma que **não impede a subida mas desliga silenciosamente** todo o catálogo de
permissões de endpoint — se você usa `@HasPermission`, ela é obrigatória na prática:

```properties
archbase.security.scan-packages=com.suaempresa.seuapp
```

`whitelist` pode ficar vazia, mas **precisa estar declarada**. As demais propriedades do módulo
têm padrão e estão listadas no `CLAUDE.md`; as de endurecimento de segurança, em
[deployment/security-hardening.md](../deployment/security-hardening.md).

---

## Documentação × código

Divergências verificadas linha a linha. Todos os exemplos abaixo estão em documentos que os
desenvolvedores usam hoje.

| Divergência | Onde | Realidade no código |
|---|---|---|
| `@HasPermission(resource=, action=)` sem `description` | `readme-security.md`, `README.md` | `description()` é obrigatório — **o exemplo não compila** |
| `archbase.security.jwt.secret` e `.expiration` | `CLAUDE.md` | Os nomes corretos são `secret-key` e `token-expiration` |
| `refresh-expiration` não aparece em nenhum doc | todos | É **obrigatória** |
| `whitelist` e as quatro de CORS não documentadas | todos | São **obrigatórias** |
| "`@RequireRole` e `@RequirePersona` são extensíveis via enrichers" | `readme-security.md` | Não há ligação entre enrichers e os managers. `@RequireRole` usa `ArchbaseRoleResolver` |
| `@RequirePersona(context=, contextData=)` | `readme-security.md` | Nenhum dos dois é lido na decisão |
| `@RequireRole(requirePlatformAdmin=true)` descrito como "admin E role" | `readme-security.md` | `allowSystemAdmin` (padrão `true`) libera o admin **antes** dessa checagem |
| Login social listado como funcionalidade | `readme-security.md` | Sem um `ArchbaseSocialTokenValidator`, responde 501 |
| `groupId com.archbase`, versão `1.0.0` | `archbase-starter-security/readme.md` | É `br.com.archbase`, versão 3.0.x |

---

## Fragilidades arquiteturais

Não são bugs — são decisões de desenho que custam caro na manutenção.

**Quatro sistemas de autorização sem critério de uso.** `@RequireProfile`, `@RequireRole` e
`@RequirePersona` resolvem variações do mesmo problema com semânticas diferentes e graus de
completude muito diferentes. `@RequirePersona` chega a ter um `switch` com nomes de negócio
(`STORE_ADMIN`, `DRIVER`) embutido no framework — regra de uma aplicação específica dentro do
código compartilhado. Consolidar em `@HasPermission` + um SPI para papéis reduziria a superfície
sem perder capacidade.

**Perfil é um só.** `UserEntity.profile` é `@ManyToOne` — um único perfil por usuário. Toda a API
de `@RequireProfile` fala em arrays e `requireAll`, sugerindo múltiplos. `requireAll = true` com
mais de um perfil é uma condição que nunca pode ser satisfeita.

**O bypass de administrador é absoluto e invisível.** Não há como marcar um recurso como
inalcançável por administrador, nem registro de que o bypass ocorreu.

**`getAuthorities()` vazio desliga o Spring Security padrão.** Qualquer expressão `hasRole`,
`hasAuthority` ou `@Secured` nega silenciosamente. Falha fechada, mas quem não souber vai perder
tempo. Vale documentar no lugar mais visível ou popular as authorities a partir do perfil.

**O campo `active` do catálogo não participa da autorização.** Desativar um recurso ou uma ação
pelo admin não corta acesso nenhum — a consulta casa só por nome. Ou a consulta passa a filtrar
`active`, ou o campo deveria sumir da tela para não sugerir um efeito que não tem. Hoje ele é uma
promessa quebrada na interface.

**O catálogo automático não é multi-tenant.** A varredura roda no `@PostConstruct`, fora de
requisição, e grava só no tenant padrão. Ou a sincronização passa a iterar os tenants conhecidos,
ou a documentação precisa dizer que aplicações multi-tenant devem popular o catálogo por outro
caminho. Hoje não diz nem uma coisa nem outra.

**Token de acesso stateful a cada requisição.** Toda chamada autenticada faz uma consulta em
`SEGURANCA_TOKEN_ACESSO`. É o que dá revogação imediata, mas é uma leitura por requisição — vale
saber ao dimensionar.
