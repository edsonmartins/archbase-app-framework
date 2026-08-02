# Unified authorization core — implementation plan

Status: **plano aprovado, implementação não iniciada**
Substitui as opções em aberto de [`PROPOSTA_MODELO_AUTORIZACAO.md`](PROPOSTA_MODELO_AUTORIZACAO.md),
que fica como registro do levantamento.

> **Convenção.** Código e schema do framework são em inglês. Documentação e mensagens ao
> desenvolvedor, em português. O que a proposta anterior chamava de *patamar* aparece aqui como
> `AccessLevel`.

---

## O princípio

> **As anotações param de decidir. Elas passam a declarar. Quem decide é um avaliador só.**

Hoje existem cinco `AuthorizationManager` independentes — `CustomAuthorizationManager`,
`ProfileAuthorizationManager`, `RoleAuthorizationManager`, `PersonaAuthorizationManager`,
`SecurityAdminAuthorizationManager` — cada um com seu pointcut, seu critério de falha e nenhuma
noção dos outros. O Spring compõe isso como um E implícito, por acidente da ordem dos
interceptadores. Ninguém projetou essa composição.

Pior: `ProfileAuthorizationManager:117` e `PersonaAuthorizationManager:120` já chamam
`securityService.hasPermission()` por dentro quando a anotação declara `resource`. Restrição e
concessão já estão misturadas, com regra diferente da do `CustomAuthorizationManager`.

E há **duas consultas de autorização distintas, com regras diferentes**:

| Caminho | Consulta | Filtra `active`? |
|---|---|---|
| Frontend — `GET /resource/permissions/{nome}` | `ResourcePersistenceAdapter:84` (QueryDSL) | **sim** |
| Backend — `@HasPermission` | `PermissionJpaRepository` (JPQL) | **não** |

As duas montam o conjunto usuário ∪ grupos ∪ perfil por conta própria, em linguagens diferentes.
Consequência prática no gestor-rq: as 1.279 permissões sobre ações inativas já são invisíveis para
a tela, mas seriam honradas pelo backend. **Ligar `@HasPermission` hoje concederia mais do que a UI
mostra.**

---

## Hierarquia de decisão: cinco portões, e só um concede

```
  subject                                                    requirement
     │                                                            │
     ▼                                                            ▼
  ┌────────────────────────────────────────────────────────────────┐
  │ 1  IDENTITY      conta ativa, principal resolvível              │ nega
  ├────────────────────────────────────────────────────────────────┤
  │ 2  SCOPE         tenant · company · project batem               │ nega
  ├────────────────────────────────────────────────────────────────┤
  │ 3  RESTRICTION   @RequireRole @RequireProfile @RequirePersona    │ nega
  ├────────────────────────────────────────────────────────────────┤
  │ 4  LEVEL         nível do sujeito ≥ mínimo da capacidade         │ nega
  ├────────────────────────────────────────────────────────────────┤
  │ 5  GRANT         catálogo: perfil ∪ grupos ∪ direto              │ CONCEDE
  └────────────────────────────────────────────────────────────────┘
```

**A regra que organiza tudo: os portões 1 a 4 só sabem negar. Só o portão 5 concede.**

Restrição e catálogo nunca competem, porque fazem coisas de natureza diferente — uma tranca, a
outra abre. Uma tranca fechada não é contornável por concessão nenhuma. É isso que responde ao
risco de alguém atribuir uma capacidade a quem não devia.

Cada portão devolve **motivo**, não booleano. O mesmo objeto alimenta o log, o corpo do 403, a tela
de diagnóstico e a simulação.

### Onde o administrador entra

Hoje `ArchbaseSecurityService:26-28` retorna `true` antes de qualquer coisa, sem log.

No core, `isAdministrator` vira **duas propriedades do sujeito**: concessão universal no portão 5 e
`AccessLevel` máximo no portão 4. Continua passando em tudo que passava, mas sujeito a escopo
(portão 2) e a restrição explícita (portão 3) — um admin do tenant A deixa de alcançar o tenant B
por esquecimento de contexto.

