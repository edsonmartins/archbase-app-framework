# Guia de Segurança do Archbase

Este documento aborda todos os aspectos de segurança do Archbase, incluindo:
- Sistema de autenticação unificado com suporte a múltiplos contextos
- Customização da configuração de segurança
- Anotações de segurança disponíveis
- Sistema de enriquecimento de respostas
- Integração com lógica de negócio via delegates

## Visão Geral

O Archbase oferece um sistema completo de segurança que inclui:

### 1. Sistema de Autenticação Unificado
- Login flexível (email/telefone)
- Login social (Google, Facebook, etc.)
- Suporte a múltiplos contextos (STORE_APP, CUSTOMER_APP, DRIVER_APP, WEB_ADMIN)
- Sistema de enriquecimento de respostas via `AuthenticationResponseEnricher`
- Integração com lógica de negócio via `AuthenticationBusinessDelegate`

### 2. Configuração de Segurança
- Proteção CSRF configurável
- Configuração de CORS
- Filtro JWT para autenticação
- Lista de endpoints públicos (whitelist)
- Configuração básica de autorização

### 3. Anotações de Segurança
- `@HasPermission` - Controle baseado em Resource/Action
- `@RequireProfile` - Controle baseado em profiles
- `@RequireRole` - Controle baseado em roles customizadas
- `@RequirePersona` - Controle baseado em personas de negócio

## Como Sobrescrever a Configuração Padrão

### Passo 1: Crie uma Nova Classe de Configuração

Crie uma classe que estenda `BaseArchbaseSecurityConfiguration` e implemente a interface `CustomSecurityConfiguration`:

```java
package seu.pacote.config;

import br.com.archbase.security.config.BaseArchbaseSecurityConfiguration;
import br.com.archbase.security.config.CustomSecurityConfiguration;
import br.com.archbase.security.config.CustomAccessDeniedHandler;
import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.List;

@Configuration
@EnableWebSecurity
public class MinhaConfiguracaoSeguranca extends BaseArchbaseSecurityConfiguration 
                                        implements CustomSecurityConfiguration {

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;
    
    @Autowired
    private ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter;
    
    // Outros beans necessários
    
    // Implementação dos métodos abstratos
}
```

### Passo 2: Implemente os Métodos Abstratos Obrigatórios

Você precisa implementar todos os métodos abstratos definidos em `BaseArchbaseSecurityConfiguration`:

```java
@Override
protected CustomAccessDeniedHandler getAccessDeniedHandler() {
    return accessDeniedHandler;
}

@Override
protected List<String> getAllowedOrigins() {
    return List.of("https://seu-dominio.com", "http://localhost:3000");
}

@Override
protected List<String> getAllowedMethods() {
    return List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
}

@Override
protected List<String> getAllowedHeaders() {
    return List.of("Authorization", "Content-Type", "X-TENANT-ID");
}

@Override
protected boolean getAllowCredentials() {
    return true;
}

@Override
protected List<String> getWhiteListUrls() {
    return List.of(
        "/api/v1/auth/**",
        "/api/v1/public/**",
        "/swagger-ui/**",
        "/v3/api-docs/**"
        // Adicione outras URLs públicas aqui
    );
}

@Override
protected void configureAuthorizationRules(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        // Configurações específicas para diferentes caminhos
        .requestMatchers("/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/v1/private/**").authenticated()
        .anyRequest().authenticated()
    );
}

@Override
protected ArchbaseJwtAuthenticationFilter getJwtAuthenticationFilter() {
    return jwtAuthenticationFilter;
}
```

### Passo 3: Personalize a Configuração Conforme Necessário

Você pode sobrescrever o método `configure` para modificar comportamentos específicos:

```java
@Override
public void configure(HttpSecurity http) throws Exception {
    List<String> whiteListUrls = getWhiteListUrls();
    AntPathRequestMatcher[] matchers = whiteListUrls.stream()
            .map(AntPathRequestMatcher::new)
            .toArray(AntPathRequestMatcher[]::new);

    // Exemplo: Habilitar CSRF para um ambiente específico
    http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/auth/**"))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(matchers).permitAll();
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                try {
                    configureAuthorizationRules(http);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .addFilterBefore(getJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(handling -> handling
                    .accessDeniedHandler(getAccessDeniedHandler()));
}
```

