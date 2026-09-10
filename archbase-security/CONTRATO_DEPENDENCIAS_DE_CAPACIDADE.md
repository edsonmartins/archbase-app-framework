# Dependências entre capacidades — contrato

Status: **implementado** (passos 1 a 6)
Camada 3 do plano de melhoria da tela de permissões. A Camada 1 (classificação `VIEW`/`API` e
identificador técnico na tela) está entregue; a Camada 2 (rótulo, descrição e categoria como campos
distintos) é independente desta e pode vir antes ou depois.

> **Convenção.** Código e schema em inglês, documentação em português — a mesma de
> [`MODELO_CORE_AUTORIZACAO.md`](MODELO_CORE_AUTORIZACAO.md).

---

## O problema

Duas queixas distintas de quem administra o sistema:

1. **"Não sei do que esta permissão depende."** Conceder `aprovar_custo` sem `view` produz um
   usuário que passa na autorização do endpoint e não consegue chegar até ele pela interface. O
   catálogo não registra essa relação em lugar nenhum, então ninguém a descobre antes de o usuário
   reclamar.
2. **"Tela e endpoint aparecem misturados."** A Camada 1 os separou em seções, o que ajuda mas não
   resolve: continuam sendo duas listas paralelas, e nada diz que a tela *Cockpit* é justamente
   quem chama `tms.ordemservico:aprovar_custo`.

**As duas são a mesma aresta.** "A capacidade X precisa da capacidade Y" descreve tanto a
dependência funcional entre dois endpoints quanto o botão de uma tela que aciona um endpoint. Um
mecanismo só resolve as duas, e é isso que este contrato define.

---

## O modelo

Uma **aresta dirigida entre capacidades**:

```
  origem                                    alvo
  (recurso:ação)   ──── requires ────▶   (recurso:ação)
```

Origem e alvo são sempre capacidades — nunca recursos. Uma aresta declara: *sem o alvo, a origem
não serve para nada na prática.*

### Por que não existe uma aresta separada para "tela usa endpoint"

A versão anterior desta proposta tinha dois conceitos: `requires` (entre capacidades) e `uses` (do
recurso `VIEW` para uma capacidade `API`). Foram colapsados em um só, porque a tela não usa um
endpoint — **um botão da tela** usa. `uses` no nível do recurso perderia justamente a informação
que interessa: qual gesto da interface depende de qual permissão.

O agrupamento visual continua possível: as capacidades de API que uma tela consome são a união dos
alvos `API` das arestas que partem das ações daquela tela. É **derivado**, não armazenado — e um
dado derivado nunca diverge da sua fonte.

---

## Declaração

### No backend, na própria anotação

```java
@GetMapping("/{id}/aprovar-custo")
@HasPermission(action = "aprovar_custo",
               description = "Aprovar o custo da OS",
               requires = {"view", "tms.cliente:view"})
public ResponseEntity<?> aprovarCusto(...) { ... }
```

| Forma | Significa |
|---|---|
| `"view"` | a ação `view` **do mesmo recurso** da capacidade que declara |
| `"tms.cliente:view"` | a ação `view` do recurso `tms.cliente` |

O separador é `:`. Nome de recurso que contenha `:` torna a forma qualificada ambígua: a aresta
**não é catalogada** e a varredura registra `ERROR` apontando o método. Não derruba a subida —
recusar a aplicação inteira por causa de um nome mal escolhido troca um defeito de catálogo por
uma indisponibilidade.

`requires` tem default `{}`. Quem não declara nada não muda de comportamento.

### No frontend, no registro da ação

```ts
registerAction('aprovar', 'Aprovar custo', {
  requires: ['tms.ordemservico:aprovar_custo'],
});
```

O terceiro parâmetro é opcional, e `SimpleActionDto` ganha `requires?: string[]`. Cliente que não
o envia continua funcionando exatamente como hoje — e não declara aresta nenhuma, que é diferente
de declarar uma lista vazia (ver **Poda**).

A forma curta vale aqui também, e resolve contra o recurso da própria tela.

---

## Persistência

Tabela nova, `SEGURANCA_ACAO_DEPENDENCIA`:

| Coluna | Tipo | Nota |
|---|---|---|
| `ID_DEPENDENCIA` | `varchar(40)` PK | |
| `TENANT_ID` | `varchar` | de `TenantPersistenceEntityBase`, como `SEGURANCA_ACAO` |
| `ID_ACAO` | `varchar(40)` FK → `SEGURANCA_ACAO` | a origem |
| `CAPACIDADE_REQUERIDA` | `varchar(200)` | o alvo, em texto: `recurso:acao` |
| `ID_ACAO_REQUERIDA` | `varchar(40)` FK, nulável | o alvo resolvido, quando existe |
| `DECLARADA_POR` | `varchar(20)` | `SCAN` ou `REGISTER` — ver **Poda** |
| `CD_DEPENDENCIA`, `VERSAO`, datas, usuários | | herdadas da base |

Restrição de unicidade em `(TENANT_ID, ID_ACAO, CAPACIDADE_REQUERIDA)`.

### Por que o alvo é texto, e o id é apenas um espelho

A varredura não tem ordem garantida entre recursos: quando a aresta é gravada, a ação alvo pode
ainda não ter sido criada. Uma FK obrigatória exigiria duas passadas, ou ordenação artificial do
catálogo.

Pior: o alvo **pode nunca existir**. Um erro de digitação, um módulo que não foi implantado naquele
ambiente, um recurso que ainda não foi registrado porque nenhum administrador abriu aquela tela.
Nesses casos a aresta precisa **permanecer visível como não resolvida**, e não sumir. Aresta que
some em silêncio é a mesma classe de defeito que zerou as ações de 56 dos 100 recursos do
gestor-rq, quando o registro de tela ainda desativava o que não vinha no payload.

`ID_ACAO_REQUERIDA` é preenchido quando a resolução encontra o alvo, e **re-resolvido a cada
sincronização** — é assim que uma aresta pendente passa a valer no dia em que o alvo é catalogado,
sem que ninguém precise reprocessar nada.

### Sem trilha de auditoria

`ActionEntity` e `ResourceEntity` são `@Audited`; esta não é. A trilha existe para responder *quem
mudou o acesso e quando* — e aresta não é mudada por gente, é mudada por deploy. A fonte da verdade
é o histórico do repositório, e duplicá-la em `_AUD` só aumentaria o volume da tabela de revisões
sem responder nenhuma pergunta nova.

### Uma linha no inicializador de schema

`ArchbaseSecuritySchemaInitializer.ENTIDADES` precisa da entidade nova. A lista é explícita de
propósito — ela delimita o que o DDL automático pode tocar, e uma varredura silenciosa arrastaria
para dentro dela qualquer entidade nova do pacote.

---

## Quem é dono da aresta

**O código. O administrador não edita dependências.**

É a decisão que torna esta camada barata, e vale explicar por quê. Descrição e `minimumLevel` são
semeados pelo código no primeiro registro e a partir dali pertencem ao admin — daí a sincronização
nunca poder sobrescrevê-los, e daí o problema conhecido de "as ações já coletadas precisariam ser
atualizadas ao menos uma vez".

Aresta não tem esse dilema: ela é um fato sobre o código, não uma configuração. A sincronização a
reconcilia integralmente a cada subida, e **nenhum mecanismo de refresh especial é necessário**.

Se um dia for preciso que o admin acrescente uma aresta que o código não declara, ela entra como
`DECLARADA_POR = ADMIN` e fica fora da poda — mas isso está **fora deste contrato**.

---

## Poda

Reconciliação diferente por origem, porque o que cada lado sabe é diferente.

### `SCAN` — as arestas declaradas em `@HasPermission`

A varredura tem a **lista completa** do que o código declara. Reconcilia integralmente: cria o que
falta, remove o que não é mais declarado.

Com uma trava herdada da sincronização de ações: se houver qualquer método cujo recurso não pôde
ser resolvido, a lista está incompleta e **a remoção não roda**. Podar a partir de lista incompleta
apagaria arestas válidas.

### `REGISTER` — as arestas declaradas pelas telas

Quem registra conhece uma parte. Um recurso pode ser declarado por mais de uma tela, e cada uma
envia só as ações que usa — foi o que tornou o registro de tela aditivo em primeiro lugar.

