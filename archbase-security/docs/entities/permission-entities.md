[← Anterior: Entidades Core](core-entities.md) | [Voltar ao Índice](../README.md) | [Próximo: Gerenciamento de Tokens →](token-entities.md)

---

# Sistema de Permissões - Entidades

## Introdução

O sistema de permissões do archbase-security implementa um modelo de controle de acesso baseado em permissões (RBAC - Role-Based Access Control) flexível e contextual. Este documento descreve as três entidades principais que compõem este sistema.

## Conceitos Fundamentais

### Modelo de Permissões

O modelo segue a seguinte estrutura:

```
SecurityEntity (User/Group/Profile) + Action → PermissionEntity
                                                       ↓
                                         Contexto (Tenant/Company/Project)
```

**Tradução**: "Este **SecurityEntity** (usuário, grupo ou perfil) pode executar esta **Action** (sobre este Resource) **neste contexto** (tenant/company/project específico ou global)"

### Exemplo Conceitual

```
User "João" pode "VIEW" o recurso "SALES_REPORT" no Tenant "ACME Corp"
    ↓           ↓              ↓                        ↓
  Security   Action        Resource              Contexto
```

---

## 1. PermissionEntity

**Localização**: `br.com.archbase.security.persistence.PermissionEntity`
**Tabela**: `SEGURANCA_PERMISSAO`

A entidade central do sistema de autorização, estabelecendo a ligação entre entidades de segurança e ações que podem ser executadas.

### Campos Principais

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_PERMISSAO | UUID | Identificador único |
| `security` | ID_SEGURANCA | UUID (FK) | Entidade de segurança (user, group ou profile) |
| `action` | ID_ACAO | UUID (FK) | Ação a ser permitida |

### Campos de Escopo Contextual

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `tenantId` | TENTANT_ID | String | Restringe permissão a um tenant específico (`null` = todos) |
| `companyId` | COMPANY_ID | String | Restringe permissão a uma empresa específica (`null` = todas) |
| `projectId` | PROJECT_ID | String | Restringe permissão a um projeto específico (`null` = todos) |

**Importante**: Valores `null` funcionam como **wildcards** (qualquer valor é aceito).

### Campos de Auditoria

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `tenantId` | TENANT_ID | String | Tenant da permissão (multi-tenancy) |
| `version` | VERSION | Long | Versionamento otimista |
| `createEntityDate` | CREATE_DATE | LocalDateTime | Data de criação |
| `createdByUser` | CREATED_BY | String | Usuário criador |

### Relacionamentos

| Relacionamento | Entidade | Tipo | Descrição |
|----------------|----------|------|-----------|
| `security` | SecurityEntity | ManyToOne | Usuário, grupo ou perfil que recebe a permissão |
| `action` | ActionEntity | ManyToOne | Ação sendo permitida |

### Método Especial: allowAllTenantsAndCompaniesAndProjects()

```java
@JsonIgnore
@Transient
public boolean allowAllTenantsAndCompaniesAndProjects() {
    return tenantId == null && companyId == null && projectId == null;
}
```

Este método identifica **permissões globais** (sem restrições de contexto).

**Uso**: Durante avaliação de permissões, o sistema verifica primeiro se existe uma permissão global antes de fazer matching de contextos específicos.

### Lógica de Matching de Contextos

A permissão é concedida se **TODOS** os seguintes critérios forem satisfeitos:

```java
(permissionTenantId == null OR contextTenantId == null OR permissionTenantId.equals(contextTenantId))
AND
(permissionCompanyId == null OR contextCompanyId == null OR permissionCompanyId.equals(contextCompanyId))
AND
(permissionProjectId == null OR contextProjectId == null OR permissionProjectId.equals(contextProjectId))
```

**Tabela de Verdade**:

| Valor na Permissão | Valor no Contexto | Match? | Explicação |
|-------------------|-------------------|--------|------------|
| `null` | `null` | ✓ | Sem restrição em ambos |
| `null` | `"abc"` | ✓ | Permissão permite qualquer valor |
| `"abc"` | `null` | ✓ | Contexto não especifica restrição |
| `"abc"` | `"abc"` | ✓ | Valores coincidem |
| `"abc"` | `"xyz"` | ✗ | Valores diferentes |

