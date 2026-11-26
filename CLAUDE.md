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
Archbase is a multi-module Maven project built on Spring Boot 4.1.2 and Java 25. It provides a comprehensive framework for building enterprise applications using Domain-Driven Design (DDD) principles.

### Module Organization

The root `pom.xml` lists 40 modules. Opt-in modules create no bean unless the application adds the
dependency **and** enables them.

| Module | Purpose |
|--------|---------|
| `archbase-domain-driven-design-spec` | DDD contracts: `Repository`, `AggregateRoot`, entity bases, `ArchbaseSpecification`, `ArchbaseTenantContext` |
| `archbase-domain-driven-design` | JPA implementation: `ArchbaseCommonJpaRepository`, `ArchbaseJpaRepository`, infrastructure |
| `archbase-security` | JWT authentication, five-gate authorization core, API tokens, MFA, audit trail, password policy, schema initializer |
| `archbase-security-oidc` | **Opt-in.** External IdP (ArchGuard, Keycloak, Entra ID, Auth0, Okta): bearer validation, token exchange, introspection, logout, back-channel logout, migration between IdPs (`archbase.security.oidc.*`) |
| `archbase-controlplane` | **Opt-in.** Control Plane integration: session token (Ed25519/JWKS), license, entitlements, seats, restricted mode, MQTT provisioning via `TenantProvisioner`, Micrometer metrics + vmalert rules |
| `archbase-multitenancy` | Tenant interceptor, service aspect, tenant-aware task decorator (column isolation with `@TenantId`) |
| `archbase-query` | RSQL for JPA (`ArchbaseRSQLJPASupport`); the row-level security classes are inert (not wired) |
| `archbase-query-contract` | Adapter to `rsql-jpa-search` (`archbase.query.contract.enabled`) |
| `archbase-event-driven-spec` / `archbase-event-driven` | CQRS contracts and annotations / command, query and event buses |
| `archbase-workflow-process` | Workflow engine (`WorkFlowEngine`, `WorkFlowEngineBuilder`): sequential, parallel, conditional, repeat |
| `archbase-plugin-manager` / `archbase-semver-implementation` | Dynamic plugin loading / semver resolution |
| `archbase-validation` / `archbase-validation-ddd-model` | Validation / ArchUnit predicates for the DDD model |
| `archbase-shared-kernel` / `archbase-value-objects` | Common value objects (CPF, CNPJ, Email, Telefone, Endereço) and converters |
| `archbase-architecture` / `archbase-architecture-rules` | Hexagonal, layered and onion annotations / Taikai-ArchUnit rules |
| `archbase-modular-monolith` | Module boundaries, integration events, health checks (`archbase.modulith.*`) |
| `archbase-hypersistence` | Hypersistence Utils dependency (JSON, arrays, ranges), SQL statement count assertions |
| `archbase-analytics` | Audited proxy to the Cube semantic layer, saved queries (`archbase.analytics.*`) |
| `archbase-offline-sync` | Batch sync protocol for offline clients |
| `archbase-error-handling` | `@ControllerAdvice` with standardized error responses |
| `archbase-logging` | Structured logging with correlation id |
| `archbase-mapper` | `ArchbaseMapper` with auto-configuration |
| `archbase-resource-logger` | REST request logging aspect (`@Logging`, `@NoLogging`) |
| `archbase-test-utils` | Fixtures, builders and security helpers for tests |
| `archbase-transformation`, `archbase-annotation-processor`, `archbase-codegen-maven-plugin` | Build-time tooling: ByteBuddy transformations, QueryDSL APT, repository/service generator |
| Starters | `archbase-starter` (= core + security + multitenancy), `-core`, `-security`, `-multitenancy`, `-hypersistence`, `-flyway`, `-messaging` (no JPA/Security), `-analytics`, `-offline-sync` |

### Core Architecture Patterns