## Exemplos Comuns de Customização

### 1. Habilitando CSRF para Certos Endpoints

```java
@Override
public void configure(HttpSecurity http) throws Exception {
    List<String> whiteListUrls = getWhiteListUrls();
    AntPathRequestMatcher[] matchers = whiteListUrls.stream()
            .map(AntPathRequestMatcher::new)
            .toArray(AntPathRequestMatcher[]::new);

    http.csrf(csrf -> csrf
            // Desabilitar CSRF apenas para endpoints específicos
            .ignoringRequestMatchers("/api/v1/auth/**", "/api/v1/webhook/**")
        )
        // Resto da configuração...
}
```

### 2. Configurando Regras de Autorização Avançadas

```java
@Override
protected void configureAuthorizationRules(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/v1/financeiro/**").hasAnyRole("FINANCEIRO", "ADMIN")
        .requestMatchers("/api/v1/relatorios/**").hasAuthority("GERAR_RELATORIOS")
        .requestMatchers("/api/v1/produtos/**").authenticated()
        .anyRequest().authenticated()
    );
}
```

### 3. Configurando CORS para Domínios Específicos

```java
@Override
protected List<String> getAllowedOrigins() {
    return List.of(
        "https://app.seudominio.com",
        "https://admin.seudominio.com",
        "http://localhost:3000"
    );
}

@Override
protected List<String> getAllowedMethods() {
    return List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
}

@Override
protected List<String> getAllowedHeaders() {
    return List.of(
        "Authorization", 
        "Content-Type", 
        "X-TENANT-ID", 
        "X-COMPANY-ID",
        "X-Custom-Header"
    );
}
```

### 4. Adicionando Tratamento de Exceção Personalizado

```java
@Override
public void configure(HttpSecurity http) throws Exception {
    // Configuração básica...
    
    http.exceptionHandling(ex -> ex
        .accessDeniedHandler(getAccessDeniedHandler())
        .authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\":\"Autenticação necessária\",\"mensagem\":\"" 
                    + authException.getMessage() + "\"}");
        })
    );
}
```

## Resolução de Problemas

### A Configuração Personalizada Não Está Sendo Aplicada

1. Verifique se sua classe está anotada corretamente com `@Configuration` e `@EnableWebSecurity`
2. Confirme que sua classe implementa a interface `CustomSecurityConfiguration`
3. Certifique-se de que a classe está sendo escaneada pelo componente scan do Spring:
    - Ela deve estar em um pacote escaneado pelo `@ComponentScan`
    - Ou deve ser explicitamente importada usando `@Import`

### Diagnosticando Problemas de Configuração

Adicione logs em sua implementação para verificar se ela está sendo carregada:

```java
@PostConstruct
public void init() {
    System.out.println("============================================");
    System.out.println("CONFIGURAÇÃO DE SEGURANÇA PERSONALIZADA ATIVA");
    System.out.println("============================================");
}
```

## Exemplo Completo

Aqui está um exemplo completo de uma configuração personalizada:

```java
package com.minhaempresa.config;

import br.com.archbase.security.config.BaseArchbaseSecurityConfiguration;
import br.com.archbase.security.config.CustomSecurityConfiguration;
import br.com.archbase.security.config.CustomAccessDeniedHandler;
import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class MinhaConfiguracaoSeguranca extends BaseArchbaseSecurityConfiguration 
                                        implements CustomSecurityConfiguration {

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;
    
    @Autowired
    private ArchbaseJwtAuthenticationFilter jwtAuthenticationFilter;
    
    @PostConstruct
    public void init() {
        System.out.println("============================================");
        System.out.println("CONFIGURAÇÃO DE SEGURANÇA PERSONALIZADA ATIVA");
        System.out.println("============================================");
    }
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        List<String> whiteListUrls = getWhiteListUrls();
        AntPathRequestMatcher[] matchers = whiteListUrls.stream()
                .map(AntPathRequestMatcher::new)
                .toArray(AntPathRequestMatcher[]::new);

        http.csrf().disable()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(matchers).permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    try {
                        configureAuthorizationRules(http);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(getJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handling -> handling
                        .accessDeniedHandler(getAccessDeniedHandler()));
    }

    @Override
    protected CustomAccessDeniedHandler getAccessDeniedHandler() {
        return accessDeniedHandler;
    }

    @Override
    protected List<String> getAllowedOrigins() {
        return List.of("https://app.minhaempresa.com", "http://localhost:3000");
    }

    @Override
    protected List<String> getAllowedMethods() {
        return List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

    @Override
    protected List<String> getAllowedHeaders() {
        return List.of("Authorization", "Content-Type", "X-TENANT-ID");
    }

    @Override
    protected boolean getAllowCredentials() {
        return true;
    }

    @Override
    protected List<String> getWhiteListUrls() {
        return Lists.newArrayList(
                "/api/v1/auth/**",
                "/api/v1/apiToken/activate",
                "/api/v1/webhook/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
        );
    }

    @Override
    protected void configureAuthorizationRules(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/gestao/**").hasAnyRole("GESTOR", "ADMIN")
                .anyRequest().authenticated()
        );
    }

    @Override
    protected ArchbaseJwtAuthenticationFilter getJwtAuthenticationFilter() {
        return jwtAuthenticationFilter;
    }
}
```