### Exemplos de Uso

#### Exemplo 1: Permissão Global

```java
PermissionEntity permission = PermissionEntity.builder()
    .security(adminUser)
    .action(viewAction)
    .tenantId(null)    // Wildcard - qualquer tenant
    .companyId(null)   // Wildcard - qualquer empresa
    .projectId(null)   // Wildcard - qualquer projeto
    .build();
```

**Resultado**: Usuário pode executar a ação em **qualquer contexto**.

#### Exemplo 2: Permissão Restrita a Tenant

```java
PermissionEntity permission = PermissionEntity.builder()
    .security(operatorUser)
    .action(editAction)
    .tenantId("tenant-123")  // Apenas neste tenant
    .companyId(null)         // Qualquer empresa
    .projectId(null)         // Qualquer projeto
    .build();
```

**Resultado**: Usuário pode executar a ação apenas no tenant "tenant-123", mas em qualquer empresa/projeto dentro dele.

#### Exemplo 3: Permissão Altamente Restrita

```java
PermissionEntity permission = PermissionEntity.builder()
    .security(contractorUser)
    .action(viewAction)
    .tenantId("tenant-123")      // Apenas neste tenant
    .companyId("company-456")    // Apenas nesta empresa
    .projectId("project-789")    // Apenas neste projeto
    .build();
```

**Resultado**: Usuário pode executar a ação **apenas** no projeto específico.

### Índices Recomendados

Para performance:

```sql
CREATE INDEX idx_permission_security_action ON SEGURANCA_PERMISSAO(ID_SEGURANCA, ID_ACAO);
CREATE INDEX idx_permission_context ON SEGURANCA_PERMISSAO(TENTANT_ID, COMPANY_ID, PROJECT_ID);
```

---

## 2. ResourceEntity

**Localização**: `br.com.archbase.security.persistence.ResourceEntity`
**Tabela**: `SEGURANCA_RECURSO`

Representa recursos protegidos no sistema (páginas, APIs, funcionalidades).

### Campos

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_RECURSO | UUID | Identificador único |
| `name` | NOME | String(150) | Nome único do recurso |
| `description` | DESCRICAO | String(500) | Descrição detalhada |
| `active` | BO_ATIVO | Boolean | Indica se o recurso está ativo |
| `type` | TIPO_RECURSO | Enum | Tipo do recurso (VIEW ou API) |

### Constraint Único

`UNIQUE (TENANT_ID, NOME)` - Nomes de recursos devem ser únicos **dentro de um tenant**.

**Importante**: Dois tenants diferentes podem ter recursos com o mesmo nome.

### Tipos de Recursos (Enum TipoRecurso)

```java
public enum TipoRecurso {
    VIEW,   // Recursos de interface (páginas, componentes, telas)
    API     // Endpoints de API REST
}
```

#### VIEW (Interface)

Recursos que representam elementos de interface do usuário.

**Exemplos**:
- `"USER_MANAGEMENT_PAGE"`
- `"DASHBOARD"`
- `"REPORTS_SECTION"`
- `"SETTINGS_PANEL"`

**Uso**: Controlar visibilidade de menus, botões, seções da interface.

#### API (Endpoints)

Recursos que representam endpoints de API.

**Exemplos**:
- `"USER_API"`
- `"SALES_REPORT_API"`
- `"PAYMENT_SERVICE"`

**Uso**: Controlar acesso a endpoints REST via anotação `@HasPermission`.

### Relacionamentos

| Relacionamento | Entidade | Tipo | Descrição |
|----------------|----------|------|-----------|
| `actions` | ActionEntity | OneToMany | Ações que podem ser executadas sobre este recurso |

### Exemplo de Uso

```java
// Criar recurso de API
ResourceEntity userApi = ResourceEntity.builder()
    .name("USER_API")
    .description("API de gerenciamento de usuários")
    .type(TipoRecurso.API)
    .active(true)
    .build();
resourceRepository.save(userApi);

// Criar recurso de VIEW
ResourceEntity dashboardView = ResourceEntity.builder()
    .name("DASHBOARD")
    .description("Dashboard principal do sistema")
    .type(TipoRecurso.VIEW)
    .active(true)
    .build();
resourceRepository.save(dashboardView);
```

