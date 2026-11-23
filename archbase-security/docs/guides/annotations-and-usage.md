[← Anterior: Configuração](configuration.md) | [Voltar ao Índice](../README.md) | [Próximo: Exemplos de Código →](code-examples.md)

---

# Anotações e Uso do Módulo archbase-security

## Introdução

Este documento descreve todas as anotações de segurança disponíveis no módulo `archbase-security`, com foco especial na anotação `@HasPermission`, que é o principal mecanismo de controle de acesso baseado em permissões.

---

## 1. Anotação @HasPermission

### Localização

```
br.com.archbase.security.annotation.HasPermission
```

### Descrição

A anotação `@HasPermission` é o **principal mecanismo** para implementar controle de acesso baseado em permissões (PBAC - Permission-Based Access Control) no Archbase. Ela permite definir permissões granulares no nível de método, verificando se o usuário autenticado possui a permissão necessária antes de executar a operação.

### Características

- Aplica-se apenas em **métodos** (não em classes)
- Verifica permissões em tempo de execução
- Suporta estreitamento por empresa e projeto; o isolamento entre tenants é do Hibernate (`@TenantId`)
- É avaliada pelo core de cinco portões (`ARQUITETURA.md`, `MODELO_CORE_AUTORIZACAO.md`): uma permissão com
  `EFFECT=DENY` vence qualquer concessão, inclusive a do administrador
- `description` é obrigatória: é o texto que aparece na tela de concessão

---

## 2. Parâmetros da Anotação

### Definição Completa

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HasPermission {
    String action();                                   // Nome da ação (obrigatório)
    String description();                              // Descrição legível (obrigatório)
    String resource() default "";                      // Vazio = herda de @ArchbaseResource da classe
    String label() default "";                         // Rótulo curto no admin (vazio = usa a descrição)
    String category() default "";                      // Agrupamento dentro do recurso
    AccessLevel minimumLevel() default AccessLevel.NONE; // Piso de nível (semente; portão LEVEL)
    String[] requires() default {};                    // Dependências informativas, nunca portão
    String tenantId() default "";                      // Ver 2.2 — não dá acesso entre tenants
    String companyId() default "";                     // Estreitamento por empresa
    String projectId() default "";                     // Estreitamento por projeto
}
```

Sem `resource` e sem `@ArchbaseResource` na classe, o acesso é **negado** e o erro vai para o log.

```java
@RestController
@ArchbaseResource(value = "USER", description = "Usuários")
public class UserController {

    @GetMapping
    @HasPermission(action = "VIEW", description = "Consultar usuários")   // recurso USER herdado
    public List<UserDto> list() { ... }
}
```

### 2.1 Parâmetros Obrigatórios

#### action

**Tipo**: `String` (obrigatório)

**Descrição**: Nome da ação a ser executada sobre o recurso.

**Valores Recomendados**:

| Ação | Uso | Operação HTTP Típica |
|------|-----|---------------------|
| `VIEW` | Visualizar recurso | GET |
| `CREATE` | Criar novo recurso | POST |
| `UPDATE` | Atualizar recurso existente | PUT, PATCH |
| `DELETE` | Excluir recurso | DELETE |
| `EXECUTE` | Executar operação especial | POST |
| `EXPORT` | Exportar dados | GET |
| `IMPORT` | Importar dados | POST |
| `APPROVE` | Aprovar recurso | POST, PUT |
| `REJECT` | Rejeitar recurso | POST, PUT |

**Boas Práticas**:
- Use verbos no infinitivo
- Mantenha consistência em toda a aplicação
- Prefira ações padrão antes de criar ações customizadas

#### resource

**Tipo**: `String` (obrigatório, a menos que a classe declare `@ArchbaseResource`)

**Descrição**: Nome do recurso sobre o qual a ação será executada.

**Convenções de Nomenclatura**:
- Use nomes no **singular**: `USER`, `PRODUCT`, `ORDER`
- Use **UPPER_SNAKE_CASE**: `USER_PROFILE`, `SALES_REPORT`
- Seja **específico**: `FINANCIAL_REPORT` ao invés de `REPORT`
- Agrupe recursos relacionados: `INVOICE`, `INVOICE_ITEM`, `INVOICE_PAYMENT`

**Exemplos**:

```
✓ Bom:
- USER
- PRODUCT
- SALES_REPORT
- INVOICE_PAYMENT
- USER_PROFILE

