[← Voltar ao Índice](../README.md) | [Próximo: Sistema de Autenticação →](authentication-system.md)

---

# Arquitetura do Sistema de Permissões

## Introdução

O sistema de permissões do `archbase-security` implementa um modelo de controle de acesso flexível baseado em permissões (RBAC - Role-Based Access Control) com suporte para escopos contextuais hierárquicos. Este documento descreve o fluxo completo de avaliação de permissões e o modelo de escopos.

---

## Fluxo de Avaliação de Permissões

O sistema de permissões é implementado através da anotação `@HasPermission` e interceptado pelo `CustomAuthorizationManager`. O fluxo completo consiste em 9 passos principais.

### Visão Geral do Fluxo

```
Requisição HTTP → @HasPermission → CustomAuthorizationManager → ArchbaseSecurityService
                                                                          ↓
                                                                   Verificar Admin?
                                                                          ↓
                                                              Consultar Permissões no BD
                                                                          ↓
                                                                Matching de Contextos
                                                                          ↓
                                                            AuthorizationDecision (Allow/Deny)
```

### Passo 1: Interceptação de Método

Quando um método anotado com `@HasPermission` é invocado, o Spring AOP intercepta a chamada antes da execução do método.

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    @HasPermission(
        action = "VIEW",
        resource = "USER_PROFILE",
        description = "View user profile"
    )
    public UserDto getUserProfile(@PathVariable String id) {
        // Implementação
    }
}
```

**Componente Responsável**: Spring AOP (Aspect-Oriented Programming)

### Passo 2: Extração de Parâmetros

O `CustomAuthorizationManager` extrai os parâmetros da anotação `@HasPermission`.

**Localização**: `br.com.archbase.security.config.CustomAuthorizationManager:25-47`

```java
@Override
public AuthorizationDecision check(Supplier<Authentication> authentication,
                                   MethodInvocation methodInvocation) {
    Method method = methodInvocation.getMethod();
    HasPermission hasPermission = method.getAnnotation(HasPermission.class);

    if (hasPermission == null) {
        return new AuthorizationDecision(true);  // Sem anotação = permitir
    }

    // Extrair parâmetros
    String action = hasPermission.action();
    String resource = hasPermission.resource();
    String tenantId = hasPermission.tenantId();
    String companyId = hasPermission.companyId();
    String projectId = hasPermission.projectId();

    // Continua para passo 3...
}
```

**Parâmetros Extraídos**:
- `action`: Nome da ação a ser executada (ex: "VIEW", "CREATE")
- `resource`: Nome do recurso sendo acessado (ex: "USER_PROFILE")
- `tenantId`, `companyId`, `projectId`: Contextos opcionais

### Passo 3: Obtenção do Contexto do Tenant

Se os contextos não forem especificados explicitamente na anotação, são obtidos de `ArchbaseTenantContext` (ThreadLocal).

```java
String tenantId = hasPermission.tenantId().isEmpty() ?
    ArchbaseTenantContext.getTenantId() : hasPermission.tenantId();

String companyId = hasPermission.companyId().isEmpty() ?
    ArchbaseTenantContext.getCompanyId() : hasPermission.companyId();

String projectId = hasPermission.projectId().isEmpty() ?
    ArchbaseTenantContext.getProjectId() : hasPermission.projectId();
```

**Lógica**:
- Se o parâmetro na anotação está vazio → buscar de `ArchbaseTenantContext`
- Se o parâmetro na anotação tem valor → usar valor explícito

**Ver**: [Multi-Tenancy](multi-tenancy.md) para detalhes sobre propagação de contexto.

### Passo 4: Chamada ao Security Service

O método `ArchbaseSecurityService.hasPermission()` é invocado para determinar se o acesso deve ser concedido.

```java
boolean hasAccess = securityService.hasPermission(
    authentication.get(),
    action,
    resource,
    tenantId,
    companyId,
    projectId
);

