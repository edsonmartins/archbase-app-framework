[← Anterior: Exemplos de Código](code-examples.md) | [Voltar ao Índice](../README.md) | [Próximo: Troubleshooting →](troubleshooting.md)

---

# Melhores Práticas - archbase-security

## Introdução

Este guia apresenta as melhores práticas para utilização do módulo `archbase-security`, cobrindo design de permissões, configuração de segurança, multi-tenancy e gerenciamento de tokens. Seguir estas recomendações garante uma implementação segura, escalável e de fácil manutenção.

---

## 1. Design de Permissões

### 1.1 Nomenclatura de Recursos e Ações

#### Recursos

**Padrão**: Nomes claros, específicos, singular, em UPPER_SNAKE_CASE.

**Exemplos Corretos**:
```
USER
PRODUCT
SALES_ORDER
FINANCIAL_REPORT
INVOICE
CUSTOMER
PAYMENT
```

**Exemplos Incorretos**:
```
Users              // Plural
user               // Lowercase
sales-order        // Kebab-case
sls_ord            // Abreviação não clara
```

**Regras**:
- Use substantivos no singular
- Seja específico e descritivo
- Evite abreviações pouco claras
- Mantenha consistência com o domínio de negócio
- Use underscores para separar palavras

#### Ações

**Ações Padrão CRUD**:
```
VIEW      // Visualizar/Listar
CREATE    // Criar novo
UPDATE    // Modificar existente
DELETE    // Remover
```

**Ações Especiais** (claras e específicas):
```
EXPORT         // Exportar dados
IMPORT         // Importar dados
EXECUTE        // Executar operação/processo
APPROVE        // Aprovar documento/solicitação
REJECT         // Rejeitar documento/solicitação
CANCEL         // Cancelar operação
ARCHIVE        // Arquivar registro
RESTORE        // Restaurar registro arquivado
PRINT          // Imprimir documento
DOWNLOAD       // Baixar arquivo
UPLOAD         // Enviar arquivo
```

**Ações a Evitar** (muito genéricas):
```
MANAGE         // Muito amplo
ACCESS         // Vago
DO             // Não descritivo
PROCESS        // Use EXECUTE ao invés
```

**Exemplo de Modelagem Completa**:
```java
// Recursos do domínio
public static final String CUSTOMER = "CUSTOMER";
public static final String SALES_ORDER = "SALES_ORDER";
public static final String INVOICE = "INVOICE";

// Ações CRUD
public static final String VIEW = "VIEW";
public static final String CREATE = "CREATE";
public static final String UPDATE = "UPDATE";
public static final String DELETE = "DELETE";

// Ações especiais
public static final String APPROVE = "APPROVE";
public static final String EXPORT = "EXPORT";

// Uso
@HasPermission(action = VIEW, resource = CUSTOMER)
@HasPermission(action = APPROVE, resource = SALES_ORDER)
@HasPermission(action = EXPORT, resource = INVOICE)
```

---

### 1.2 Granularidade de Permissões

A granularidade ideal equilibra segurança, flexibilidade e simplicidade de manutenção.

#### Muito Granular (Não Recomendado)

**Problema**: Dificulta manutenção e cria complexidade desnecessária.

```java
// ❌ Muito granular
VIEW_USER_EMAIL
VIEW_USER_PHONE
VIEW_USER_ADDRESS
VIEW_USER_NAME
UPDATE_USER_EMAIL
UPDATE_USER_PHONE
UPDATE_USER_ADDRESS

// Resultado: 50+ permissões para gerenciar usuários
```

**Desvantagens**:
- Difícil de atribuir corretamente
- Muitas permissões para gerenciar
- Usuários confusos sobre o que precisam
- Matriz de permissões complexa

#### Equilíbrio Adequado (Recomendado)

**Solução**: Agrupe campos relacionados por sensibilidade ou contexto de negócio.

```java
// ✓ Granularidade equilibrada
VIEW_USER                    // Dados básicos
VIEW_USER_SENSITIVE_DATA     // Dados pessoais sensíveis (CPF, RG)
VIEW_USER_FINANCIAL_DATA     // Dados financeiros (salário)
UPDATE_USER                  // Atualização geral
UPDATE_USER_CREDENTIALS      // Senha, autenticação
```

