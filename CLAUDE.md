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

### Core Architecture Patterns

1. **Domain-Driven Design (DDD)**
   - Entities extend `DomainEntityBase` or `PersistenceEntityBase`
   - Aggregate roots implement `AggregateRoot<T, ID>`
   - Value objects implement `ValueObject`
   - Repositories extend `Repository<T, ID, N>` with QueryDSL and RSQL support

2. **Multi-Tenancy**
   - Tenant context managed via `ArchbaseTenantContext`
   - Entities extend `TenantPersistenceEntityBase` for tenant awareness
   - Configuration: `archbase.multitenancy.enabled=true`

3. **Security**
   - JWT-based authentication with `ArchbaseSecurityService`
   - Permission system using `@HasPermission(action="ACTION", resource="RESOURCE")`
   - Custom security configuration via `CustomSecurityConfiguration` interface

4. **Event-Driven Architecture**
   - Commands dispatched via `CommandBus`
   - Events published through `EventBus`
   - Queries handled by `QueryBus`
   - Handlers auto-discovered via `@CommandHandler`, `@EventHandler`, `@QueryHandler`

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
}
```

2. **Repository Pattern:**
```java
@DomainRepository
public interface YourRepository extends Repository<YourEntity, UUID, Long> {
    // Custom queries using QueryDSL or RSQL
}
```

3. **Security Annotations:**
```java
@RestController
@HasPermission(action = "VIEW", resource = "YOUR_RESOURCE")
public class YourController {
    // Controller methods
}
```

4. **Event Handling:**
```java
@Component
public class YourEventHandler {
    @EventHandler
    public void handle(YourDomainEvent event) {
        // Handle event
    }
}
```

### Configuration Properties

Key properties to configure:
```properties
# Multi-tenancy
archbase.multitenancy.enabled=true
archbase.multitenancy.scan-packages=your.package

# Security
archbase.security.enabled=true
archbase.security.jwt.secret=your-secret
archbase.security.jwt.expiration=86400000

# RSQL
archbase.rsql.enabled=true
archbase.rsql.page.parameter=page
archbase.rsql.size.parameter=size
```

### Testing Approach

- Unit tests use JUnit Jupiter 5.2.0
- Integration tests with `@SpringBootTest`
- Repository tests with `@DataJpaTest`
- Security tests with `@WithMockUser` or custom JWT tokens
- Always verify multi-tenant context in tests when enabled

### Important Considerations

1. **Tenant Context**: When multi-tenancy is enabled, always ensure tenant context is properly set before database operations
2. **Security**: All endpoints are secured by default; use `@PermitAll` for public endpoints
3. **Entity Validation**: Entities must implement `validate()` method returning `ValidationResult`
4. **Event Ordering**: Events are processed asynchronously; don't rely on ordering unless using saga pattern
5. **Repository Methods**: Prefer RSQL filters over custom queries for better API consistency