✗ Ruim:
- users (plural)
- UserProfile (camelCase)
- report (muito genérico)
- fp (abreviação não clara)
```

#### description

**Tipo**: `String` (obrigatório)

**Descrição**: Descrição legível da permissão para documentação e interfaces de usuário.

**Características**:
- Deve ser claro e conciso
- Escrito em linguagem natural
- Útil para geração automática de documentação
- Exibido em telas de administração de permissões

**Exemplos**:

```java
@HasPermission(
    action = "VIEW",
    resource = "USER",
    description = "View user details"  // Claro e conciso
)

@HasPermission(
    action = "APPROVE",
    resource = "INVOICE",
    description = "Approve invoice for payment"  // Descreve o propósito
)
```

### 2.2 Parâmetros Opcionais (Contextos)

#### tenantId

**Tipo**: `String` (opcional, padrão: `""`)

**Descrição**: Define o contexto de tenant para a verificação de permissão.

**Comportamento**:
- **Vazio (`""`)**: usa o tenant atual de `ArchbaseTenantContext`. É o uso correto.
- **Não vazio**: o valor é comparado literalmente. **Não dá acesso entre tenants**: as permissões são
  lidas com o filtro `@TenantId` do Hibernate, que só devolve as linhas do tenant corrente. Um valor
  fixo como `"MASTER_TENANT"` não alcança dados de outro tenant.

```java
@HasPermission(
    action = "VIEW",
    resource = "USER",
    description = "View users in current tenant"
    // tenantId vazio = usa contexto atual
)
```

Operação que precise ler vários tenants (purga, relatório da plataforma) deve rodar fora da
requisição, com o tenant definido explicitamente em `ArchbaseTenantContext` para cada um.

#### companyId

**Tipo**: `String` (opcional, padrão: `""`)

**Descrição**: Define o contexto de empresa para a verificação de permissão.

**Comportamento**:
- **Vazio (`""`)**: usa a empresa atual de `ArchbaseTenantContext.getCompanyId()`.
- **Não vazio**: comparado literalmente com `SEGURANCA_PERMISSAO.COMPANY_ID`. Não é expressão: `"#companyId"`
  não é avaliado como SpEL.
- Permissão com `COMPANY_ID` nulo vale para qualquer empresa; a negação (DENY) com escopo tem regra própria.

**Exemplo**:

```java
@GetMapping("/company/{companyId}/reports")
@HasPermission(
    action = "VIEW",
    resource = "FINANCIAL_REPORT",
    description = "View financial reports for company",
    companyId = ""  // Usar empresa atual do contexto
)
public List<ReportDto> getCompanyReports(@PathVariable String companyId) {
    return reportService.findByCompany(companyId);
}
```

#### projectId

**Tipo**: `String` (opcional, padrão: `""`)

**Descrição**: Define o contexto de projeto para a verificação de permissão.

**Comportamento**:
- **Vazio (`""`)**: não estreita por projeto. Não existe "projeto atual" no contexto:
  `ArchbaseTenantContext` só guarda tenant e empresa.
- **Não vazio**: comparado literalmente com `SEGURANCA_PERMISSAO.PROJECT_ID`.
- O valor é fixo na anotação; não é possível usar o `{projectId}` da URL. Para checar acesso ao projeto
  da requisição, combine com `@PreAuthorize` e um bean (ver 5.2).

**Exemplo**:

```java
@PostMapping("/project/{projectId}/task")
@HasPermission(
    action = "CREATE",
    resource = "TASK",
    description = "Create task in project"
)
public TaskDto createTask(@PathVariable String projectId, @RequestBody TaskDto task) {
    return taskService.create(projectId, task);
}
```

---

## 3. Uso Básico

### 3.1 CRUD Operations

Exemplo completo de controller com operações CRUD:

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Visualizar detalhes do usuário
     */
    @GetMapping("/{id}")
    @HasPermission(
        action = "VIEW",
        resource = "USER",
        description = "View user details"
    )
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        UserDto user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Criar novo usuário
     */
    @PostMapping
    @HasPermission(
        action = "CREATE",
        resource = "USER",
        description = "Create new user"
    )
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto user) {
        UserDto created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Atualizar usuário existente
     */
    @PutMapping("/{id}")
    @HasPermission(
        action = "UPDATE",
        resource = "USER",
        description = "Update user details"
    )
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String id,
            @RequestBody UserDto user) {
        UserDto updated = userService.update(id, user);
        return ResponseEntity.ok(updated);
    }

    /**
     * Excluir usuário
     */
    @DeleteMapping("/{id}")
    @HasPermission(
        action = "DELETE",
        resource = "USER",
        description = "Delete user"
    )
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 3.2 Operações Especializadas

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    /**
     * Exportar catálogo de produtos
     */
    @GetMapping("/export")
    @HasPermission(
        action = "EXPORT",
        resource = "PRODUCT",
        description = "Export product catalog"
    )
    public ResponseEntity<byte[]> exportCatalog() {
        byte[] file = productService.exportCatalog();
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=catalog.xlsx")
            .body(file);
    }

    /**
     * Importar produtos em lote
     */
    @PostMapping("/import")
    @HasPermission(
        action = "IMPORT",
        resource = "PRODUCT",
        description = "Import products in batch"
    )
    public ResponseEntity<ImportResultDto> importProducts(
            @RequestParam("file") MultipartFile file) {
        ImportResultDto result = productService.importProducts(file);
        return ResponseEntity.ok(result);
    }

    /**
     * Aprovar produto para venda
     */
    @PostMapping("/{id}/approve")
    @HasPermission(
        action = "APPROVE",
        resource = "PRODUCT",
        description = "Approve product for sale"
    )
    public ResponseEntity<ProductDto> approveProduct(@PathVariable String id) {
        ProductDto product = productService.approve(id);
        return ResponseEntity.ok(product);
    }
}
```