**Vantagens**:
- Fácil de entender
- Simples de atribuir
- Protege dados sensíveis
- Escalável

**Exemplo de Implementação**:
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    // Dados básicos
    @GetMapping("/{id}")
    @HasPermission(action = VIEW, resource = USER)
    public UserBasicDTO getUser(@PathVariable String id) {
        return userService.getBasicInfo(id);
    }

    // Dados sensíveis
    @GetMapping("/{id}/sensitive")
    @HasPermission(action = VIEW, resource = USER_SENSITIVE_DATA)
    public UserSensitiveDTO getSensitiveData(@PathVariable String id) {
        return userService.getSensitiveData(id);
    }

    // Dados financeiros
    @GetMapping("/{id}/financial")
    @HasPermission(action = VIEW, resource = USER_FINANCIAL_DATA)
    public UserFinancialDTO getFinancialData(@PathVariable String id) {
        return userService.getFinancialData(id);
    }
}
```

#### Muito Genérico (Não Recomendado)

**Problema**: Não fornece segurança adequada e viola princípio de menor privilégio.

```java
// ❌ Muito genérico
MANAGE_EVERYTHING
ADMIN_ACCESS
FULL_CONTROL
SUPER_USER
```

**Desvantagens**:
- Viola princípio de menor privilégio
- Dificulta auditoria
- Impossível rastrear quem pode fazer o quê
- Risco de segurança elevado

---

### 1.3 Composição de Permissões

Permita que perfis e grupos componham múltiplas permissões para criar papéis de negócio.

#### Estrutura Hierárquica

```
Empresa
├── Perfis (Roles)
│   ├── Gerente de Vendas
│   ├── Vendedor
│   ├── Analista Financeiro
│   └── Administrador de Sistema
└── Grupos
    ├── Equipe de Vendas
    ├── Departamento Financeiro
    └── TI
```

#### Exemplo: Gerente de Vendas

**Perfil** com conjunto de permissões:

```java
// Criação de perfil composto
Profile salesManager = createProfile("SALES_MANAGER", "Gerente de Vendas");

// Composição de permissões
assignPermissions(salesManager, Arrays.asList(
    // Clientes: acesso completo
    new Permission(VIEW, CUSTOMER),
    new Permission(CREATE, CUSTOMER),
    new Permission(UPDATE, CUSTOMER),
    new Permission(DELETE, CUSTOMER),

    // Pedidos: criar, visualizar e aprovar
    new Permission(VIEW, SALES_ORDER),
    new Permission(CREATE, SALES_ORDER),
    new Permission(UPDATE, SALES_ORDER),
    new Permission(APPROVE, SALES_ORDER),

    // Relatórios: apenas visualizar e exportar
    new Permission(VIEW, SALES_REPORT),
    new Permission(EXPORT, SALES_REPORT),

    // Equipe: gerenciar membros
    new Permission(VIEW, USER),
    new Permission(UPDATE, USER)
));
```

#### Exemplo: Vendedor

**Perfil** com permissões limitadas:

```java
Profile salesperson = createProfile("SALESPERSON", "Vendedor");

assignPermissions(salesperson, Arrays.asList(
    // Clientes: visualizar e criar
    new Permission(VIEW, CUSTOMER),
    new Permission(CREATE, CUSTOMER),

    // Pedidos: criar e visualizar próprios
    new Permission(VIEW, SALES_ORDER),
    new Permission(CREATE, SALES_ORDER),

    // Relatórios: apenas visualizar
    new Permission(VIEW, SALES_REPORT)
));
```

#### Herança e Grupos

**Grupos** podem herdar permissões de múltiplos perfis:

```java
// Grupo com múltiplos perfis
Group salesTeam = createGroup("SALES_TEAM", "Equipe de Vendas");

// Adicionar perfis ao grupo
addProfilesToGroup(salesTeam, Arrays.asList(
    salesManager,
    salesperson
));

// Adicionar usuários ao grupo
addUsersToGroup(salesTeam, Arrays.asList(user1, user2, user3));