### Nomenclatura Recomendada

#### Para APIs:
- Use sufixo `_API`: `USER_API`, `REPORT_API`
- Descreva o domínio: `SALES_ORDER_API`, `INVENTORY_API`

#### Para Views:
- Use sufixo `_PAGE`, `_VIEW` ou `_SECTION`: `USER_MANAGEMENT_PAGE`, `SETTINGS_VIEW`
- Seja descritivo: `CREATE_USER_FORM`, `EDIT_PROFILE_PANEL`

---

## 3. ActionEntity

**Localização**: `br.com.archbase.security.persistence.ActionEntity`
**Tabela**: `SEGURANCA_ACAO`

Representa operações que podem ser executadas sobre recursos.

### Campos

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_ACAO | UUID | Identificador único |
| `name` | NOME | String(150) | Nome da ação (ex: "VIEW", "CREATE") |
| `description` | DESCRICAO | String(500) | Descrição detalhada da ação |
| `resource` | ID_RECURSO | UUID (FK) | Recurso ao qual a ação pertence |
| `category` | CATEGORIA | String(100) | Categoria opcional para agrupamento |
| `active` | BO_ATIVA | Boolean | Indica se a ação está ativa |
| `actionVersion` | VERSAO_ACAO | String(20) | Versionamento da ação |

### Relacionamentos

| Relacionamento | Entidade | Tipo | Descrição |
|----------------|----------|------|-----------|
| `resource` | ResourceEntity | ManyToOne | Recurso ao qual a ação pertence |
| `permissions` | PermissionEntity | OneToMany | Permissões que concedem esta ação |

### Constraint Único

`UNIQUE (ID_RECURSO, NOME, TENANT_ID)` - Uma ação com o mesmo nome não pode existir duas vezes para o mesmo recurso no mesmo tenant.

### Ações CRUD Padrão

Para a maioria dos recursos, um conjunto padrão de ações CRUD é recomendado:

| Ação | Nome | Descrição | Uso Típico |
|------|------|-----------|------------|
| **View** | `VIEW` | Visualizar/listar | Endpoints GET, visualização de páginas |
| **Create** | `CREATE` | Criar novo | Endpoints POST, formulários de criação |
| **Update** | `UPDATE` | Editar existente | Endpoints PUT/PATCH, formulários de edição |
| **Delete** | `DELETE` | Remover | Endpoints DELETE, botões de exclusão |

### Ações Customizadas

Além das ações CRUD, você pode criar ações específicas do domínio:

**Exemplos**:
- `APPROVE` - Aprovar um documento
- `REJECT` - Rejeitar uma solicitação
- `EXPORT` - Exportar dados
- `IMPORT` - Importar dados
- `PUBLISH` - Publicar conteúdo
- `ARCHIVE` - Arquivar registros

### Categorização de Ações

O campo `category` permite agrupar ações relacionadas:

**Exemplos**:
- Categoria `"CRUD"`: VIEW, CREATE, UPDATE, DELETE
- Categoria `"APPROVAL"`: APPROVE, REJECT, REVIEW
- Categoria `"EXPORT"`: EXPORT_PDF, EXPORT_EXCEL, EXPORT_CSV

### Exemplo de Uso Completo

```java
// 1. Criar recurso
ResourceEntity userApi = ResourceEntity.builder()
    .name("USER_API")
    .description("API de usuários")
    .type(TipoRecurso.API)
    .active(true)
    .build();
resourceRepository.save(userApi);

// 2. Criar ações CRUD
ActionEntity viewAction = ActionEntity.builder()
    .name("VIEW")
    .description("Visualizar usuários")
    .resource(userApi)
    .category("CRUD")
    .active(true)
    .build();

ActionEntity createAction = ActionEntity.builder()
    .name("CREATE")
    .description("Criar novos usuários")
    .resource(userApi)
    .category("CRUD")
    .active(true)
    .build();

ActionEntity updateAction = ActionEntity.builder()
    .name("UPDATE")
    .description("Editar usuários existentes")
    .resource(userApi)
    .category("CRUD")
    .active(true)
    .build();

ActionEntity deleteAction = ActionEntity.builder()
    .name("DELETE")
    .description("Remover usuários")
    .resource(userApi)
    .category("CRUD")
    .active(true)
    .build();

actionRepository.saveAll(Arrays.asList(
    viewAction, createAction, updateAction, deleteAction
));

// 3. Criar permissões (conceder ações a usuários/perfis)
PermissionEntity permission = PermissionEntity.builder()
    .security(operatorProfile)
    .action(viewAction)  // Operador pode apenas VISUALIZAR
    .build();
permissionRepository.save(permission);

// Agora, qualquer usuário com o perfil "operatorProfile" pode executar VIEW em USER_API
```