1. **Domain-Driven Design (DDD)**
   - JPA entities extend `PersistenceEntityBase` or `TenantPersistenceEntityBase` (not generic; `String` id)
   - Domain model entities extend `DomainEntityBase<T>`, which declares `validate()`
   - Aggregate roots implement `AggregateRoot<T, ID>`
   - Value objects implement `ValueObject`
   - Repositories extend `ArchbaseCommonJpaRepository<T, ID, N>` (JPA + `JpaSpecificationExecutor`, QueryDSL, RSQL)
   - `ArchbaseSpecification<T>` is an in-memory predicate (`isSatisfiedBy`); database filters use RSQL or JPA `Specification`

2. **Multi-Tenancy**
   - Tenant context managed via `ArchbaseTenantContext` (thread-local)
   - Entities extend `TenantPersistenceEntityBase` for tenant awareness
   - Automatic tenant context propagation via async task decorators
   - Configuration: `archbase.multitenancy.enabled=true`

3. **Security**
   - JWT-based authentication with access and refresh tokens (both persisted; `token_use` claim)
   - Permission system using `@HasPermission(action, description, resource, ...)` evaluated by the
     five-gate core (`archbase-security/ARQUITETURA.md`, `MODELO_CORE_AUTORIZACAO.md`)
   - API tokens (`Bearer <uuid>`, stored as hash) for service-to-service communication
   - Access schedule entities exist, but **the framework does not enforce them at login**; an
     application that needs the restriction must validate it itself
   - Custom security configuration via `CustomSecurityConfiguration` / `BaseArchbaseSecurityConfiguration`
   - External bearer tokens through the `ExternalBearerAuthenticator` SPI (Control Plane, OIDC)

4. **Event-Driven Architecture (CQRS)**
   - Commands dispatched via `CommandBus`
   - Events published through `EventBus`
   - Queries handled by `QueryBus`
   - In-memory and synchronous; no auto-configuration: the application `@Import`s `MessageBusSpringConfiguration`
   - Handlers are classes implementing `CommandHandler` / `QueryHandler`, found by `@HandlerScan` package scan and
     mapped with `@CommandMapping` / `@QueryMapping`; the method annotations `@CommandHandler`, `@QueryHandler`,
     `@EventHandler` and `@EventSubscribe` are **not read**
   - `EventBus` is not a bean: `SimpleEventBus` calls each `Subscriber` on the publishing thread

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
   - N+1 query detection with `SQLStatementCountAssertions`
   - `ArchbaseHypersistenceRepository` (`persist`/`merge`/`update`) is **not wired** by the framework; see
     `archbase-hypersistence/README.md`

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
@Table(name = "YOUR_ENTITY")
public class YourEntity extends TenantPersistenceEntityBase {
    // Without multi-tenancy: extends PersistenceEntityBase
    // id is a String (UUID generated in the constructor); audit columns DH_CRIACAO, VERSAO etc. are inherited
}
```

2. **Repository Pattern:**
```java
public interface YourRepository extends ArchbaseCommonJpaRepository<YourEntity, String, Long> {
    // N parameter is the numeric revision type (Envers)
    // Custom queries using QueryDSL or RSQL
}
```
`ArchbaseJpaRepository<T, ID, N>` is the variant for domain aggregates: it requires
`T extends AggregateRoot<T, ID>` and `ID extends Identifier`.

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

4. **Command and Query Handling** (`archbase-event-driven/README.md`):
```java
@Configuration
@Import(MessageBusSpringConfiguration.class)
@HandlerScan(basePackages = "com.yourcompany.handlers")
public class HandlerConfig {
}

// Not a @Component: the bus creates a new instance per dispatch, with dependency injection
@QueryMapping(FindOrder.class)
public class FindOrderHandler implements QueryHandler<FindOrder, OrderDto> {
    @Override
    public OrderDto handle(FindOrder query) { ... }
}

OrderDto dto = queryBus.dispatch(new FindOrder("42"));
```

5. **Specification Pattern and RSQL:**
```java
// In-memory business rule, composable with and()/or()/not()
public class ActiveCustomer implements AbstractArchbaseSpecification<Customer> {
    @Override
    public boolean isSatisfiedBy(Customer candidate) {
        return candidate.isActive();
    }
}

// Database filter from an RSQL string
Specification<YourEntity> spec = ArchbaseRSQLJPASupport.toSpecification("name==John;age>18");
Page<YourEntity> page = repository.findAll(spec, pageable);
```

6. **JSON Column with Hypersistence:**
```java
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

