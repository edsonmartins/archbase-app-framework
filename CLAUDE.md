# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Building the Framework
```bash
# Clean build all modules
mvn clean install

# Build with Maven Central profile (includes GPG signing, sources, and javadoc)
mvn clean install -P maven-central

# Build with local profile (includes GPG signing, sources, and javadoc)
mvn clean install -P local

# Skip tests during build
mvn clean install -DskipTests

# Build a specific module
mvn clean install -pl archbase-security -am
```

### Testing
```bash
# Run all tests
mvn test

# Run tests for a specific module
mvn test -pl archbase-security

# Run a single test class
mvn test -Dtest=YourTestClassName

# Run tests with coverage
mvn verify
```

### Development Tasks
```bash
# Compile without packaging
mvn compile

# Package modules
mvn package

# Generate JavaDocs
mvn javadoc:javadoc

# Update dependencies
mvn versions:display-dependency-updates
```

## Architecture Overview

### Framework Structure
Archbase is a multi-module Maven project built on Spring Boot 4.1.0 and Java 17. It provides a comprehensive framework for building enterprise applications using Domain-Driven Design (DDD) principles.

### Module Organization

| Module | Purpose |
|--------|---------|
| `archbase-domain-driven-design` | Core DDD implementation with repositories, specifications, REST controllers |
| `archbase-security` | JWT authentication, role-based permissions, API tokens, password reset |
| `archbase-multitenancy` | Tenant context management, tenant-aware repositories |
| `archbase-event-driven` | CQRS command/query buses, event publishing with handler annotations |
| `archbase-query` | RSQL parser and converter for RESTful queries |
| `archbase-workflow-process` | Workflow engine with sequential, parallel, conditional flows |
| `archbase-plugin-manager` | Dynamic plugin loading with extension discovery |
| `archbase-validation` | DDD-specific validation patterns |
| `archbase-shared-kernel` | Common value objects (CPF, CNPJ, Email), converters |
| `archbase-architecture` | Hexagonal, layered, and onion architecture patterns |
| `archbase-hypersistence` | Advanced Hibernate types (JSON, arrays, ranges), optimized repositories |

### Core Architecture Patterns

1. **Domain-Driven Design (DDD)**
   - Entities extend `DomainEntityBase` or `PersistenceEntityBase`
   - Aggregate roots implement `AggregateRoot<T, ID>`
   - Value objects implement `ValueObject`
   - Repositories extend `Repository<T, ID, N>` with QueryDSL and RSQL support
   - Specification pattern with `ArchbaseSpecification<T>` for complex queries

2. **Multi-Tenancy**
   - Tenant context managed via `ArchbaseTenantContext` (thread-local)
   - Entities extend `TenantPersistenceEntityBase` for tenant awareness
   - Automatic tenant context propagation via async task decorators
   - Configuration: `archbase.multitenancy.enabled=true`

3. **Security**
   - JWT-based authentication with access and refresh tokens
   - Permission system using `@HasPermission(action, resource, tenantId, companyId, projectId)`
   - API token management for service-to-service communication
   - Access scheduling and interval controls
   - Custom security configuration via `CustomSecurityConfiguration` interface

4. **Event-Driven Architecture (CQRS)**
   - Commands dispatched via `CommandBus`
   - Events published through `EventBus`
   - Queries handled by `QueryBus`
   - Handlers auto-discovered via `@CommandHandler`, `@EventHandler`, `@QueryHandler`
   - Handler discovery enabled with `@HandlerScan` on configuration classes

5. **Workflow Engine**
   - Sequential, parallel, conditional, and repeat flows
   - Work context sharing between workflow steps
   - Status tracking and execution monitoring

6. **Plugin System**
   - Dynamic plugin loading from JAR files
   - Extension point discovery using annotations
   - Plugin lifecycle management with version resolution (semver)

7. **Hypersistence Utils Integration**
   - JSON column types for PostgreSQL (jsonb), MySQL (json), Oracle, and H2
   - PostgreSQL array types (text[], int[], uuid[], etc.)
   - PostgreSQL range types (daterange, int4range, numrange, etc.)
   - Optimized repository methods: `persist()`, `merge()`, `update()`
   - N+1 query detection with `SQLStatementCountAssertions`
   - TSID (Time-Sorted ID) generator

### Module Dependencies

**For Basic Applications:**
```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-core</artifactId>
</dependency>
```

**For Secured Multi-Tenant Applications:**
```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter</artifactId>
</dependency>
```

**For Advanced Persistence (JSON, Arrays, Ranges):**
```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-hypersistence</artifactId>
</dependency>
```

### Key Implementation Patterns

1. **Entity Creation:**
```java
@Entity
@DomainEntity
public class YourEntity extends PersistenceEntityBase<YourEntity, UUID> {
    // For multi-tenant: extends TenantPersistenceEntityBase
    @Override
    public ValidationResult validate() {
        return ValidationResult.success();
    }
}
```

2. **Repository Pattern:**
```java
@DomainRepository
public interface YourRepository extends Repository<YourEntity, UUID, Long> {
    // N parameter is for numeric version type
    // Custom queries using QueryDSL or RSQL
}
```

3. **Security Annotations:**

`@HasPermission` is `@Target(METHOD)` — it does **not** compile on a class:
```java
@RestController
public class YourController {

    @GetMapping
    @HasPermission(action = "VIEW", resource = "YOUR_RESOURCE", description = "...")
    public ResponseEntity<?> list() { ... }
}
```

`@RequireProfile`, `@RequireRole` and `@RequirePersona` do accept class level; when placed on the
class they apply to every method, and a method-level annotation overrides the class one.