// Resultado: Todos os usuários herdam permissões dos perfis do grupo
```

---

### 1.4 Permissões Contextuais

Use os parâmetros `tenantId`, `companyId` e `projectId` para permissões contextuais.

#### Cenário: Multi-Empresa

```java
// Gerente pode aprovar pedidos apenas na própria empresa
@HasPermission(
    action = APPROVE,
    resource = SALES_ORDER,
    companyId = "#context.companyId"  // Apenas sua empresa
)
public void approveSalesOrder(String orderId, SecurityContext context) {
    // Implementação
}
```

#### Cenário: Multi-Projeto

```java
// Desenvolvedor pode modificar código apenas no projeto atribuído
@HasPermission(
    action = UPDATE,
    resource = SOURCE_CODE,
    projectId = "#context.projectId"  // Apenas seu projeto
)
public void updateSourceCode(String fileId, SecurityContext context) {
    // Implementação
}
```

---

## 2. Configuração de Segurança

### 2.1 Secrets e Chaves

#### Nunca Faça Isso

```java
// ❌ NUNCA hardcode secrets
@Configuration
public class SecurityConfig {
    private static final String JWT_SECRET = "my-secret-key";  // NUNCA!
}
```

```properties
# ❌ NUNCA commite secrets no repositório
archbase.security.jwt.secret-key=hardcoded-secret-12345
```

```java
// ❌ NUNCA use a mesma chave em todos os ambientes
if (environment.equals("dev")) {
    secret = "dev-secret";
} else if (environment.equals("prod")) {
    secret = "dev-secret";  // Mesmo secret!
}
```

#### Sempre Faça Isso

**1. Use variáveis de ambiente**:

```bash
# .env (NÃO commitado, listado em .gitignore)
JWT_SECRET=Kj7xYz2Qw9Lp4MnVbBcXfGhTrEwQaZsXdCvFgBnMlKjHgFdSaQwErTyUiOpPaSdF
DATABASE_PASSWORD=another-very-secure-password-here
```

```properties
# application.properties
archbase.security.jwt.secret-key=${JWT_SECRET}
spring.datasource.password=${DATABASE_PASSWORD}
```

**2. Use serviços de gerenciamento de secrets**:

```java
// AWS Secrets Manager
@Configuration
public class AwsSecretsConfig {

    @Bean
    public String jwtSecret() {
        SecretsManagerClient client = SecretsManagerClient.create();
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId("prod/archbase/jwt-secret")
            .build();

        GetSecretValueResponse response = client.getSecretValue(request);
        return response.secretString();
    }
}
```

```java
// Azure Key Vault
@Configuration
public class AzureKeyVaultConfig {

    @Bean
    public String jwtSecret() {
        SecretClient client = new SecretClientBuilder()
            .vaultUrl("https://myvault.vault.azure.net/")
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();

        KeyVaultSecret secret = client.getSecret("jwt-secret");
        return secret.getValue();
    }
}
```

**3. Gere chaves fortes** (mínimo 256 bits):

```bash
# OpenSSL (recomendado)
openssl rand -base64 32

# Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Resultado exemplo:
# Kj7xYz2Qw9Lp4MnVbBcXfGhTrEwQaZsXdCvF
```

**4. Rotacione chaves periodicamente**:

```java
@Service
public class KeyRotationService {

    // Suporta múltiplas chaves para transição suave
    private final List<String> activeKeys;

    public String getCurrentKey() {
        return activeKeys.get(0);  // Chave mais recente
    }

    public List<String> getValidationKeys() {
        return activeKeys;  // Valida com todas as chaves ativas
    }

    @Scheduled(cron = "0 0 0 1 * ?")  // Todo primeiro dia do mês
    public void rotateKeys() {
        String newKey = generateNewKey();
        activeKeys.add(0, newKey);

        // Manter apenas últimas 2 chaves
        if (activeKeys.size() > 2) {
            activeKeys.remove(activeKeys.size() - 1);
        }

        updateKeyInSecretsManager(newKey);
    }
}
```

---

### 2.2 Configuração de CORS

#### Desenvolvimento

Permita todas as origens para facilitar desenvolvimento:

```java
@Configuration
public class CorsConfigDev {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ⚠️ Apenas para desenvolvimento
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);  // Deve ser false quando origin é "*"

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

#### Produção

Restrinja a domínios específicos:

```java
@Configuration
@Profile("prod")
public class CorsConfigProd {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✓ Apenas domínios conhecidos
        configuration.setAllowedOrigins(Arrays.asList(
            "https://app.example.com",
            "https://admin.example.com",
            "https://mobile.example.com"
        ));

        // ✓ Apenas métodos necessários
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ✓ Headers específicos
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Tenant-ID",
            "X-Company-ID"
        ));

        // ✓ Permitir credenciais
        configuration.setAllowCredentials(true);

        // ✓ Cache de preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

#### Configuração Dinâmica por Ambiente

```properties
# application-dev.properties
cors.allowed-origins=*

# application-prod.properties
cors.allowed-origins=https://app.example.com,https://admin.example.com
```

```java
@Configuration
public class DynamicCorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Resto da configuração...
    }
}
```

---

### 2.3 Whitelist de Endpoints Públicos

Seja explícito sobre endpoints públicos para evitar exposição acidental.

#### Estrutura Clara

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            // === Endpoints de Autenticação ===
            .requestMatchers(
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/api/v1/auth/forgot-password",
                "/api/v1/auth/reset-password"
            ).permitAll()

            // === Health Checks ===
            .requestMatchers(
                "/health",
                "/actuator/health",
                "/actuator/info"
            ).permitAll()

            // === Documentação (apenas dev) ===
            .requestMatchers(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**"
            ).permitAll()  // Considere restringir em produção

            // === Recursos Estáticos ===
            .requestMatchers(
                "/public/**",
                "/static/**",
                "/favicon.ico"
            ).permitAll()

            // === Tudo mais requer autenticação ===
            .anyRequest().authenticated()
        );

        return http.build();
    }
}
```

#### Documentação em Produção

**Opção 1**: Desabilitar completamente

```java
@Configuration
@Profile("prod")
public class SwaggerConfigProd {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .enable(false);  // Desabilita Swagger em produção
    }
}
```

**Opção 2**: Proteger com autenticação

```java
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
    .hasRole("ADMIN")  // Apenas admins podem acessar
```

---

### 2.4 HTTPS em Produção

Sempre force HTTPS em ambientes de produção.

#### Forçar HTTPS

```java
@Configuration
@Profile("prod")
public class HttpsConfig {

    @Bean
    public SecurityFilterChain httpsFilterChain(HttpSecurity http) throws Exception {
        http.requiresChannel(channel -> channel
            .anyRequest().requiresSecure()  // Força HTTPS
        );

        return http.build();
    }
}
```

#### Redirect HTTP → HTTPS

```java
@Configuration
@Profile("prod")
public class HttpToHttpsRedirect {

    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");

                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);

                context.addConstraint(securityConstraint);
            }
        };

        tomcat.addAdditionalTomcatConnectors(httpConnector());
        return tomcat;
    }

    private Connector httpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

#### Configuração HSTS (HTTP Strict Transport Security)

```java
http.headers(headers -> headers
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)  // 1 ano
    )
);
```

---

## 3. Multi-Tenancy

### 3.1 Propagação de Contexto

O contexto de tenant deve ser propagado corretamente em toda a aplicação.

#### Filtro de Contexto

**Sempre** defina e limpe o contexto adequadamente:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            // Extrair contexto de headers ou token JWT
            String tenantId = extractTenantId(httpRequest);
            String companyId = extractCompanyId(httpRequest);
            String projectId = extractProjectId(httpRequest);

            // Validar contexto
            if (tenantId == null) {
                throw new SecurityException("Tenant ID obrigatório");
            }

            // Definir contexto
            ArchbaseTenantContext.setTenantId(tenantId);
            ArchbaseTenantContext.setCompanyId(companyId);
            ArchbaseTenantContext.setProjectId(projectId);

            log.debug("Contexto definido: tenant={}, company={}, project={}",
                tenantId, companyId, projectId);

            chain.doFilter(request, response);
        } finally {
            // SEMPRE limpar contexto no finally
            ArchbaseTenantContext.clear();
            log.debug("Contexto limpo");
        }
    }

    private String extractTenantId(HttpServletRequest request) {
        // Prioridade 1: Header HTTP
        String tenantId = request.getHeader("X-Tenant-ID");

        if (tenantId != null) {
            return tenantId;
        }

        // Prioridade 2: Token JWT
        String token = extractJwtToken(request);
        if (token != null) {
            return extractTenantFromToken(token);
        }

        // Prioridade 3: Parâmetro de query (apenas para casos específicos)
        return request.getParameter("tenantId");
    }

    private String extractCompanyId(HttpServletRequest request) {
        String companyId = request.getHeader("X-Company-ID");
        if (companyId != null) {
            return companyId;
        }

        String token = extractJwtToken(request);
        if (token != null) {
            return extractCompanyFromToken(token);
        }

        return null;
    }

    private String extractProjectId(HttpServletRequest request) {
        return request.getHeader("X-Project-ID");
    }
}
```