@Entity
public class ProductEntity extends TenantPersistenceEntityBase {
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;
}
```

7. **Inserting without the SELECT of `save()`:** `ArchbaseHypersistenceRepository` is not wired to
   `ArchbaseCommonJpaRepository` (no implementation path); call `EntityManager.persist` directly.

### Configuration Properties

```properties
# Multi-tenancy (starter ligado por padrão)
archbase.multitenancy.enabled=true
archbase.app.tenant.default.id=archbase
archbase.multitenancy.async.propagate-security-context=true

# Security (starter ligado por padrão)
archbase.security.enabled=true
archbase.security.scan-packages=your.package               # varredura de @HasPermission
# OBRIGATÓRIAS, sem default — sem elas a aplicação não sobe (placeholder não resolvido):
archbase.security.jwt.secret-key=${JWT_SECRET}             # Base64 de >= 32 bytes (openssl rand -base64 48)
archbase.security.jwt.token-expiration=3600000             # ms
archbase.security.jwt.refresh-expiration=604800000         # ms
archbase.security.whitelist=/api/v1/public/**              # rotas públicas, separadas por vírgula ("" = nenhuma)
archbase.security.cors.allowed-origins=https://app.example.com
archbase.security.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
archbase.security.cors.allowed-headers=*
archbase.security.cors.allow-credentials=true
# Chave AES do segredo MFA em repouso: Base64 de 16/24/32 bytes (openssl rand -base64 32).
# Vazia, cifrar o segredo falha — ativar MFA não funciona.
archbase.security.crypto.key=${ARCHBASE_CRYPTO_KEY}
archbase.security.mfa.issuer=Archbase
# Validade da senha em dias (0 = sem expiração periódica)
archbase.security.password.expiration-days=0
# Só considera permissões de ações ativas
archbase.security.permission.require-active=false
# Trilha de eventos de segurança e purga
archbase.security.audit.enabled=false
archbase.security.audit.retention-months=12
# Limpeza de tokens expirados
archbase.security.token.cleanup.enabled=false
archbase.security.token.cleanup.retention-days=30

# Esquema de segurança entregue pelo framework (ver deployment/esquema-de-seguranca.md)
# Na subida, compara o mapeamento das entidades de segurança com o banco e cria o que falta.
# Sem versão nem baseline: em banco que já tem tudo, não faz nada. Só comandos aditivos —
# nunca drop, nunca alteração do que já existe — e nada fora das 16 entidades do módulo.
archbase.security.schema.mode=apply                          # apply | report | off
archbase.security.schema.fail-on-error=false                 # true = não sobe se o DDL falhar

# Pre-validacao: nao sobe se uma protecao habilitada nao puder funcionar (fail|warn|off)
archbase.security.hardening.validation=fail

# Security hardening (auditoria 3.0.11)
# Os defaults abaixo PRESERVAM o comportamento anterior à auditoria. Enquanto não forem
# alterados, a proteção correspondente está inerte — ver deployment/security-hardening.md.
archbase.security.admin-endpoints.policy=permit             # permit | admin-only | permission
archbase.security.require-role.no-resolver-policy=permit    # permit | deny
archbase.security.jwt.strict-token-use=false                # recusa token sem o claim token_use
archbase.security.jwt.accept-token-query-param=true         # aceita credencial em ?token=
archbase.security.prevent-user-enumeration=false            # resposta uniforme no reset de senha
archbase.security.api-token.purge-plaintext=false           # IRREVERSÍVEL: apaga o token em claro
archbase.security.api-token.hash-enabled=true                # false = grava o token em claro
archbase.security.admin-guard.enabled=true                   # trava contra criacao/promocao de admin
archbase.security.admin-guard.allow-unverifiable-principal=false
archbase.security.password-change.revoke-sessions=false      # true = desloga ao trocar a senha
archbase.security.logout.enabled=true
archbase.security.logout.url=/api/v1/auth/logout
archbase.security.public-paths.actuator=true
archbase.security.public-paths.swagger=true                 # Swagger/OpenAPI público
archbase.security.public-paths.static-files=true
archbase.security.public-paths.registration=true            # auto-cadastro anônimo
archbase.security.public-paths.legacy-app-routes=true       # rotas de aplicação (deprecado)
archbase.app.tenant.fail-on-missing=false                   # recusa acesso sem tenant no contexto
archbase.app.tenant.accept-query-param=true                 # aceita X-TENANT-ID na query string
# Confere o X-COMPANY-ID contra o usuário autenticado (off | warn | enforce). Exige um bean
# ArchbaseCompanyAccessResolver: o vínculo usuário-empresa é do domínio da aplicação.
# enforce: 403 COMPANY_NOT_ALLOWED; anônimo tem a empresa descartada. Sem resolver, não sobe.
archbase.security.company.validation=off

# Core único de autorização (ver archbase-security/MODELO_CORE_AUTORIZACAO.md)
# Cinco portões: IDENTITY, SCOPE, RESTRICTION, LEVEL, GRANT. Os quatro primeiros só NEGAM;
# só o GRANT concede. Todos os defaults abaixo reproduzem o comportamento anterior ao core.
archbase.security.access-level.enabled=false                # liga o portão LEVEL (piso por capacidade)
archbase.security.access-level.default=READER               # nível de quem não tem perfil, ou perfil sem nível
archbase.security.diagnostics.enabled=false                 # expõe /api/v1/security/diagnostics/*
archbase.security.sync.mode=apply                           # apply | report | refresh
# report não escreve nada. refresh REESCREVE descrição, rótulo e categoria das capacidades
# existentes a partir do código, descartando ajuste do admin — use uma vez e volte para apply.
# O nível mínimo NUNCA é reescrito: é o único dos quatro que muda uma decisão de acesso.
# Os endpoints de diagnóstico exigem isAdministrator MESMO quando ligados — não dependem de
# admin-endpoints.policy, cujo padrão permit deixaria qualquer autenticado entrar.

# Segunda auditoria (delta 3.1.1 -> 3.3.0). Mesmos defaults compatíveis, mesmo WARN de inerte.
# Leitura da trilha restrita ao tenant de quem consulta. SecurityEventEntity não tem @TenantId
# (como ArchbaseSecurityRevision, a revisão do Envers): gravava TENANT_ID e não filtrava na leitura, então admin de um tenant
# lia os eventos de todos. Não vira @TenantId porque a purga é @Scheduled e roda sem tenant.
archbase.security.audit.tenant-scoped=false
# Segredo HS256 do analytics conferido na subida (warn | fail | off). Nascia "" e ninguém checava:
# token de escopo assinado com chave vazia é forjável e anula o DataScopeProvider.
archbase.analytics.secret-validation=warn
archbase.analytics.secret-min-bytes=32

# Rate limiting dos fluxos de credencial (ligado por padrão)
archbase.security.rate-limit.enabled=true
archbase.security.rate-limit.max-attempts=10
archbase.security.rate-limit.window-seconds=900
archbase.security.rate-limit.block-seconds=900

# Descoberta de tenants (GET /auth/tenants) — limite PRÓPRIO e folgado.
# A tela chama esse endpoint a cada digitação de e-mail, e a chave por origem é
# compartilhada por todos atrás do mesmo proxy: reusar o limite do login o
# transformava em negação de serviço.
archbase.security.rate-limit.discovery.max-attempts=200
archbase.security.rate-limit.discovery.block-seconds=300

# Origem da requisição para efeito de contagem. Atrás de proxy, getRemoteAddr()
# devolve o IP do proxy — igual para todos. Ligue SE houver proxy à frente que
# sobrescreva o cabeçalho; sem ele, o atacante troca de identidade a cada
# requisição e ainda bloqueia terceiros forjando o endereço deles.
archbase.security.client-ip.trust-forwarded-for=false

# Força de senha (inteiramente desligada por padrão)
archbase.security.password.min-length=0
archbase.security.password.require-digit=false
archbase.security.password.require-uppercase=false
archbase.security.password.require-lowercase=false
archbase.security.password.require-special=false
archbase.security.password.block-common=false

# Control Plane (archbase-controlplane; ver archbase-controlplane/README.md). Opt-in: sem enabled=true
# nenhum bean é criado. Ligado, exige archbase.app.tenant.fail-on-missing=true e não sobe sem url,
# public-url, product e jwks.url/jwks.local-path. Nenhum valor padrão para segredo.
archbase.controlplane.enabled=false
archbase.controlplane.url=
archbase.controlplane.public-url=                           # o aud do token tem de conter
archbase.controlplane.product=
archbase.controlplane.jwks.url=
archbase.controlplane.jwks.local-path=                      # cópia offline, regravada a cada busca
archbase.controlplane.legacy-jwt=accept                     # accept | reject (reject desliga login local)
archbase.controlplane.seat-required=false
archbase.controlplane.instance-id=
archbase.controlplane.license.path=                         # JWS da licença; sem licença = modo restrito
archbase.controlplane.grace-period=7d
archbase.controlplane.restricted-mode.enforcement=enforce   # enforce | report (use report na implantação)
archbase.controlplane.instance-credential=                  # senha no broker e Bearer do heartbeat HTTPS
archbase.controlplane.product-version=
archbase.controlplane.mqtt.uri=                             # vazio = canal MQTT desligado
archbase.controlplane.heartbeat.interval=5m

# IdP externo (archbase-security-oidc; ver archbase-security-oidc/README.md). Opt-in: sem ao menos
# um provedor, nenhum bean é criado. Erros de configuração impedem a subida, todos listados de uma vez.
# archbase.security.oidc.providers.<id>.issuer=https://idp.example.com/realms/x
# archbase.security.oidc.providers.<id>.audiences=minha-api
# archbase.security.oidc.providers.<id>.tenant.claim=tenant_id    # exatamente uma fonte de tenant
archbase.security.oidc.exchange.enabled=false

# RSQL
archbase.rsql.enabled=true
archbase.rsql.max-page-size=1000
# Lista branca do RSQL também nos segmentos intermediários (cliente em cliente.cpf). A lista negra vale em todos sempre.
archbase.rsql.strict-path-access-control=false

# Plugin manager e workflow engine não têm propriedades: são configurados por código
# (ArchbasePluginManager, WorkFlowEngineBuilder).

# Hypersistence Utils
archbase.hypersistence.enabled=true
# json.*, postgresql.* and repository.* are declared but have no effect (only logged at startup)
```

### Testing Approach

- Unit tests use JUnit Jupiter 6 (from the Spring Boot 4.1.2 BOM)
- Integration tests with `@SpringBootTest`
- Repository tests with `@DataJpaTest`
- Security tests with `@WithMockUser` or custom JWT tokens
- Always verify multi-tenant context in tests when enabled

### Important Considerations

1. **Tenant Context**: When multi-tenancy is enabled, always ensure tenant context is properly set before database operations. Background tasks require the async task decorator for context propagation.
2. **Security**: All endpoints are secured by default (`anyRequest().authenticated()`). Public endpoints go in
   `archbase.security.whitelist`. Only `prePostEnabled` method security is on: `@PermitAll`, `@RolesAllowed`
   and `@Secured` are **ignored silently** and leave the endpoint protected only by the filter chain.
3. **Entity Validation**: Domain entities (`DomainEntityBase`) implement `validate()` returning `ValidationResult`;
   JPA persistence bases do not declare it
4. **Event Ordering**: `SimpleEventBus` delivers synchronously, on the publishing thread, with no defined order among subscribers
5. **Repository Methods**: Prefer RSQL filters over custom queries for better API consistency
6. **Handler Discovery**: `@HandlerScan` finds classes implementing `CommandHandler`/`QueryHandler`; the method annotations are not read
7. **Specification Composition**: Specifications can be composed using `and()`, `or()`, and `not()` for complex queries
8. **Hypersistence Types**: JSON types work with all databases; Array and Range types work only with PostgreSQL. Use `@Type(JsonType.class)` with `@Column(columnDefinition = "jsonb")` for JSON columns
9. **Optimized Persistence**: Use `persist()` for new entities and `update()` for existing ones to avoid unnecessary SELECT queries before INSERT/UPDATE