---

## 4. Uso com Contextos

### 4.1 Contextos Dinâmicos (Recomendado)

**Cenário**: Aplicação SaaS multi-tenant onde permissões são verificadas no contexto atual.

```java
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Listar faturas do tenant atual
     * O tenantId é obtido automaticamente de ArchbaseTenantContext
     */
    @GetMapping
    @HasPermission(
        action = "VIEW",
        resource = "INVOICE",
        description = "View invoices in current tenant"
        // Contextos vazios = usar contexto atual
    )
    public Page<InvoiceDto> listInvoices(Pageable pageable) {
        // A query será automaticamente filtrada pelo tenantId atual
        return invoiceService.findAll(pageable);
    }

    /**
     * Criar fatura no tenant atual
     */
    @PostMapping
    @HasPermission(
        action = "CREATE",
        resource = "INVOICE",
        description = "Create invoice in current tenant"
    )
    public ResponseEntity<InvoiceDto> createInvoice(@RequestBody InvoiceDto invoice) {
        // A fatura será criada no tenantId atual
        InvoiceDto created = invoiceService.create(invoice);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
```

**Como o Contexto é Determinado**:

1. JWT contém `tenantId`, `companyId`, `projectId`
2. `ArchbaseTenantContext` é populado durante autenticação
3. `@HasPermission` verifica permissão no contexto atual

### 4.2 Contextos Explícitos

**Cenário**: Operações administrativas que precisam acessar contextos específicos.

