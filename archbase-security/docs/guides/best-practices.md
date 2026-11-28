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

Os valores da anotação são **literais**: `"#context.companyId"` não é SpEL, é comparado como texto e
nunca casa. O uso normal é deixar `companyId` vazio — a verificação usa a empresa do contexto
(`X-COMPANY-ID`) e casa com permissões cujo `COMPANY_ID` é aquela empresa ou nulo. O estreitamento fica na
**permissão concedida**, não na anotação.

#### Cenário: Multi-Empresa

```java
// Gerente aprova pedidos só na empresa para a qual recebeu a permissão:
// conceda APPROVE em SALES_ORDER com COMPANY_ID = <empresa do gerente>
@HasPermission(action = "APPROVE", resource = "SALES_ORDER", description = "Aprovar pedido de venda")
public void approveSalesOrder(String orderId) {
    // Implementação
}
```

Isso só protege com `archbase.security.company.validation=enforce` e um `ArchbaseCompanyAccessResolver` (ver 3.1).

#### Cenário: Multi-Projeto

Não há projeto no contexto. `projectId` na anotação é um valor fixo, útil apenas quando o método é de um
projeto conhecido em tempo de compilação. Para "o projeto da URL", use `@PreAuthorize` com um bean que
consulte o domínio.

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
JWT_SECRET=<saída de: openssl rand -base64 48>
DATABASE_PASSWORD=<senha forte>
```

```properties
# application.properties
archbase.security.jwt.secret-key=${JWT_SECRET}
spring.datasource.password=${DATABASE_PASSWORD}
```

**2. Use serviços de gerenciamento de secrets** pela integração de configuração do Spring
(`spring.config.import` com Spring Cloud AWS, Azure ou Vault), de modo que o segredo chegue como a
propriedade `archbase.security.jwt.secret-key`. Um `@Bean String jwtSecret()` não serve: o
`ArchbaseJwtService` lê a propriedade com `@Value`, não um bean.

**3. Gere chaves no formato certo.** A chave é **decodificada como Base64 padrão** e precisa dar pelo
menos 32 bytes (HS256):

```bash
# OpenSSL (recomendado): 48 bytes aleatórios em Base64
openssl rand -base64 48

# Node.js
node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"
```

Não use `secrets.token_urlsafe()` do Python nem texto livre: o alfabeto URL-safe (`-`, `_`) é recusado
pelo decoder, e uma string curta dá menos de 32 bytes depois de decodificada. Em ambos os casos a
emissão de token falha.

**4. Rotação de chave.** O framework valida com **uma** chave. Trocar `secret-key` invalida todos os
tokens emitidos, e os usuários precisam entrar de novo. Planeje a troca para uma janela de manutenção;
não existe validação com várias chaves ativas.

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
            // login, cadastro, refresh, sendResetPasswordEmail, resetPassword, mfa/verify
            .requestMatchers("/api/v1/auth/**").permitAll()

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

```properties
# Tira o Swagger da lista pública: passa a exigir autenticação (padrão: true = público)
archbase.security.public-paths.swagger=false
```

Não use `.hasRole("ADMIN")` numa cadeia própria: `getAuthorities()` do usuário é vazio e a regra nega sempre.

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

**Não escreva um filtro de tenant próprio.** O `ArchbaseJwtAuthenticationFilter` já define e limpa o
`ArchbaseTenantContext` em toda requisição, e um filtro anterior a ele que lesse `X-TENANT-ID` do cliente
abriria troca de tenant por cabeçalho forjado.

O que o framework faz:

- **O tenant do token é a fonte de verdade.** Um `X-TENANT-ID` diferente do tenant do token não troca de tenant:
  - JWT próprio e token de API: a credencial é procurada no tenant do cabeçalho, não é achada e a requisição
    segue **não autenticada (401)**;
  - token de IdP externo (Control Plane, OIDC): **403 `TENANT_MISMATCH`**.

  Com o cabeçalho ausente ou igual, o contexto é alinhado ao tenant do token.
- **Sem token** (rota pública), vale o `X-TENANT-ID` — e, com `archbase.app.tenant.accept-query-param=true`
  (padrão), também o parâmetro de query. Desligue o parâmetro em produção.
- `archbase.app.tenant.fail-on-missing=true` recusa requisição sem tenant em vez de seguir com contexto vazio.
- `ArchbaseTenantContext` guarda só **tenant e empresa**; não existe projeto no contexto.

#### Empresa (`X-COMPANY-ID`): ligue a conferência

> **Atenção.** Por padrão, o `X-COMPANY-ID` (cabeçalho ou query) vai para o contexto **sem conferir se o
> usuário pertence àquela empresa**. Ele alimenta o `companyFilter` do Hibernate e o escopo de empresa do
> `@HasPermission`: um usuário com permissão sem escopo de empresa que troque o cabeçalho lê os dados de outra
> empresa do mesmo tenant.

Se a aplicação usa empresa como fronteira de dados, registre um `ArchbaseCompanyAccessResolver` — o vínculo
usuário–empresa é do seu domínio — e ligue a conferência:

```java
@Component
@RequiredArgsConstructor
public class EmpresasDoUsuario implements ArchbaseCompanyAccessResolver {

    private final UserCompanyRepository memberships; // repositório da aplicação

