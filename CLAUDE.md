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
Archbase is a multi-module Maven project built on Spring Boot 3.2.5 and Java 17. It provides a comprehensive framework for building enterprise applications using Domain-Driven Design (DDD) principles.

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
```java
@RestController
@HasPermission(action = "VIEW", resource = "YOUR_RESOURCE")
public class YourController {
    // Method-level security also supported
}
```

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