---

# Sistema de Autenticação Unificado

O Archbase fornece um sistema de autenticação unificado que permite que aplicações customizem o processo de login e registro através de interfaces bem definidas.

## AuthenticationBusinessDelegate

Interface que permite que aplicações adicionem lógica de negócio específica durante autenticação:

```java
public interface AuthenticationBusinessDelegate {
    
    // Chamado após registro bem-sucedido
    String onUserRegistered(User user, Map<String, Object> registrationData);
    
    // Enriquece resposta de autenticação com dados específicos
    AuthenticationResponse enrichAuthenticationResponse(
        AuthenticationResponse baseResponse, 
        String context, 
        HttpServletRequest request
    );
    
    // Valida se um contexto é suportado
    boolean supportsContext(String context);
    
    // Retorna lista de contextos suportados
    List<String> getSupportedContexts();
    
    // Validações pré-autenticação
    default void preAuthenticate(String email, String context) { }
    
    // Ações pós-autenticação
    default void postAuthenticate(User user, String context) { }
    
    // Login social
    default String onSocialLogin(String provider, Map<String, Object> providerData) {
        throw new UnsupportedOperationException("Login social não implementado");
    }
}
```

### Implementação na Aplicação

```java
@Component
@Primary
public class MinhaAppAuthenticationDelegate implements AuthenticationBusinessDelegate {
    
    @Override
    public String onUserRegistered(User user, Map<String, Object> registrationData) {
        // Criar entidade de negócio (ex: UserApp)
        UserApp userApp = UserApp.builder()
            .securityUser(user)
            .name((String) registrationData.get("name"))
            .phone((String) registrationData.get("phone"))
            .build();
            
        userApp = userAppService.save(userApp);
        return userApp.getId();
    }
    
    @Override
    public AuthenticationResponse enrichAuthenticationResponse(
            AuthenticationResponse baseResponse, 
            String context, 
            HttpServletRequest request) {
        
        // Enriquecer resposta baseado no contexto
        switch (context) {
            case "STORE_APP":
                return enrichStoreResponse(baseResponse);
            case "CUSTOMER_APP":
                return enrichCustomerResponse(baseResponse);
            default:
                return baseResponse;
        }
    }
}
```

## AuthenticationResponseEnricher

Interface para enriquecer respostas de autenticação:

```java
public interface AuthenticationResponseEnricher {
    
    // Enriquece a resposta de autenticação
    AuthenticationResponse enrich(
        AuthenticationResponse baseResponse, 
        String context, 
        HttpServletRequest request
    );
    
    // Verifica se suporta o contexto
    default boolean supports(String context) { return true; }
    
    // Ordem de execução (menor = primeiro)
    default int getOrder() { return 0; }
}
```

## Endpoints de Autenticação

### 1. Login Contextual
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "usuario@exemplo.com",
  "password": "senha123",
  "context": "STORE_APP",
  "contextData": "{\"storeId\": \"123\"}"
}
```

### 2. Login Flexível (Email ou Telefone)
```http
POST /api/v1/auth/login-flexible
Content-Type: application/json

{
  "identifier": "usuario@exemplo.com ou 11999999999",
  "password": "senha123",
  "context": "CUSTOMER_APP"
}
```

### 3. Login Social
```http
POST /api/v1/auth/login-social
Content-Type: application/json

