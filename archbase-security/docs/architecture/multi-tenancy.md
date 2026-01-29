[← Anterior: Sistema de Autenticação](authentication-system.md) | [Voltar ao Índice](../README.md)

---

# Multi-Tenancy

## Introdução

O módulo `archbase-security` foi projetado desde o início para suportar arquiteturas multi-tenant, onde múltiplas organizações (tenants) compartilham a mesma infraestrutura com isolamento completo de dados. Este documento descreve como o contexto de tenant é propagado e como funciona o isolamento multi-nível.

---

## Propagação de Contexto

### ThreadLocal Storage

O contexto do tenant é armazenado em `ArchbaseTenantContext` usando **ThreadLocal**, garantindo isolamento entre requisições concorrentes.

**Localização**: `br.com.archbase.multitenancy.context.ArchbaseTenantContext`

```java
public class ArchbaseTenantContext {
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> COMPANY_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> PROJECT_ID = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void setCompanyId(String companyId) {
        COMPANY_ID.set(companyId);
    }

    public static String getCompanyId() {
        return COMPANY_ID.get();
    }

    public static void setProjectId(String projectId) {
        PROJECT_ID.set(projectId);
    }

    public static String getProjectId() {
        return PROJECT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
        COMPANY_ID.remove();
        PROJECT_ID.remove();
    }
}
```

**Características do ThreadLocal**:
- Cada thread tem sua própria cópia do contexto
- Não há interferência entre requisições concorrentes
- Contexto é isolado por thread

### Filtragem Automática

Todas as entidades de segurança estendem `TenantPersistenceEntityBase`, que inclui o campo `tenantId`. O Hibernate/JPA aplica filtros automáticos baseados no tenant atual.

**Herança de Entidades**:
```java
@MappedSuperclass
public abstract class TenantPersistenceEntityBase<T, ID> extends PersistenceEntityBase<T, ID> {

    @Column(name = "TENANT_ID", length = 36)
    private String tenantId;

    // Getters e setters
}
```

**Todas as entidades principais herdam de `TenantPersistenceEntityBase`**:
- `SecurityEntity` (e suas subclasses: User, Group, Profile)
- `PermissionEntity`
- `ResourceEntity`
- `ActionEntity`
- `AccessTokenEntity`
- Etc.

**Resultado**: Queries automáticas incluem filtro `WHERE TENANT_ID = ?`

### Definição de Contexto

O contexto do tenant é tipicamente definido em um dos seguintes momentos:

#### 1. No Filtro de Autenticação (Recomendado)

Extraído do token JWT ou header HTTP:

```java
@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Opção 1: Extrair do header
        String tenantId = httpRequest.getHeader("X-Tenant-ID");
        String companyId = httpRequest.getHeader("X-Company-ID");
        String projectId = httpRequest.getHeader("X-Project-ID");

        // Opção 2: Extrair do token JWT (mais seguro)
        String jwt = extractJwtFromRequest(httpRequest);
        if (jwt != null) {
            tenantId = jwtService.extractTenantId(jwt);
            companyId = jwtService.extractCompanyId(jwt);
            projectId = jwtService.extractProjectId(jwt);
        }

        // Definir contexto
        if (tenantId != null) {
            ArchbaseTenantContext.setTenantId(tenantId);
        }
        if (companyId != null) {
            ArchbaseTenantContext.setCompanyId(companyId);
        }
        if (projectId != null) {
            ArchbaseTenantContext.setProjectId(projectId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // IMPORTANTE: Limpar contexto após requisição
            ArchbaseTenantContext.clear();
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

**Importante**: Sempre limpar o contexto no bloco `finally` para evitar vazamento de contexto entre requisições.

#### 2. No Início de uma Requisição (Interceptor HTTP)

Usando um `HandlerInterceptor` do Spring MVC:

```java
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) throws Exception {

        // Extrair contexto (do header, JWT, etc.)
        String tenantId = determineTenantId(request);

        if (tenantId != null) {
            ArchbaseTenantContext.setTenantId(tenantId);
        } else {
            // Opcional: Rejeitar requisições sem tenant
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant ID required");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) throws Exception {
        ArchbaseTenantContext.clear();
    }

    private String determineTenantId(HttpServletRequest request) {
        // Lógica para determinar tenant
        // Pode ser de header, JWT, subdomain, etc.
        return request.getHeader("X-Tenant-ID");
    }
}
```

**Registro do Interceptor**:
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/public/**");
    }
}
```