**Ressalva de compatibilidade:** `@RequireProfile` e `@RequirePersona` têm hoje `allowSystemAdmin()`
com default `true`, e liberam o administrador **antes** de checar o perfil. Sob "portões só negam",
o admin passaria a ser barrado por um `@RequireProfile("X")` que ele não tem. Para não quebrar,
`allowSystemAdmin` continua sendo honrado como isenção declarada **daquela** restrição — não como
desvio global.

---

## Hierarquia de sujeito: união, com negação explícita

O acúmulo **não muda**. `collectSecurityIds` faz perfil ∪ grupos ∪ direto, e a produção do
gestor-rq confirma que os três níveis são usados de verdade (1.088 / 1.013 / 128 concessões).
Trocar união por precedência quebraria instalações existentes e não resolveria nada que a negação
não resolva melhor.

| | hoje | core |
|---|---|---|
| Conceder | perfil, grupo, usuário | igual |
| Combinar | união (OR) | igual |
| Negar | não existe | `PermissionEntity.effect = DENY` |
| Desempate | — | **DENY vence, em qualquer nível** |

Uma regra, uma frase, explicável sem diagrama: *o perfil libera para o time inteiro, e a negação no
usuário tira daquela pessoa.* É o caso que hoje obriga a criar um grupo paralelo só para excluir
alguém.

**Interação com escopo:** um `DENY` só vence dentro do escopo em que foi declarado. `DENY` com
`tenantId = A` não afeta o tenant B. Um `DENY` sem escopo vence em todos — mesma semântica que o
`GRANT` já tem em `allowAllTenantsAndCompaniesAndProjects()`.

---

## AccessLevel: dado, não código

O mínimo precisa ser editável no admin, como as ações são hoje. Duas colunas:

- **`SEGURANCA_ACAO.MINIMUM_LEVEL`** — nulo herda do recurso, que herda da classe de verbo, que cai
  em `READER`. A anotação `@HasPermission(minimumLevel = SUPERVISOR)` **semeia** o valor no primeiro
  registro; depois o admin manda, igual já acontece com a descrição da ação.
- **`SEGURANCA_PERFIL.ACCESS_LEVEL`** — um perfil por usuário, logo um nível por usuário. Encaixa
  sem tabela nova.

A escala é ordinal e curta. O core precisa do `compareTo`, não dos nomes:

```java
public enum AccessLevel { READER, OPERATOR, SUPERVISOR, TENANT_ADMIN }
```

### O que é fixo e o que é vocabulário do cliente

| Camada | Quem manda | Muda por cliente? |
|---|---|---|
| **Capability** `tms.ordemservico:aprovar_custo` | o código | **não** — é contrato de API |
| **AccessLevel** — a escala ordinal | o framework | ordem não, **rótulos sim** |
| **Profile, group, persona** | o cliente | **sim, inteiramente** |
| **Quem recebe o quê** | o cliente, no admin | **sim** |

O núcleo invariante é minúsculo: *capability + nível ordinal*. Todo o resto é vocabulário, e
vocabulário é do cliente. Os rótulos exibidos saem de `SEGURANCA_PERFIL`, não de constante no
código.

---

## A API do core

```java
public interface ArchbaseAccessEvaluator {
    AccessDecision decide(AccessSubject subject, AccessRequirement requirement);
}
```

A decisão de projeto que mais importa: **`AccessSubject` é construível a partir de um `userId`, não
só de um `Authentication`.**

É isso que torna a simulação gratuita — simular o acesso de outra pessoa é chamar o mesmo avaliador
com outro sujeito. Sem código paralelo, sem risco de a simulação divergir da decisão real, que é o
defeito clássico desse tipo de ferramenta.

