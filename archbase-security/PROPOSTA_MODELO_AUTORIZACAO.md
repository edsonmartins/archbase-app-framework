# Proposta: um único modelo de autorização

Documento de decisão, escrito a partir da auditoria do framework e do uso real no Gestor-RQ.
O objetivo é eliminar a tentativa-e-erro: ao terminar a leitura, deve estar claro **onde declarar,
onde conceder, a quem conceder e como o conflito se resolve**.

---

## O diagnóstico em uma frase

> Não existem dois sistemas concorrentes. Existe **um** modelo de permissão completo — o granular
> — e **três anotações que não falam com ele**.

O que parece "RBAC misturado com granular" é RBAC pela metade: os papéis existem (perfil, grupo),
o modelo de permissão existe (recurso → ação → permissão), mas o código nunca faz a ponte. As
anotações `@RequireProfile`, `@RequireRole` e `@RequirePersona` decidem por conta própria, sem
consultar o catálogo — e `@RequireRole` sequer decide, porque depende de um resolver que ninguém
implementa.

**RBAC bem-feito é exatamente o sistema granular**, com papéis funcionando como pacotes de
permissão. Não são coisas diferentes que precisam conviver. É uma coisa só, hoje partida ao meio.

---

## A hierarquia: cinco perguntas, nesta ordem

A dúvida "qual a hierarquia disso tudo" tem resposta, e ela não está escrita em lugar nenhum hoje.
Toda requisição atravessa cinco níveis, cada um respondendo uma pergunta:

```mermaid
flowchart TD
    N1["1 · AUTENTICAÇÃO<br/><b>Quem é você?</b><br/>token válido e vivo em banco"]
    N2["2 · TENANT<br/><b>De qual organização?</b><br/>claim do token, confrontado com o header"]
    N3["3 · IDENTIDADE EFETIVA<br/><b>O que você é?</b><br/>usuário + seus grupos + seu perfil"]
    N4["4 · PERMISSÃO<br/><b>O que você pode?</b><br/>par (recurso, ação)"]
    N5["5 · ESCOPO<br/><b>Sobre qual recorte?</b><br/>tenant / company / project da permissão"]
    OK([Acesso permitido])

    N1 --> N2 --> N3 --> N4 --> N5 --> OK

    ADM["<b>isAdministrator</b><br/>atalho: pula 3, 4 e 5"] -.->|"bypass total"| OK

    style ADM fill:#ef6c00,color:#fff
    style OK fill:#2e7d32,color:#fff
```

**Nível 3 é o que confunde.** Perfil, grupo e usuário **não são hierarquia** — são três formas de
carregar permissão, e o resultado é a **união** delas. Não existe "o perfil manda mais que o
grupo". Quem tem permissão por qualquer um dos três, tem.

Duas propriedades do modelo atual que precisam ser ditas em voz alta, porque a equipe assume o
contrário:

- **Não existe negação.** Uma permissão só concede. Não há como dizer "este grupo NÃO pode",
  porque o mais permissivo sempre vence.
- **Não existe herança de recurso.** `tms.manutencao` e `tms.manutencao.os` são recursos sem
  relação alguma. Permissão num não alcança o outro.

---

## As três regras para parar de adivinhar

### Regra 1 — O código declara, o admin concede

| | Código | Admin |
|---|---|---|
| Declara **que existe** (recurso, ação) | ✅ `@HasPermission` | ✅ tela de recursos |
| Decide **quem tem** | ❌ nunca | ✅ tela de permissões |

O desenvolvedor **nunca** escreve nomes de perfil, grupo ou papel no código. Ele escreve apenas
*qual permissão o endpoint exige*:

```java
@PostMapping
@HasPermission(resource = "tms.ordemservico", action = "aprovar",
               description = "Aprovar ordem de serviço")
public OrdemServico aprovar(@PathVariable String id) { ... }
```

Quem pode aprovar é decisão de negócio, muda sem deploy, e vive no admin. É essa separação que
hoje não existe — e é por isso que `SYSTEM_ADMIN`, `GESTOR` e `OPERADOR` acabaram cravados em 15
métodos.

### Regra 2 — Três níveis de atribuição, com propósito distinto

| Nível | Cardinalidade | Para que serve | Exemplo |
|---|---|---|---|
| **Perfil** | 1 por usuário | o **papel principal** da pessoa | `ATENDIMENTO`, `GESTOR_FROTA` |
| **Grupo** | N por usuário | **recortes transversais** ao papel | `TIME-SAC`, `CATALOGO-FOTOS` |
| **Usuário** | — | **exceção individual**, sempre temporária | acesso pontual a um relatório |

A pergunta a fazer ao conceder: *isso vale para todo mundo com este papel?* Se sim, perfil. *Vale
para um recorte que cruza papéis?* Grupo. *É exceção para uma pessoa?* Usuário — e anote por quê.

No Gestor-RQ isso já acontece na prática (1.088 permissões em perfis, 1.013 em grupos, 128 em
usuários). O que falta é a regra estar escrita, para as próximas concessões não serem por
intuição.