---

### 3.2 Validação de Tenant

Valide que o usuário autenticado pertence ao tenant requisitado.

#### Validator Service

```java
@Component
public class TenantValidator {

    @Autowired
    private UserRepository userRepository;

    /**
     * Valida se usuário pertence ao tenant requisitado
     */
    public void validateUserBelongsToTenant(String userId, String requestedTenantId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        if (!user.getTenantId().equals(requestedTenantId)) {
            throw new SecurityException(
                String.format("Usuário %s não pertence ao tenant %s", userId, requestedTenantId)
            );
        }
    }

    /**
     * Valida se usuário tem acesso à empresa requisitada
     */
    public void validateUserHasAccessToCompany(String userId, String companyId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        // Verificar se usuário pertence à empresa
        boolean hasAccess = user.getCompanies().stream()
            .anyMatch(company -> company.getId().equals(companyId));

        if (!hasAccess) {
            throw new SecurityException(
                String.format("Usuário %s não tem acesso à empresa %s", userId, companyId)
            );
        }
    }

    /**
     * Valida contexto completo
     */
    public void validateContext(String userId, String tenantId, String companyId) {
        validateUserBelongsToTenant(userId, tenantId);

        if (companyId != null) {
            validateUserHasAccessToCompany(userId, companyId);
        }
    }
}
```

#### Uso em Controllers

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private TenantValidator tenantValidator;

    @GetMapping
    public List<OrderDTO> getOrders(@AuthenticationPrincipal UserDetails userDetails) {
        String tenantId = ArchbaseTenantContext.getTenantId();
        String companyId = ArchbaseTenantContext.getCompanyId();

        // Validar contexto
        tenantValidator.validateContext(userDetails.getUsername(), tenantId, companyId);

        // Buscar pedidos (automaticamente filtrados por tenant)
        return orderService.findAll();
    }
}
```

---

### 3.3 Operações Assíncronas

Use decorators para propagar contexto para threads assíncronas.

#### Task Decorator

```java
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capturar contexto da thread atual
        String tenantId = ArchbaseTenantContext.getTenantId();
        String companyId = ArchbaseTenantContext.getCompanyId();
        String projectId = ArchbaseTenantContext.getProjectId();

        // Retornar Runnable que propaga o contexto
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

#### Configuração Async

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

        // Configurar decorator para propagar contexto
        executor.setTaskDecorator(new TenantAwareTaskDecorator());

        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            String tenantId = ArchbaseTenantContext.getTenantId();
            log.error("Erro em execução assíncrona [tenant={}]: {}", tenantId, ex.getMessage(), ex);
        };
    }
}
```

#### Uso com @Async

```java
@Service
public class NotificationService {

    @Async
    public void sendNotification(String userId, String message) {
        // Contexto de tenant automaticamente propagado
        String tenantId = ArchbaseTenantContext.getTenantId();

        log.info("Enviando notificação para usuário {} no tenant {}", userId, tenantId);

        // Implementação...
    }
}
```

---

## 4. Gerenciamento de Tokens

### 4.1 TTL (Time To Live)

Configure TTLs apropriados baseados no ambiente e requisitos de segurança.

#### Access Tokens

| Ambiente | TTL Recomendado | Milissegundos | Motivo |
|----------|-----------------|---------------|--------|
| **Desenvolvimento** | 24 horas | 86400000 | Conveniência |
| **Homologação** | 4 horas | 14400000 | Simular produção |
| **Produção (Baixa Segurança)** | 4 horas | 14400000 | Aplicações internas |
| **Produção (Média Segurança)** | 1 hora | 3600000 | Recomendado |
| **Produção (Alta Segurança)** | 15-30 min | 900000-1800000 | Sistemas financeiros/saúde |

#### Refresh Tokens

| Ambiente | TTL Recomendado | Milissegundos | Motivo |
|----------|-----------------|---------------|--------|
| **Desenvolvimento** | 30 dias | 2592000000 | Conveniência |
| **Produção (Padrão)** | 7 dias | 604800000 | Equilíbrio |
| **Produção (Mobile)** | 14 dias | 1209600000 | UX móvel |
| **Produção (Alta Segurança)** | 1 dia | 86400000 | Máxima segurança |

#### Configuração por Ambiente

```properties
# application-dev.properties
archbase.security.jwt.token-expiration=86400000      # 24h
archbase.security.jwt.refresh-expiration=2592000000  # 30 dias

