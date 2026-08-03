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

> **Referência completa:** [ARQUITETURA.md](ARQUITETURA.md). Esta seção é o resumo prático.

As anotações **não decidem** — elas declaram o que o endpoint exige. Quem decide é um avaliador
único, o `ArchbaseAccessEvaluator`, que roda cinco portões na mesma ordem, sempre:

```
1 IDENTITY   → 2 SCOPE → 3 RESTRICTION → 4 LEVEL → 5 GRANT
  nega          nega       nega            nega      CONCEDE
```

**A regra que organiza tudo: os portões 1 a 4 só sabem negar. Só o portão 5 concede.**

| Anotação | Declara | Pode conceder? | Precisa de |
|---|---|---|---|
| `@HasPermission` | uma capacidade (recurso + ação) | **sim** — é a única | catálogo alimentado |
| `@RequireProfile` | tranca por perfil | não, só nega | o usuário ter perfil |
| `@RequireRole` | tranca por papel do domínio | não, só nega | um `ArchbaseRoleResolver` |
| `@RequirePersona` | tranca por persona | não, só nega | mapeamento perfil → persona |
| `@ArchbaseResource` | o recurso da classe | — | vai na **classe** |

---

## 1. `@HasPermission` — a que concede

```java
@RestController
@RequestMapping("/api/v1/ordens-servico")
@ArchbaseResource(value = "tms.ordemservico", description = "Ordem de serviço")
public class OrdemServicoController {

    @GetMapping
    @HasPermission(action = "view", description = "Listar ordens de serviço")
    public ResponseEntity<Page<OrdemServicoDto>> listar() { ... }

    @PostMapping("/{id}/aprovar-custo")
    @HasPermission(action = "aprovar_custo", description = "Aprovar o custo da OS",
                   minimumLevel = AccessLevel.SUPERVISOR)
    public ResponseEntity<Void> aprovarCusto(@PathVariable String id) { ... }
}
```

**Parâmetros**

| Parâmetro | Obrigatório | O que faz |
|---|---|---|
| `action` | sim | nome da ação no catálogo |
| `description` | **sim** | vira a descrição da Action; é o que o admin lê ao conceder |
| `resource` | não | vazio herda de `@ArchbaseResource` na classe |
| `minimumLevel` | não | piso da capacidade; semeia `MINIMUM_LEVEL` no primeiro registro |
| `tenantId`, `companyId`, `projectId` | não | estreitamento de escopo |

> ⚠ **`description` não tem valor padrão.** Exemplos que a omitem — inclusive versões antigas desta
> documentação — **não compilam**.

> ⚠ **`@HasPermission` é `@Target(METHOD)`**, de propósito. Uma capacidade é *recurso + ação*, e a
> ação é sempre por método; na classe, todo método herdaria a mesma ação e um `DELETE` passaria a
> exigir apenas `view`. Para não repetir o recurso, use `@ArchbaseResource`.

### `minimumLevel` é semente, não lei

O valor é gravado em `SEGURANCA_ACAO.MINIMUM_LEVEL` no **primeiro registro** da ação e nunca
sobrescrito depois. A partir daí quem manda é o admin — igual já acontece com a descrição. O
desenvolvedor declara o piso que conhece; a operação ajusta o que conhece melhor, sem deploy.

A escala tem quatro degraus: `READER < OPERATOR < SUPERVISOR < TENANT_ADMIN`. O portão só é avaliado
com `archbase.security.access-level.enabled=true`.

**Piso, não substituto:** alcançar o nível não concede nada. O acesso continua dependendo de
permissão no catálogo; o nível apenas impede que uma concessão indevida valha.

---

## 2. `@RequireProfile`

```java
@RequireProfile("SUPERVISOR")
public void fecharCompetencia() { ... }

// Administrador NÃO é isento: a tranca vale para todos.
@RequireProfile(value = "AUDITORIA", allowSystemAdmin = false)
public void exportarTrilha() { ... }
```

| Parâmetro | Padrão | O que faz |
|---|---|---|
| `value` | — | perfis aceitos |
| `requireAll` | `false` | exige todos em vez de um |
| `allowSystemAdmin` | `true` | isenta o administrador **desta** tranca |
| `requireActiveUser` | `true` | exige conta ativa |
| `resource`, `action` | vazio | soma uma capacidade ao requisito |
| `message` | — | texto do 403 |

> ⚠ **O usuário tem um perfil só** (`UserEntity.profile` é `@ManyToOne`). `requireAll = true` com
> dois perfis é insatisfazível por construção.

---

## 3. `@RequireRole`

```java
@RequireRole("GESTOR_FROTA")
public void reatribuirVeiculo() { ... }
```

| Parâmetro | Padrão | O que faz |
|---|---|---|
| `value` | — | papéis aceitos |
| `requireAll` | `false` | exige todos em vez de um |
| `requirePlatformAdmin` | `false` | exige `isAdministrator` |
| `ownerOnly` | `false` | exige propriedade, respondida pelo SPI |
| `allowSystemAdmin` | `true` | isenta o administrador desta tranca |

**As roles são do domínio da aplicação, não do Archbase.** Quem as conhece é o
`ArchbaseRoleResolver` que o projeto registra:

```java
@Component
public class MinhasRoles implements ArchbaseRoleResolver {

    @Override
    public Set<String> resolveRoles(UserEntity user) {
        return colaboradorRepository.rolesDe(user.getId());
    }

    @Override
    public boolean isOwner(UserEntity user) {
        return colaboradorRepository.ehProprietario(user.getId());
    }
}
```

> ⚠ **Sem esse bean, `@RequireRole` não valida nada.** O comportamento é decidido por
> `archbase.security.require-role.no-resolver-policy`, cujo padrão é `permit` — ou seja, a anotação
> **libera qualquer usuário ativo, ignorando os próprios valores**. Registre o resolver e mude para
> `deny`.

> ⚠ `ownerOnly` com **mais de um** resolver registrado sempre nega: `isOwner(user)` não identifica o
> domínio, então nada liga a propriedade que um resolver afirma à role que outro forneceu.
> Consolide em um resolver que conheça os dois lados.

> ⚠ `allowSystemAdmin` (padrão `true`) libera o administrador **antes** de `requirePlatformAdmin`.
> A combinação não significa "admin E role".

---

## 4. `@RequirePersona`

**Evite em código novo.** O mapeamento de perfil para persona é uma tabela fixa embutida no
framework, com vocabulário de outro domínio:

| Persona | Casa com o perfil |
|---|---|
| `PLATFORM_ADMIN` | `ADMIN`, `PLATFORM_ADMIN` |
| `STORE_ADMIN` | `STORE_MANAGER`, `STORE_ADMIN` |
| `CUSTOMER` | `CUSTOMER`, `USER` |
| `DRIVER` | `DRIVER` |
| qualquer outra | perfil de mesmo nome |

> ⚠ **`context` e `contextData` não são lidos na decisão.** Estão na assinatura, não no
> comportamento.

> ⚠ **Não há ligação entre enrichers e estas anotações.** Versões anteriores desta documentação
> afirmavam que `@RequireRole` e `@RequirePersona` eram extensíveis via enrichers — não são. A
> extensão de `@RequireRole` é o `ArchbaseRoleResolver`.

---

## Combinando anotações

Somam — todas precisam permitir. **Não existe OR entre elas.**

```java
@PostMapping("/{id}/cancelar")
@RequireProfile(value = "SUPERVISOR", allowSystemAdmin = false)
@HasPermission(action = "cancelar", description = "Cancelar a OS",
               minimumLevel = AccessLevel.SUPERVISOR)
public ResponseEntity<Void> cancelar(@PathVariable String id) { ... }
```

Lê-se: *precisa ser SUPERVISOR (mesmo sendo admin), precisa ter a capacidade concedida, e precisa
alcançar o nível.*

### Uma tranca sozinha concede

```java
@RequireProfile("SUPERVISOR")   // sem @HasPermission
public void fecharCompetencia() { ... }
```

Sem capacidade declarada não há catálogo a consultar, então passar na tranca é a decisão inteira.
Qualquer pessoa com o perfil executa, sem que ninguém tenha concedido nada.

É legítimo quando a regra é mesmo "só o perfil X" — mas **não substitui** `@HasPermission`: não
aparece no catálogo, não pode ser concedido nem revogado pelo admin, e não muda sem deploy.

---

## Nível de classe

`@RequireProfile`, `@RequireRole` e `@RequirePersona` aceitam a classe; a anotação no método vence a
da classe. `@HasPermission` não — use `@ArchbaseResource` para o recurso compartilhado.

---

## Por que negou

A decisão carrega o motivo. Os códigos mais comuns:

| Código | O que significa |
|---|---|
| `NO_GRANT` | ninguém concedeu — nem direto, nem grupo, nem perfil |
| `LEVEL_TOO_LOW` | concessão existe, o nível não alcança |
| `EXPLICIT_DENY` | há uma permissão `DENY` alcançando o escopo |
| `OUT_OF_SCOPE` | a permissão existe, mas para outro tenant/empresa/projeto |
| `PROFILE_NOT_MATCHED` | perfil diferente do exigido |
| `ROLE_RESOLVER_MISSING` | `no-resolver-policy=deny` sem resolver registrado |
| `PRINCIPAL_NOT_SUPPORTED` | o principal não é `UserEntity` |

> **`NO_GRANT` num sistema recém-anotado** quase sempre significa **catálogo vazio**, não permissão
> faltando. Confira `archbase.security.scan-packages`.

A tabela completa e os endpoints de diagnóstico — incluindo **simulação de acesso de outra pessoa** —
estão em [ARQUITETURA.md](ARQUITETURA.md).

---

## Configuração

As anotações são configuradas por `MethodSecurityConfig` e interceptadas por um
`AuthorizationManager` cada, que monta o requisito e delega ao avaliador:

| Interceptador | Anotação |
|---|---|
| `CustomAuthorizationManager` | `@HasPermission` |
| `ProfileAuthorizationManager` | `@RequireProfile` |
| `RoleAuthorizationManager` | `@RequireRole` |
| `PersonaAuthorizationManager` | `@RequirePersona` |

A regra de cada tranca vive num `RestrictionEvaluator`; a composição e a decisão, no
`ArchbaseAccessEvaluator`.

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