`@RequireRole` only enforces anything if the application registers an `ArchbaseRoleResolver` bean —
the roles it checks belong to the application domain, not to Archbase. Without that bean the
behaviour is controlled by `archbase.security.require-role.no-resolver-policy`.

4. **Event Handling:**
```java
@Configuration
@HandlerScan(basePackages = "com.yourcompany.handlers")
public class HandlerConfig {
}

@Component
public class YourEventHandler {
    @EventHandler
    public void handle(YourDomainEvent event) {
        // Handle event
    }
}
```

5. **Specification Pattern:**
```java
public class YourSpecification extends ArchbaseSpecification<YourEntity> {
    public YourEntity someCondition() {
        return new EqualSpecification<>("field", value);
    }
}
```

6. **JSON Column with Hypersistence:**
```java
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

@Entity
public class ProductEntity extends TenantPersistenceEntityBase<ProductEntity, String> {
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;
}
```

7. **Optimized Repository with Hypersistence:**
```java
public interface ProductRepository
    extends ArchbaseJpaRepository<ProductEntity, String, Long>,
            ArchbaseHypersistenceRepository<ProductEntity, String> {
}

// Usage - persist() is more efficient than save() for new entities
repository.persist(newEntity);  // No SELECT before INSERT
repository.update(existingEntity);  // No SELECT before UPDATE
```

### Configuration Properties

```properties
# Multi-tenancy
archbase.multitenancy.enabled=true
archbase.multitenancy.scan-packages=your.package

# Security
archbase.security.enabled=true
archbase.security.jwt.secret=your-secret
archbase.security.jwt.expiration=86400000
archbase.security.method.enabled=true
archbase.security.permission.cache.enabled=true
# Validade da senha em dias (0 = sem expiração periódica)
archbase.security.password.expiration-days=0

# Security hardening (auditoria 3.0.11)
# Os defaults abaixo PRESERVAM o comportamento anterior à auditoria. Enquanto não forem
# alterados, a proteção correspondente está inerte — ver deployment/security-hardening.md.
archbase.security.admin-endpoints.policy=permit             # permit | admin-only | permission
archbase.security.require-role.no-resolver-policy=permit    # permit | deny
archbase.security.jwt.strict-token-use=false                # recusa token sem o claim token_use
archbase.security.jwt.accept-token-query-param=true         # aceita credencial em ?token=
archbase.security.prevent-user-enumeration=false            # resposta uniforme no reset de senha
archbase.security.api-token.purge-plaintext=false           # IRREVERSÍVEL: apaga o token em claro
archbase.security.public-paths.actuator=true
archbase.security.public-paths.registration=true            # auto-cadastro anônimo
archbase.security.public-paths.legacy-app-routes=true       # rotas de aplicação (deprecado)
archbase.app.tenant.fail-on-missing=false                   # recusa acesso sem tenant no contexto
archbase.app.tenant.accept-query-param=true                 # aceita X-TENANT-ID na query string

# Rate limiting dos fluxos de credencial (ligado por padrão)
archbase.security.rate-limit.enabled=true
archbase.security.rate-limit.max-attempts=10
archbase.security.rate-limit.window-seconds=900
archbase.security.rate-limit.block-seconds=900

# Força de senha (inteiramente desligada por padrão)
archbase.security.password.min-length=0
archbase.security.password.require-digit=false
archbase.security.password.require-uppercase=false
archbase.security.password.require-lowercase=false
archbase.security.password.require-special=false
archbase.security.password.block-common=false

# RSQL
archbase.rsql.enabled=true
archbase.rsql.page.parameter=page
archbase.rsql.size.parameter=size

# Plugin Management
archbase.plugin.manager.enabled=true
archbase.plugin.manager.scan-packages=your.plugins.packages

# Workflow Engine
archbase.workflow.engine.enabled=true
archbase.workflow.engine.execution.mode=sync

# Hypersistence Utils
archbase.hypersistence.enabled=true
archbase.hypersistence.json.enabled=true
archbase.hypersistence.postgresql.array-types-enabled=true
archbase.hypersistence.postgresql.range-types-enabled=true
archbase.hypersistence.repository.enhanced-methods-enabled=false
```

### Testing Approach

- Unit tests use JUnit Jupiter 5.2.0
- Integration tests with `@SpringBootTest`
- Repository tests with `@DataJpaTest`
- Security tests with `@WithMockUser` or custom JWT tokens
- Always verify multi-tenant context in tests when enabled

### Important Considerations

1. **Tenant Context**: When multi-tenancy is enabled, always ensure tenant context is properly set before database operations. Background tasks require the async task decorator for context propagation.
2. **Security**: All endpoints are secured by default; use `@PermitAll` for public endpoints
3. **Entity Validation**: Entities must implement `validate()` method returning `ValidationResult`
4. **Event Ordering**: Events are processed asynchronously; don't rely on ordering unless using saga pattern
5. **Repository Methods**: Prefer RSQL filters over custom queries for better API consistency
6. **Handler Discovery**: Use `@HandlerScan` to enable auto-discovery of `@CommandHandler`, `@EventHandler`, and `@QueryHandler` annotated methods
7. **Specification Composition**: Specifications can be composed using `and()`, `or()`, and `not()` for complex queries
8. **Hypersistence Types**: JSON types work with all databases; Array and Range types work only with PostgreSQL. Use `@Type(JsonType.class)` with `@Column(columnDefinition = "jsonb")` for JSON columns
9. **Optimized Persistence**: Use `persist()` for new entities and `update()` for existing ones to avoid unnecessary SELECT queries before INSERT/UPDATE