# application-prod.properties
archbase.security.jwt.token-expiration=3600000       # 1h
archbase.security.jwt.refresh-expiration=604800000   # 7 dias
```

---

### 4.2 Revogação de Tokens

Sempre revogue tokens em situações de segurança.

#### Service de Revogação

```java
@Service
public class TokenRevocationService {

    @Autowired
    private AccessTokenJpaRepository tokenRepository;

    /**
     * Revoga todos os tokens de um usuário
     */
    @Transactional
    public void revokeAllUserTokens(String userId) {
        List<AccessTokenEntity> tokens = tokenRepository.findAllValidTokenByUserId(userId);

        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setExpired(true);
        });

        tokenRepository.saveAll(tokens);

        log.warn("Todos os tokens do usuário {} foram revogados", userId);
    }

    /**
     * Revoga um token específico
     */
    @Transactional
    public void revokeToken(String tokenValue) {
        AccessTokenEntity token = tokenRepository.findByToken(tokenValue)
            .orElseThrow(() -> new RuntimeException("Token não encontrado"));

        token.setRevoked(true);
        tokenRepository.save(token);

        log.info("Token revogado: {}", tokenValue);
    }

    /**
     * Revoga todos os tokens exceto o atual
     */
    @Transactional
    public void revokeOtherTokens(String userId, String currentToken) {
        List<AccessTokenEntity> tokens = tokenRepository.findAllValidTokenByUserId(userId);

        tokens.stream()
            .filter(token -> !token.getToken().equals(currentToken))
            .forEach(token -> {
                token.setRevoked(true);
                token.setExpired(true);
            });

        tokenRepository.saveAll(tokens);

        log.info("Outros tokens do usuário {} foram revogados", userId);
    }
}
```

#### Cenários de Revogação

```java
@Service
public class SecurityEventService {

    @Autowired
    private TokenRevocationService revocationService;

    /**
     * Logout: revoga token atual
     */
    public void handleLogout(String userId, String token) {
        revocationService.revokeToken(token);
    }

    /**
     * Mudança de senha: revoga todos os tokens
     */
    public void handlePasswordChange(String userId) {
        revocationService.revokeAllUserTokens(userId);
    }

    /**
     * Atividade suspeita: revoga todos os tokens
     */
    public void handleSuspiciousActivity(String userId) {
        revocationService.revokeAllUserTokens(userId);

        // Bloquear conta temporariamente
        userService.lockAccount(userId);

        // Notificar usuário
        notificationService.sendSecurityAlert(userId);
    }

    /**
     * Desativação de conta: revoga todos os tokens
     */
    public void handleAccountDeactivation(String userId) {
        revocationService.revokeAllUserTokens(userId);
        userService.deactivateAccount(userId);
    }