```java
public record AccessDecision(
    boolean allowed,
    Gate deniedAt,            // null quando permitido
    String reasonCode,        // LEVEL_TOO_LOW, EXPLICIT_DENY, NO_GRANT, OUT_OF_SCOPE, ...
    String message,           // legível, em português, para o dev e para o 403
    String grantedBy,         // id do profile/group/user que concedeu
    String grantedByName,
    List<GateOutcome> chain   // a cadeia inteira, para diagnóstico
) {}
```

`AccessSubject` é carregado por **uma consulta com fetch join** de grupos e perfil. Montá-lo a
partir de entidades lazy fora de transação é `LazyInitializationException` — o mesmo defeito que já
apareceu no logout durante a auditoria.

---

## Diagnóstico na view

Três endpoints, todos alimentados pelo mesmo avaliador.

### Simulação — "esta pessoa consegue fazer isto?"

```
POST /api/v1/security/simulate
{ "userId": "...", "resource": "tms.ordemservico", "action": "aprovar_custo" }

→ AccessDecision completo, com a cadeia dos cinco portões
```

Avalia sem executar nada. Responde não só *se* pode, mas **em que portão parou e por quê** — que é
o que hoje obriga a abrir grupo por grupo.

### Efetivo — "o que esta pessoa pode?"

```
GET /api/v1/security/users/{id}/effective
→ lista achatada: capability, origem, nível exigido, situação
```

Aceita `?requireActive=true` para responder *"o que ela ainda poderia depois de ligarmos o filtro"*
— é o relatório que precede a virada da flag.

### Panorama

```
GET /api/v1/security/overview     contadores e saúde
GET /api/v1/security/coverage     endpoints anotados × catálogo × órfãos
```

**Estes endpoints revelam a estrutura de acesso e precisam ser protegidos como tal**: `AccessLevel`
mínimo `TENANT_ADMIN`, sob o recurso `security.diagnostics`, e desligados por padrão. Simulação é
ferramenta de diagnóstico, não de reconhecimento. A resposta nunca inclui dado pessoal além do
identificador consultado.

---

## Fases

| # | O que entra | Muda comportamento? |
|---|---|---|
| **0** | Testes de caracterização do comportamento atual | não |
| **A** | `AccessSubject`, `AccessRequirement`, `AccessDecision`, `ArchbaseAccessEvaluator`; `hasPermission` delega; correção do cast do principal | **não** |
| **B** | Os quatro managers de anotação viram adaptadores; a regra das trancas vai para os `RestrictionEvaluator` | **não** |
| **C** | Endpoints de diagnóstico + simulação | **não** — só leitura |
| **D** | `EFFECT` (GRANT/DENY), `MINIMUM_LEVEL`, `ACCESS_LEVEL` + DDL no `R__` | só quando preenchidos |
| **E** | `minimumLevel` na anotação, `@Target(TYPE)` com herança de `resource` | **não** |
| **F** | Varredura: modo `report`, execução por tenant | corrige desativação silenciosa |
| **G** | Flags + pré-validação na subida | — |

### Fase 0 vem primeiro, e não é formalidade

O refactor troca o motor de decisão de um sistema em produção. `ArchbaseSecurityServiceTest` já
cobre parte do comportamento; falta caracterizar o que **não** está coberto e o que só existe nos
outros managers. Nenhuma linha da fase A entra antes de a fase 0 passar verde no comportamento
atual.

### O que ficou fora da fase B, e por quê

**`SecurityAdminAuthorizationManager` continua como está.** Ele não avalia uma tranca declarada por
método: aplica uma *política* global (`permit` / `admin-only` / `permission`) sobre um marcador.
Modelar política dentro do requisito acrescentaria conceito ao core em troca de pouco — e o ramo que
de fato decide por capacidade, `permission`, já passa pelo core através de `hasPermission`.

**`ResourcePersistenceAdapter` foi para a fase C.** Ele não decide, *lista* — devolve as capacidades
de um usuário sobre um recurso. Unificá-lo exige a operação "listar capacidades de um sujeito", que
é exatamente o que o endpoint de efetivo constrói. Feito na fase B, seria escrito duas vezes.

### A correção que precede o piloto