#### 3. Manualmente (Operações Administrativas)

Para operações que precisam acessar dados de múltiplos tenants:

```java
@Service
public class AdminReportService {

    public Report generateCrossTenantReport(List<String> tenantIds) {
        Report report = new Report();

        for (String tenantId : tenantIds) {
            try {
                // Definir contexto temporariamente
                ArchbaseTenantContext.setTenantId(tenantId);

                // Executar operações para este tenant
                List<Data> data = dataRepository.findAll();
                report.addData(tenantId, data);

            } finally {
                // Limpar contexto
                ArchbaseTenantContext.clear();
            }
        }

        return report;
    }
}
```

**Cuidado**: Operações cross-tenant devem ser cuidadosamente auditadas e restritas a administradores.

### Uso na Avaliação de Permissões

O `CustomAuthorizationManager` obtém o contexto automaticamente se não especificado na anotação:

**Localização**: `CustomAuthorizationManager:35-38`

```java
String tenantId = hasPermission.tenantId().isEmpty() ?
    ArchbaseTenantContext.getTenantId() : hasPermission.tenantId();

String companyId = hasPermission.companyId().isEmpty() ?
    ArchbaseTenantContext.getCompanyId() : hasPermission.companyId();

String projectId = hasPermission.projectId().isEmpty() ?
    ArchbaseTenantContext.getProjectId() : hasPermission.projectId();
```

**Lógica**:
1. Se o contexto está vazio na anotação `@HasPermission` → buscar de `ArchbaseTenantContext`
2. Se o contexto está explícito na anotação → usar valor explícito

**Ver**: [Sistema de Permissões](permissions-system.md) para detalhes sobre avaliação.

### Propagação em Operações Assíncronas

Para tarefas assíncronas (threads separadas), o contexto **não é propagado automaticamente** pelo ThreadLocal. É necessário usar um `TaskDecorator` customizado.

#### Configuração de Async com Propagação de Contexto

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new TenantAwareTaskDecorator());
        executor.initialize();
        return executor;
    }
}
```

#### TenantAwareTaskDecorator

```java
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capturar contexto da thread atual (antes de executar async)
        String tenantId = ArchbaseTenantContext.getTenantId();
        String companyId = ArchbaseTenantContext.getCompanyId();
        String projectId = ArchbaseTenantContext.getProjectId();

        return () -> {
            try {
                // Restaurar contexto na nova thread
                ArchbaseTenantContext.setTenantId(tenantId);
                ArchbaseTenantContext.setCompanyId(companyId);
                ArchbaseTenantContext.setProjectId(projectId);

                // Executar tarefa
                runnable.run();

            } finally {
                // Limpar contexto
                ArchbaseTenantContext.clear();
            }
        };
    }
}
```

#### Uso em Serviços Async

```java
@Service
public class NotificationService {

    @Async
    public void sendNotification(String userId, String message) {
        // O contexto de tenant foi propagado automaticamente
        // pelo TenantAwareTaskDecorator

        String currentTenant = ArchbaseTenantContext.getTenantId();
        logger.info("Sending notification to user {} in tenant {}", userId, currentTenant);

        // Lógica de envio de notificação
    }
}
```

**Importante**: Sem o `TenantAwareTaskDecorator`, o contexto seria `null` na thread async.

---

## Isolamento Multi-Nível

O sistema suporta **três níveis hierárquicos** de isolamento de dados:

```
Tenant (Organização)
  └── Company (Empresa)
       └── Project (Projeto)
```

### Nível 1: Tenant (Organização)

**Isolamento Completo de Dados**

Dois tenants diferentes **nunca** compartilham dados. Este é o nível mais forte de isolamento.

**Casos de Uso**:
- Aplicações SaaS multi-inquilino
- Plataformas com múltiplas organizações independentes
- Sistemas onde cada cliente é uma organização distinta

**Exemplo**:
```
Tenant: "ACME Corp"
  - Usuários: user1@acme.com, user2@acme.com
  - Dados: Completamente isolados do Tenant "XYZ Ltd"

Tenant: "XYZ Ltd"
  - Usuários: admin@xyz.com, operator@xyz.com
  - Dados: Completamente isolados do Tenant "ACME Corp"