    @Override
    public boolean canAccess(UserEntity user, String companyId) {
        return memberships.existsByUserIdAndCompanyId(user.getId(), companyId);
    }
}
```

```properties
archbase.security.company.validation=warn      # mede: registra no log o que seria recusado
# depois de conferir o log:
archbase.security.company.validation=enforce   # 403 COMPANY_NOT_ALLOWED; anônimo tem a empresa descartada
```

O filtro de autenticação faz a conferência para JWT próprio, token de API e token de IdP externo. Detalhes em
[`deployment/security-hardening.md`](../../../deployment/security-hardening.md#empresa-no-cabeçalho-x-company-id).

---

### 3.2 Operações Assíncronas

Não é preciso escrever `TaskDecorator`. O `archbase-starter-multitenancy` registra o
`ArchbaseTenantAwareTaskDecorator` (se não houver outro `TaskDecorator`), e o executor auto-configurado do
Spring Boot o aplica. Ele copia tenant, empresa e o `SecurityContext` na submissão e **restaura** o estado
anterior da thread do pool no fim.

- `archbase.multitenancy.async.propagate-security-context=false` deixa de copiar o usuário autenticado.
- Executor criado à mão precisa do decorator explicitamente:

```java
ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
executor.setTaskDecorator(new ArchbaseTenantAwareTaskDecorator());
executor.initialize();
```

- Tarefa `@Scheduled` não tem requisição: defina o tenant você mesmo, para cada tenant processado, e limpe
  no `finally`.

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

O framework já revoga nos casos comuns. Use os mecanismos dele em vez de reescrever o serviço:

| Situação | Como |
|----------|------|
| Logout | `POST /api/v1/auth/logout` (`archbase.security.logout.enabled=true`, padrão) revoga o token da requisição |
| Troca de senha | `archbase.security.password-change.revoke-sessions=true` revoga todas as sessões do usuário (padrão `false`) |
| Novo login sem multilogin | Se o usuário não tem `BO_PERMITE_MULTIPLICOS_LOGINS`, as sessões anteriores são revogadas |
| Atividade suspeita, desativação | Chame `AccessTokenJpaRepository.revokeAllTokensOfUser(userId)` dentro de uma transação |
| Um token específico | `AccessTokenJpaRepository.revokeTokenByValue(token)` |

`revokeAllTokensOfUser` e `revokeTokenByValue` são consultas nativas em lote: não carregue os tokens e
salve um a um. Ao desativar um usuário, revogue também os tokens dele: não conte com a desativação
sozinha para encerrar sessões abertas.

---

### 4.3 Limpeza de Tokens Expirados

Ligue o job do framework em vez de escrever um:

```properties
archbase.security.token.cleanup.enabled=true
archbase.security.token.cleanup.cron=0 0 1 * * ?      # padrão: 1h da manhã
archbase.security.token.cleanup.retention-days=30
```

A aplicação precisa ter `@EnableScheduling`.

---

### 4.4 Auditoria

A trilha de eventos de segurança do framework registra `LOGIN`, `LOGIN_FALHOU`, `LOGOUT`, `ACESSO_NEGADO`
e `SIMULACAO` na tabela `SEGURANCA_EVENTO`. `LOGOUT` é gravado pelo `POST /api/v1/auth/logout` com o usuário e o
tenant do token encerrado; `SIMULACAO`, pelo `POST /api/v1/security/diagnostics/simulate`, com quem perguntou e,
no detalhe, sobre quem. Até a 3.5.0 esses dois tipos existiam e nada os gravava.

```properties
archbase.security.audit.enabled=true
archbase.security.audit.tenant-scoped=true     # leitura restrita ao tenant de quem consulta
archbase.security.audit.retention-months=12
```

A consulta é `GET /api/v1/security/audit/events`. Com `audit.tenant-scoped=false` (padrão, por
compatibilidade), um administrador de um tenant lê os eventos de todos — ligue-o.

---

## Resumo das Melhores Práticas

### Design de Permissões
- Use nomenclatura clara (UPPER_SNAKE_CASE para recursos)
- Mantenha granularidade equilibrada
- Componha permissões em perfis e grupos
- Estreite por empresa na permissão concedida, não na anotação

### Configuração de Segurança
- Nunca hardcode secrets
- Use variáveis de ambiente ou gerenciadores de secrets
- Gere a chave JWT com `openssl rand -base64 48` (Base64 padrão, >= 32 bytes)
- Configure CORS restritivo em produção
- Force HTTPS em produção
- Seja explícito sobre endpoints públicos

### Multi-Tenancy
- Não escreva filtro de tenant: o tenant do token é a fonte de verdade
- Com empresa como fronteira de dados, registre `ArchbaseCompanyAccessResolver` e ligue `company.validation`
- Desligue `archbase.app.tenant.accept-query-param` e ligue `fail-on-missing` em produção
- Use o `ArchbaseTenantAwareTaskDecorator` em executores criados à mão

### Gerenciamento de Tokens
- Configure TTLs apropriados ao ambiente
- Ligue `password-change.revoke-sessions` e revogue com `revokeAllTokensOfUser` ao desativar
- Ligue `token.cleanup.enabled` em vez de escrever um job
- Ligue `audit.enabled` e `audit.tenant-scoped`

---

**Ver também**:
- [Configuração](configuration.md) - Propriedades detalhadas
- [Anotações e Uso](annotations-and-usage.md) - Como usar @HasPermission
- [Troubleshooting](troubleshooting.md) - Problemas comuns

---

[← Anterior: Exemplos de Código](code-examples.md) | [Voltar ao Índice](../README.md) | [Próximo: Troubleshooting →](troubleshooting.md)
