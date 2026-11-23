[← Anterior: Entidades Core](core-entities.md) | [Voltar ao Índice](../README.md) | [Próximo: Gerenciamento de Tokens →](token-entities.md)

---

# Sistema de Permissões — Entidades

Colunas conferidas contra `persistence/*.java` na versão 3.5.0. As colunas herdadas da base
(`VERSAO`, `DH_CRIACAO`, `USUARIO_CRIOU`, `DH_ATUALIZACAO`, `ULTIMO_USUARIO_ALTEROU`, `TENANT_ID`)
estão em [Visão Geral](overview.md#colunas-herdadas). "Nulável" é o que o mapeamento declara.

## O modelo em uma frase

Uma linha de `SEGURANCA_PERMISSAO` liga **quem** (`ID_SEGURANCA`: usuário, grupo ou perfil) a
**uma capacidade** (`ID_ACAO`, que pertence a um recurso), opcionalmente estreitada a uma empresa e a
um projeto, com efeito `GRANT` ou `DENY`.

```
ResourceEntity (SEGURANCA_RECURSO)
   └── ActionEntity (SEGURANCA_ACAO) ──── ActionDependencyEntity (SEGURANCA_ACAO_DEPENDENCIA)
          └── PermissionEntity (SEGURANCA_PERMISSAO) ── SecurityEntity (usuário | grupo | perfil)
```

O tenant de cada linha é o `TENANT_ID` herdado (`@TenantId`): isolamento do Hibernate, não um
estreitamento da permissão.

---

## ResourceEntity

**Classe**: `br.com.archbase.security.persistence.ResourceEntity`
**Tabela**: `SEGURANCA_RECURSO` (auditada)
**Único**: `(TENANT_ID, NOME)`

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_RECURSO` (40) | `String` | não (PK) | |
| `code` | `CD_RECURSO` (40) | `String` | sim | |
| `name` | `NOME` | `String` | **não** | É o `resource` de `@HasPermission`. |
| `description` | `DESCRICAO` | `String` | **não** | |
| `active` | `BO_ATIVO` (1) | `Boolean` | **não** | `S`/`N`. |
| `type` | `TIPO_RECURSO` (50) | `TipoRecurso` | sim | `@Enumerated(STRING)`: `VIEW` (tela, registrada por `POST /api/v1/resource/register`) ou `API` (descoberta pela varredura de `@HasPermission`). |

## ActionEntity

**Classe**: `br.com.archbase.security.persistence.ActionEntity`
**Tabela**: `SEGURANCA_ACAO` (auditada)
**Único**: `uk_seguranca_acao_recurso_nome` em `(TENANT_ID, ID_RECURSO, NOME)`

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_ACAO` (40) | `String` | não (PK) | |
| `code` | `CD_ACAO` (40) | `String` | sim | |
| `name` | `NOME` | `String` | **não** | É o `action` de `@HasPermission`. |
| `description` | `DESCRICAO` | `String` | **não** | Semeada no primeiro registro; depois é do administrador. |
| `resource` | `ID_RECURSO` | `ResourceEntity` | **não** | `@ManyToOne`. |
| `label` | `ROTULO` (120) | `String` | sim | Rótulo curto. Nulo = use a descrição. Semeado por `@HasPermission(label = ...)`. |
| `category` | `CATEGORIA` | `String` | sim | Agrupamento dentro do recurso. Semeado por `@HasPermission(category = ...)`. |
| `active` | `BO_ATIVA` (1) | `Boolean` | **não** | `S`/`N`. Feminino: `BO_ATIVA`, não `BO_ATIVO`. |
| `actionVersion` | `VERSAO_ACAO` | `String` | sim | |
| `minimumLevel` | `MINIMUM_LEVEL` (30) | `AccessLevel` | sim | Piso do portão `LEVEL`. Nulo = sem piso. Semeado por `@HasPermission(minimumLevel = ...)`; só é avaliado com `archbase.security.access-level.enabled=true`. |

**Semente, depois administrador.** Descrição, rótulo, categoria e nível mínimo são gravados a partir
do código no primeiro registro da ação. Daí em diante o código não os sobrescreve, salvo
`archbase.security.sync.mode=refresh`, que reescreve descrição, rótulo e categoria — **nunca** o
nível mínimo.

**Ativa ou não.** A varredura desativa ações de tipo `API` que o código deixou de declarar; o
registro de tela é aditivo e nunca desativa. A decisão de acesso só exige ação e recurso ativos com
`archbase.security.permission.require-active=true` (padrão `false`).

**Unicidade em bases antigas.** A restrição única está no mapeamento. Em tabela criada antes dela,
confira no banco se ela existe.

## ActionDependencyEntity

**Classe**: `br.com.archbase.security.persistence.ActionDependencyEntity`
**Tabela**: `SEGURANCA_ACAO_DEPENDENCIA` (**não** auditada)
**Único**: `uk_seguranca_acao_dependencia` em `(TENANT_ID, ID_ACAO, CAPACIDADE_REQUERIDA)`

Uma aresta dirigida: a ação de origem não serve para nada sem a capacidade alvo.

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_DEPENDENCIA` (40) | `String` | não (PK) | |
| `code` | `CD_DEPENDENCIA` (40) | `String` | sim | |
| `action` | `ID_ACAO` | `ActionEntity` | **não** | `@ManyToOne(LAZY)`. A ação que declara a dependência. |
| `requiredCapability` | `CAPACIDADE_REQUERIDA` (200) | `String` | **não** | Alvo em texto, `recurso:acao`. É a chave: o alvo pode ainda não existir no catálogo. |
| `requiredAction` | `ID_ACAO_REQUERIDA` | `ActionEntity` | sim | Espelho resolvido do alvo. Nulo enquanto o alvo não estiver catalogado; preenchido a cada sincronização. |
| `declaredBy` | `DECLARADA_POR` (20) | `DependencySource` | **não** | `@Enumerated(STRING)`: `SCAN` (de `@HasPermission(requires = ...)`, reconciliação integral) ou `REGISTER` (de `POST /api/v1/resource/register`, poda só da ação presente no payload). |

**Informativa, nunca portão.** O avaliador de acesso não lê esta tabela, e não há flag que o faça
ler. Ela alimenta a tela de concessão e o relatório de efetivo (`unmetDependencies`).

## PermissionEntity

**Classe**: `br.com.archbase.security.persistence.PermissionEntity`
**Tabela**: `SEGURANCA_PERMISSAO` (auditada)

| Campo | Coluna | Tipo Java | Nulável | Observação |
|-------|--------|-----------|:-------:|------------|
| `id` | `ID_PERMISSAO` (40) | `String` | não (PK) | |
| `code` | `CD_PERMISSAO` (40) | `String` | sim | |
| `security` | `ID_SEGURANCA` | `SecurityEntity` | **não** | `@ManyToOne(LAZY)`: usuário, grupo ou perfil. |
| `action` | `ID_ACAO` | `ActionEntity` | **não** | `@ManyToOne(LAZY)`. |
| `companyId` | `COMPANY_ID` | `String` | sim | Estreitamento por empresa. |
| `projectId` | `PROJECT_ID` | `String` | sim | Estreitamento por projeto. |
| `effect` | `EFFECT` (10) | `PermissionEffect` | sim | `PermissionEffectConverter`. `GRANT` ou `DENY`; **nulo = `GRANT`** (é o valor de toda linha anterior à coluna). |

**Não existe campo `tenantId` de escopo.** Existiu, declarado com `@Column(name = "TENTANT_ID")`,
e nunca funcionou: o campo `tenantId` colidia com o discriminador de tenant herdado, o Hibernate
resolvia a ambiguidade em favor do discriminador e a coluna `TENTANT_ID` sequer chegava a ser
criada. Documentação e exemplos com `.tenantId(...)` como estreitamento descrevem esse campo morto. O tenant da linha continua
disponível pelo pai (`getTenantId()`), como discriminador. O estreitamento de uma permissão é por
**empresa** e **projeto**.

Métodos úteis: `effectOrGrant()`, `isDeny()`, `semEstreitamentoDeEscopo()` (`COMPANY_ID` e
`PROJECT_ID` nulos). `allowAllTenantsAndCompaniesAndProjects()` está `@Deprecated(forRemoval = true)`
e apenas delega a `semEstreitamentoDeEscopo()`.

### Como uma linha casa com a requisição

O avaliador (`DefaultArchbaseAccessEvaluator`) busca as linhas da capacidade entre as origens do
usuário — ele próprio, seus grupos e seu perfil — e aplica, nesta ordem:

1. **Negação.** Uma linha `DENY` vence se o seu estreitamento for confirmado pela requisição:
   `COMPANY_ID` nulo **ou** igual ao `X-COMPANY-ID` pedido, e o mesmo para projeto. Vence em
   qualquer origem — usuário, grupo ou perfil —, sem precedência entre elas, e **alcança também o
   administrador**: com `isAdministrator`, só as negações são consultadas, mas elas são consultadas.
2. **Concessão.** Entre as linhas `GRANT`, vale a que alcança o escopo: para tenant, empresa e
   projeto, o valor pedido é nulo, o da linha é nulo, ou os dois são iguais. Nenhuma alcança →
   negado no portão `SCOPE` (`OUT_OF_SCOPE`). Nenhuma linha → `NO_GRANT`.
3. **Nível**, se `archbase.security.access-level.enabled=true`: o maior `MINIMUM_LEVEL` entre as
   ações concedidas contra o nível do usuário.

Note a assimetria: uma concessão com `COMPANY_ID = 'A'` vale numa requisição **sem** empresa; uma
negação com `COMPANY_ID = 'A'` **não** vale numa requisição sem empresa.

### Exemplo

```java
import br.com.archbase.security.annotation.HasPermission;

@RestController
@RequestMapping("/api/sales/reports")
public class SalesReportController {

    @GetMapping
    @HasPermission(action = "VIEW", resource = "SALES_REPORT_API",
                   description = "Listar relatórios de vendas")
    public List<SalesReportDto> getReports() { ... }

    @PostMapping("/export")
    @HasPermission(action = "EXPORT", resource = "SALES_REPORT_API",
                   description = "Exportar relatório de vendas",
                   label = "Exportar", category = "Relatórios",
                   requires = {"VIEW"})
    public byte[] exportReport(@RequestBody ExportRequest request) { ... }
}
```

`action` e `description` são obrigatórios em `@HasPermission` (sem `description` não compila), e a
anotação é `@Target(METHOD)`. A varredura da subida cria `SALES_REPORT_API` (tipo `API`), as ações
`VIEW` e `EXPORT` e a aresta `EXPORT → SALES_REPORT_API:VIEW`. A concessão em si — a linha de
`SEGURANCA_PERMISSAO` — é feita pelo administrador.

---

## Resumo

| Classe | Tabela | Colunas próprias |
|--------|--------|------------------|
| `ResourceEntity` | `SEGURANCA_RECURSO` | `ID_RECURSO`, `CD_RECURSO`, `NOME`, `DESCRICAO`, `BO_ATIVO`, `TIPO_RECURSO` |
| `ActionEntity` | `SEGURANCA_ACAO` | `ID_ACAO`, `CD_ACAO`, `NOME`, `DESCRICAO`, `ID_RECURSO`, `ROTULO`, `CATEGORIA`, `BO_ATIVA`, `VERSAO_ACAO`, `MINIMUM_LEVEL` |
| `ActionDependencyEntity` | `SEGURANCA_ACAO_DEPENDENCIA` | `ID_DEPENDENCIA`, `CD_DEPENDENCIA`, `ID_ACAO`, `CAPACIDADE_REQUERIDA`, `ID_ACAO_REQUERIDA`, `DECLARADA_POR` |
| `PermissionEntity` | `SEGURANCA_PERMISSAO` | `ID_PERMISSAO`, `CD_PERMISSAO`, `ID_SEGURANCA`, `ID_ACAO`, `COMPANY_ID`, `PROJECT_ID`, `EFFECT` |

**Ver também**:
- [Entidades Core](core-entities.md)
- [Arquitetura de autorização](../../ARQUITETURA.md)
- [Anotações](../guides/annotations-and-usage.md)

---

[← Anterior: Entidades Core](core-entities.md) | [Voltar ao Índice](../README.md) | [Próximo: Gerenciamento de Tokens →](token-entities.md)