```

**Configuração**:
```properties
archbase.multitenancy.enabled=true
archbase.multitenancy.mode=database  # ou schema, ou shared
```

**Implementação no Código**:
```java
// Todas as queries automáticas incluem filtro de tenant
List<UserEntity> users = userRepository.findAll();
// SQL gerado: SELECT * FROM SEGURANCA WHERE TP_SEGURANCA='USUARIO' AND TENANT_ID=?
```

### Nível 2: Company (Empresa)

**Sub-Organização dentro do Tenant**

Uma organização (tenant) pode ter múltiplas empresas. Cada empresa é uma subdivisão lógica com seus próprios dados e usuários.

**Casos de Uso**:
- Corporações com múltiplas subsidiárias
- Grupos empresariais com filiais independentes
- Holdings com empresas separadas

**Exemplo**:
```
Tenant: "Grupo XYZ"
  ├── Company: "XYZ Brasil Ltda"
  │    ├── Usuários: 50
  │    └── Dados: Isolados de outras companies
  │
  ├── Company: "XYZ Argentina SA"
  │    ├── Usuários: 30
  │    └── Dados: Isolados de outras companies
  │
  └── Company: "XYZ Chile SpA"
       ├── Usuários: 20
       └── Dados: Isolados de outras companies
```

**Configuração de Permissões**:
```java
// Usuário com acesso a uma company específica
PermissionEntity permission = PermissionEntity.builder()
    .security(user)
    .action(viewAction)
    .tenantId("GRUPO_XYZ")
    .companyId("XYZ_BRASIL")  // Apenas esta company
    .projectId(null)          // Todos os projetos
    .build();
```

**Query com Filtro de Company**:
```java
@HasPermission(
    action = "VIEW",
    resource = "SALES_DATA",
    companyId = "XYZ_BRASIL"  // Contexto explícito
)
public List<Sale> getSalesByCompany() {
    return saleRepository.findAll();
}
```

### Nível 3: Project (Projeto)

**Granularidade Mais Fina**

Dentro de uma empresa, múltiplos projetos podem existir com dados e permissões segregadas.

**Casos de Uso**:
- Gestão de projetos de consultoria
- Desenvolvimento de software com múltiplos clientes
- Gestão de obras/construção
- Projetos de pesquisa em universidades

**Exemplo**:
```
Tenant: "Consultoria ABC"
  └── Company: "ABC Brasil"
       ├── Project: "Implementação ERP - Cliente A"
       │    ├── Equipe: 5 consultores
       │    └── Dados: Isolados de outros projetos
       │
       ├── Project: "Manutenção CRM - Cliente B"
       │    ├── Equipe: 3 consultores
       │    └── Dados: Isolados de outros projetos
       │
       └── Project: "Migração Cloud - Cliente C"
            ├── Equipe: 7 consultores
            └── Dados: Isolados de outros projetos
```

**Configuração de Permissões**:
```java
// Usuário com acesso apenas a um projeto específico
PermissionEntity permission = PermissionEntity.builder()
    .security(contractorUser)
    .action(viewAction)
    .tenantId("CONSULTORIA_ABC")
    .companyId("ABC_BRASIL")
    .projectId("PROJ_ERP_CLIENTE_A")  // Apenas este projeto
    .build();
```

**Query com Filtro de Project**:
```java
@HasPermission(
    action = "EDIT",
    resource = "PROJECT_DOCUMENT"
    // Contextos obtidos automaticamente de ArchbaseTenantContext
)
public Document editDocument(String docId, Document updates) {
    // Apenas documentos do projeto atual são acessíveis
    return documentRepository.findById(docId);
}
```

### Matriz de Isolamento

| Nível | Tenant | Company | Project | Isolamento | Uso Típico |
|-------|--------|---------|---------|------------|------------|
| **Tenant** | ✓ | - | - | Completo | SaaS multi-inquilino |
| **Company** | ✓ | ✓ | - | Por Empresa | Grupos empresariais |
| **Project** | ✓ | ✓ | ✓ | Por Projeto | Consultoria, projetos |

**Legenda**:
- ✓ = Campo especificado (isolamento ativo)
- \- = Campo null (wildcard)

### Configuração de Permissões Multi-Nível

#### Permissão Global (Sem Isolamento)

```java
PermissionEntity permission = PermissionEntity.builder()
    .security(adminUser)
    .action(manageAction)
    .tenantId(null)    // Qualquer tenant
    .companyId(null)   // Qualquer empresa
    .projectId(null)   // Qualquer projeto
    .build();