return new AuthorizationDecision(hasAccess);
```

**Componente**: `ArchbaseSecurityService` (`br.com.archbase.security.service.ArchbaseSecurityService`)

### Passo 5: Verificação de Administrador (Bypass)

Antes de consultar permissões no banco, o sistema verifica se o usuário é administrador.

**Localização**: `ArchbaseSecurityService:18-36`

```java
public boolean hasPermission(Authentication authentication, String action, String resource,
                            String tenantId, String companyId, String projectId) {

    // BYPASS PARA ADMINISTRADORES
    UserEntity user = (UserEntity) authentication.getPrincipal();

    if (user.getIsAdministrator() && user.isEnabled()) {
        return true;  // Acesso concedido imediatamente
    }

    // Continua para verificação normal de permissões...
}
```

**Condições para Bypass**:
1. `UserEntity.isAdministrator == true`
2. `UserEntity.isEnabled() == true` (conta não desativada e não bloqueada)

**Importante**: Mesmo administradores com contas desativadas ou bloqueadas **NÃO** terão acesso.

### Passo 6: Consulta de Permissões

Se o usuário não é administrador (ou está desativado), o sistema busca permissões no banco de dados.

```java
String userId = user.getId();

List<PermissionEntity> permissions = permissionRepository
    .findBySecurityIdAndActionNameAndResourceName(
        userId,
        action,
        resource
    );
```

**Query Executada**:
```sql
SELECT p.*
FROM SEGURANCA_PERMISSAO p
JOIN SEGURANCA_ACAO a ON p.ID_ACAO = a.ID_ACAO
JOIN SEGURANCA_RECURSO r ON a.ID_RECURSO = r.ID_RECURSO
WHERE p.ID_SEGURANCA = :userId
  AND a.NOME = :action
  AND r.NOME = :resource
```

**Retorno**: Lista de todas as permissões do usuário para aquela ação+recurso (pode incluir permissões de perfil e grupos).

### Passo 7: Verificação de Permissões Globais

O sistema primeiro verifica se existe alguma permissão global (sem restrições de contexto).

```java
if (permissions.stream().anyMatch(PermissionEntity::allowAllTenantsAndCompaniesAndProjects)) {
    return true;  // Permissão global encontrada
}
```

**Método Helper**:
```java
@Transient
public boolean allowAllTenantsAndCompaniesAndProjects() {
    return tenantId == null && companyId == null && projectId == null;
}
```

**Vantagem**: Evita matching complexo se existe permissão global.

### Passo 8: Matching de Contextos

Se não houver permissão global, o sistema realiza matching de contextos para cada permissão encontrada.

```java
return permissions.stream().anyMatch(permission ->
    (tenantId == null || permission.getTenantId() == null ||
     tenantId.equals(permission.getTenantId())) &&

    (companyId == null || permission.getCompanyId() == null ||
     companyId.equals(permission.getCompanyId())) &&

    (projectId == null || permission.getProjectId() == null ||
     projectId.equals(permission.getProjectId()))
);
```

**Lógica de Matching**:

Para cada campo de contexto (tenant, company, project), a permissão é aceita SE:
- Valor na permissão é `null` (wildcard - aceita qualquer valor), **OU**
- Valor no contexto é `null` (sem restrição), **OU**
- Valores coincidem exatamente

**Tabela de Verdade**:

| Permissão | Contexto | Match? | Explicação |
|-----------|----------|--------|------------|
| `null` | `null` | ✓ | Sem restrição em ambos |
| `null` | `"abc"` | ✓ | Permissão permite qualquer valor |
| `"abc"` | `null` | ✓ | Contexto não especifica restrição |
| `"abc"` | `"abc"` | ✓ | Valores coincidem |
| `"abc"` | `"xyz"` | ✗ | Valores diferentes |

### Passo 9: Decisão de Autorização

Com base no resultado da verificação, uma `AuthorizationDecision` é retornada:

```java
return new AuthorizationDecision(hasAccess);
```

**Resultado**:
- `AuthorizationDecision(true)`: Acesso concedido → método executado
- `AuthorizationDecision(false)`: Acesso negado → `AccessDeniedException` lançada

**Exceção Lançada** (quando negado):
```
org.springframework.security.access.AccessDeniedException: Access Denied
```

**Resposta HTTP**: `403 Forbidden`

---

## Modelo de Escopo de Permissões

O sistema suporta **quatro níveis de granularidade** para permissões, permitindo controle de acesso desde global até altamente específico.

### Hierarquia de Escopos

```
Nível 1: Global (Sem Restrições)
   ↓