{
  "provider": "google",
  "token": "token-do-google",
  "context": "CUSTOMER_APP"
}
```

### 4. Registro com Dados Adicionais
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "João Silva",
  "email": "joao@exemplo.com",
  "password": "senha123",
  "role": "USER",
  "additionalData": {
    "phone": "+5511999999999",
    "storeId": "123",
    "cpf": "12345678900"
  }
}
```

### 5. Listar Contextos Suportados
```http
GET /api/v1/auth/contexts

Response:
{
  "supportedContexts": ["STORE_APP", "CUSTOMER_APP", "DRIVER_APP", "WEB_ADMIN"],
  "defaultContext": "WEB_ADMIN"
}
```

---

# Anotações de Segurança do Archbase

O Archbase Security oferece um conjunto de anotações para controle de acesso granular em métodos e classes. Estas anotações são processadas através do Spring Security usando `AuthorizationManager` customizados.

## Anotações Disponíveis

### 1. @HasPermission (Existente)

Controle baseado no sistema Resource/Action do Archbase:

```java
@HasPermission(resource = "USER", action = "CREATE")
public User createUser(UserDto userDto) {
    // Método protegido por permissão específica
}
```

**Parâmetros:**
- `resource` - Nome do recurso (ex: "USER", "PRODUCT", "ORDER")
- `action` - Ação permitida (ex: "CREATE", "READ", "UPDATE", "DELETE")
- `tenantId`, `companyId`, `projectId` - Contexto multi-tenant

### 2. @RequireProfile

Controle baseado em profiles do Archbase:

```java
@RequireProfile({"ADMIN", "MANAGER"})
public void adminOnlyMethod() {
    // Método acessível por usuários com profile ADMIN ou MANAGER
}

@RequireProfile(value = {"ADMIN", "FINANCE"}, requireAll = true)
public void restrictedMethod() {
    // Usuário deve ter AMBOS os profiles: ADMIN E FINANCE
}
```

**Parâmetros:**
- `value` - Array de profiles necessários
- `requireAll` - Se true, usuário deve ter TODOS os profiles (AND). Se false, apenas UM (OR)
- `resource`, `action` - Validação adicional de permissão se especificado
- `allowSystemAdmin` - Permite bypass para administradores (default: true)
- `requireActiveUser` - Verifica se usuário está ativo (default: true)

### 3. @RequireRole

Controle baseado em roles customizadas (extensível):

```java
@RequireRole("STORE_MANAGER")
public void storeManagerMethod() {
    // Método para gerentes de loja
}

@RequireRole(value = {"OWNER", "PARTNER"}, requirePlatformAdmin = true)
public void platformAdminMethod() {
    // Método que requer ser admin da plataforma E ter role OWNER ou PARTNER
}
```

**Parâmetros:**
- `value` - Array de roles necessárias
- `requireAll` - Se true, usuário deve ter TODAS as roles (AND)
- `requirePlatformAdmin` - Requer que seja admin da plataforma
- `ownerOnly` - Permite acesso apenas para owners (não funcionários)
- `context` - Contexto específico para validação condicional
- `allowSystemAdmin` - Permite bypass para administradores

### 4. @RequirePersona (Nova)

Controle baseado em personas de negócio com suporte a contexto:

```java
@RequirePersona("CUSTOMER")
public void customerOnlyMethod() {
    // Método acessível apenas por clientes
}

@RequirePersona(value = "STORE_ADMIN", context = "STORE_APP")
public void storeAdminMethod() {
    // Método para admins de loja no contexto do app da loja
}

@RequirePersona(
    value = {"DRIVER", "STORE_ADMIN"}, 
    context = "DELIVERY_APP",
    contextData = "{\"region\": \"SP\", \"activeOnly\": true}"
)
public void deliveryMethod() {
    // Método para motoristas ou admins no app de delivery
    // com dados de contexto específicos
}
```

**Parâmetros:**
- `value` - Array de personas necessárias
- `requireAll` - Se true, usuário deve ter TODAS as personas
- `context` - Contexto da aplicação ("STORE_APP", "CUSTOMER_APP", "DRIVER_APP", "WEB_ADMIN")
- `contextData` - Dados de contexto como JSON para validações específicas
- `ownerOnly` - Permite acesso apenas para proprietários
- `resource`, `action` - Validação adicional de permissão
- `allowSystemAdmin` - Permite bypass para administradores

