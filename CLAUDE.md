# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Archbase is a Java application framework built on Spring Boot 3.2.5 with Java 17. It implements Domain-Driven Design (DDD) principles with Onion Architecture, providing a modular foundation for enterprise applications. The framework supports both simple CRUD applications and complex DDD-based systems.

## Build Commands

```bash
# Build and package
mvn clean package

# Build with sources and javadoc
mvn clean package source:jar javadoc:jar

# Run tests
mvn test

# Update parent version
mvn versions:update-parent -DparentVersion=X.Y.Z -DgenerateBackupPoms=false

# Update all module versions
mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false

# Deploy to Maven Central (requires GPG signing)
mvn clean package source:jar javadoc:jar -Pmaven-central deploy
```

## Architecture Overview

The framework follows **Domain-Driven Design (DDD)** with **Onion Architecture**. Key architectural principles:

### Layered Structure (Onion Architecture)
1. **Domain Layer** (core): Entities, Value Objects, Aggregates, Domain Services - no external dependencies
2. **Application Layer**: Use cases, application services, orchestrates domain objects
3. **Infrastructure Layer**: Persistence (JPA/JDBC), messaging, external integrations
4. **Interface Layer**: REST controllers, DTOs, API endpoints

**Important**: Inner layers must NOT depend on outer layers. Domain entities should be persistence-agnostic (avoid JPA annotations directly on domain classes when possible).

### DDD Building Blocks

- **@DomainEntity**: Marks domain entities (defined by identity, not attributes)
- **@DomainAggregateRoot**: Marks aggregate roots (consistency boundaries)
- **@DomainValueObject**: Marks value objects (immutable, defined by value)
- **Repositories**: Abstract persistence access, one per aggregate root
- **Specifications**: Composable query patterns (see `archbase-domain-driven-design-spec`)

### Architecture Annotations

The framework provides architectural enforcement via ArchUnit annotations in `archbase-architecture`:
- `@DomainLayer`, `@ApplicationLayer`, `@InfrastructureLayer`, `@InterfaceLayer` (layered)
- `@Port`, `@PrimaryPort`, `@SecondaryPort`, `@Adapter` (hexagonal)
- `@DomainRing`, `@ApplicationRing`, `@InfrastructureRing` (onion)

## Module Structure

### Starter Modules (Spring Boot Auto-configuration)
- **archbase-starter**: Main aggregator, imports all other starters
- **archbase-starter-core**: Core configuration (RSQL, Jackson, Swagger, TaskExecutor, Locale)
- **archbase-starter-security**: JWT authentication, flexible permission system
- **archbase-starter-multitenancy**: Multi-tenant context switching

Auto-configuration is registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 3.x format).

### Domain-Driven Design
- **archbase-domain-driven-design**: DDD infrastructure (repositories, services, REST controllers)
- **archbase-domain-driven-design-spec**: Specification pattern for queries
- **archbase-query**: Query handling and RSQL support
- **archbase-shared-kernel**: Shared value objects (CPF, CNPJ, Email, PhoneNumber, Money, Quantity)

### Infrastructure
- **archbase-validation**: Validation framework
- **archbase-validation-ddd-model**: DDD-specific validation
- **archbase-error-handling**: Centralized error handling with `ApiErrorResponse`
- **archbase-transformation**: Data transformation utilities
- **archbase-resource-logger**: HTTP request/response logging via aspects

### Cross-Cutting Concerns
- **archbase-security**: `@HasPermission` annotation, `SystemUserContext` for programmatic authentication
- **archbase-multitenancy**: Tenant isolation and context management
- **archbase-event-driven**: Event bus (Command/Query/Event), Outbox pattern, Saga support
- **archbase-workflow-process**: Workflow management
- **archbase-plugin-manager**: Dynamic plugin loading

### Developer Tools
- **archbase-codegen-maven-plugin**: Maven plugin for code generation
- **archbase-annotation-processor**: Annotation processing for domain objects
- **archbase-architecture**: ArchUnit annotations for architectural testing

## Configuration Patterns

### Disabling Modules
Use properties to disable optional modules:
```properties
archbase.multitenancy.enabled=false
archbase.security.enabled=true
```

### Bean Customization
Modules use `@ConditionalOnMissingBean` - override by declaring a bean with the same name/qualifier in your application.

### Swagger Configuration
```properties
archbase.swagger.title=My API
archbase.swagger.description=API Description
```

## Security

### Permission-Based Access Control
Use `@HasPermission` annotation on methods:
```java
@HasPermission(action="VIEW", resource="USER_PROFILE")
public UserProfile getUserProfile(String userId) { ... }
```

### System User Authentication
For non-authenticated contexts (webhooks, jobs), use `SystemUserContext`:
```java
@Autowired
private SystemUserContext systemUserContext;

public void processJob() {
    systemUserContext.runAsSystemUser("system@example.com", () -> {
        // code to run as system user
    });
}
```

## Testing

- Uses JUnit 5, Spring Boot Test, Spring Security Test
- Tests located in `src/test/java`
- Integration tests use `@SpringBootTest` with MockMvc
- No dedicated test profile - run with `mvn test`

## Event-Driven Architecture

The framework implements CQRS with separate buses:
- **CommandBus**: For state-changing commands
- **QueryBus**: For read queries
- **EventBus**: For domain events

Supports middleware pattern and outbox for reliable event delivery.

## Value Objects in Shared Kernel

Pre-built value objects available:
- `CPF`, `CNPJ`: Brazilian document types
- `Email`, `PhoneNumber`: Contact information
- `Money`: Monetary amounts with javax.money
- `Quantity`, `Description`: Common domain concepts

## Query Support

- **RSQL**: Advanced filtering via URL parameters (e.g., `?filter=name==John;age>=18`)
- **Specification Pattern**: Type-safe query composition
- **Querydsl**: Type-safe queries with code generation

## Repository Patterns

- `ArchbaseJpaRepository`: JPA-based with pagination, sorting, filtering
- `ArchbaseJdbcRepository`: Spring Data JDBC for simpler persistence
- `SimpleArchbaseJpaRepository`: Basic CRUD operations

Choose based on complexity: JPA for complex domains with rich relationships, JDBC for simpler aggregates.

## Key Conventions

1. **Package Structure**: Follow DDD layering - domain logic should not leak into infrastructure
2. **Anemic Models**: Avoid - entities should contain business logic, not just data
3. **Aggregate Boundaries**: Repositories exist only for aggregate roots
4. **Immutable Value Objects**: Always use constructor + final fields
5. **Events**: Use domain events for side effects within bounded contexts
6. **Multi-tenancy**: Tenant context is automatically propagated when enabled

## Portuguese Documentation Note

Much of the codebase documentation (README.md, module docs) is in Portuguese. The framework is developed by Relevant Solutions (Brazil).
