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

## Considerações Finais

A customização da configuração de segurança oferece grande flexibilidade, mas também traz responsabilidades. Certifique-se de entender completamente as implicações das alterações que você fizer na configuração de segurança.

Lembre-se que desabilitar mecanismos de segurança como CSRF deve ser feito com consciência dos riscos associados, e idealmente apenas para endpoints específicos onde essa proteção não é aplicável (como APIs RESTful sem estado).