`ArchbaseSecurityService:25` faz `(UserEntity) authentication.getPrincipal()` sem verificação. Em
aplicação com `UserDetailsService` próprio, ou com principal anônimo, isso é `ClassCastException` →
`CustomAuthorizationManager:63` captura → **nega e loga stack trace**. O primeiro `@HasPermission`
de um piloto pode negar todo mundo por um motivo que não tem nada a ver com permissão.

---

## Schema

As colunas entram em `src/main/resources/db/migration/archbase/R__archbase_security_schema.sql`,
que já existe e é entregue pelo framework.

```sql
alter table SEGURANCA_ACAO       add column if not exists MINIMUM_LEVEL varchar(30);
alter table SEGURANCA_PERFIL     add column if not exists ACCESS_LEVEL  varchar(30);
alter table SEGURANCA_PERMISSAO  add column if not exists EFFECT        varchar(10) default 'GRANT';
```

Todas nulas ou com default — nenhum `not null`, nenhum backfill obrigatório.

**Duas ressalvas herdadas, que o plano não resolve e não piora:**
- o arquivo é **específico de PostgreSQL** (`add column if not exists`); MySQL e Oracle não sobem
  com ele, e isso já é assim hoje;
- nomes de coluna nessas tabelas são mistos (`NOME`, `BO_ATIVA` em português; `COMPANY_ID`,
  `PROJECT_ID` em inglês). As novas seguem o inglês, alinhadas às mais recentes.

---

## Compatibilidade

Todo default reproduz o comportamento atual:

- `effect` nasce `GRANT` — as concessões existentes não mudam de sentido
- `MINIMUM_LEVEL` nulo é ausência de piso — portão 4 passa direto
- portão sem restrição declarada passa direto
- `allowSystemAdmin` continua isentando o administrador das restrições que o declaram
- administrador com nível máximo e concessão universal dá exatamente o resultado de hoje
- o filtro de `active` no caminho do backend continua **desligado**

Um sistema existente sobe idêntico. Só muda quando alguém preenche um mínimo, cria uma negação ou
liga uma flag — mesma disciplina das flags de endurecimento, com pré-validação na subida.

### Flags

```properties
archbase.security.permission.require-active=false      # alinha o backend ao frontend
archbase.security.permission.deny-effect-enabled=true   # honra effect=DENY
archbase.security.access-level.enabled=false            # liga o portão 4
archbase.security.access-level.applies-to-administrator=false
archbase.security.sync.mode=apply                       # apply | report
archbase.security.diagnostics.enabled=false             # expõe overview/effective/simulate
```

`require-active` é o mais sensível: ligá-lo **tira acesso** de quem depende de permissão sobre ação
inativa — 57% das concessões no gestor-rq. Rodar `effective?requireActive=true` antes, nome a nome.
Note que ligá-lo **aproxima** o backend do que a tela já faz.

---

# Revisão do plano

Revisão crítica feita contra o código, depois de escrito. Sete achados; todos já refletidos acima.

### 1. Unificar a lógica não unifica a decisão — e o plano confundia as duas

**Achado.** Manter cinco interceptadores delegando ao mesmo avaliador unifica as *regras*, mas não
a *decisão*: o avaliador roda cinco vezes, cada vez com um requisito parcial, e a "cadeia única de
motivos" não existe. Um método com `@HasPermission` e `@RequireProfile` produz duas cadeias
desconexas — exatamente o que o core deveria eliminar.

**Por que não unifiquei o interceptador.** Um único interceptador com pointcut sobre as quatro
anotações mudaria ordem de execução e semântica de falha de aplicações em produção. Trocar isso no
mesmo passo que troca o motor é risco em cima de risco.

**Resolução.** Fases A–B unificam a lógica; a unificação do interceptador vira **fase H**, atrás de
flag, depois de o core estar em produção. Até lá, as decisões parciais são compostas em um traço
por requisição, que é o que os endpoints de diagnóstico leem. Registrado como limitação conhecida,
não como pendência esquecida.