### Regra 3 — Toda tela protegida tem endpoint protegido

Recursos `VIEW` controlam o que aparece; recursos `API` controlam o que executa. **Um sem o outro
é falso.** Esconder o botão sem proteger o endpoint é segurança de fachada — que é a situação
atual do Gestor-RQ.

Convenção proposta: para cada recurso `VIEW` que esconde uma ação, existe um recurso `API` com o
mesmo nome de ação, protegendo o endpoint correspondente.

---

## O que muda no framework

Três mudanças, em ordem de custo. Nenhuma quebra o que existe.

### Fase 1 — Tornar visível o que hoje é invisível *(sem mudança de comportamento)*

O maior gerador de tentativa-e-erro é a **ausência de retorno**. Nada avisa quando o contrato
código↔admin não fecha. Proposta: um relatório na subida e um endpoint de diagnóstico, listando:

- endpoints **sem** `@HasPermission` (superfície desprotegida);
- `@HasPermission` cujo par (recurso, ação) **não existe** no catálogo do tenant;
- permissões concedidas a ações **inativas** — 57% do total no Gestor-RQ hoje;
- recursos/ações no catálogo **sem nenhum endpoint** correspondente.

Isso sozinho resolve boa parte da dúvida: o time passa a ver o descasamento em vez de descobri-lo
por reclamação de usuário.

### Fase 2 — Fazer `active` valer *(mudança de comportamento, atrás de chave)*

Hoje `bo_ativa` não participa da decisão: desativar uma ação no admin não corta acesso. Ou a
consulta passa a filtrar `active`, ou o campo sai da tela. Recomendo filtrar, atrás de
`archbase.security.permission.respect-active=true`, com o relatório da Fase 1 mostrando o impacto
antes de ligar — no Gestor-RQ, 1.279 permissões mudariam de efeito.

### Fase 3 — Herança por nome de recurso *(a "hierarquia" que falta)*

Adotar nome hierárquico com ponto (já é a convenção de fato: `tms.mecanico`, `ticket.kanban`) e
fazer a consulta reconhecer o prefixo:

```
permissão em  tms.manutencao.*     alcança  tms.manutencao.os, tms.manutencao.plano
permissão em  tms.manutencao.os    alcança  apenas ele
```

Resolve o crescimento do catálogo — hoje cada tela nova exige conceder tudo de novo, uma a uma —
sem tocar no schema: muda só o `WHERE` da consulta de permissão. É o que dá sensação de hierarquia
sem inventar um segundo modelo.

### Não recomendado agora — negação explícita

Permitiria "o perfil concede, mas este grupo nega". É poderoso e é a porta de entrada para
regras que ninguém consegue depurar depois ("por que fulano não vê a tela?"). Só vale se aparecer
um caso real que a união não resolva. Até lá, a ausência de negação é uma simplificação, não uma
limitação.

---

## O que aposentar

| Anotação | Situação | Destino |
|---|---|---|
| `@HasPermission` | completa, integrada ao catálogo | **única recomendada** |
| `@RequireProfile` | funciona, mas crava papel no código | desencorajar; migrar para permissão |
| `@RequireRole` | não valida nada sem resolver | `@Deprecated` |
| `@RequirePersona` | ignora `context`/`contextData`; tem nomes de negócio de um cliente dentro do framework | `@Deprecated` |

Depreciar não é remover: o código existente continua compilando e se comportando igual. Muda a
recomendação, e o Javadoc passa a apontar o caminho.

---

## Caminho para o Gestor-RQ

Ordem sugerida, do mais barato ao mais estrutural:

1. **Medir.** Rodar o diagnóstico da Fase 1 e ver o tamanho real do descasamento.
2. **Proteger o que importa.** Anotar com `@HasPermission` os endpoints sensíveis, reaproveitando
   os nomes de recurso que **já estão no catálogo** — os 8 recursos `API` já existem, com 132
   permissões concedidas, só aguardando alguém apontar para eles.
3. **Reconciliar os documentos.** `RBAC_ROLES_PERMISSIONS.md` e `RBAC_SEEDS_GESTOR_RQ.md`
   descrevem sistemas que não existem. Corrigir ou marcar como proposta.
4. **Escolher um vocabulário.** Hoje são quatro (`SYSTEM_ADMIN`/`GESTOR`/`OPERADOR` no código,
   dois conjuntos nos documentos, `TIME-SAC`/`MASTER-TOTAL` no banco).
5. **Só então** avaliar as Fases 2 e 3.

---

## Decisões que dependem de vocês

Este documento propõe; não decide. Quatro pontos precisam da sua palavra:

1. **`@HasPermission` como única forma de autorizar** — aceita depreciar as outras três?
2. **Herança por nome de recurso** (Fase 3) — resolve o problema que vocês sentem, ou o catálogo
   plano é suficiente?
3. **`active` passar a valer** (Fase 2) — sabendo que 57% das permissões do Gestor-RQ mudariam de
   efeito.
4. **Negação explícita** — existe hoje algum caso real que a união não resolve?