A poda é **escopada à ação**: para cada ação **presente no payload**, as arestas `REGISTER` daquela
ação são substituídas pelas declaradas. Ação ausente do payload não é tocada.

Duas consequências que precisam ser ditas:

- **Ação declarada por duas telas com `requires` diferentes alterna.** Cada abertura sobrescreve a
  anterior. Quando a substituição de fato muda o conjunto, a operação registra `WARN` com os dois
  conjuntos — visível no log em vez de silencioso. Duas telas que declaram a mesma capacidade com
  dependências diferentes estão discordando sobre o que aquela capacidade é, e isso é um defeito de
  quem declarou.
- **Só administrador registra.** Desde a correção do 403 em toda tela, `registerResource` é chamado
  apenas por administradores; os demais leem. As arestas de uma tela, portanto, só são atualizadas
  quando um administrador abre aquela tela. É o mesmo alcance que o catálogo de ações já tem hoje.

### Modo relatório

`archbase.security.sync.mode=report` passa a relatar também as arestas que criaria e removeria,
com a mesma regra de não escrever nada.

---

## Semântica: o que a aresta faz, e o que ela nunca faz

> **Dependência é informativa. Nunca é portão.**

O `ArchbaseAccessEvaluator` **não lê esta tabela**. Não há sexto portão, não há flag para ligar um.
Se `aprovar_custo` passasse a exigir `view` na decisão, toda instalação existente perderia acesso
na primeira subida após a atualização — e perderia em silêncio, porque ninguém declarou aquelas
arestas pensando em autorização.

O que ela faz:

| Momento | Comportamento |
|---|---|
| **Ao conceder** | a tela oferece conceder junto as dependências que a entidade ainda não alcança, com as caixas marcadas por padrão e desmarcáveis |
| **Ao revogar** | avisa quantas concessões daquela entidade passam a não ter efeito prático, e quais |
| **No diagnóstico** | cada capacidade efetiva carrega as dependências que o sujeito **não** alcança |

### Por que não uma `Situation` nova

`EffectiveCapability.Situation` tem `EFFECTIVE`, `DENIED` e `INERT`, e cada valor corresponde ao
que a **decisão** faz. Uma capacidade com dependência faltando continua `EFFECTIVE`, porque é isso
que o backend faz com ela: deixa passar.

Introduzir `MISSING_DEPENDENCY` faria o diagnóstico afirmar algo que a decisão não sustenta — a
divergência exata entre tela e decisão que o core existe para eliminar. A informação entra em campo
separado:

```java
public record EffectiveCapability(
        // ... campos atuais ...
        List<String> unmetDependencies) { }
```

---

## Leitura: direta e transitiva

O armazenamento é de **profundidade 1** — cada linha é uma aresta direta. A pergunta original,
porém, era sobre dependência "direta ou indireta", e a leitura responde as duas.

### Fecho transitivo, calculado na leitura

Percurso em largura com conjunto de visitados, **teto de profundidade 10**. Ciclos são permitidos
no armazenamento (`A → B`, `B → A` é uma declaração possível e às vezes correta) e o conjunto de
visitados os encerra sem travar. Quando o teto é atingido, a resposta traz `truncated: true` — em
vez de mentir por omissão.

### Onde cada uma aparece

| Consumidor | Endpoint | O que recebe |
|---|---|---|
| Catálogo da tela de concessão | `GET /api/v1/resource/permissions` (já existe) | `PermissionWithTypesDto.requires?: string[]` — apenas as **diretas**, em texto |
| Detalhe de uma capacidade | `GET /api/v1/resource/permissions/dependencies/{actionId}` (novo) | fecho **transitivo**, com origem de cada nível e marcação do que não foi resolvido |
| Relatório de efetivo | `/api/v1/security/diagnostics/users/{id}/effective` | `unmetDependencies` por capacidade |

As diretas viajam junto do catálogo porque a tela já baixa esse catálogo inteiro e precisa delas
para sugerir no momento da concessão. O fecho transitivo não: mandar o fecho de 600 ações numa
resposta só custaria mais do que a informação vale, e ele só é olhado quando alguém abre o detalhe
de uma linha.

---

## Compatibilidade

Nada aqui é obrigatório, e nenhuma combinação de versões quebra.