## Exemplos de Uso em Controllers

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @GetMapping
    @RequireProfile("USER") // Qualquer usuário logado
    public List<Product> listProducts() {
        return productService.findAll();
    }

    @PostMapping
    @HasPermission(resource = "PRODUCT", action = "CREATE")
    public Product createProduct(@RequestBody ProductDto dto) {
        return productService.create(dto);
    }

    @PutMapping("/{id}")
    @RequirePersona(value = "STORE_ADMIN", context = "STORE_APP")
    public Product updateProduct(@PathVariable String id, @RequestBody ProductDto dto) {
        return productService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @RequireRole(value = "STORE_OWNER", ownerOnly = true)
    public void deleteProduct(@PathVariable String id) {
        productService.delete(id);
    }

    @GetMapping("/reports")
    @RequireProfile(value = {"ADMIN", "FINANCE"}, requireAll = true)
    @HasPermission(resource = "REPORTS", action = "GENERATE")
    public ProductReport generateReport() {
        return reportService.generateProductReport();
    }
}
```

## Combinando Anotações

As anotações podem ser combinadas para validações mais complexas:

```java
@RequireProfile("MANAGER")
@HasPermission(resource = "FINANCIAL", action = "READ")
public FinancialReport getFinancialReport() {
    // Usuário deve ter profile MANAGER E permissão FINANCIAL:READ
}
```

## Extensibilidade via Enrichers

As anotações `@RequireRole` e `@RequirePersona` são extensíveis através do sistema de enrichers do Archbase. Aplicações podem implementar lógica customizada de validação baseada em:

- Contexto da aplicação (STORE_APP, CUSTOMER_APP, etc.)
- Dados específicos do domínio (store, region, etc.)
- Regras de negócio complexas

## Tratamento de Erros

Quando o acesso é negado, as anotações lançam `AccessDeniedException` com mensagens customizáveis:

```java
@RequirePersona(value = "STORE_ADMIN", message = "Apenas administradores de loja podem acessar este recurso")
public void restrictedMethod() {
    // ...
}
```

## Configuração

As anotações são automaticamente configuradas através do `MethodSecurityConfig` e processadas pelos respectivos `AuthorizationManager`:

- `CustomAuthorizationManager` - processa `@HasPermission`
- `ProfileAuthorizationManager` - processa `@RequireProfile`
- `RoleAuthorizationManager` - processa `@RequireRole`
- `PersonaAuthorizationManager` - processa `@RequirePersona`

---

## Fluxo de Autenticação Completo

```
1. Cliente faz login com contexto
   ↓
2. ArchbaseAuthenticationController recebe request
   ↓
3. AuthenticationBusinessDelegate.preAuthenticate() (se existir)
   ↓
4. ArchbaseAuthenticationService.authenticate()
   ↓
5. AuthenticationBusinessDelegate.postAuthenticate() (se existir)
   ↓
6. AuthenticationResponseEnricher.enrich() (todos os enrichers)
   ↓
7. AuthenticationBusinessDelegate.enrichAuthenticationResponse() (se existir)
   ↓
8. Retorna resposta enriquecida ao cliente
```

## Migração de Sistema Legado

Para migrar de um sistema de autenticação legado:

1. **Implemente AuthenticationBusinessDelegate** na sua aplicação
2. **Mova lógica de criação de UserApp** para `onUserRegistered()`
3. **Mova lógica de enriquecimento** para `enrichAuthenticationResponse()`
4. **Configure validações customizadas** em `preAuthenticate()` e `postAuthenticate()`
5. **Remova controllers de autenticação duplicados** e use os do Archbase

## Considerações Finais

O sistema de segurança do Archbase oferece:

1. **Separação clara** entre infraestrutura (Archbase) e lógica de negócio (aplicação)
2. **Extensibilidade** através de interfaces bem definidas
3. **Suporte a múltiplos contextos** para diferentes tipos de aplicações
4. **Flexibilidade** para customizar cada aspecto do processo de autenticação

Certifique-se de:
- Implementar corretamente as interfaces quando necessário
- Entender as implicações de segurança das customizações
- Manter a separação entre infraestrutura e negócio
- Documentar contextos e personas específicas da sua aplicação