### Nomenclatura Recomendada

#### Ações Padrão (sempre em UPPERCASE):
- `VIEW`, `CREATE`, `UPDATE`, `DELETE`
- `APPROVE`, `REJECT`
- `EXPORT`, `IMPORT`

#### Ações Específicas:
- Use verbos descritivos: `PUBLISH_ARTICLE`, `SEND_NOTIFICATION`
- Seja específico: `APPROVE_PURCHASE_ORDER` (não apenas `APPROVE`)

---

## Fluxo Completo: Resource → Action → Permission

### Passo 1: Criar Resource

```java
ResourceEntity salesApi = ResourceEntity.builder()
    .name("SALES_REPORT_API")
    .description("API de relatórios de vendas")
    .type(TipoRecurso.API)
    .active(true)
    .build();
resourceRepository.save(salesApi);
```

### Passo 2: Criar Actions

```java
ActionEntity viewReports = ActionEntity.builder()
    .name("VIEW")
    .description("Visualizar relatórios de vendas")
    .resource(salesApi)
    .build();

ActionEntity exportReports = ActionEntity.builder()
    .name("EXPORT")
    .description("Exportar relatórios de vendas")
    .resource(salesApi)
    .build();

actionRepository.saveAll(Arrays.asList(viewReports, exportReports));
```

### Passo 3: Criar Permissions

```java
// Gerentes podem visualizar e exportar
PermissionEntity managerView = PermissionEntity.builder()
    .security(managerProfile)
    .action(viewReports)
    .build();

PermissionEntity managerExport = PermissionEntity.builder()
    .security(managerProfile)
    .action(exportReports)
    .build();

// Analistas podem apenas visualizar
PermissionEntity analystView = PermissionEntity.builder()
    .security(analystProfile)
    .action(viewReports)
    .build();

permissionRepository.saveAll(Arrays.asList(
    managerView, managerExport, analystView
));
```

### Passo 4: Proteger Endpoint

```java
@RestController
@RequestMapping("/api/sales/reports")
public class SalesReportController {

    @GetMapping
    @HasPermission(action = "VIEW", resource = "SALES_REPORT_API")
    public List<SalesReportDto> getReports() {
        // Gerentes e Analistas podem acessar
    }

    @PostMapping("/export")
    @HasPermission(action = "EXPORT", resource = "SALES_REPORT_API")
    public byte[] exportReport(@RequestBody ExportRequest request) {
        // Apenas Gerentes podem acessar
    }
}
```

---

## Resumo das Entidades de Permissão

| Entidade | Tabela | Propósito | Campos Chave |
|----------|--------|-----------|--------------|
| **PermissionEntity** | SEGURANCA_PERMISSAO | Liga Security → Action com contexto | `security`, `action`, `tenantId`, `companyId`, `projectId` |
| **ResourceEntity** | SEGURANCA_RECURSO | Define recursos protegidos | `name`, `type` (VIEW/API), `active` |
| **ActionEntity** | SEGURANCA_ACAO | Define operações sobre recursos | `name`, `resource`, `category` |

---

**Ver também**:
- [Entidades Core](core-entities.md) - User, Group, Profile
- [Fluxo de Avaliação de Permissões](../architecture/permissions-system.md) - Como permissões são verificadas
- [Anotação @HasPermission](../guides/annotations-and-usage.md) - Como usar em código
- [Exemplos de Código](../guides/code-examples.md) - Casos de uso práticos

---

[← Anterior: Entidades Core](core-entities.md) | [Voltar ao Índice](../README.md) | [Próximo: Gerenciamento de Tokens →](token-entities.md)
