# Guia para Customização da Configuração de Segurança no Archbase

Este documento descreve como sobrescrever a configuração de segurança padrão do Archbase com uma configuração personalizada para atender às necessidades específicas do seu projeto.

## Visão Geral

O Archbase oferece uma configuração de segurança padrão através da classe `DefaultArchbaseSecurityConfiguration`. Esta configuração inclui:

- Proteção CSRF desabilitada
- Configuração de CORS
- Filtro JWT para autenticação
- Lista de endpoints públicos (whitelist)
- Configuração básica de autorização

Se você precisar personalizar esses comportamentos, pode criar sua própria configuração de segurança.

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

## Considerações Finais

A customização da configuração de segurança oferece grande flexibilidade, mas também traz responsabilidades. Certifique-se de entender completamente as implicações das alterações que você fizer na configuração de segurança.

Lembre-se que desabilitar mecanismos de segurança como CSRF deve ser feito com consciência dos riscos associados, e idealmente apenas para endpoints específicos onde essa proteção não é aplicável (como APIs RESTful sem estado).