| Situação | Resultado |
|---|---|
| Código sem `requires` | nenhuma aresta; comportamento idêntico ao atual |
| Frontend antigo + backend novo | payload sem `requires`; nenhuma aresta declarada pelas telas |
| Frontend novo + backend antigo | campo ignorado pelo servidor; a tela não recebe `requires` e não sugere nada |
| Banco sem a tabela | criada pelo esquema aditivo na subida, como as demais |
| `sync.mode=report` | relata as arestas, não escreve |

---

## Validações na subida

| Situação | Comportamento |
|---|---|
| Nome de recurso contendo `:` na forma qualificada | `ERROR` apontando o método; a aresta não é catalogada; a subida continua |
| `A requires A` | ignorada, com `WARN` — não é erro, é redundância |
| Alvo inexistente | aresta gravada como **não resolvida**; sem log de erro, porque é um estado legítimo |
| Forma curta em capacidade cujo recurso não resolveu | já coberto: sem recurso não há capacidade, e o método inteiro é reportado |

---

## O aninhamento que não aconteceu

O passo 6 previa **aninhar as capacidades de API sob a tela que as consome**. Foi entregue de outra
forma, e a razão é técnica.

A árvore do Mantine identifica cada nó pelo `value`, e é pela seleção que a tela concede e remove.
Uma capacidade de API usada por **duas** telas apareceria duas vezes, com o mesmo `value` — valores
repetidos quebram a seleção. Contornar exigiria valores compostos (`tela:capacidade`) e um mapeamento
de volta em cada ponto do fluxo de concessão e remoção, que é a parte mais delicada desta tela.

Pior que o custo é o significado: as duas cópias seriam a **mesma** linha de permissão. Marcá-las
independentemente seria a interface afirmando algo falso.

O que a pergunta "quem usa este endpoint?" pedia foi entregue como **etiqueta na própria linha** —
`usada por N`, com os nomes das telas no tooltip. A informação é a mesma, calculada no cliente a
partir do `requires` que o catálogo já traz, sem requisição a mais e sem mexer na seleção.

Se o aninhamento visual for mesmo desejado, ele cabe numa terceira forma: um modo de exibição
alternativo, com a árvore em modo somente-leitura e a concessão continuando pela lista plana. Fica
registrado como possibilidade, não como pendência.

## Fora deste contrato

Ditos explicitamente para que não sejam presumidos:

- **Aplicação da dependência na decisão.** Não há, e não haverá por flag.
- **Edição de arestas pela tela de administração.** As arestas são do código.
- **Dependência condicional** ("só precisa de X quando o escopo é Y").
- **Trilha de auditoria das arestas.**
- **Sugestão automática de arestas** a partir de tráfego observado.

---

## Ordem de implementação

1. ~~Entidade, repositório, linha em `ENTIDADES` do inicializador de schema.~~ **Feito.**
2. ~~`requires` em `@HasPermission` e reconciliação `SCAN` na varredura, com suporte a
   `sync.mode=report`.~~ **Feito.**
3. ~~`requires` em `SimpleActionDto` e reconciliação `REGISTER` escopada à ação.~~ **Feito.**
4. ~~`requires` no catálogo (`findAllResourcesPermissions`) e o endpoint do fecho transitivo.~~
   **Feito.**
5. ~~`unmetDependencies` no `EffectiveCapability` e no relatório de efetivo.~~ **Feito.**
6. ~~Tela: sugerir ao conceder, avisar ao revogar, aninhar as capacidades de API sob a tela que as
   consome.~~ **Feito, com uma mudança** — ver abaixo.

> **Sobre o passo 1 e quem usa `ddl-auto=validate`.** O inicializador de schema do framework é um
> `SmartInitializingSingleton` e roda **depois** da validação do Hibernate — não chega a tempo de
> criar a tabela para quem valida. Por isso o `create table if not exists` também entrou no
> `R__archbase_security_schema.sql`, que o Flyway aplica antes. Projeto com `validate` e **sem**
> Flyway precisa criar `seguranca_acao_dependencia` pelo próprio versionamento.

Os passos 1–3 já entregam valor sem tocar em nenhuma tela: o catálogo passa a registrar a relação,
e o relatório de efetivo passa a poder explicá-la.