### 2. "Portões só negam" quebraria `allowSystemAdmin`

**Achado.** `@RequireProfile` e `@RequirePersona` têm `allowSystemAdmin()` default `true` e liberam
o admin **antes** de checar o perfil. Isso é uma restrição que *concede*. Sob a regra nova, o admin
passaria a ser barrado por `@RequireProfile("X")` — mudança de comportamento silenciosa em
aplicações existentes.

**Resolução.** `allowSystemAdmin` é honrado como isenção declarada daquela restrição específica.
A regra "portões só negam" vale para o modelo; a isenção é explícita, local e visível na cadeia de
motivos. Incorporado ao texto acima.

### 3. O plano ignorava a segunda consulta de autorização

**Achado.** `ResourcePersistenceAdapter:84` reimplementa usuário ∪ grupos ∪ perfil em QueryDSL, com
regra diferente da do `PermissionJpaRepository` — filtra `action.active`. É o caminho que o
frontend usa. Um core que unifica só o lado do `@HasPermission` deixa metade do problema de pé, e é
a metade que hoje está em produção.

**Resolução.** `ResourcePersistenceAdapter` entra na fase B. E a conclusão da auditoria fica mais
precisa: ligar `@HasPermission` hoje concede **mais** que a UI, não menos.

### 4. `AccessSubject` a partir de `userId` é armadilha de lazy loading

**Achado.** `UserEntity.getGroups()` e `getProfile()` são lazy. Construir o sujeito fora de
transação — que é exatamente o caso da simulação, chamada de um controller — dá
`LazyInitializationException`. É o mesmo defeito que apareceu no logout durante a auditoria; repeti
o padrão.

**Resolução.** Consulta dedicada com fetch join, e o sujeito é um record imutável desacoplado das
entidades. Anotado na seção da API.

### 5. Faltava fase 0

**Achado.** O plano trocava o motor de autorização de um sistema em produção sem rede. Havia sete
fases e nenhuma de caracterização — depois de três rodadas de code review nesta mesma auditoria
terem encontrado regressões que só apareceram por teste.

**Resolução.** Fase 0 explícita, bloqueante.

### 6. `DENY` sem regra de escopo é ambíguo

**Achado.** "DENY vence em qualquer nível" não dizia o que acontece com um `DENY` de `tenantId = A`
diante de um `GRANT` global. Sem definir, cada implementação escolhe — e o resultado é uma negação
que vaza entre tenants, ou uma que não funciona.

**Resolução.** `DENY` vence dentro do escopo em que foi declarado; sem escopo, vence em todos.
Mesma semântica que o `GRANT` já tem.

### 7. Um alarme falso que eu ia carregar para o plano

**Achado.** `getPermissionsForUser` devolve, para administrador, o vocabulário fixo
`READ/CREATE/UPDATE/DELETE` sob `resourceName = "*"` — que não casa com as ações do gestor-rq
(`aprovar_custo`). Eu tratei isso como defeito a corrigir.

**Verificação.** O método **não tem chamador em produção** — o frontend passa por
`findLoggedUserResourcePermissions`, no adapter. É código morto.

**Resolução.** Sai do escopo do core. Vira nota de limpeza: remover ou marcar como
`@Deprecated`, sem urgência.

---

## O que a revisão não cobre

- **Desempenho.** O core acrescenta uma consulta de sujeito por decisão. Onde hoje há uma consulta
  por `@HasPermission`, passa a haver duas. Cache de sujeito por requisição resolve, mas não está
  desenhado, e não estimei o impacto sob carga.
- **Multi-tenant na varredura.** A fase F diz "execução por tenant" sem definir de onde sai a lista
  de tenants — o framework não tem esse registro hoje. Precisa de desenho próprio antes de virar
  tarefa.
- **Rótulos de `AccessLevel` por tenant.** Ficou dito que os rótulos são configuráveis, mas não
  onde moram. Provavelmente `SEGURANCA_PERFIL`, o que os amarra a perfil e não a tenant. Em aberto.
