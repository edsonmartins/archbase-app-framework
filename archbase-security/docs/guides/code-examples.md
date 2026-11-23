# Exemplos de Código Completos

[← Anterior: Anotações e Uso](annotations-and-usage.md) | [Voltar ao Índice](../README.md) | [Próximo: Melhores Práticas →](best-practices.md)

---

## Visão Geral

Exemplos dos principais casos de uso do `archbase-security`, escritos contra a API real do módulo. Onde o
código é da aplicação (serviços de negócio, DTOs próprios), isso está indicado.

### Exemplos Cobertos

1. **[Criação de Usuário com Permissões](#1-criação-de-usuário-com-permissões)** - Catálogo, usuário e concessões pelos serviços do framework
2. **[Uso de @HasPermission em Controllers](#2-uso-de-haspermission-em-controllers)** - Proteção de endpoints REST e escopo
3. **[Configuração Customizada de Segurança](#3-configuração-customizada-de-segurança)** - Substituir a configuração padrão, auditoria, properties e testes
4. **[Trabalho com API Tokens](#4-trabalho-com-api-tokens)** - Integração service-to-service
5. **[Configuração de Horários de Acesso](#5-configuração-de-horários-de-acesso)** - Restrições temporais (aplicadas pela aplicação)

---

## 1. Criação de Usuário com Permissões

Três peças: o **catálogo** (recurso + ação), o **usuário** e a **concessão** que liga os dois. Cada uma tem
um caminho próprio no framework, e nenhum deles passa por gravar entidade direto no repositório.

### 1.1 O catálogo nasce do código

Recurso e ação não são cadastrados à mão. Na subida, o framework varre os pacotes de
`archbase.security.scan-packages` e cria um recurso + ação para cada `@HasPermission` encontrado (ver §2):

```properties
archbase.security.scan-packages=com.example
```

A varredura roda fora de requisição e grava no tenant `archbase.app.tenant.default.id`. Para conferir o que
ela faria sem gravar nada, use `archbase.security.sync.mode=report` e leia o log.

### 1.2 Usuário e concessão pelo serviço

`UserUseCase` (implementado por `UserService`) é o mesmo caminho do `POST /api/v1/user`. Ele confere e-mail
duplicado, aplica a política de senha (`archbase.security.password.*`), cifra a senha, aplica a trava de
administrador (`admin-guard`) e chama o `UserServiceListener`.

```java
package com.example.security;

import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.usecase.UserUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SecuritySetupService {

    private final UserUseCase users;
    private final ActionJpaRepository actions;   // só leitura do catálogo

    public SecuritySetupService(UserUseCase users, ActionJpaRepository actions) {
        this.users = users;
        this.actions = actions;
    }

    /**
     * Cria o usuário (se ainda não existe) e concede "view" em "app.pedido".
     * Deve ser chamado com o tenant JÁ definido no ArchbaseTenantContext — ver 1.3.
     */
    @Transactional
    public String criarOperador(String email, String senhaInicial) {
        if (users.getUserByEmail(email).isPresent()) {
            return null;                                   // idempotente: addPermission não confere duplicata
        }

        UserDto dto = UserDto.builder()
                .id(UUID.randomUUID().toString())          // o caminho DTO -> entidade não gera id
                .name("John Doe")
                .userName(email)
                .email(email)
                .password(senhaInicial)                    // em claro: o serviço valida e cifra
                .isAdministrator(false)
                .accountDeactivated(false)
                .accountLocked(false)
                .allowPasswordChange(true)
                .allowMultipleLogins(false)
                .passwordNeverExpires(false)
                .unlimitedAccessHours(true)
                .changePasswordOnNextLogin(true)
                .build();
        UserDto criado = users.createUser(dto);

        ActionEntity view = actions.findByActionNameAndResourceName("view", "app.pedido")
                .orElseThrow(() -> new IllegalStateException(
                        "Capacidade app.pedido:view não está no catálogo deste tenant"));

        // Concessão em todo o tenant (sem empresa nem projeto)
        users.addPermission(criado.getId(), view.getId());
        return criado.getId();
    }
}
```

Chamada num seed de desenvolvimento:

```java
@Bean
@Profile("dev")
CommandLineRunner seedSeguranca(SecuritySetupService setup) {
    return args -> {
        ArchbaseTenantContext.setTenantId("tenant-dev");
        try {
            setup.criarOperador("john.doe@example.com", System.getenv("SEED_PASSWORD"));
        } finally {
            ArchbaseTenantContext.clear();
        }
    };
}
```

(`ArchbaseTenantContext` está em `br.com.archbase.ddd.context`.)

**Administrador.** Mesmo DTO com `.isAdministrator(true)`. Sem ninguém autenticado (seed, importação), a
trava de administrador deixa passar e registra um WARN; numa requisição, só um administrador cria ou
promove outro.

### 1.3 Tenant: vem do contexto, não do builder

Usuário, recurso, ação e permissão estendem `TenantPersistenceEntityBase`, cujo `tenantId` é `@TenantId` do
Hibernate. Consequências:

- **O tenant é preenchido pelo Hibernate** a partir do tenant corrente (`ArchbaseTenantContext`, ou
  `archbase.app.tenant.default.id` quando o contexto está vazio). `UserDto` nem tem campo de tenant.
- **Um valor diferente passado à mão não muda de tenant:** a gravação é recusada com
  `PropertyValueException` ("assigned tenant id differs from current tenant id").
- **O tenant é resolvido quando a sessão abre.** Defina o contexto *antes* de entrar no método
  `@Transactional`, como no runner acima — defini-lo dentro do método é tarde demais.
- **Leituras só enxergam o tenant corrente.** Se o catálogo não existe no tenant do seed,
  `findByActionNameAndResourceName` volta vazio.

### 1.4 Concessão com escopo de empresa ou projeto

`addPermission` grava a concessão sem escopo (vale em todo o tenant). Para estreitar a empresa ou projeto, ou
para gravar uma negação, use o endpoint administrativo que a tela de segurança usa:

```http
POST /api/v1/resource/permissions
Authorization: Bearer <jwt de quem administra>
Content-Type: application/json

{
  "securityId": "<id do usuário>",
  "type": "USER",
  "actionId": "<id da ação>",
  "companyId": "COMPANY_BR",
  "projectId": "PROJECT_001",
  "effect": "GRANT"
}
```

- `type`: `USER`, `GROUP` ou `PROFILE`. `effect`: `GRANT` (padrão quando ausente) ou `DENY`.
- Se já existe concessão para o par (segurança, ação), ela é atualizada. `companyId`/`projectId` ausentes
  preservam o escopo gravado; string vazia o limpa.
- O endpoint segue `archbase.security.admin-endpoints.policy`. O serviço por trás
  (`ResourceService.grantPermission`) exige usuário autenticado, por isso não serve para seed.

### 1.5 Senha

Não codifique a senha antes de passar ao `UserUseCase`: ele mesmo valida e cifra, e uma senha já cifrada
seria cifrada de novo. O `PasswordEncoder` padrão é `BCryptPasswordEncoder` e só é registrado se a aplicação
não declarar outro:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);   // hashes antigos de força 10 continuam conferindo
}
```

---

## 2. Uso de @HasPermission em Controllers

`@HasPermission` declara a capacidade que o método exige e alimenta o catálogo (§1.1). Regras da anotação:

- `@Target(METHOD)`: não compila na classe.
- `action` e `description` são **obrigatórios**. `description` é o texto que quem administra lê ao conceder.
- `resource` vem do atributo ou, vazio, de `@ArchbaseResource(value = ..., description = ...)` na classe. Sem
  nenhum dos dois, o acesso é negado e o erro vai para o log.
- Opcionais: `label` (rótulo curto no admin), `category` (agrupamento), `minimumLevel` (piso do portão LEVEL,
  só avaliado com `archbase.security.access-level.enabled=true`), `requires` (dependências informativas,
  nunca exigidas na decisão).
- Só um principal `UserEntity` do Archbase é avaliado. Outro principal (de um `UserDetailsService` próprio,
  de `@WithMockUser`) é negado no portão IDENTITY.

### 2.1 Controller REST

O serviço e os DTOs abaixo são da aplicação.

```java
package com.example.pedido;

import br.com.archbase.security.access.AccessLevel;
import br.com.archbase.security.annotation.ArchbaseResource;
import br.com.archbase.security.annotation.HasPermission;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
@ArchbaseResource(value = "app.pedido", description = "Pedidos de venda")
public class PedidoController {

    private final PedidoService pedidos;

    public PedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping
    @HasPermission(action = "view", description = "Listar pedidos")
    public Page<PedidoDto> listar(Pageable pageable) {
        return pedidos.listar(pageable);
    }

    @GetMapping("/{id}")
    @HasPermission(action = "view", description = "Listar pedidos")
    public PedidoDto buscar(@PathVariable String id) {
        return pedidos.buscar(id);
    }

    @PostMapping
    @HasPermission(action = "create", description = "Criar pedido", requires = "view")
    public ResponseEntity<PedidoDto> criar(@RequestBody @Valid PedidoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidos.criar(dto));
    }

    @PutMapping("/{id}")
    @HasPermission(action = "update", description = "Alterar pedido", requires = "view")
    public PedidoDto alterar(@PathVariable String id, @RequestBody @Valid PedidoDto dto) {
        return pedidos.alterar(id, dto);
    }

    @DeleteMapping("/{id}")
    @HasPermission(action = "delete", description = "Excluir pedido")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        pedidos.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/aprovar")
    @HasPermission(action = "aprovar", description = "Aprovar pedido acima da alçada",
                   label = "Aprovar pedido", category = "Aprovação",
                   minimumLevel = AccessLevel.SUPERVISOR)
    public PedidoDto aprovar(@PathVariable String id) {
        return pedidos.aprovar(id);
    }

    // Recurso declarado no método vence o da classe
    @GetMapping("/relatorio")
    @HasPermission(action = "export", resource = "app.relatorio",
                   description = "Exportar relatório de pedidos")
    public ResponseEntity<byte[]> exportar() {
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .body(pedidos.relatorioPdf());
    }
}
```

Duas anotações com o mesmo `action` no mesmo recurso (`listar` e `buscar`) são a mesma capacidade; mantenha
a mesma `description`.

### 2.2 Escopo: tenant, empresa e projeto

A anotação aceita `tenantId`, `companyId` e `projectId`. São **literais**, não SpEL. O que cada um vale na
decisão:

| Atributo | Vazio (padrão) | Preenchido |
|---|---|---|
| `tenantId` | Tenant do `ArchbaseTenantContext` | O literal |
| `companyId` | Empresa do `ArchbaseTenantContext` (`X-COMPANY-ID`) | O literal |
| `projectId` | Sem estreitamento — não há projeto no contexto | O literal |

Uma concessão alcança o pedido quando, em cada um dos três, um dos lados é nulo ou os dois são iguais. Uma
concessão sem empresa vale para qualquer empresa; uma concessão para `COMPANY_BR` não vale para `COMPANY_AR`.

```java
@GetMapping("/brasil")
@HasPermission(action = "view", description = "Listar pedidos da operação Brasil",
               companyId = "COMPANY_BR")
public List<PedidoDto> listarBrasil() { ... }

@PostMapping("/{id}/faturar")
@HasPermission(action = "faturar", description = "Faturar pedido do projeto piloto",
               projectId = "PROJECT_001")
public PedidoDto faturar(@PathVariable String id) { ... }
```

O que o escopo **não** faz:

- **`tenantId` fixo não dá acesso a outro tenant.** A consulta de permissões só devolve linhas do tenant
  corrente; um literal diferente dele faz a concessão não casar, e o acesso é negado. Ele só restringe.
- **`companyId` fixo não troca a empresa dos dados.** Ele decide a autorização; o filtro dos dados continua
  vindo do `X-COMPANY-ID`. Para conferir que o usuário pertence à empresa do cabeçalho, use
  `archbase.security.company.validation`.
- **Administrador ignora o escopo.** `isAdministrator` concede sem consultar as concessões; só uma negação
  (`DENY`) explícita o alcança.

### 2.3 Dicas de Implementação

**Contexto.** O tenant e a empresa chegam pelo `ArchbaseTenantContext`, preenchido pelos cabeçalhos
`X-TENANT-ID`/`X-COMPANY-ID`. Não passe tenant como parâmetro:

```java
@GetMapping
@HasPermission(action = "view", resource = "app.cliente", description = "Listar clientes")
public List<ClienteDto> listar() {
    return clientes.listar();   // repositórios filtram pelo tenant corrente
}
```

**Validação de entrada.** `@Valid` roda **antes** da autorização: o corpo é validado na resolução dos
argumentos, e o interceptador de `@HasPermission` só age na chamada do método. Quem não tem permissão e manda
um corpo inválido recebe 400, não 403.

```java
@PostMapping
@HasPermission(action = "create", resource = "app.cliente", description = "Criar cliente")
public ResponseEntity<ClienteDto> criar(@RequestBody @Valid ClienteDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(clientes.criar(dto));
}
```

**Proxy.** A checagem é um interceptador de método do Spring: vale para chamadas que passam pelo bean.
Um método anotado chamado de dentro da própria classe não é checado.

**Tratamento da negação.** A negação lança `AuthorizationDeniedException` (subclasse de
`AccessDeniedException`). Sem tratamento, o `CustomAccessDeniedHandler` do framework responde 403. Para um
corpo próprio:

```java
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> negado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "ACCESS_DENIED",
                "message", "Você não tem permissão para acessar este recurso"));
    }
}
```

Não há exceção separada para erro *ao avaliar* a permissão: o interceptador registra o erro no log e nega,
com o mesmo 403.

---

## 3. Configuração Customizada de Segurança

A configuração padrão (`DefaultArchbaseSecurityConfiguration`) já monta a cadeia completa: filtro JWT,
CORS, sessão stateless, logout, 401/403 e caminhos públicos, tudo controlado por properties
(ver [Configuração](configuration.md)). Prefira ajustar as properties.

Quando for preciso mudar a cadeia, **não declare um `SecurityFilterChain` solto**. A configuração padrão é
`@ConditionalOnMissingBean(CustomSecurityConfiguration.class)`: um bean de filtro avulso não a desliga, e as
duas cadeias passam a coexistir. O caminho é estender `BaseArchbaseSecurityConfiguration` e implementar a
interface marcadora `CustomSecurityConfiguration`.

### 3.1 Configuração Principal

```java
package com.example.config;