```

**Uso**: Administradores globais, serviços de sistema.

#### Permissão de Tenant

```java
PermissionEntity permission = PermissionEntity.builder()
    .security(tenantAdmin)
    .action(manageUsersAction)
    .tenantId("ACME_CORP")  // Apenas este tenant
    .companyId(null)        // Todas as empresas
    .projectId(null)        // Todos os projetos
    .build();
```

**Uso**: Administradores de tenant, gerentes de organização.

#### Permissão de Empresa

```java
PermissionEntity permission = PermissionEntity.builder()
    .security(companyManager)
    .action(viewReportsAction)
    .tenantId("ACME_CORP")
    .companyId("ACME_BRASIL")  // Apenas esta empresa
    .projectId(null)           // Todos os projetos
    .build();
```

**Uso**: Gerentes de filial, supervisores de empresa.

#### Permissão de Projeto

```java
PermissionEntity permission = PermissionEntity.builder()
    .security(projectLead)
    .action(editDocumentsAction)
    .tenantId("ACME_CORP")
    .companyId("ACME_BRASIL")
    .projectId("PROJ_2024_001")  // Apenas este projeto
    .build();
```

**Uso**: Líderes de projeto, membros de equipe específica.

---

## Boas Práticas de Multi-Tenancy

### 1. Sempre Limpar Contexto

```java
try {
    ArchbaseTenantContext.setTenantId(tenantId);
    // Operações
} finally {
    ArchbaseTenantContext.clear();  // SEMPRE limpar
}
```

### 2. Validar Tenant no Início da Requisição

```java
@Component
public class TenantValidationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String tenantId = extractTenantId(request);

        // Validar que o tenant existe
        if (tenantId == null || !tenantRepository.existsById(tenantId)) {
            ((HttpServletResponse) response).sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "Invalid or missing tenant"
            );
            return;
        }

        ArchbaseTenantContext.setTenantId(tenantId);

        try {
            chain.doFilter(request, response);
        } finally {
            ArchbaseTenantContext.clear();
        }
    }
}
```

### 3. Usar TaskDecorator para Operações Async

Sempre configurar `TenantAwareTaskDecorator` para propagar contexto em threads assíncronas.

### 4. Auditar Operações Cross-Tenant

```java
@Aspect
@Component
public class CrossTenantAuditAspect {

    @Around("@annotation(CrossTenant)")
    public Object auditCrossTenantAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        logger.warn("Cross-tenant operation by user: {}, method: {}",
            currentUser, joinPoint.getSignature());

        // Executar operação
        return joinPoint.proceed();
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossTenant {
}
```

### 5. Incluir Tenant em Logs

```java
@Slf4j
@Component
public class TenantLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String tenantId = ArchbaseTenantContext.getTenantId();

        // Adicionar tenant ao MDC (Mapped Diagnostic Context)
        MDC.put("tenantId", tenantId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("tenantId");
        }
    }
}
```

**Resultado em Logs**:
```
2024-01-15 10:30:45.123 [tenantId=ACME_CORP] INFO  UserService - Creating new user
```

---

## Configuração

```properties
# Habilitar multi-tenancy
archbase.multitenancy.enabled=true

# Modo de isolamento (database, schema, ou shared)
archbase.multitenancy.mode=shared

# Packages a serem escaneados para entidades multi-tenant
archbase.multitenancy.scan-packages=com.myapp.domain

# Header HTTP para tenant ID (padrão: X-Tenant-ID)
archbase.multitenancy.tenant-header=X-Tenant-ID
```

---

## Resumo

| Aspecto | Descrição |
|---------|-----------|
| **Armazenamento de Contexto** | ThreadLocal (`ArchbaseTenantContext`) |
| **Propagação** | Automática na mesma thread, manual em async |
| **Filtragem** | Automática via `TenantPersistenceEntityBase` |
| **Níveis de Isolamento** | Tenant, Company, Project |
| **Limpeza de Contexto** | Obrigatória no `finally` block |

---

**Ver também**:
- [Sistema de Permissões](permissions-system.md) - Como contextos são usados em permissões
- [Entidades Core](../entities/core-entities.md) - TenantPersistenceEntityBase
- [Melhores Práticas](../guides/best-practices.md) - Recomendações de multi-tenancy

---

[← Anterior: Sistema de Autenticação](authentication-system.md) | [Voltar ao Índice](../README.md)