```java
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final TenantService tenantService;

    /**
     * Auditoria global (acesso cross-tenant)
     */
    @GetMapping("/audit")
    @HasPermission(
        action = "VIEW",
        resource = "AUDIT_LOG",
        description = "View global audit logs",
        tenantId = "SYSTEM"  // Contexto fixo para operações de sistema
    )
    public List<AuditLogDto> getGlobalAuditLogs() {
        return auditService.findAll();
    }

    /**
     * Gerenciar tenant específico
     */
    @PutMapping("/tenant/{tenantId}")
    @HasPermission(
        action = "UPDATE",
        resource = "TENANT",
        description = "Update tenant configuration",
        tenantId = "SYSTEM"  // Apenas admins globais podem gerenciar tenants
    )
    public ResponseEntity<TenantDto> updateTenant(
            @PathVariable String tenantId,
            @RequestBody TenantDto tenant) {
        TenantDto updated = tenantService.update(tenantId, tenant);
        return ResponseEntity.ok(updated);
    }
}
```

### 4.3 Contextos por Company e Project

**Cenário**: Aplicação com hierarquia Tenant → Company → Project.

```java
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    /**
     * Relatório de vendas por empresa
     */
    @GetMapping("/sales/company/{companyId}")
    @HasPermission(
        action = "VIEW",
        resource = "SALES_REPORT",
        description = "View sales report for company",
        companyId = ""  // Usar companyId do contexto atual
    )
    public SalesReportDto getSalesReport(@PathVariable String companyId) {
        return reportService.getSalesReport(companyId);
    }

    /**
     * Relatório financeiro de projeto
     */
    @GetMapping("/financial/project/{projectId}")
    @HasPermission(
        action = "VIEW",
        resource = "FINANCIAL_REPORT",
        description = "View financial report for project",
        projectId = ""  // Usar projectId do contexto atual
    )
    public FinancialReportDto getFinancialReport(@PathVariable String projectId) {
        return reportService.getFinancialReport(projectId);
    }

    /**
     * Consolidado de todos os projetos da empresa
     */
    @GetMapping("/consolidated")
    @HasPermission(
        action = "VIEW",
        resource = "CONSOLIDATED_REPORT",
        description = "View consolidated report for company",
        companyId = "",
        projectId = "*"  // Todos os projetos da empresa
    )
    public ConsolidatedReportDto getConsolidatedReport() {
        return reportService.getConsolidatedReport();
    }
}
```

---

## 5. Outras Anotações de Segurança

### 5.1 @PreAuthorize (Spring Security)

**Descrição**: Anotação padrão do Spring Security para verificações antes da execução do método.

**Uso**: Para lógica de autorização customizada ou baseada em roles/profiles.

> **`hasRole`/`hasAuthority` sempre negam.** `UserEntity.getAuthorities()` devolve lista vazia: papéis e
> perfis do Archbase não viram `GrantedAuthority`. Para papel, use `@RequireRole` (com um
> `ArchbaseRoleResolver`), `@RequireProfile` ou `@RequirePersona`; para administrador, `@HasPermission`
> já concede a quem tem `isAdministrator`.

```java
@PreAuthorize("authentication.principal.username == #username")
public void ownProfileOperation(@PathVariable String username) {
    // Apenas o próprio usuário pode executar
}
```

### 5.2 @PreAuthorize com Expressões Customizadas

**Cenário**: Verificações complexas usando beans customizados.

```java
/**
 * Service para verificações de segurança customizadas
 */
@Service
public class CustomSecurityService {

    @Autowired
    private PermissionRepository permissionRepository;

    public boolean canAccessProject(String projectId) {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return permissionRepository.userHasAccessToProject(userId, projectId);
    }

    public boolean canApproveInvoice(String invoiceId) {
        // Lógica customizada para aprovação de faturas
        Invoice invoice = invoiceRepository.findById(invoiceId);
        // Não use hasRole(): getAuthorities() é vazio. Consulte o domínio ou o ArchbaseRoleResolver.
        return invoice.getAmount() < 1000.00 || roleResolver.resolveRoles(currentUser()).contains("FINANCIAL_DIRECTOR");
    }
}

/**
 * Controller usando verificações customizadas
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @GetMapping("/{projectId}")
    @PreAuthorize("@customSecurityService.canAccessProject(#projectId)")
    public ProjectDto getProject(@PathVariable String projectId) {
        return projectService.findById(projectId);
    }

    @PostMapping("/invoice/{invoiceId}/approve")
    @PreAuthorize("@customSecurityService.canApproveInvoice(#invoiceId)")
    public void approveInvoice(@PathVariable String invoiceId) {
        invoiceService.approve(invoiceId);
    }
}
```