import br.com.archbase.security.config.ArchbaseJwtAuthenticationFilter;
import br.com.archbase.security.config.BaseArchbaseSecurityConfiguration;
import br.com.archbase.security.config.CustomAccessDeniedHandler;
import br.com.archbase.security.config.CustomSecurityConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.util.List;

@Configuration
@EnableWebSecurity
public class ApplicationSecurityConfig extends BaseArchbaseSecurityConfiguration
        implements CustomSecurityConfiguration {

    private final ArchbaseJwtAuthenticationFilter jwtFilter;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    public ApplicationSecurityConfig(ArchbaseJwtAuthenticationFilter jwtFilter,
                                     CustomAccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /** Tudo o que fica acessível sem autenticação. */
    @Override
    protected List<String> getWhiteListUrls() {
        return List.of(
                "/api/v1/auth/**",            // login, refresh, reset de senha, descoberta de tenant
                "/api/v1/apiToken/activate",  // link de ativação do token de API
                "/error",                     // sem ela, um 404/401 vira 403 no redespacho de erro
                "/actuator/health",
                "/api/v1/publico/**");
    }

    /** Exceções dentro da whitelist que voltam a exigir autenticação. */
    @Override
    protected List<String> getProtectedPathsWithinWhitelist() {
        return List.of("/api/v1/auth/register");   // sem auto-cadastro anônimo
    }

    /** Regras depois da whitelist; não alcançam caminhos já liberados nela. */
    @Override
    protected void configureAuthorizationRules(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/interno/**").denyAll()
                .anyRequest().authenticated());
    }

    @Override
    protected ArchbaseJwtAuthenticationFilter getJwtAuthenticationFilter() {
        return jwtFilter;
    }

    @Override
    protected CustomAccessDeniedHandler getAccessDeniedHandler() {
        return accessDeniedHandler;
    }

    @Override
    protected List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    @Override
    protected List<String> getAllowedMethods() {
        return List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    @Override
    protected List<String> getAllowedHeaders() {
        return List.of("*");
    }

    @Override
    protected boolean getAllowCredentials() {
        return true;
    }
}
```

O que a classe base já faz e não precisa ser repetido: CSRF desligado, CORS com `Authorization` exposto,
sessão stateless, filtro JWT antes do `UsernamePasswordAuthenticationFilter`, logout em
`archbase.security.logout.url`, 401 para quem não está autenticado e 403 para quem não tem permissão, e a
linha `Superfície anônima da API` no log da subida.

Cuidados:

- **As chaves da configuração padrão deixam de valer.** `archbase.security.whitelist`,
  `archbase.security.cors.*` e `archbase.security.public-paths.*` são lidas pela
  `DefaultArchbaseSecurityConfiguration`; com a sua, o que vale é o que os métodos acima devolvem.
- **Mantenha `/api/v1/auth/**` público.** Os endpoints de credencial ficam todos ali: `POST /authenticate`,
  `POST /refresh-token`, `POST /sendResetPasswordEmail/{email}`, `POST /resetPassword`, `GET /tenants`.
- **Não use `.hasRole(...)` nem `.hasAuthority(...)` nas regras de URL.** `UserEntity.getAuthorities()`
  devolve uma coleção vazia, então essas regras negam sempre. Autorização é por `@HasPermission`,
  `@RequireProfile`, `@RequireRole` e `@RequirePersona`.
- **Não redeclare autenticação.** `AuthenticationProvider`, `AuthenticationManager`, `UserDetailsService` e
  `PasswordEncoder` já são registrados pelo framework, todos com `@ConditionalOnMissingBean`. Se precisar
  trocar o provider, no Spring Security 7.1 o `DaoAuthenticationProvider` só tem o construtor que recebe o
  `UserDetailsService` (não há mais `setUserDetailsService`):

  ```java
  @Bean
  public AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
      DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
      provider.setPasswordEncoder(encoder);
      return provider;
  }
  ```

  O `UserDetailsService` precisa devolver `UserEntity`: é o principal que `@HasPermission` e a trava de
  administrador sabem avaliar.
- **HTTPS** costuma terminar no proxy. Se a aplicação precisar redirecionar, o Spring Security 7.1 oferece
  `http.redirectToHttps(...)`.

### 3.2 Auditoria de Segurança

Em vez de um listener próprio, ligue a trilha do framework. Ela grava em `SEGURANCA_EVENTO`, consultável em
`GET /api/v1/security/audit/events`, e registra o histórico de alterações de usuário, grupo, perfil,
permissão, ação e recurso nas tabelas `_AUD`:

```properties
archbase.security.audit.enabled=true        # crie antes as tabelas: deployment/sql/auditoria-seguranca-*.sql
archbase.security.audit.tenant-scoped=true
archbase.security.audit.retention-months=12
```

Os eventos que o próprio framework grava:

| Evento | Origem |
|---|---|
| `LOGIN_FALHOU` | O serviço de autenticação, com o e-mail digitado e o tipo da falha |
| `ACESSO_NEGADO` | A decisão de `@HasPermission`, com recurso, ação e o portão que negou |
| `LOGIN` | Listener de `AuthenticationSuccessEvent` do Spring Security |

Por que não escutar os eventos do Spring na aplicação, como era sugerido aqui antes:

- **Falha de login** não chega como `AbstractAuthenticationFailureEvent` nesse fluxo — por isso o serviço a
  grava diretamente.
- **`AuthorizationDeniedEvent`** não é publicado para `@HasPermission`: os interceptadores do framework não
  configuram publicador de eventos de autorização.
- **`LogoutSuccessEvent`** não é publicado: o logout do framework não registra o handler que o emite.

O bloqueio após N tentativas falhas já existe: `archbase.security.rate-limit.*`.

### 3.3 Configuração de Properties

```properties
# === application.properties ===

# Obrigatórias
archbase.security.jwt.secret-key=${JWT_SECRET}
archbase.security.jwt.token-expiration=3600000          # 1 hora
archbase.security.jwt.refresh-expiration=604800000      # 7 dias

# Com a configuração customizada do 3.1, a whitelist e o CORS vêm dela; estas chaves só
# são exigidas pela configuração padrão.
archbase.security.whitelist=
archbase.security.cors.allowed-origins=${CORS_ORIGINS:http://localhost:3000}
archbase.security.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
archbase.security.cors.allowed-headers=*
archbase.security.cors.allow-credentials=true

# Lida pela ApplicationSecurityConfig do 3.1
app.cors.allowed-origins=${CORS_ORIGINS:http://localhost:3000}

# Catálogo de @HasPermission
archbase.security.scan-packages=com.example

# Tenant
archbase.app.tenant.default.id=archbase

# Logging
logging.level.org.springframework.security=INFO
logging.level.br.com.archbase.security=INFO

# === application-prod.properties ===

archbase.security.jwt.token-expiration=1800000          # 30 min
archbase.security.jwt.refresh-expiration=43200000       # 12 horas
archbase.security.admin-endpoints.policy=admin-only
archbase.security.jwt.accept-token-query-param=false
archbase.app.tenant.fail-on-missing=true
archbase.app.tenant.accept-query-param=false
archbase.security.audit.enabled=true
archbase.security.audit.tenant-scoped=true
logging.level.org.springframework.security=WARN
```

A lista completa, com padrões e pré-requisitos de cada proteção, está em [Configuração](configuration.md) e
no [guia de endurecimento](../../../deployment/security-hardening.md).

### 3.4 Dicas de Implementação

**Segredos.**

```bash
export JWT_SECRET="$(openssl rand -base64 48)"   # Base64 padrão, >= 32 bytes decodificados
export CORS_ORIGINS="https://app.example.com,https://admin.example.com"
```

**Testes de segurança.** Com Spring Boot 4, `@AutoConfigureMockMvc` está em
`org.springframework.boot.webmvc.test.autoconfigure` (artefato `spring-boot-webmvc-test`).

```java
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rotaDeAutenticacaoEhPublica() throws Exception {
        mockMvc.perform(get("/api/v1/auth/tenants").param("email", "ninguem@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    void credencialInvalidaDa401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@example.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegidaSemTokenDa401() throws Exception {
        mockMvc.perform(get("/api/v1/pedidos"))
                .andExpect(status().isUnauthorized());
    }
}
```

`@WithMockUser` **não** passa por `@HasPermission`: o principal que ele cria não é um `UserEntity`, e a
decisão nega no portão IDENTITY (403). Para testar uma rota protegida com sucesso, autentique um usuário real
(`POST /api/v1/auth/authenticate`) e envie o access token no cabeçalho `Authorization: Bearer ...`.

---

## 4. Trabalho com API Tokens

O token de API é do framework: use o `ApiTokenService` / `ApiTokenController`. **Não reimplemente** o
serviço nem o filtro — o filtro do Archbase só reconhece o formato que o próprio serviço gera, e uma
versão caseira que grave o valor em claro ou use outro cabeçalho fica incompatível e menos segura.

### 4.1 Como o token funciona

| Aspecto | Comportamento |
|---------|---------------|
| Formato | UUID aleatório |
| Armazenamento | Só o hash SHA-256 (`TOKEN_HASH`). Com `archbase.security.api-token.hash-enabled=false` o valor também fica em claro, para telas que o listam |
| Exibição | O valor aparece **uma única vez**, na resposta da criação (e no e-mail de ativação) |
| Ciclo de vida | Criado inativo → ativado → válido até `DH_EXPIRACAO` ou revogação |
| Validade | Aceito só se ativado, não revogado e não expirado |
| Tenant | O tenant do token vira o tenant da requisição; `X-TENANT-ID` diferente dá 403 |

### 4.2 Endpoints

Os endpoints de gestão são administrativos (`@ArchbaseSecurityAdminEndpoint(resource = "API_TOKEN")`):
seguem `archbase.security.admin-endpoints.policy`.

```http
POST /api/v1/apiToken/create?email=integracao@empresa.com&expirationDate=2027-12-31T23:59:59&name=erp&description=Integração ERP
Authorization: Bearer <jwt do administrador>

→ 200 { "id": "...", "token": "3f6c1b2e-...", ... }     # guarde o token agora; não há como recuperá-lo

GET  /api/v1/apiToken/activate?token=<uuid>&tenantId=<tenant>    # link do e-mail; anônimo por desenho
POST /api/v1/apiToken/revoke?token=<uuid>
```

A criação envia o e-mail de ativação pelo `ArchbaseEmailService` da aplicação. Sem implementação, o
envio falha com WARN no log, mas o token é criado e devolvido mesmo assim; ative-o pelo endpoint.

### 4.3 Uso pelo cliente

```bash
curl -H "Authorization: Bearer 3f6c1b2e-..." https://api.empresa.com/api/v1/pedidos
```

O UUID cru no cabeçalho (`Authorization: 3f6c1b2e-...`) também é aceito. Qualquer outro esquema
(`ApiToken <x>`, `Token <x>`) é recusado como "formato não reconhecido".

```java
RestClient client = RestClient.builder()
        .baseUrl("https://api.empresa.com")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + System.getenv("ARCHBASE_API_TOKEN"))
        .build();
```

### 4.4 Uso programático

```java
@Service
@RequiredArgsConstructor
public class IntegracaoSetup {

    private final ApiTokenService apiTokens;

    public String criarTokenDoErp() {
        ApiTokenDto dto = apiTokens.createToken("integracao@empresa.com",
                LocalDateTime.now().plusYears(1), "erp", "Integração ERP");
        apiTokens.activateToken(dto.getToken(), ArchbaseTenantContext.getTenantId());
        return dto.getToken();   // exiba/armazene agora: o banco só guarda o hash
    }
}
```

### 4.5 Bases antigas

Tokens criados antes do hash ficam em claro. Com `archbase.security.api-token.hash-migration-enabled=true`
(padrão), a subida calcula o hash dos que não têm. `archbase.security.api-token.purge-plaintext=true` apaga
os valores em claro — **irreversível**; ligue só depois de confirmar que nenhuma tela depende deles.

---

## 5. Configuração de Horários de Acesso

> **O framework não aplica horário de acesso.** As entidades (`AccessScheduleEntity`, `AccessIntervalEntity`)
> e o campo `unlimitedAccessHours` do usuário existem e têm CRUD, mas nenhum código do Archbase os consulta
> no login nem nas requisições. Cadastrar um horário **não restringe nada** por si só.
>
> Se a aplicação precisa da restrição, ela mesma tem de validar — como nos exemplos abaixo, que são código
> **da aplicação**, não do framework. Lembre que o login não é o único ponto: um token emitido dentro do
> horário continua válido depois dele, até expirar.

Este exemplo demonstra como a aplicação pode configurar horários e implementar a restrição temporal de login.

### 5.1 Service de Gerenciamento de Horários

```java
package com.example.security.service;

import br.com.archbase.ddd.domain.annotations.DomainService;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import br.com.archbase.security.persistence.AccessIntervalEntity;
import br.com.archbase.security.persistence.AccessScheduleEntity;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.AccessScheduleJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import com.example.security.dto.AccessIntervalDto;
import com.example.security.dto.AccessScheduleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de horários de acesso.
 *
 * Horários de acesso controlam quando usuários podem fazer login
 * e acessar o sistema, útil para:
 * - Horário comercial
 * - Turnos de trabalho
 * - Restrições de segurança
 * - Compliance regulatório
 */
@Service
@DomainService
public class AccessScheduleService {

    private static final Logger logger =
        LoggerFactory.getLogger(AccessScheduleService.class);

    @Autowired
    private AccessScheduleJpaRepository scheduleRepository;

    @Autowired
    private UserJpaRepository userRepository;

    /**
     * Cria um horário de acesso comercial padrão (Seg-Sex 8h-18h).
     *
     * Configuração típica para horário comercial brasileiro.
     */
    @Transactional
    public AccessScheduleDto createBusinessHoursSchedule(String tenantId) {
        logger.info("Criando horário comercial para tenant: {}", tenantId);

        // Criar horário de acesso
        AccessScheduleEntity schedule = new AccessScheduleEntity();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setCode("SCH_BUSINESS_HOURS");
        schedule.setDescription("Horário Comercial - Segunda a Sexta 8h às 18h");
        schedule.setTenantId(tenantId);
        schedule.setCreateEntityDate(LocalDateTime.now());
        schedule.setCreatedByUser("SYSTEM");

        // Criar intervalos para dias úteis
        // DayOfWeek: 1=Domingo, 2=Segunda, ..., 7=Sábado
        List<AccessIntervalEntity> intervals = new ArrayList<>();
        for (long day = 2; day <= 6; day++) {  // Segunda a Sexta
            AccessIntervalEntity interval = AccessIntervalEntity.builder()
                .id(UUID.randomUUID().toString())
                .code("INT_BUSINESS_DAY_" + day)
                .accessSchedule(schedule)
                .dayOfWeek(day)
                .startTime("08:00")
                .endTime("18:00")
                .tenantId(tenantId)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("SYSTEM")
                .build();
            intervals.add(interval);
        }

        schedule.setIntervals(intervals);
        scheduleRepository.save(schedule);

        logger.info("Horário comercial criado: id={}", schedule.getId());
        return schedule.toDto();
    }

    /**
     * Cria um horário de acesso 24/7 (todos os dias, 24 horas).
     *
     * Útil para sistemas que operam continuamente ou usuários
     * que necessitam acesso irrestrito.
     */
    @Transactional
    public AccessScheduleDto create24x7Schedule(String tenantId) {
        logger.info("Criando horário 24x7 para tenant: {}", tenantId);

        AccessScheduleEntity schedule = new AccessScheduleEntity();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setCode("SCH_24X7");
        schedule.setDescription("Acesso 24 horas - Todos os dias");
        schedule.setTenantId(tenantId);
        schedule.setCreateEntityDate(LocalDateTime.now());
        schedule.setCreatedByUser("SYSTEM");

        // Criar intervalos para todos os dias
        List<AccessIntervalEntity> intervals = new ArrayList<>();
        for (long day = 1; day <= 7; day++) {  // Todos os dias
            AccessIntervalEntity interval = AccessIntervalEntity.builder()
                .id(UUID.randomUUID().toString())
                .code("INT_24X7_DAY_" + day)
                .accessSchedule(schedule)
                .dayOfWeek(day)
                .startTime("00:00")
                .endTime("23:59")
                .tenantId(tenantId)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("SYSTEM")
                .build();
            intervals.add(interval);
        }

        schedule.setIntervals(intervals);
        scheduleRepository.save(schedule);

        logger.info("Horário 24x7 criado: id={}", schedule.getId());
        return schedule.toDto();
    }

    /**
     * Cria horário de turnos (exemplo: 3 turnos de 8 horas).
     *
     * Turno 1: 06:00-14:00
     * Turno 2: 14:00-22:00
     * Turno 3: 22:00-06:00
     */
    @Transactional
    public List<AccessScheduleDto> createShiftSchedules(String tenantId) {
        logger.info("Criando horários de turnos para tenant: {}", tenantId);

        List<AccessScheduleDto> schedules = new ArrayList<>();

        // Turno 1: Manhã/Tarde (06:00-14:00)
        schedules.add(createShiftSchedule(
            tenantId,
            "SHIFT_1",
            "Turno 1 - Manhã/Tarde (06:00-14:00)",
            "06:00",
            "14:00"
        ));

        // Turno 2: Tarde/Noite (14:00-22:00)
        schedules.add(createShiftSchedule(
            tenantId,
            "SHIFT_2",
            "Turno 2 - Tarde/Noite (14:00-22:00)",
            "14:00",
            "22:00"
        ));

        // Turno 3: Noite/Madrugada (22:00-06:00)
        schedules.add(createShiftSchedule(
            tenantId,
            "SHIFT_3",
            "Turno 3 - Noite/Madrugada (22:00-06:00)",
            "22:00",
            "06:00"
        ));

        logger.info("Horários de turnos criados: count={}", schedules.size());
        return schedules;
    }

    /**
     * Cria um horário de acesso customizado.
     *
     * Permite especificar intervalos arbitrários por dia da semana.
     */
    @Transactional
    public AccessScheduleDto createCustomSchedule(
            String tenantId,
            String description,
            List<AccessIntervalDto> intervalDtos) {

        logger.info("Criando horário customizado: description={}",
            description);

        // Validar intervalos
        validateIntervals(intervalDtos);

        AccessScheduleEntity schedule = new AccessScheduleEntity();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setCode("SCH_CUSTOM_" + generateShortId());
        schedule.setDescription(description);
        schedule.setTenantId(tenantId);
        schedule.setCreateEntityDate(LocalDateTime.now());
        schedule.setCreatedByUser("SYSTEM");

        List<AccessIntervalEntity> intervals = intervalDtos.stream()
            .map(dto -> AccessIntervalEntity.builder()
                .id(UUID.randomUUID().toString())
                .code("INT_" + generateShortId())
                .accessSchedule(schedule)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .tenantId(tenantId)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("SYSTEM")
                .build())
            .collect(Collectors.toList());

        schedule.setIntervals(intervals);
        scheduleRepository.save(schedule);

        logger.info("Horário customizado criado: id={}", schedule.getId());
        return schedule.toDto();
    }

    /**
     * Atribui horário de acesso a um usuário.
     */
    @Transactional
    public void assignScheduleToUser(String userId, String scheduleId) {
        logger.info("Atribuindo horário a usuário: userId={}, scheduleId={}",
            userId, scheduleId);

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Usuário não encontrado"));

        AccessScheduleEntity schedule = scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Horário de acesso não encontrado"));

        user.setAccessSchedule(schedule);
        user.setUnlimitedAccessHours(false);
        user.setUpdateEntityDate(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Horário atribuído com sucesso");
    }

    /**
     * Remove horário de acesso de um usuário (acesso ilimitado).
     */
    @Transactional
    public void removeScheduleFromUser(String userId) {
        logger.info("Removendo restrição de horário: userId={}", userId);

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ArchbaseValidationException(
                "Usuário não encontrado"));

        user.setAccessSchedule(null);
        user.setUnlimitedAccessHours(true);
        user.setUpdateEntityDate(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Restrição de horário removida");
    }

    /**
     * Helper para criar horário de turno.
     */
    private AccessScheduleDto createShiftSchedule(
            String tenantId,
            String code,
            String description,
            String startTime,
            String endTime) {

        AccessScheduleEntity schedule = new AccessScheduleEntity();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setCode("SCH_" + code);
        schedule.setDescription(description);
        schedule.setTenantId(tenantId);
        schedule.setCreateEntityDate(LocalDateTime.now());
        schedule.setCreatedByUser("SYSTEM");

        // Criar intervalos para todos os dias úteis
        List<AccessIntervalEntity> intervals = new ArrayList<>();
        for (long day = 2; day <= 6; day++) {
            AccessIntervalEntity interval = AccessIntervalEntity.builder()
                .id(UUID.randomUUID().toString())
                .code("INT_" + code + "_DAY_" + day)
                .accessSchedule(schedule)
                .dayOfWeek(day)
                .startTime(startTime)
                .endTime(endTime)
                .tenantId(tenantId)
                .createEntityDate(LocalDateTime.now())
                .createdByUser("SYSTEM")
                .build();
            intervals.add(interval);
        }

        schedule.setIntervals(intervals);
        scheduleRepository.save(schedule);

        return schedule.toDto();
    }

    /**
     * Valida lista de intervalos.
     */
    private void validateIntervals(List<AccessIntervalDto> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            throw new ArchbaseValidationException(
                "Lista de intervalos não pode ser vazia");
        }

        for (AccessIntervalDto interval : intervals) {
            // Validar dia da semana (1-7)
            if (interval.getDayOfWeek() < 1 || interval.getDayOfWeek() > 7) {
                throw new ArchbaseValidationException(
                    "Dia da semana inválido: " + interval.getDayOfWeek());
            }

            // Validar formato de horários
            if (!isValidTime(interval.getStartTime()) ||
                !isValidTime(interval.getEndTime())) {
                throw new ArchbaseValidationException(
                    "Formato de horário inválido. Use HH:mm");
            }
        }
    }

    /**
     * Valida formato de horário (HH:mm).
     */
    private boolean isValidTime(String time) {
        return time != null && time.matches("^([01]\\d|2[0-3]):([0-5]\\d)$");
    }

    /**
     * Gera ID curto para código.
     */
    private String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
```

### 5.2 Validação de Horário no Login

```java
package com.example.security.validator;

import br.com.archbase.security.persistence.AccessIntervalEntity;
import br.com.archbase.security.persistence.AccessScheduleEntity;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Validador de horário de acesso durante login.
 */
@Component
public class AccessScheduleValidator {

    /**
     * Valida se usuário pode fazer login no horário atual.
     *
     * @param user Usuário tentando fazer login
     * @return true se acesso permitido, false caso contrário
     */
    public boolean canAccessNow(UserEntity user) {
        // Acesso ilimitado
        if (user.getUnlimitedAccessHours()) {
            return true;
        }

        // Sem horário definido = sem acesso
        AccessScheduleEntity schedule = user.getAccessSchedule();
        if (schedule == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        // Converter DayOfWeek para número (1=Domingo, 7=Sábado)
        long dayNumber = (dayOfWeek.getValue() % 7) + 1;

        // Verificar se existe intervalo válido para dia/hora atual
        return schedule.getIntervals().stream()
            .anyMatch(interval -> isTimeInInterval(
                dayNumber,
                currentTime,
                interval
            ));
    }

    /**
     * Verifica se horário está dentro de um intervalo.
     */
    private boolean isTimeInInterval(
            long dayNumber,
            LocalTime currentTime,
            AccessIntervalEntity interval) {

        // Verificar dia da semana
        if (interval.getDayOfWeek() != dayNumber) {
            return false;
        }

        LocalTime startTime = LocalTime.parse(interval.getStartTime());
        LocalTime endTime = LocalTime.parse(interval.getEndTime());

        // Caso normal: startTime < endTime (ex: 08:00-18:00)
        if (startTime.isBefore(endTime)) {
            return !currentTime.isBefore(startTime) &&
                   !currentTime.isAfter(endTime);
        }

        // Caso especial: startTime > endTime (ex: 22:00-06:00, noite/madrugada)
        return !currentTime.isBefore(startTime) ||
               !currentTime.isAfter(endTime);
    }

    /**
     * Retorna próximo horário de acesso disponível.
     */
    public LocalDateTime getNextAccessTime(UserEntity user) {
        if (user.getUnlimitedAccessHours()) {
            return LocalDateTime.now();
        }

        AccessScheduleEntity schedule = user.getAccessSchedule();
        if (schedule == null || schedule.getIntervals().isEmpty()) {
            return null;  // Sem acesso
        }

        LocalDateTime now = LocalDateTime.now();

        // Procurar próximo intervalo (simplificado)
        // TODO: Implementar lógica completa de próximo horário
        return now.plusDays(1).withHour(8).withMinute(0);
    }
}
```

### 5.3 Uso no AuthenticationService

```java
@Service
public class CustomAuthenticationService {

    @Autowired
    private AccessScheduleValidator scheduleValidator;

    public AuthenticationResponse authenticate(
            AuthenticationRequest request) {

        // Autenticar credenciais
        UserEntity user = validateCredentials(request);

        // Validar horário de acesso
        if (!scheduleValidator.canAccessNow(user)) {
            LocalDateTime nextAccess =
                scheduleValidator.getNextAccessTime(user);

            throw new AccessDeniedException(
                "Acesso fora do horário permitido. " +
                "Próximo acesso disponível em: " + nextAccess
            );
        }

        // Gerar tokens e retornar resposta
        return generateAuthenticationResponse(user);
    }
}
```

---

## Navegação

[← Anterior: Anotações e Uso](annotations-and-usage.md) | [Voltar ao Índice](../README.md) | [Próximo: Melhores Práticas →](best-practices.md)

---

## Próximos Passos

Após estudar estes exemplos completos, recomendamos:

1. **[Melhores Práticas](best-practices.md)** - Aprenda recomendações de design e segurança
2. **[Troubleshooting](troubleshooting.md)** - Resolva problemas comuns
3. **[Arquitetura de autorização](../../ARQUITETURA.md)** - Entenda o funcionamento interno

Para dúvidas ou contribuições, consulte o [README principal](../README.md).