Nível 2: Tenant-Scoped (Organização)
   ↓
Nível 3: Company-Scoped (Empresa)
   ↓
Nível 4: Project-Scoped (Projeto)
```

**Princípio**: Quanto mais específico o escopo, mais restrita a permissão.

### Nível 1: Global (Sem Restrições)

**Características**:
- Todos os campos de contexto são `null`
- Acesso permitido em **qualquer contexto**
- Usado para administradores e serviços globais

**Exemplo**:
```java
PermissionEntity permission = PermissionEntity.builder()
    .security(adminUser)
    .action(viewAction)
    .tenantId(null)    // Wildcard
    .companyId(null)   // Wildcard
    .projectId(null)   // Wildcard
    .build();
```

**Uso Típico**: Administradores do sistema, serviços de monitoramento global.

**Matching**: Qualquer contexto é aceito (sempre retorna `true`).

### Nível 2: Tenant-Scoped

**Características**:
- `tenantId` específico
- `companyId` e `projectId` são `null`
- Acesso permitido em qualquer empresa/projeto **dentro do tenant**

**Exemplo**:
```java
PermissionEntity permission = PermissionEntity.builder()
    .security(operatorUser)
    .action(viewAction)
    .tenantId("tenant-123")  // Restrito a este tenant
    .companyId(null)         // Qualquer empresa
    .projectId(null)         // Qualquer projeto
    .build();
```

**Uso Típico**: Usuários que trabalham em um tenant específico mas em múltiplas empresas/projetos.

**Matching**:
- `tenantId` deve coincidir
- `companyId` e `projectId` são irrelevantes

### Nível 3: Company-Scoped

**Características**:
- `tenantId` e `companyId` específicos
- `projectId` é `null`
- Acesso permitido em qualquer projeto **dentro da empresa**

**Exemplo**:
```java
PermissionEntity permission = PermissionEntity.builder()
    .security(managerUser)
    .action(editAction)
    .tenantId("tenant-123")     // Restrito a este tenant
    .companyId("company-456")   // Restrito a esta empresa
    .projectId(null)            // Qualquer projeto
    .build();
```

**Uso Típico**: Gerentes de empresa que supervisionam múltiplos projetos.

**Matching**:
- `tenantId` e `companyId` devem coincidir
- `projectId` é irrelevante

### Nível 4: Project-Scoped (Mais Granular)

**Características**:
- Todos os campos especificados (`tenantId`, `companyId`, `projectId`)
- Acesso permitido **apenas no projeto específico**
- Máxima granularidade

**Exemplo**:
```java
PermissionEntity permission = PermissionEntity.builder()
    .security(developerUser)
    .action(deployAction)
    .tenantId("tenant-123")     // Restrito a este tenant
    .companyId("company-456")   // Restrito a esta empresa
    .projectId("project-789")   // Restrito a este projeto
    .build();
```

**Uso Típico**: Desenvolvedores ou consultores alocados a projetos específicos.

**Matching**: Todos os três campos devem coincidir.

---

## Lógica de Matching Detalhada

### Algoritmo Completo

```
Para cada permissão encontrada:

  SE permissão.tenantId == null OU contexto.tenantId == null OU permissão.tenantId == contexto.tenantId
    E
  permissão.companyId == null OU contexto.companyId == null OU permissão.companyId == contexto.companyId
    E
  permissão.projectId == null OU contexto.projectId == null OU permissão.projectId == contexto.projectId

  ENTÃO: Permissão concedida