### 5.3 @PostAuthorize

**Descrição**: Verificação após execução do método, útil para filtragem baseada no resultado.

```java
@PostAuthorize("returnObject.owner == authentication.principal.username")
public Document getDocument(String documentId) {
    return documentService.findById(documentId);
}
```

### 5.4 @Secured, @RolesAllowed e @PermitAll — NÃO USE

O Archbase liga só `@EnableMethodSecurity(prePostEnabled = true)`. `securedEnabled` e `jsr250Enabled`
ficam desligados, então estas anotações são **ignoradas em silêncio**:

| Anotação | O que acontece |
|----------|----------------|
| `@Secured("ROLE_ADMIN")` | Ignorada. Qualquer usuário autenticado executa o método. |
| `@RolesAllowed("ADMIN")` | Ignorada. Qualquer usuário autenticado executa o método. |
| `@PermitAll` | Ignorada. O endpoint continua exigindo autenticação. |

Mesmo que a aplicação ligasse `securedEnabled`/`jsr250Enabled`, os papéis nunca casariam, porque
`getAuthorities()` é vazio (ver 5.1).

Use no lugar:
- controle de acesso: `@HasPermission`;
- papel do domínio: `@RequireRole` + `ArchbaseRoleResolver` (sem o resolver, vale
  `archbase.security.require-role.no-resolver-policy`);
- perfil ou persona: `@RequireProfile`, `@RequirePersona` (aceitam classe; a do método prevalece);
- endpoint público: `archbase.security.whitelist`.

---

## 6. Combinando @HasPermission com Outras Anotações

### 6.1 @HasPermission + @PreAuthorize

**Cenário**: Verificação de permissão E verificação customizada.

```java
@PostMapping("/{id}/publish")
@HasPermission(
    action = "PUBLISH",
    resource = "ARTICLE",
    description = "Publish article"
)
@PreAuthorize("@articleService.isOwnerOrEditor(#id)")
public ArticleDto publishArticle(@PathVariable String id) {
    return articleService.publish(id);
}
```

### 6.2 @HasPermission + Validação de Dados

```java
@PostMapping
@HasPermission(
    action = "CREATE",
    resource = "USER",
    description = "Create new user"
)
public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto user) {
    // @Valid garante validação de dados
    // @HasPermission garante permissão
    UserDto created = userService.create(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

---

## 7. Exemplos Práticos por Domínio

### 7.1 E-commerce

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @PostMapping
    @HasPermission(
        action = "CREATE",
        resource = "ORDER",
        description = "Create new order"
    )
    public OrderDto createOrder(@RequestBody OrderDto order) {
        return orderService.create(order);
    }

    @PostMapping("/{id}/cancel")
    @HasPermission(
        action = "CANCEL",
        resource = "ORDER",
        description = "Cancel order"
    )
    public OrderDto cancelOrder(@PathVariable String id) {
        return orderService.cancel(id);
    }

    @PostMapping("/{id}/refund")
    @HasPermission(
        action = "REFUND",
        resource = "ORDER",
        description = "Process order refund"
    )
    public RefundDto processRefund(@PathVariable String id) {
        return orderService.processRefund(id);
    }
}
```

### 7.2 Sistema Financeiro