    /**
     * Troca de dispositivo: revoga tokens antigos
     */
    public void handleDeviceChange(String userId, String newToken) {
        revocationService.revokeOtherTokens(userId, newToken);
    }
}
```

---

### 4.3 Limpeza de Tokens Expirados

Implemente jobs periódicos para limpar tokens antigos e manter performance do banco.

#### Job de Limpeza

```java
@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    @Autowired
    private AccessTokenJpaRepository tokenRepository;

    /**
     * Executa limpeza diariamente às 2h da manhã
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Iniciando limpeza de tokens expirados");

        // Remover tokens expirados há mais de 30 dias
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deleted = tokenRepository.deleteExpiredTokensOlderThan(cutoffDate);

        log.info("Limpeza concluída: {} tokens expirados removidos", deleted);
    }

    /**
     * Limpeza adicional de tokens revogados
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupRevokedTokens() {
        log.info("Iniciando limpeza de tokens revogados");

        // Remover tokens revogados há mais de 7 dias
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        int deleted = tokenRepository.deleteRevokedTokensOlderThan(cutoffDate);

        log.info("Limpeza concluída: {} tokens revogados removidos", deleted);
    }
}
```

#### Repository Methods

```java
@Repository
public interface AccessTokenJpaRepository extends JpaRepository<AccessTokenEntity, String> {

    @Modifying
    @Query("DELETE FROM AccessTokenEntity t WHERE t.expired = true AND t.updatedAt < :cutoffDate")
    int deleteExpiredTokensOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM AccessTokenEntity t WHERE t.revoked = true AND t.updatedAt < :cutoffDate")
    int deleteRevokedTokensOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
```

---

### 4.4 Auditoria de Tokens

Mantenha logs de criação, uso e revogação de tokens para auditoria de segurança.

#### Aspect para Auditoria

```java
@Aspect
@Component
public class TokenAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(TokenAuditAspect.class);

    @Autowired
    private AuditLogRepository auditRepository;

    /**
     * Audita criação de tokens
     */
    @AfterReturning(
        pointcut = "execution(* br.com.archbase.security.service.ArchbaseAuthenticationService.authenticate(..))",
        returning = "response"
    )
    public void auditTokenCreation(AuthenticationResponse response) {
        UserEntity user = response.getUser();

        log.info("Token criado: usuário={}, email={}, expiração={}",
            user.getId(),
            user.getEmail(),
            response.getExpirationTime());

        auditRepository.save(AuditLog.builder()
            .action("TOKEN_CREATED")
            .userId(user.getId())
            .tenantId(user.getTenantId())
            .details(String.format("Token criado com expiração em %s", response.getExpirationTime()))
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Audita revogação de tokens
     */
    @AfterReturning("execution(* TokenRevocationService.revokeAllUserTokens(..))")
    public void auditTokenRevocation(JoinPoint joinPoint) {
        String userId = (String) joinPoint.getArgs()[0];

        log.warn("Todos os tokens revogados: usuário={}", userId);

        auditRepository.save(AuditLog.builder()
            .action("TOKENS_REVOKED")
            .userId(userId)
            .severity("HIGH")
            .details("Todos os tokens do usuário foram revogados")
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Audita falhas de validação de tokens
     */
    @AfterThrowing(
        pointcut = "execution(* JwtService.validateToken(..))",
        throwing = "exception"
    )
    public void auditTokenValidationFailure(JoinPoint joinPoint, Exception exception) {
        log.error("Falha na validação de token: {}", exception.getMessage());

        auditRepository.save(AuditLog.builder()
            .action("TOKEN_VALIDATION_FAILED")
            .severity("MEDIUM")
            .details(exception.getMessage())
            .timestamp(LocalDateTime.now())
            .build());
    }
}
```

---

## Resumo das Melhores Práticas

### Design de Permissões
- Use nomenclatura clara (UPPER_SNAKE_CASE para recursos)
- Mantenha granularidade equilibrada
- Componha permissões em perfis e grupos
- Use permissões contextuais (tenantId, companyId, projectId)

### Configuração de Segurança
- Nunca hardcode secrets
- Use variáveis de ambiente ou gerenciadores de secrets
- Gere chaves fortes (mínimo 256 bits)
- Configure CORS restritivo em produção
- Force HTTPS em produção
- Seja explícito sobre endpoints públicos

### Multi-Tenancy
- Sempre defina e limpe contexto corretamente
- Use filtros com finally blocks
- Valide que usuário pertence ao tenant
- Propague contexto para operações assíncronas
- Use TaskDecorator para threads assíncronas

### Gerenciamento de Tokens
- Configure TTLs apropriados ao ambiente
- Revogue tokens em situações de segurança
- Implemente limpeza periódica de tokens
- Mantenha auditoria de criação e revogação
- Monitore falhas de validação

---

**Ver também**:
- [Configuração](configuration.md) - Propriedades detalhadas
- [Anotações e Uso](annotations-and-usage.md) - Como usar @HasPermission
- [Troubleshooting](troubleshooting.md) - Problemas comuns

---

[← Anterior: Exemplos de Código](code-examples.md) | [Voltar ao Índice](../README.md) | [Próximo: Troubleshooting →](troubleshooting.md)