```

### Exemplos Práticos

#### Exemplo 1: Permissão Global vs. Contexto Específico

**Permissão**:
```java
tenantId = null
companyId = null
projectId = null
```

**Contexto da Requisição**:
```java
tenantId = "ABC"
companyId = "ABC-BR"
projectId = "PROJ-1"
```

**Resultado**: ✓ **MATCH** (permissão global aceita qualquer contexto)

#### Exemplo 2: Permissão Tenant vs. Contexto Company

**Permissão**:
```java
tenantId = "ABC"
companyId = null
projectId = null
```

**Contexto da Requisição**:
```java
tenantId = "ABC"
companyId = "ABC-BR"
projectId = "PROJ-1"
```

**Resultado**: ✓ **MATCH** (tenant coincide, company/project são wildcards)

#### Exemplo 3: Permissão Project vs. Contexto Diferente

**Permissão**:
```java
tenantId = "ABC"
companyId = "ABC-BR"
projectId = "PROJ-1"
```

**Contexto da Requisição**:
```java
tenantId = "ABC"
companyId = "ABC-BR"
projectId = "PROJ-2"  // Projeto diferente!
```

**Resultado**: ✗ **NO MATCH** (projeto não coincide)

#### Exemplo 4: Múltiplas Permissões

**Permissões do Usuário**:
1. `tenantId="ABC", companyId="ABC-BR", projectId="PROJ-1"`
2. `tenantId="ABC", companyId="ABC-AR", projectId=null`

**Contexto da Requisição**:
```java
tenantId = "ABC"
companyId = "ABC-AR"
projectId = "PROJ-5"
```

**Análise**:
- Permissão 1: ✗ NO MATCH (company diferente)
- Permissão 2: ✓ **MATCH** (tenant e company coincidem, project é wildcard)

**Resultado Final**: ✓ **ACESSO CONCEDIDO** (pelo menos uma permissão fez match)

---

## Performance e Otimizações

### Cache de Permissões

O sistema suporta cache de permissões para melhorar performance:

```properties
archbase.security.permission.cache.enabled=true
archbase.security.permission.cache.ttl=300  # 5 minutos
```

**Funcionamento**:
- Permissões consultadas são armazenadas em cache
- Cache invalidado ao modificar permissões
- TTL configurável (padrão: 5 minutos)

### Índices de Banco de Dados

Recomendações de índices para otimizar queries:

```sql
-- Índice composto para busca de permissões
CREATE INDEX idx_permission_security_action_resource
ON SEGURANCA_PERMISSAO(ID_SEGURANCA, ID_ACAO);

-- Índice para matching de contexto
CREATE INDEX idx_permission_context
ON SEGURANCA_PERMISSAO(TENTANT_ID, COMPANY_ID, PROJECT_ID);
```

### Early Return para Administradores

A verificação de administrador (Passo 5) acontece **antes** de consultar o banco, evitando queries desnecessárias.

```java
// Verificação rápida (sem query)
if (user.getIsAdministrator() && user.isEnabled()) {
    return true;  // Sem consultar banco
}
```

---

## Diagrama de Sequência

Ver: [Fluxo de Avaliação de Permissões](../diagrams/flows.md#permission-evaluation-flow) para diagrama visual completo.

---

## Resumo

| Passo | Ação | Componente | Pode Falhar? |
|-------|------|------------|--------------|
| 1 | Interceptação de método | Spring AOP | Não |
| 2 | Extração de parâmetros | CustomAuthorizationManager | Não |
| 3 | Obtenção de contexto | ArchbaseTenantContext | Não (pode ser null) |
| 4 | Chamada ao service | ArchbaseSecurityService | Não |
| 5 | Verificação de admin | ArchbaseSecurityService | Não |
| 6 | Consulta de permissões | PermissionRepository | Não (pode retornar vazio) |
| 7 | Verificação global | ArchbaseSecurityService | Não |
| 8 | Matching de contextos | ArchbaseSecurityService | Não |
| 9 | Decisão final | CustomAuthorizationManager | **Sim** (AccessDeniedException) |

---

**Ver também**:
- [Sistema de Autenticação](authentication-system.md) - Como usuários são autenticados
- [Multi-Tenancy](multi-tenancy.md) - Propagação de contexto de tenant
- [Anotação @HasPermission](../guides/annotations-and-usage.md) - Como usar em código
- [Entidades de Permissão](../entities/permission-entities.md) - Modelo de dados

---

[← Voltar ao Índice](../README.md) | [Próximo: Sistema de Autenticação →](authentication-system.md)