```java
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    @GetMapping
    @HasPermission(
        action = "VIEW",
        resource = "TRANSACTION",
        description = "View transactions"
    )
    public Page<TransactionDto> listTransactions(Pageable pageable) {
        return transactionService.findAll(pageable);
    }

    @PostMapping("/{id}/approve")
    @HasPermission(
        action = "APPROVE",
        resource = "TRANSACTION",
        description = "Approve transaction"
    )
    @RequireRole("FINANCIAL_APPROVER")   // exige um ArchbaseRoleResolver na aplicação
    public TransactionDto approveTransaction(@PathVariable String id) {
        return transactionService.approve(id);
    }

    @GetMapping("/audit")
    @HasPermission(
        action = "AUDIT",
        resource = "TRANSACTION",
        description = "Audit transaction history"
    )
    public List<AuditLogDto> auditTransactions(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return transactionService.audit(startDate, endDate);
    }
}
```

### 7.3 Sistema de Conteúdo

```java
@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

    @PostMapping
    @HasPermission(
        action = "CREATE",
        resource = "CONTENT",
        description = "Create content draft"
    )
    public ContentDto createContent(@RequestBody ContentDto content) {
        return contentService.createDraft(content);
    }

    @PostMapping("/{id}/submit-review")
    @HasPermission(
        action = "SUBMIT",
        resource = "CONTENT",
        description = "Submit content for review"
    )
    public ContentDto submitForReview(@PathVariable String id) {
        return contentService.submitForReview(id);
    }

    @PostMapping("/{id}/review")
    @HasPermission(
        action = "REVIEW",
        resource = "CONTENT",
        description = "Review submitted content"
    )
    public ContentDto reviewContent(
            @PathVariable String id,
            @RequestBody ReviewDto review) {
        return contentService.review(id, review);
    }

    @PostMapping("/{id}/publish")
    @HasPermission(
        action = "PUBLISH",
        resource = "CONTENT",
        description = "Publish approved content"
    )
    public ContentDto publishContent(@PathVariable String id) {
        return contentService.publish(id);
    }
}
```

---

## 8. Padrões e Melhores Práticas

### 8.1 Nomenclatura Consistente

**Ações Padrão por Operação**:

| Operação | Ação Recomendada | Alternativas |
|----------|------------------|--------------|
| Leitura de dados | `VIEW` | `READ`, `LIST` |
| Criação de recurso | `CREATE` | `ADD`, `INSERT` |
| Atualização de recurso | `UPDATE` | `EDIT`, `MODIFY` |
| Exclusão de recurso | `DELETE` | `REMOVE`, `DESTROY` |
| Operação especial | `EXECUTE` | `RUN`, `PERFORM` |
| Aprovação | `APPROVE` | `ACCEPT`, `CONFIRM` |
| Rejeição | `REJECT` | `DECLINE`, `DENY` |
| Exportação | `EXPORT` | `DOWNLOAD`, `EXTRACT` |
| Importação | `IMPORT` | `UPLOAD`, `LOAD` |

### 8.2 Granularidade de Permissões

**Nível Adequado de Granularidade**:

```java
// ✓ Granularidade adequada
@HasPermission(action = "VIEW", resource = "USER", description = "View user")
@HasPermission(action = "UPDATE", resource = "USER", description = "Update user")
@HasPermission(action = "DELETE", resource = "USER", description = "Delete user")

// ✗ Muito granular (dificulta gerenciamento)
@HasPermission(action = "VIEW_NAME", resource = "USER", description = "View user name")
@HasPermission(action = "VIEW_EMAIL", resource = "USER", description = "View user email")
@HasPermission(action = "VIEW_PHONE", resource = "USER", description = "View user phone")

// ✗ Pouco granular (controle insuficiente)
@HasPermission(action = "MANAGE", resource = "USER", description = "Manage users")
```

### 8.3 Organização de Recursos

**Hierarquia de Recursos**:

```java
// ✓ Hierarquia clara
INVOICE              → Fatura principal
INVOICE_ITEM         → Item da fatura
INVOICE_PAYMENT      → Pagamento da fatura
INVOICE_ATTACHMENT   → Anexo da fatura

// ✓ Agrupamento lógico
USER                 → Usuário
USER_PROFILE         → Perfil do usuário
USER_PREFERENCES     → Preferências do usuário

// ✗ Sem hierarquia clara
INVOICE
ITEM
PAYMENT
ATTACHMENT
```

### 8.4 Descrições Claras

```java
// ✓ Descrições claras e acionáveis
@HasPermission(
    action = "APPROVE",
    resource = "INVOICE",
    description = "Approve invoice for payment"
)

@HasPermission(
    action = "EXPORT",
    resource = "SALES_REPORT",
    description = "Export sales report to Excel"
)

// ✗ Descrições vagas
@HasPermission(
    action = "APPROVE",
    resource = "INVOICE",
    description = "Do something with invoice"
)
```

---

## 9. Troubleshooting

### 9.1 Permissão Negada Inesperadamente

**Problema**: Método anotado com `@HasPermission` retorna `403 Forbidden`.

**Verificações**:

1. **Por que foi negado?** Ligue `archbase.security.diagnostics.enabled=true` e, como administrador, use
   `POST /api/v1/security/diagnostics/simulate` ou `GET /api/v1/security/diagnostics/users/{id}/effective`.
   A resposta traz o portão que negou e o código do motivo. O log da negação traz o mesmo código.

   A consulta direta abaixo cobre só a permissão dada ao usuário; a concessão também pode vir dos
   grupos ou do perfil dele (`ID_SEGURANCA` do grupo/perfil), e uma linha com `EFFECT='DENY'` vence:
   ```sql
   SELECT p.ID_PERMISSAO, p.EFFECT, p.COMPANY_ID, p.PROJECT_ID
   FROM SEGURANCA_PERMISSAO p
   JOIN SEGURANCA_ACAO a ON p.ID_ACAO = a.ID_ACAO
   JOIN SEGURANCA_RECURSO r ON a.ID_RECURSO = r.ID_RECURSO
   WHERE p.ID_SEGURANCA = '<user_id>'
     AND a.NOME = 'VIEW'
     AND r.NOME = 'USER';
   ```

2. **Contexto está correto?**
   ```java
   String currentTenant = ArchbaseTenantContext.getTenantId();
   System.out.println("Current tenant: " + currentTenant);
   ```

3. **A ação está inativa?** Com `archbase.security.permission.require-active=true`, permissão de ação
   inativa não conta.

4. **O portão de nível está ligado?** Com `archbase.security.access-level.enabled=true`, o nível do perfil
   precisa alcançar o `MINIMUM_LEVEL` da ação.

Não há cache de permissões: a decisão lê o banco a cada requisição.

### 9.2 Anotação Não Está Funcionando

**Problema**: `@HasPermission` parece ser ignorada.

**Soluções**:

1. Verifique se o método é **público**:
   ```java
   // ✓ Correto
   @HasPermission(...)
   public void publicMethod() { }

   // ✗ Não funciona (método privado)
   @HasPermission(...)
   private void privateMethod() { }
   ```

2. Verifique se a classe está sendo gerenciada pelo Spring:
   ```java
   @RestController  // ou @Service, @Component
   public class MyController { }
   ```

3. Não faça chamadas internas (bypass proxy):
   ```java
   // ✗ Não funciona (chamada interna)
   public void methodA() {
       this.methodB();  // Bypass do proxy Spring
   }

   // ✓ Correto (injetar self-reference)
   @Autowired
   private ApplicationContext context;

   public void methodA() {
       MyController self = context.getBean(MyController.class);
       self.methodB();  // Passa pelo proxy
   }
   ```

---

**Ver também**:
- [Configuração](configuration.md) - Propriedades de configuração
- [Exemplos de Código](code-examples.md) - Exemplos completos de uso
- [Melhores Práticas](best-practices.md) - Recomendações de implementação

---

[← Anterior: Configuração](configuration.md) | [Voltar ao Índice](../README.md) | [Próximo: Exemplos de Código →](code-examples.md)
