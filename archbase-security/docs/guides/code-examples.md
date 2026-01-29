# Exemplos de Código Completos

[← Anterior: Anotações e Uso](annotations-and-usage.md) | [Voltar ao Índice](../README.md) | [Próximo: Melhores Práticas →](best-practices.md)

---

## Visão Geral

Este guia fornece exemplos completos e executáveis demonstrando os principais casos de uso do módulo `archbase-security`. Cada exemplo inclui código completo, explicações detalhadas e dicas de implementação.

### Exemplos Cobertos

1. **[Criação de Usuário com Permissões](#1-criação-de-usuário-com-permissões)** - Setup completo de entidades de segurança
2. **[Uso de @HasPermission em Controllers](#2-uso-de-haspermission-em-controllers)** - Proteção de endpoints REST
3. **[Configuração Customizada de Segurança](#3-configuração-customizada-de-segurança)** - SecurityConfig avançada
4. **[Trabalho com API Tokens](#4-trabalho-com-api-tokens)** - Integração service-to-service
5. **[Configuração de Horários de Acesso](#5-configuração-de-horários-de-acesso)** - Restrições temporais

---

## 1. Criação de Usuário com Permissões

Este exemplo demonstra como criar um usuário completo com recursos, ações e permissões associadas. É ideal para entender o fluxo de setup inicial de entidades de segurança.

### 1.1 Service de Setup Completo

```java
package com.example.security.service;

import br.com.archbase.security.persistence.*;
import br.com.archbase.security.repository.*;
import br.com.archbase.ddd.domain.annotations.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service para criação e configuração de usuários com permissões.
 *
 * Este service encapsula toda a lógica de criação de entidades de segurança,
 * incluindo recursos, ações, usuários e permissões, seguindo o padrão
 * Domain-Driven Design.
 */
@Service
@DomainService
public class SecuritySetupService {

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ResourceJpaRepository resourceRepository;

    @Autowired
    private ActionJpaRepository actionRepository;

    @Autowired
    private PermissionJpaRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Cria um usuário completo com permissões de gerenciamento.
     *
     * Este método demonstra o fluxo completo:
     * 1. Criação de recurso (o que está sendo protegido)
     * 2. Criação de ações no recurso (o que pode ser feito)
     * 3. Criação do usuário
     * 4. Atribuição de permissões ao usuário
     *
     * @return ID do usuário criado
     */
    @Transactional
    public String createUserWithPermissions() {
        // === PASSO 1: Criar Recurso ===
        // Recursos representam entidades ou funcionalidades da aplicação
        ResourceEntity resource = ResourceEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("RES_USER_MANAGEMENT")
            .name("USER_MANAGEMENT")
            .description("Gerenciamento de Usuários do Sistema")
            .active(true)
            .type(TipoRecurso.API)
            .tenantId("TENANT_ABC")
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        resourceRepository.save(resource);

        // === PASSO 2: Criar Ações no Recurso ===
        // Ações definem operações específicas que podem ser realizadas no recurso

        // Ação VIEW - Para consulta de dados
        ActionEntity viewAction = ActionEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("ACT_VIEW_USER")
            .name("VIEW")
            .description("Visualizar usuários do sistema")
            .resource(resource)
            .category("READ")
            .active(true)
            .tenantId("TENANT_ABC")
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        actionRepository.save(viewAction);

        // Ação CREATE - Para criação de novos registros
        ActionEntity createAction = ActionEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("ACT_CREATE_USER")
            .name("CREATE")
            .description("Criar novos usuários no sistema")
            .resource(resource)
            .category("WRITE")
            .active(true)
            .tenantId("TENANT_ABC")
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        actionRepository.save(createAction);

        // Ação UPDATE - Para modificação de registros existentes
        ActionEntity updateAction = ActionEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("ACT_UPDATE_USER")
            .name("UPDATE")
            .description("Atualizar dados de usuários existentes")
            .resource(resource)
            .category("WRITE")
            .active(true)
            .tenantId("TENANT_ABC")
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        actionRepository.save(updateAction);

        // Ação DELETE - Para remoção de registros
        ActionEntity deleteAction = ActionEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("ACT_DELETE_USER")
            .name("DELETE")
            .description("Excluir usuários do sistema")
            .resource(resource)
            .category("DELETE")
            .active(true)
            .tenantId("TENANT_ABC")
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        actionRepository.save(deleteAction);

        // === PASSO 3: Criar Usuário ===
        // Usuários são as entidades de segurança que podem fazer login
        UserEntity user = UserEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("USR_JOHN_DOE")
            .name("John Doe")
            .description("Gerente de TI - Responsável pelo gerenciamento de usuários")
            .userName("john.doe")
            .email("john.doe@example.com")
            .password(passwordEncoder.encode("SenhaSegura@2024"))
            .isAdministrator(false)
            .accountDeactivated(false)
            .accountLocked(false)
            .allowPasswordChange(true)
            .allowMultipleLogins(false)
            .passwordNeverExpires(false)
            .unlimitedAccessHours(true)
            .changePasswordOnNextLogin(false)
            .tenantId("TENANT_ABC")
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        userRepository.save(user);

        // === PASSO 4: Atribuir Permissões ao Usuário ===
        // Permissões conectam usuários a ações específicas em recursos

        // Permissão VIEW - Escopo: toda a empresa COMPANY_BR
        // Esta permissão permite visualizar usuários em todos os projetos da empresa
        PermissionEntity viewPermission = PermissionEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("PERM_VIEW_USER_BR")
            .security(user)
            .action(viewAction)
            .tenantId("TENANT_ABC")
            .companyId("COMPANY_BR")
            .projectId(null)  // null = Todos os projetos da empresa
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        permissionRepository.save(viewPermission);

        // Permissão CREATE - Escopo: projeto específico PROJECT_001
        // Esta permissão permite criar usuários apenas no projeto 001
        PermissionEntity createPermission = PermissionEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("PERM_CREATE_USER_PROJ1")
            .security(user)
            .action(createAction)
            .tenantId("TENANT_ABC")
            .companyId("COMPANY_BR")
            .projectId("PROJECT_001")  // Restrito ao projeto 001
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        permissionRepository.save(createPermission);

        // Permissão UPDATE - Escopo: projeto específico PROJECT_001
        PermissionEntity updatePermission = PermissionEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("PERM_UPDATE_USER_PROJ1")
            .security(user)
            .action(updateAction)
            .tenantId("TENANT_ABC")
            .companyId("COMPANY_BR")
            .projectId("PROJECT_001")
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        permissionRepository.save(updatePermission);

        System.out.println("✓ Usuário criado com sucesso!");
        System.out.println("  - Username: john.doe");
        System.out.println("  - Email: john.doe@example.com");
        System.out.println("  - Pode VISUALIZAR usuários em toda empresa COMPANY_BR");
        System.out.println("  - Pode CRIAR usuários apenas no PROJECT_001");
        System.out.println("  - Pode ATUALIZAR usuários apenas no PROJECT_001");

        return user.getId();
    }

    /**
     * Cria um usuário administrador com permissões globais.
     *
     * Administradores têm acesso irrestrito e não necessitam permissões
     * específicas, porém é boa prática criar permissões explícitas para
     * auditoria e controle granular.
     *
     * @param tenantId ID do tenant
     * @return ID do usuário administrador
     */
    @Transactional
    public String createAdminUser(String tenantId) {
        UserEntity admin = UserEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("USR_ADMIN")
            .name("Administrador do Sistema")
            .description("Usuário com privilégios administrativos completos")
            .userName("admin")
            .email("admin@example.com")
            .password(passwordEncoder.encode("AdminPass@2024"))
            .isAdministrator(true)  // Flag de administrador
            .accountDeactivated(false)
            .accountLocked(false)
            .allowPasswordChange(true)
            .allowMultipleLogins(true)
            .passwordNeverExpires(true)
            .unlimitedAccessHours(true)
            .changePasswordOnNextLogin(false)
            .tenantId(tenantId)
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();

        userRepository.save(admin);

        System.out.println("✓ Usuário administrador criado!");
        System.out.println("  - Username: admin");
        System.out.println("  - Acesso irrestrito a todos os recursos");

        return admin.getId();
    }

    /**
     * Cria um usuário com acesso somente leitura.
     *
     * Útil para relatórios, auditorias e visualizações sem riscos
     * de modificação de dados.
     *
     * @param tenantId ID do tenant
     * @param companyId ID da empresa (opcional)
     * @return ID do usuário criado
     */
    @Transactional
    public String createReadOnlyUser(String tenantId, String companyId) {
        // Buscar ou criar recurso
        ResourceEntity resource = resourceRepository
            .findByNameAndTenantId("USER_MANAGEMENT", tenantId)
            .orElseGet(() -> {
                ResourceEntity newResource = ResourceEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .code("RES_USER_MANAGEMENT")
                    .name("USER_MANAGEMENT")
                    .description("Gerenciamento de Usuários")
                    .active(true)
                    .type(TipoRecurso.API)
                    .tenantId(tenantId)
                    .createEntityDate(LocalDateTime.now())
                    .createdByUser("SYSTEM")
                    .build();
                return resourceRepository.save(newResource);
            });

        // Buscar ou criar ação VIEW
        ActionEntity viewAction = actionRepository
            .findByNameAndResourceId("VIEW", resource.getId())
            .orElseGet(() -> {
                ActionEntity newAction = ActionEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .code("ACT_VIEW_USER")
                    .name("VIEW")
                    .description("Visualizar usuários")
                    .resource(resource)
                    .category("READ")
                    .active(true)
                    .tenantId(tenantId)
                    .createEntityDate(LocalDateTime.now())
                    .createdByUser("SYSTEM")
                    .build();
                return actionRepository.save(newAction);
            });

        // Criar usuário somente leitura
        UserEntity readOnlyUser = UserEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("USR_READONLY")
            .name("Usuário Somente Leitura")
            .description("Acesso apenas para visualização de dados")
            .userName("readonly.user")
            .email("readonly@example.com")
            .password(passwordEncoder.encode("ReadOnly@2024"))
            .isAdministrator(false)
            .accountDeactivated(false)
            .accountLocked(false)
            .allowPasswordChange(true)
            .allowMultipleLogins(true)
            .passwordNeverExpires(false)
            .unlimitedAccessHours(true)
            .changePasswordOnNextLogin(false)
            .tenantId(tenantId)
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        userRepository.save(readOnlyUser);

        // Criar permissão somente leitura
        PermissionEntity viewPermission = PermissionEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("PERM_VIEW_READONLY")
            .security(readOnlyUser)
            .action(viewAction)
            .tenantId(tenantId)
            .companyId(companyId)
            .projectId(null)
            .createEntityDate(LocalDateTime.now())
            .createdByUser("SYSTEM")
            .build();
        permissionRepository.save(viewPermission);

        System.out.println("✓ Usuário somente leitura criado!");
        System.out.println("  - Username: readonly.user");
        System.out.println("  - Pode apenas VISUALIZAR dados");

        return readOnlyUser.getId();
    }
}
```

### 1.2 Exemplo de Uso do Service

```java
package com.example.security;

import com.example.security.service.SecuritySetupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class SecurityApplication {

    @Autowired
    private SecuritySetupService setupService;

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }

    /**
     * Executa setup inicial apenas em perfil de desenvolvimento.
     * Em produção, use migrations ou scripts SQL.
     */
    @Bean
    @Profile("dev")
    public CommandLineRunner initializeSecurityData() {
        return args -> {
            System.out.println("=== Inicializando Dados de Segurança ===");

            // Criar usuário com permissões
            String userId = setupService.createUserWithPermissions();
            System.out.println("Usuário criado: " + userId);

            // Criar usuário administrador
            String adminId = setupService.createAdminUser("TENANT_ABC");
            System.out.println("Admin criado: " + adminId);

            // Criar usuário somente leitura
            String readOnlyId = setupService.createReadOnlyUser("TENANT_ABC", "COMPANY_BR");
            System.out.println("Usuário readonly criado: " + readOnlyId);

            System.out.println("=== Setup Concluído ===");
        };
    }
}
```

### 1.3 Dicas de Implementação

**Geração de IDs**:
```java
// Use UUID v4 para garantir unicidade global
String id = UUID.randomUUID().toString();

// Ou use uma estratégia customizada baseada em timestamp + random
String customId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
    + "-" + UUID.randomUUID().toString().substring(0, 8);
```

**Encoding de Senhas**:
```java
// Sempre use BCrypt com força adequada (10-12)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

// NUNCA armazene senhas em texto plano
String hashedPassword = passwordEncoder.encode(plainPassword);
```

**Transações**:
```java
// Use @Transactional para garantir atomicidade
@Transactional
public void createComplexSetup() {
    // Se qualquer operação falhar, todas são revertidas
    createResource();
    createActions();
    createUser();
    createPermissions();
}
```

---

## 2. Uso de @HasPermission em Controllers

Este exemplo demonstra como proteger endpoints REST usando a anotação `@HasPermission`, implementando controle de acesso fino para operações CRUD.

### 2.1 Controller REST Completo

```java
package com.example.api.controller;

import br.com.archbase.security.annotation.HasPermission;
import com.example.api.dto.UserDto;
import com.example.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gerenciamento de usuários.
 *
 * Todos os endpoints são protegidos por permissões específicas usando
 * a anotação @HasPermission. O contexto de tenant/company/project é
 * obtido automaticamente do ArchbaseTenantContext.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "API de Gerenciamento de Usuários")
public class UserManagementController {

    @Autowired
    private UserService userService;

    /**
     * Lista todos os usuários com paginação.
     *
     * Requer: Permissão VIEW no recurso USER_MANAGEMENT
     * Contexto: Tenant, Company e Project são obtidos automaticamente
     */
    @GetMapping
    @HasPermission(
        action = "VIEW",
        resource = "USER_MANAGEMENT",
        description = "List all users with pagination in current context"
    )
    @Operation(
        summary = "List users",
        description = "Returns a paginated list of users in the current context"
    )
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    public ResponseEntity<Page<UserDto>> listUsers(
            @Parameter(description = "Pagination parameters")
            Pageable pageable) {

        Page<UserDto> users = userService.findAll(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Busca usuário por ID.
     *
     * Requer: Permissão VIEW no recurso USER_MANAGEMENT
     */
    @GetMapping("/{id}")
    @HasPermission(
        action = "VIEW",
        resource = "USER_MANAGEMENT",
        description = "Get user by ID"
    )
    @Operation(summary = "Get user", description = "Returns user details by ID")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "User ID")
            @PathVariable String id) {

        UserDto user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Busca usuário por username.
     *
     * Requer: Permissão VIEW no recurso USER_MANAGEMENT
     */
    @GetMapping("/by-username/{username}")
    @HasPermission(
        action = "VIEW",
        resource = "USER_MANAGEMENT",
        description = "Get user by username"
    )
    @Operation(summary = "Get user by username")
    public ResponseEntity<UserDto> getUserByUsername(
            @Parameter(description = "Username")
            @PathVariable String username) {

        UserDto user = userService.findByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Cria novo usuário.
     *
     * Requer: Permissão CREATE no recurso USER_MANAGEMENT
     */
    @PostMapping
    @HasPermission(
        action = "CREATE",
        resource = "USER_MANAGEMENT",
        description = "Create new user"
    )
    @Operation(summary = "Create user", description = "Creates a new user")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    public ResponseEntity<UserDto> createUser(
            @Parameter(description = "User data")
            @RequestBody @Valid UserDto userDto) {

        UserDto createdUser = userService.create(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Atualiza dados do usuário.
     *
     * Requer: Permissão UPDATE no recurso USER_MANAGEMENT
     */
    @PutMapping("/{id}")
    @HasPermission(
        action = "UPDATE",
        resource = "USER_MANAGEMENT",
        description = "Update user data"
    )
    @Operation(summary = "Update user")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "User ID")
            @PathVariable String id,
            @Parameter(description = "Updated user data")
            @RequestBody @Valid UserDto userDto) {

        UserDto updatedUser = userService.update(id, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Atualização parcial de usuário (PATCH).
     *
     * Requer: Permissão UPDATE no recurso USER_MANAGEMENT
     */
    @PatchMapping("/{id}")
    @HasPermission(
        action = "UPDATE",
        resource = "USER_MANAGEMENT",
        description = "Partial update of user data"
    )
    @Operation(summary = "Partial update user")
    public ResponseEntity<UserDto> patchUser(
            @PathVariable String id,
            @RequestBody UserDto userDto) {

        UserDto patchedUser = userService.patch(id, userDto);
        return ResponseEntity.ok(patchedUser);
    }

    /**
     * Exclui usuário.
     *
     * Requer: Permissão DELETE no recurso USER_MANAGEMENT
     */
    @DeleteMapping("/{id}")
    @HasPermission(
        action = "DELETE",
        resource = "USER_MANAGEMENT",
        description = "Delete user"
    )
    @Operation(summary = "Delete user")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID")
            @PathVariable String id) {

        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exporta relatório de usuários em PDF.
     *
     * Requer: Permissão EXPORT no recurso USER_MANAGEMENT
     * Contexto: Empresa fixada em COMPANY_BR
     */
    @GetMapping("/export")
    @HasPermission(
        action = "EXPORT",
        resource = "USER_MANAGEMENT",
        description = "Export users report to PDF",
        companyId = "COMPANY_BR"  // Contexto fixo
    )
    @Operation(summary = "Export users report")
    @ApiResponse(responseCode = "200", description = "Report generated successfully")
    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    public ResponseEntity<byte[]> exportUsers() {
        byte[] report = userService.generatePdfReport();

        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=users-report.pdf")
            .body(report);
    }

    /**
     * Ativa/desativa conta de usuário.
     *
     * Requer: Permissão ACTIVATE no recurso USER_MANAGEMENT
     */
    @PutMapping("/{id}/active")
    @HasPermission(
        action = "ACTIVATE",
        resource = "USER_MANAGEMENT",
        description = "Activate or deactivate user account"
    )
    @Operation(summary = "Toggle user active status")
    public ResponseEntity<UserDto> toggleUserActive(
            @PathVariable String id,
            @Parameter(description = "Active status")
            @RequestParam boolean active) {

        UserDto updatedUser = userService.setActive(id, active);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Bloqueia/desbloqueia conta de usuário.
     *
     * Requer: Permissão LOCK no recurso USER_MANAGEMENT
     */
    @PutMapping("/{id}/lock")
    @HasPermission(
        action = "LOCK",
        resource = "USER_MANAGEMENT",
        description = "Lock or unlock user account"
    )
    @Operation(summary = "Toggle user lock status")
    public ResponseEntity<UserDto> toggleUserLock(
            @PathVariable String id,
            @Parameter(description = "Lock status")
            @RequestParam boolean locked) {

        UserDto updatedUser = userService.setLocked(id, locked);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Força alteração de senha no próximo login.
     *
     * Requer: Permissão UPDATE no recurso USER_MANAGEMENT
     */
    @PutMapping("/{id}/force-password-change")
    @HasPermission(
        action = "UPDATE",
        resource = "USER_MANAGEMENT",
        description = "Force password change on next login"
    )
    @Operation(summary = "Force password change")
    public ResponseEntity<Void> forcePasswordChange(@PathVariable String id) {
        userService.forcePasswordChange(id);
        return ResponseEntity.ok().build();
    }
}
```

### 2.2 Controller com Contexto Explícito

```java
package com.example.api.controller;

import br.com.archbase.security.annotation.HasPermission;
import com.example.api.dto.ProjectDto;
import com.example.api.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller com contextos explícitos em alguns endpoints.
 *
 * Útil quando endpoints específicos requerem permissões em
 * contextos fixos independente do usuário autenticado.
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * Lista projetos globais (sem restrição de company).
     *
     * Contexto: Apenas tenant é verificado, company e project são ignorados
     */
    @GetMapping("/global")
    @HasPermission(
        action = "VIEW",
        resource = "PROJECT",
        description = "View global projects across all companies"
        // companyId e projectId omitidos = contexto global no tenant
    )
    public ResponseEntity<List<ProjectDto>> listGlobalProjects() {
        List<ProjectDto> projects = projectService.findGlobalProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * Lista projetos de uma empresa específica.
     *
     * Contexto: Empresa BR fixa
     */
    @GetMapping("/company-br")
    @HasPermission(
        action = "VIEW",
        resource = "PROJECT",
        description = "View projects in company BR only",
        companyId = "COMPANY_BR"  // Contexto fixo
    )
    public ResponseEntity<List<ProjectDto>> listCompanyBRProjects() {
        List<ProjectDto> projects = projectService.findByCompany("COMPANY_BR");
        return ResponseEntity.ok(projects);
    }

    /**
     * Operação administrativa - requer permissão em contexto de admin.
     *
     * Contexto: Tenant ADMIN, Company ADMIN
     */
    @PostMapping("/admin/maintenance")
    @HasPermission(
        action = "EXECUTE",
        resource = "ADMIN_OPERATIONS",
        description = "Execute administrative maintenance",
        tenantId = "ADMIN_TENANT",
        companyId = "ADMIN_COMPANY"
    )
    public ResponseEntity<Void> executeMaintenanceTask() {
        projectService.runMaintenanceTask();
        return ResponseEntity.ok().build();
    }
}
```

### 2.3 Dicas de Implementação

**Propagação de Contexto**:
```java
// O contexto é propagado automaticamente via ArchbaseTenantContext
// Não é necessário passá-lo explicitamente nos parâmetros

@GetMapping
@HasPermission(action = "VIEW", resource = "USER")
public ResponseEntity<List<UserDto>> list() {
    // userService automaticamente usa o contexto corrente
    return ResponseEntity.ok(userService.findAll());
}
```

**Validação de Entrada**:
```java
// Use @Valid para validar DTOs automaticamente
@PostMapping
@HasPermission(action = "CREATE", resource = "USER")
public ResponseEntity<UserDto> create(@RequestBody @Valid UserDto dto) {
    // Se validação falhar, retorna 400 Bad Request automaticamente
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.create(dto));
}
```

**Tratamento de Exceções**:
```java
@ControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {
        ErrorResponse error = new ErrorResponse(
            "ACCESS_DENIED",
            "Você não tem permissão para acessar este recurso"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(PermissionEvaluationException.class)
    public ResponseEntity<ErrorResponse> handlePermissionEvaluation(
            PermissionEvaluationException ex) {
        ErrorResponse error = new ErrorResponse(
            "PERMISSION_ERROR",
            "Erro ao avaliar permissões: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}
```

---

## 3. Configuração Customizada de Segurança

Este exemplo demonstra como criar uma configuração de segurança avançada, incluindo SecurityFilterChain, CORS, autenticação customizada e configurações de produção.

### 3.1 Configuração Principal

```java
package com.example.config;

import br.com.archbase.security.filter.ArchbaseJwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração customizada de segurança para a aplicação.
 *
 * Esta configuração implementa:
 * - JWT authentication filter
 * - CORS configuration (diferente para dev/prod)
 * - Whitelist de endpoints públicos
 * - Session management stateless
 * - HTTPS enforcement (produção)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true,
    prePostEnabled = true
)
public class ApplicationSecurityConfig {

    @Autowired
    private ArchbaseJwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private Environment environment;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Value("${app.security.require-https:false}")
    private boolean requireHttps;

    /**
     * Configuração principal do SecurityFilterChain.
     *
     * Define:
     * - Endpoints públicos e protegidos
     * - Políticas de sessão
     * - Filtros de autenticação
     * - Configurações de HTTPS
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Desabilitar CSRF (stateless API)
            .csrf(csrf -> csrf.disable())

            // Configurar CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configurar autorização de requisições
            .authorizeHttpRequests(auth -> auth
                // === Endpoints Públicos ===
                // Autenticação
                .requestMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh-token"
                ).permitAll()

                // Recuperação de senha
                .requestMatchers(
                    "/api/v1/auth/forgot-password",
                    "/api/v1/auth/reset-password/**"
                ).permitAll()

                // Health checks
                .requestMatchers(
                    "/health",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info"
                ).permitAll()

                // Documentação Swagger/OpenAPI
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**"
                ).permitAll()

                // Recursos estáticos (se houver)
                .requestMatchers(
                    "/static/**",
                    "/public/**",
                    "/assets/**"
                ).permitAll()

                // === Endpoints Protegidos ===
                // Todos os outros endpoints requerem autenticação
                .anyRequest().authenticated()
            )

            // Configurar gerenciamento de sessão (stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Configurar provider de autenticação
            .authenticationProvider(authenticationProvider)

            // Adicionar filtro JWT antes do UsernamePasswordAuthenticationFilter
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        // Forçar HTTPS em produção
        if (requireHttps) {
            http.requiresChannel(channel -> channel
                .anyRequest().requiresSecure()
            );
        }

        return http.build();
    }

    /**
     * Configuração de CORS baseada no ambiente.
     *
     * Desenvolvimento: Permite origens configuradas (geralmente localhost)
     * Produção: Apenas domínios específicos da aplicação
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origens permitidas
        if (isDevEnvironment()) {
            // Dev: Permitir localhost e portas comuns
            configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080"
            ));
        } else {
            // Prod: Apenas domínios configurados
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        }

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Permitir credenciais (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Headers expostos ao cliente
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition",
            "X-Total-Count"
        ));

        // Cache de preflight requests (em segundos)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configuração do PasswordEncoder.
     *
     * BCrypt com força 12 para equilíbrio entre segurança e performance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Força 12 = ~300ms por hash (bom equilíbrio)
        // Força 10 = ~75ms (menos seguro, mais rápido)
        // Força 14 = ~1200ms (mais seguro, mais lento)
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configuração do AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configuração do AuthenticationProvider customizado.
     *
     * Usa UserDetailsService do Archbase e PasswordEncoder configurado.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authProvider =
            new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        // Esconder informação se usuário não existe (segurança)
        authProvider.setHideUserNotFoundExceptions(true);

        return authProvider;
    }

    /**
     * Verifica se está em ambiente de desenvolvimento.
     */
    private boolean isDevEnvironment() {
        return Arrays.asList(environment.getActiveProfiles())
            .contains("dev");
    }
}
```

### 3.2 Configuração de Logging de Segurança

```java
package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Listener para eventos de segurança.
 *
 * Registra tentativas de login, falhas de autenticação e
 * acessos negados para auditoria e monitoramento.
 */
@Component
public class SecurityEventsListener {

    private static final Logger logger =
        LoggerFactory.getLogger(SecurityEventsListener.class);

    @EventListener
    public void onAuthenticationSuccess(
            AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        logger.info("Login bem-sucedido: usuário={}", username);
    }

    @EventListener
    public void onAuthenticationFailure(
            AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        logger.warn("Falha de autenticação: usuário={}, motivo={}",
            username, reason);
    }

    @EventListener
    public void onAuthorizationDenied(
            AuthorizationDeniedEvent<?> event) {
        String username = event.getAuthentication().get().getName();
        logger.warn("Acesso negado: usuário={}", username);
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        String username = event.getAuthentication().getName();
        logger.info("Logout realizado: usuário={}", username);
    }

    @EventListener
    public void onBadCredentials(
            AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        logger.warn("Credenciais inválidas: usuário={}", username);

        // TODO: Implementar bloqueio de conta após N tentativas
    }
}
```

### 3.3 Configuração de Properties

```properties
# === application.properties ===

# Segurança
archbase.security.enabled=true
archbase.security.jwt.secret-key=${JWT_SECRET}
archbase.security.jwt.expiration=3600000
archbase.security.jwt.refresh-expiration=86400000
archbase.security.method.enabled=true
archbase.security.permission.cache.enabled=true
archbase.security.permission.cache.ttl=300

# CORS
app.cors.allowed-origins=${CORS_ORIGINS:http://localhost:3000,http://localhost:4200}

# HTTPS (produção)
app.security.require-https=${REQUIRE_HTTPS:false}

# Multi-tenancy
archbase.multitenancy.enabled=true
archbase.multitenancy.scan-packages=com.example

# Logging de segurança
logging.level.org.springframework.security=INFO
logging.level.br.com.archbase.security=DEBUG
logging.level.com.example.config.SecurityEventsListener=INFO

# === application-prod.properties ===

# Forçar HTTPS em produção
app.security.require-https=true

# JWT com expirações curtas
archbase.security.jwt.expiration=1800000
archbase.security.jwt.refresh-expiration=43200000

# Logging reduzido
logging.level.org.springframework.security=WARN
logging.level.br.com.archbase.security=INFO
```

### 3.4 Dicas de Implementação

**Secrets Management**:
```bash
# Use variáveis de ambiente em produção
export JWT_SECRET="your-very-long-and-secure-secret-key-minimum-256-bits"
export CORS_ORIGINS="https://app.example.com,https://admin.example.com"
export REQUIRE_HTTPS=true

# Ou use AWS Secrets Manager, Azure Key Vault, etc.
```

**Testes de Segurança**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowPublicEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldProtectPrivateEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk());
    }
}
```

---

## 4. Trabalho com API Tokens

Este exemplo demonstra como criar, gerenciar e validar API tokens para integração service-to-service e automação.

### 4.1 Service de Gerenciamento de API Tokens

```java
package com.example.security.service;

import br.com.archbase.ddd.domain.annotations.DomainService;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.ApiTokenJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import com.example.security.dto.ApiTokenDto;
import com.example.security.dto.CreateApiTokenRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de API Tokens.
 *
 * API Tokens são usados para:
 * - Autenticação service-to-service
 * - Integração com sistemas externos
 * - Automações e scripts
 * - CI/CD pipelines
 */
@Service
@DomainService
public class ApiTokenService {

    private static final Logger logger =
        LoggerFactory.getLogger(ApiTokenService.class);

    @Autowired
    private ApiTokenJpaRepository apiTokenRepository;

    @Autowired
    private UserJpaRepository userRepository;

    /**
     * Cria um novo token de API para integração externa.
     *
     * O token é gerado com criptografia segura (SecureRandom) e
     * inicia em estado desativado por segurança.
     *
     * @param request Dados para criação do token
     * @return DTO do token criado (com token em texto plano)
     */
    @Transactional
    public ApiTokenDto createApiToken(CreateApiTokenRequest request) {
        logger.info("Criando API token: name={}, userId={}",
            request.getName(), request.getUserId());

        // Validar dados de entrada
        validateCreateRequest(request);

        // Buscar usuário associado
        UserEntity user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ArchbaseValidationException(
                "Usuário não encontrado: " + request.getUserId()));

        // Validar que usuário não está desativado
        if (user.getAccountDeactivated()) {
            throw new ArchbaseValidationException(
                "Não é possível criar token para usuário desativado");
        }

        // Gerar token seguro
        String token = generateSecureToken();
        String tokenHash = hashToken(token);

        // Calcular data de expiração
        LocalDateTime expirationDate = calculateExpirationDate(
            request.getExpirationDays()
        );

        // Criar entidade de token
        ApiTokenEntity apiToken = ApiTokenEntity.builder()
            .id(UUID.randomUUID().toString())
            .code("API_TOKEN_" + generateShortId())
            .name(request.getName())
            .description(request.getDescription())
            .user(user)
            .token(tokenHash)  // Armazenar hash, não texto plano
            .activated(false)  // Deve ser ativado explicitamente
            .revoked(false)
            .expirationDate(expirationDate)
            .tenantId(user.getTenantId())
            .createEntityDate(LocalDateTime.now())
            .createdByUser(user.getId())
            .build();

        apiTokenRepository.save(apiToken);

        logger.info("API token criado: id={}, expirationDate={}",
            apiToken.getId(), expirationDate);

        // Retornar DTO com token em texto plano (única vez)
        ApiTokenDto dto = apiToken.toDto();
        dto.setToken(token);  // Token original (não hash)
        dto.setWarning("IMPORTANTE: Guarde este token em local seguro. " +
            "Ele não será exibido novamente.");

        return dto;
    }

    /**
     * Lista todos os tokens de um usuário.
     */
    @Transactional(readOnly = true)
    public List<ApiTokenDto> listUserTokens(String userId) {
        List<ApiTokenEntity> tokens = apiTokenRepository
            .findAllByUserId(userId);

        return tokens.stream()
            .map(ApiTokenEntity::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Ativa um token de API para uso.
     *
     * Por segurança, tokens são criados desativados e devem ser
     * ativados explicitamente após criação.
     */
    @Transactional
    public void activateApiToken(String tokenId) {
        logger.info("Ativando API token: id={}", tokenId);

        ApiTokenEntity apiToken = apiTokenRepository.findById(tokenId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Token não encontrado"));

        // Validações
        if (apiToken.getRevoked()) {
            throw new ArchbaseValidationException(
                "Token foi revogado e não pode ser ativado");
        }

        if (LocalDateTime.now().isAfter(apiToken.getExpirationDate())) {
            throw new ArchbaseValidationException(
                "Token expirado não pode ser ativado");
        }

        // Ativar token
        apiToken.setActivated(true);
        apiToken.setUpdateEntityDate(LocalDateTime.now());
        apiTokenRepository.save(apiToken);

        logger.info("API token ativado: id={}", tokenId);
    }

    /**
     * Desativa um token de API.
     */
    @Transactional
    public void deactivateApiToken(String tokenId) {
        logger.info("Desativando API token: id={}", tokenId);

        ApiTokenEntity apiToken = apiTokenRepository.findById(tokenId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Token não encontrado"));

        apiToken.setActivated(false);
        apiToken.setUpdateEntityDate(LocalDateTime.now());
        apiTokenRepository.save(apiToken);

        logger.info("API token desativado: id={}", tokenId);
    }

    /**
     * Revoga um token de API permanentemente.
     *
     * Tokens revogados não podem ser reativados.
     */
    @Transactional
    public void revokeApiToken(String tokenId) {
        logger.warn("Revogando API token: id={}", tokenId);

        ApiTokenEntity apiToken = apiTokenRepository.findById(tokenId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Token não encontrado"));

        apiToken.setRevoked(true);
        apiToken.setActivated(false);
        apiToken.setUpdateEntityDate(LocalDateTime.now());
        apiTokenRepository.save(apiToken);

        logger.warn("API token revogado: id={}", tokenId);
    }

    /**
     * Valida um token de API.
     *
     * Verifica:
     * - Token existe
     * - Não está revogado
     * - Está ativado
     * - Não está expirado
     *
     * @param token Token em texto plano
     * @return true se válido, false caso contrário
     */
    public boolean validateApiToken(String token) {
        String tokenHash = hashToken(token);
        Optional<ApiTokenEntity> apiTokenOpt =
            apiTokenRepository.findByToken(tokenHash);

        if (apiTokenOpt.isEmpty()) {
            logger.warn("Tentativa de uso de token inválido");
            return false;
        }

        ApiTokenEntity apiToken = apiTokenOpt.get();

        // Verificar revogação
        if (apiToken.getRevoked()) {
            logger.warn("Tentativa de uso de token revogado: id={}",
                apiToken.getId());
            return false;
        }

        // Verificar ativação
        if (!apiToken.getActivated()) {
            logger.warn("Tentativa de uso de token desativado: id={}",
                apiToken.getId());
            return false;
        }

        // Verificar expiração
        if (LocalDateTime.now().isAfter(apiToken.getExpirationDate())) {
            logger.warn("Tentativa de uso de token expirado: id={}",
                apiToken.getId());
            return false;
        }

        // Token válido
        logger.debug("Token validado com sucesso: id={}", apiToken.getId());
        return true;
    }

    /**
     * Busca usuário associado a um token.
     */
    public Optional<UserEntity> getUserByToken(String token) {
        String tokenHash = hashToken(token);
        return apiTokenRepository.findByToken(tokenHash)
            .map(ApiTokenEntity::getUser);
    }

    /**
     * Revoga todos os tokens de um usuário.
     *
     * Útil quando conta é comprometida ou usuário é desativado.
     */
    @Transactional
    public void revokeAllUserTokens(String userId) {
        logger.warn("Revogando todos os tokens do usuário: userId={}",
            userId);

        List<ApiTokenEntity> tokens = apiTokenRepository
            .findAllByUserId(userId);

        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setActivated(false);
            token.setUpdateEntityDate(LocalDateTime.now());
        });

        apiTokenRepository.saveAll(tokens);

        logger.warn("Tokens revogados: count={}", tokens.size());
    }

    /**
     * Gera token seguro usando SecureRandom.
     *
     * Formato: 64 bytes aleatórios -> Base64 URL-safe
     * Resultado: ~86 caracteres
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }

    /**
     * Hash do token para armazenamento seguro.
     *
     * Nunca armazene tokens em texto plano no banco de dados.
     */
    private String hashToken(String token) {
        // TODO: Implementar hash apropriado (SHA-256)
        // Por enquanto, retorna o token (NÃO USAR EM PRODUÇÃO)
        return token;
    }

    /**
     * Gera ID curto para código do token.
     */
    private String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Calcula data de expiração baseada em dias.
     */
    private LocalDateTime calculateExpirationDate(Integer expirationDays) {
        if (expirationDays == null || expirationDays <= 0) {
            expirationDays = 365;  // Padrão: 1 ano
        }
        return LocalDateTime.now().plusDays(expirationDays);
    }

    /**
     * Valida requisição de criação de token.
     */
    private void validateCreateRequest(CreateApiTokenRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ArchbaseValidationException(
                "Nome do token é obrigatório");
        }

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new ArchbaseValidationException(
                "ID do usuário é obrigatório");
        }
    }
}
```

### 4.2 Cliente de API Externo

```java
package com.example.integration.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente para chamar API externa usando API Token.
 *
 * Exemplo de uso de API Token para integração service-to-service.
 */
@Component
public class ExternalApiClient {

    @Value("${external.api.base-url}")
    private String baseUrl;

    @Value("${external.api.token}")
    private String apiToken;

    private final RestTemplate restTemplate;

    public ExternalApiClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Exemplo de chamada GET com API Token.
     */
    public String getData(String endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "ApiToken " + apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + endpoint,
            HttpMethod.GET,
            entity,
            String.class
        );

        return response.getBody();
    }

    /**
     * Exemplo de chamada POST com API Token.
     */
    public <T> T postData(String endpoint, Object requestBody,
            Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "ApiToken " + apiToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<T> response = restTemplate.exchange(
            baseUrl + endpoint,
            HttpMethod.POST,
            entity,
            responseType
        );

        return response.getBody();
    }
}
```

### 4.3 Filtro de Autenticação por API Token

```java
package com.example.security.filter;

import br.com.archbase.security.persistence.UserEntity;
import com.example.security.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro para autenticação via API Token.
 *
 * Processa header "Authorization: ApiToken <token>" e autentica
 * usuário associado se token for válido.
 */
@Component
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private ApiTokenService apiTokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Extrair header Authorization
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("ApiToken ")) {
            String token = authHeader.substring(9);  // Remover "ApiToken "

            // Validar token
            if (apiTokenService.validateApiToken(token)) {
                // Buscar usuário associado
                Optional<UserEntity> userOpt =
                    apiTokenService.getUserByToken(token);

                if (userOpt.isPresent()) {
                    UserDetails userDetails = userOpt.get();

                    // Criar authentication token
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                            .buildDetails(request)
                    );

                    // Definir no contexto de segurança
                    SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### 4.4 Dicas de Implementação

**Armazenamento Seguro de Tokens**:
```bash
# Use variáveis de ambiente ou secrets manager
export EXTERNAL_API_TOKEN="<seu-token-aqui>"

# Ou AWS Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id external-api-token \
  --query SecretString \
  --output text
```

**Rotação de Tokens**:
```java
@Scheduled(cron = "0 0 1 * * ?")  // Todo dia às 1h
public void rotateExpiredTokens() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
    List<ApiTokenEntity> expiredTokens = apiTokenRepository
        .findExpiredTokensOlderThan(cutoffDate);

    expiredTokens.forEach(token -> {
        logger.info("Revogando token expirado: id={}", token.getId());
        apiTokenService.revokeApiToken(token.getId());
    });
}
```

---

## 5. Configuração de Horários de Acesso

Este exemplo demonstra como configurar e gerenciar horários de acesso para usuários, implementando restrições temporais de login.

### 5.1 Service de Gerenciamento de Horários

```java
package com.example.security.service;

import br.com.archbase.ddd.domain.annotations.DomainService;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import br.com.archbase.security.persistence.AccessIntervalEntity;
import br.com.archbase.security.persistence.AccessScheduleEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessScheduleJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import com.example.security.dto.AccessIntervalDto;
import com.example.security.dto.AccessScheduleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de horários de acesso.
 *
 * Horários de acesso controlam quando usuários podem fazer login
 * e acessar o sistema, útil para:
 * - Horário comercial
 * - Turnos de trabalho
 * - Restrições de segurança
 * - Compliance regulatório
 */
@Service
@DomainService
public class AccessScheduleService {

    private static final Logger logger =
        LoggerFactory.getLogger(AccessScheduleService.class);

    @Autowired
    private AccessScheduleJpaRepository scheduleRepository;

    @Autowired
    private UserJpaRepository userRepository;

    /**
     * Cria um horário de acesso comercial padrão (Seg-Sex 8h-18h).
     *
     * Configuração típica para horário comercial brasileiro.
     */
    @Transactional
    public AccessScheduleDto createBusinessHoursSchedule(String tenantId) {
        logger.info("Criando horário comercial para tenant: {}", tenantId);

        // Criar horário de acesso
        AccessScheduleEntity schedule = new AccessScheduleEntity();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setCode("SCH_BUSINESS_HOURS");
        schedule.setDescription("Horário Comercial - Segunda a Sexta 8h às 18h");
        schedule.setTenantId(tenantId);
        schedule.setCreateEntityDate(LocalDateTime.now());
        schedule.setCreatedByUser("SYSTEM");

        // Criar intervalos para dias úteis
        // DayOfWeek: 1=Domingo, 2=Segunda, ..., 7=Sábado
        List<AccessIntervalEntity> intervals = new ArrayList<>();
        for (long day = 2; day <= 6; day++) {  // Segunda a Sexta
            AccessIntervalEntity interval = AccessIntervalEntity.builder()
                .id(UUID.randomUUID().toString())
                .code("INT_BUSINESS_DAY_" + day)
                .accessSchedule(schedule)
                .dayOfWeek(day)
                .startTime("08:00")
                .endTime("18:00")
                .tenantId(tenantId)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("SYSTEM")
                .build();
            intervals.add(interval);
        }

        schedule.setIntervals(intervals);
        scheduleRepository.save(schedule);

        logger.info("Horário comercial criado: id={}", schedule.getId());
        return schedule.toDto();
    }

    /**
     * Cria um horário de acesso 24/7 (todos os dias, 24 horas).
     *
     * Útil para sistemas que operam continuamente ou usuários
     * que necessitam acesso irrestrito.
     */
    @Transactional
    public AccessScheduleDto create24x7Schedule(String tenantId) {
        logger.info("Criando horário 24x7 para tenant: {}", tenantId);

        AccessScheduleEntity schedule = new AccessScheduleEntity();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setCode("SCH_24X7");
        schedule.setDescription("Acesso 24 horas - Todos os dias");
        schedule.setTenantId(tenantId);
        schedule.setCreateEntityDate(LocalDateTime.now());
        schedule.setCreatedByUser("SYSTEM");

        // Criar intervalos para todos os dias
        List<AccessIntervalEntity> intervals = new ArrayList<>();
        for (long day = 1; day <= 7; day++) {  // Todos os dias
            AccessIntervalEntity interval = AccessIntervalEntity.builder()
                .id(UUID.randomUUID().toString())
                .code("INT_24X7_DAY_" + day)
                .accessSchedule(schedule)
                .dayOfWeek(day)
                .startTime("00:00")
                .endTime("23:59")
                .tenantId(tenantId)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("SYSTEM")
                .build();
            intervals.add(interval);
        }

        schedule.setIntervals(intervals);
        scheduleRepository.save(schedule);

        logger.info("Horário 24x7 criado: id={}", schedule.getId());
        return schedule.toDto();
    }

    /**
     * Cria horário de turnos (exemplo: 3 turnos de 8 horas).
     *
     * Turno 1: 06:00-14:00
     * Turno 2: 14:00-22:00
     * Turno 3: 22:00-06:00
     */
    @Transactional
    public List<AccessScheduleDto> createShiftSchedules(String tenantId) {
        logger.info("Criando horários de turnos para tenant: {}", tenantId);

        List<AccessScheduleDto> schedules = new ArrayList<>();

        // Turno 1: Manhã/Tarde (06:00-14:00)
        schedules.add(createShiftSchedule(
            tenantId,
            "SHIFT_1",
            "Turno 1 - Manhã/Tarde (06:00-14:00)",
            "06:00",
            "14:00"
        ));

        // Turno 2: Tarde/Noite (14:00-22:00)
        schedules.add(createShiftSchedule(
            tenantId,
            "SHIFT_2",
            "Turno 2 - Tarde/Noite (14:00-22:00)",
            "14:00",
            "22:00"
        ));

        // Turno 3: Noite/Madrugada (22:00-06:00)
        schedules.add(createShiftSchedule(
            tenantId,
            "SHIFT_3",
            "Turno 3 - Noite/Madrugada (22:00-06:00)",
            "22:00",
            "06:00"
        ));

        logger.info("Horários de turnos criados: count={}", schedules.size());
        return schedules;
    }

    /**
     * Cria um horário de acesso customizado.
     *
     * Permite especificar intervalos arbitrários por dia da semana.
     */
    @Transactional
    public AccessScheduleDto createCustomSchedule(
            String tenantId,
            String description,
            List<AccessIntervalDto> intervalDtos) {

        logger.info("Criando horário customizado: description={}",
            description);

        // Validar intervalos
        validateIntervals(intervalDtos);

        AccessScheduleEntity schedule = new AccessScheduleEntity();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setCode("SCH_CUSTOM_" + generateShortId());
        schedule.setDescription(description);
        schedule.setTenantId(tenantId);
        schedule.setCreateEntityDate(LocalDateTime.now());
        schedule.setCreatedByUser("SYSTEM");

        List<AccessIntervalEntity> intervals = intervalDtos.stream()
            .map(dto -> AccessIntervalEntity.builder()
                .id(UUID.randomUUID().toString())
                .code("INT_" + generateShortId())
                .accessSchedule(schedule)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .tenantId(tenantId)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("SYSTEM")
                .build())
            .collect(Collectors.toList());

        schedule.setIntervals(intervals);
        scheduleRepository.save(schedule);

        logger.info("Horário customizado criado: id={}", schedule.getId());
        return schedule.toDto();
    }

    /**
     * Atribui horário de acesso a um usuário.
     */
    @Transactional
    public void assignScheduleToUser(String userId, String scheduleId) {
        logger.info("Atribuindo horário a usuário: userId={}, scheduleId={}",
            userId, scheduleId);

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Usuário não encontrado"));

        AccessScheduleEntity schedule = scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Horário de acesso não encontrado"));

        user.setAccessSchedule(schedule);
        user.setUnlimitedAccessHours(false);
        user.setUpdateEntityDate(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Horário atribuído com sucesso");
    }

    /**
     * Remove horário de acesso de um usuário (acesso ilimitado).
     */
    @Transactional
    public void removeScheduleFromUser(String userId) {
        logger.info("Removendo restrição de horário: userId={}", userId);

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Usuário não encontrado"));

        user.setAccessSchedule(null);
        user.setUnlimitedAccessHours(true);
        user.setUpdateEntityDate(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Restrição de horário removida");
    }

    /**
     * Helper para criar horário de turno.
     */
    private AccessScheduleDto createShiftSchedule(
            String tenantId,
            String code,
            String description,
            String startTime,
            String endTime) {

        AccessScheduleEntity schedule = new AccessScheduleEntity();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setCode("SCH_" + code);
        schedule.setDescription(description);
        schedule.setTenantId(tenantId);
        schedule.setCreateEntityDate(LocalDateTime.now());
        schedule.setCreatedByUser("SYSTEM");

        // Criar intervalos para todos os dias úteis
        List<AccessIntervalEntity> intervals = new ArrayList<>();
        for (long day = 2; day <= 6; day++) {
            AccessIntervalEntity interval = AccessIntervalEntity.builder()
                .id(UUID.randomUUID().toString())
                .code("INT_" + code + "_DAY_" + day)
                .accessSchedule(schedule)
                .dayOfWeek(day)
                .startTime(startTime)
                .endTime(endTime)
                .tenantId(tenantId)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("SYSTEM")
                .build();
            intervals.add(interval);
        }

        schedule.setIntervals(intervals);
        scheduleRepository.save(schedule);

        return schedule.toDto();
    }

    /**
     * Valida lista de intervalos.
     */
    private void validateIntervals(List<AccessIntervalDto> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            throw new ArchbaseValidationException(
                "Lista de intervalos não pode ser vazia");
        }

        for (AccessIntervalDto interval : intervals) {
            // Validar dia da semana (1-7)
            if (interval.getDayOfWeek() < 1 || interval.getDayOfWeek() > 7) {
                throw new ArchbaseValidationException(
                    "Dia da semana inválido: " + interval.getDayOfWeek());
            }

            // Validar formato de horários
            if (!isValidTime(interval.getStartTime()) ||
                !isValidTime(interval.getEndTime())) {
                throw new ArchbaseValidationException(
                    "Formato de horário inválido. Use HH:mm");
            }
        }
    }

    /**
     * Valida formato de horário (HH:mm).
     */
    private boolean isValidTime(String time) {
        return time != null && time.matches("^([01]\\d|2[0-3]):([0-5]\\d)$");
    }

    /**
     * Gera ID curto para código.
     */
    private String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
```

### 5.2 Validação de Horário no Login

```java
package com.example.security.validator;

import br.com.archbase.security.persistence.AccessIntervalEntity;
import br.com.archbase.security.persistence.AccessScheduleEntity;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Validador de horário de acesso durante login.
 */
@Component
public class AccessScheduleValidator {

    /**
     * Valida se usuário pode fazer login no horário atual.
     *
     * @param user Usuário tentando fazer login
     * @return true se acesso permitido, false caso contrário
     */
    public boolean canAccessNow(UserEntity user) {
        // Acesso ilimitado
        if (user.getUnlimitedAccessHours()) {
            return true;
        }

        // Sem horário definido = sem acesso
        AccessScheduleEntity schedule = user.getAccessSchedule();
        if (schedule == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        // Converter DayOfWeek para número (1=Domingo, 7=Sábado)
        long dayNumber = (dayOfWeek.getValue() % 7) + 1;

        // Verificar se existe intervalo válido para dia/hora atual
        return schedule.getIntervals().stream()
            .anyMatch(interval -> isTimeInInterval(
                dayNumber,
                currentTime,
                interval
            ));
    }

    /**
     * Verifica se horário está dentro de um intervalo.
     */
    private boolean isTimeInInterval(
            long dayNumber,
            LocalTime currentTime,
            AccessIntervalEntity interval) {

        // Verificar dia da semana
        if (interval.getDayOfWeek() != dayNumber) {
            return false;
        }

        LocalTime startTime = LocalTime.parse(interval.getStartTime());
        LocalTime endTime = LocalTime.parse(interval.getEndTime());

        // Caso normal: startTime < endTime (ex: 08:00-18:00)
        if (startTime.isBefore(endTime)) {
            return !currentTime.isBefore(startTime) &&
                   !currentTime.isAfter(endTime);
        }

        // Caso especial: startTime > endTime (ex: 22:00-06:00, noite/madrugada)
        return !currentTime.isBefore(startTime) ||
               !currentTime.isAfter(endTime);
    }

    /**
     * Retorna próximo horário de acesso disponível.
     */
    public LocalDateTime getNextAccessTime(UserEntity user) {
        if (user.getUnlimitedAccessHours()) {
            return LocalDateTime.now();
        }

        AccessScheduleEntity schedule = user.getAccessSchedule();
        if (schedule == null || schedule.getIntervals().isEmpty()) {
            return null;  // Sem acesso
        }

        LocalDateTime now = LocalDateTime.now();

        // Procurar próximo intervalo (simplificado)
        // TODO: Implementar lógica completa de próximo horário
        return now.plusDays(1).withHour(8).withMinute(0);
    }
}
```

### 5.3 Uso no AuthenticationService

```java
@Service
public class CustomAuthenticationService {

    @Autowired
    private AccessScheduleValidator scheduleValidator;

    public AuthenticationResponse authenticate(
            AuthenticationRequest request) {

        // Autenticar credenciais
        UserEntity user = validateCredentials(request);

        // Validar horário de acesso
        if (!scheduleValidator.canAccessNow(user)) {
            LocalDateTime nextAccess =
                scheduleValidator.getNextAccessTime(user);

            throw new AccessDeniedException(
                "Acesso fora do horário permitido. " +
                "Próximo acesso disponível em: " + nextAccess
            );
        }

        // Gerar tokens e retornar resposta
        return generateAuthenticationResponse(user);
    }
}
```

---

## Navegação

[← Anterior: Anotações e Uso](annotations-and-usage.md) | [Voltar ao Índice](../README.md) | [Próximo: Melhores Práticas →](best-practices.md)

---

## Próximos Passos

Após estudar estes exemplos completos, recomendamos:

1. **[Melhores Práticas](best-practices.md)** - Aprenda recomendações de design e segurança
2. **[Troubleshooting](troubleshooting.md)** - Resolva problemas comuns
3. **[Arquitetura do Sistema](../architecture/permissions-system.md)** - Entenda o funcionamento interno

Para dúvidas ou contribuições, consulte o [README principal](../README.md).
