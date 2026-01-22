[← Anterior: Sistema de Permissões](permissions-system.md) | [Voltar ao Índice](../README.md) | [Próximo: Multi-Tenancy →](multi-tenancy.md)

---

# Arquitetura do Sistema de Autenticação

## Introdução

O sistema de autenticação do `archbase-security` implementa autenticação baseada em JWT (JSON Web Tokens) com suporte para múltiplos tipos de tokens, redefinição segura de senha e autenticação de longa duração para serviços. Este documento descreve os fluxos completos de autenticação, validação de tokens e redefinição de senha.

---

## Fluxo de Autenticação JWT

O processo de autenticação JWT consiste em 10 passos principais que transformam credenciais de usuário em tokens de acesso seguros.

### Visão Geral do Fluxo

```
Requisição de Login → Controller → AuthenticationService → AuthenticationManager
                                                                     ↓
                                                          Validar Credenciais
                                                                     ↓
                                                            Buscar Usuário
                                                                     ↓
                                                         Verificar Token Existente
                                                                     ↓
                                                    Gerar Access Token e Refresh Token
                                                                     ↓
                                                         Persistir Access Token
                                                                     ↓
                                                        Retornar Authentication Response
```

### Passo 1: Requisição de Login

Cliente envia credenciais para o endpoint de autenticação (geralmente `/api/v1/auth/login` ou `/api/v1/auth/authenticate`).

**Payload da Requisição**:
```json
{
  "email": "usuario@exemplo.com",
  "password": "senhaSegura123"
}
```

**Endpoint**: `POST /api/v1/auth/login`

**Headers**:
```
Content-Type: application/json
```

**Componente Responsável**: `ArchbaseAuthenticationController`

### Passo 2: Recepção pelo Controller

O `ArchbaseAuthenticationController` recebe a requisição e delega o processamento para `ArchbaseAuthenticationService`.

**Localização**: `br.com.archbase.security.controller.ArchbaseAuthenticationController`

```java
@RestController
@RequestMapping("/api/v1/auth")
public class ArchbaseAuthenticationController {

    @Autowired
    private ArchbaseAuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
}
```

**Responsabilidade**: Receber requisição HTTP, validar formato e delegar para a camada de serviço.

### Passo 3: Autenticação de Credenciais

O serviço utiliza `AuthenticationManager` do Spring Security para validar as credenciais fornecidas.

**Localização**: `ArchbaseAuthenticationService:114-120`

```java
public AuthenticationResponse authenticate(AuthenticationRequest request) {
    // Validação de credenciais com Spring Security
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );

    // Continua para próximo passo...
}
```

**Processo de Validação**:
1. Spring Security carrega `UserDetails` via `UserDetailsService`
2. Compara senha fornecida com hash armazenado usando `PasswordEncoder`
3. Se credenciais inválidas, lança `BadCredentialsException`
4. Se credenciais válidas, continua para próximo passo

**Exceções Possíveis**:
- `BadCredentialsException`: Email ou senha incorretos
- `DisabledException`: Conta do usuário desativada
- `LockedException`: Conta do usuário bloqueada

### Passo 4: Busca do Usuário

Após autenticação bem-sucedida, o usuário completo é carregado do repositório com todos os relacionamentos.

```java
var user = repository.findByEmail(request.getEmail())
    .orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado"));
```

**Dados Carregados**:
- Informações básicas (nome, email, etc.)
- Perfis e grupos associados
- Permissões (diretas, de perfil e de grupo)
- Configurações de acesso (horários, intervalos)
- Flags de status (isAdministrator, enabled, etc.)

**Importante**: Mesmo após autenticação bem-sucedida, verifica novamente se o usuário existe para evitar race conditions.

### Passo 5: Verificação de Token Existente

O sistema verifica se já existe um token válido não-expirado para o usuário, permitindo reutilização de tokens.

**Localização**: `ArchbaseAuthenticationService:132-140`

```java
AccessTokenEntity accessToken = accessTokenPersistenceAdapter.findValidTokenByUser(user);

if (accessToken != null && !jwtService.isTokenExpired(accessToken.getToken())) {
    log.debug("Token válido encontrado para o usuário {}, reusando token", user.getEmail());

    // Gera apenas novo refresh token
    var refreshToken = jwtService.generateRefreshToken(user);

    return buildAuthenticationResponse(accessToken, refreshToken.token(), user);
}
```

**Lógica de Reutilização**:
- Se token válido existe → reutiliza access token existente
- Gera **novo** refresh token sempre
- Evita criar múltiplos tokens desnecessários
- Melhora performance (não persiste novo token)

**Vantagens**:
- Reduz quantidade de tokens no banco
- Melhora performance de login
- Mantém consistência de sessão

### Passo 6: Revogação de Tokens Antigos

Se não houver token válido para reutilização, ou se a política de segurança não permitir múltiplos logins, todos os tokens antigos do usuário são revogados.

**Localização**: `ArchbaseAuthenticationService:191-204`

```java
public void revokeAllUserTokens(UserEntity user) {
    var validUserTokens = accessTokenPersistenceAdapter.findAllValidTokenByUser(user);

    if (!validUserTokens.isEmpty()) {
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }
}
```

**Campos Atualizados**:
- `expired = true`: Marca token como expirado
- `revoked = true`: Marca token como revogado

**Estratégias de Revogação**:

| Cenário | Comportamento |
|---------|--------------|
| Login normal | Revoga tokens antigos se `allowMultipleLogins=false` |
| Mudança de senha | **Sempre** revoga todos os tokens |
| Logout | Revoga apenas o token atual |
| Reset de senha | **Sempre** revoga todos os tokens |

**Configuração**:
```properties
archbase.security.allow-multiple-logins=false  # Padrão: false
```

### Passo 7: Geração de Tokens JWT

O serviço JWT gera dois tipos de tokens com características diferentes.

#### Access Token (Curta Duração)

**Finalidade**: Autenticar requisições API

**Duração Típica**: 15 minutos a 1 hora

```java
var jwtToken = jwtService.generateToken(user);
```

**Claims Incluídas**:
- `sub` (subject): Email do usuário
- `iat` (issued at): Timestamp de criação
- `exp` (expiration): Timestamp de expiração
- Claims customizadas (opcional): tenant, company, roles, etc.

#### Refresh Token (Longa Duração)

**Finalidade**: Renovar access token sem fornecer credenciais novamente

**Duração Típica**: 7 dias a 30 dias

```java
var refreshToken = jwtService.generateRefreshToken(user);
```

**Diferenças em relação ao Access Token**:
- Expiração muito maior
- Usado apenas em endpoint `/api/v1/auth/refresh-token`
- Não é persistido no banco (stateless)
- Não contém claims customizadas extensas

#### Implementação da Geração

**Localização**: `ArchbaseJwtService:78-97`

```java
private TokenResult buildToken(Map<String, Object> extraClaims,
                               UserDetails userDetails,
                               long expiration) {
    Instant now = Instant.now();
    Date issuedAt = Date.from(now);
    Date expiresAt = Date.from(now.plusMillis(expiration));

    String token = Jwts.builder()
            .setClaims(extraClaims)                          // Claims customizadas
            .setSubject(userDetails.getUsername())           // Email do usuário
            .setIssuedAt(issuedAt)                          // Data de criação
            .setExpiration(expiresAt)                       // Data de expiração
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)  // Assinatura HMAC-SHA256
            .compact();

    return new TokenResult(token, expiresAt.toInstant().toEpochMilli());
}
```

**Algoritmo de Assinatura**: HS256 (HMAC-SHA256)

**Chave de Assinatura**: Configurada via propriedade (ver Passo 10)

### Passo 8: Persistência do Access Token

O access token é persistido no banco de dados para controle de sessão e revogação.

**Localização**: `ArchbaseAuthenticationService:158-176`

```java
private AccessTokenEntity saveUserToken(UserEntity usuario,
                                       ArchbaseJwtService.TokenResult jwtToken) {
    LocalDateTime expirationDateTime = convertToLocalDateTimeViaInstant(
        jwtService.extractExpiration(jwtToken.token())
    );

    var token = AccessTokenEntity.builder()
            .id(UUID.randomUUID().toString())
            .user(usuario)
            .token(jwtToken.token())
            .expirationTime(jwtToken.expiresIn())           // Milissegundos
            .expirationDate(expirationDateTime)             // LocalDateTime
            .tokenType(TokenType.BEARER)
            .expired(false)
            .revoked(false)
            .build();

    return tokenRepository.save(token);
}
```

**Campos Persistidos**:
- `id`: UUID único do token
- `user`: Referência ao usuário proprietário
- `token`: String completa do JWT (até 5000 caracteres)
- `expirationTime`: Timestamp de expiração em milissegundos
- `expirationDate`: Data e hora de expiração
- `tokenType`: Tipo do token (sempre BEARER para JWT)
- `expired`: Flag de expiração (inicialmente `false`)
- `revoked`: Flag de revogação (inicialmente `false`)

**Importante**: Apenas o **access token** é persistido. O **refresh token** não é armazenado (stateless).

### Passo 9: Construção da Resposta

Uma resposta de autenticação completa é construída e retornada ao cliente.

```java
return AuthenticationResponse.builder()
        .id(accessToken.getId())
        .accessToken(accessToken.getToken())
        .expirationTime(accessToken.getExpirationTime())
        .tokenType(TokenType.BEARER)
        .refreshToken(refreshToken.token())
        .user(user.toDomain())
        .build();
```

**Estrutura da Resposta JSON**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c3VhcmlvQGV4ZW1wbG8uY29tIiwiaWF0IjoxNzA0MDYzNjAwLCJleHAiOjE3MDQwNjcyMDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "expirationTime": 1704067200000,
  "tokenType": "BEARER",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c3VhcmlvQGV4ZW1wbG8uY29tIiwiaWF0IjoxNzA0MDYzNjAwLCJleHAiOjE3MDQ2Njg0MDB9.dQw4w9WgXcQ5qpJ4RhNZ9jqxwEhv8J3nwxTqGvZ4qPo",
  "user": {
    "id": "user-uuid-123",
    "name": "Usuário Exemplo",
    "email": "usuario@exemplo.com",
    "avatar": "https://exemplo.com/avatar.jpg",
    "isAdministrator": false,
    "enabled": true
  }
}
```

### Passo 10: Armazenamento e Uso pelo Cliente

O cliente deve armazenar os tokens de forma segura e incluí-los em requisições subsequentes.

**Armazenamento Recomendado**:
- **Web**: `localStorage` ou `sessionStorage` (cuidado com XSS)
- **Mobile**: Keychain (iOS) ou Keystore (Android)
- **Server-side**: Variáveis de ambiente ou secret manager

**Uso em Requisições**:
```http
GET /api/v1/users/profile HTTP/1.1
Host: api.exemplo.com
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Renovação de Token**:
Quando o access token expira, o cliente usa o refresh token:

```http
POST /api/v1/auth/refresh-token HTTP/1.1
Host: api.exemplo.com
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## Validação de Token JWT

A validação de tokens JWT ocorre em cada requisição autenticada e envolve múltiplas camadas de verificação.

### Visão Geral do Fluxo

```
Requisição HTTP → JwtAuthenticationFilter → Extrair Token → Validar Assinatura
                                                                      ↓
                                                              Validar Expiração
                                                                      ↓
                                                          Verificar Revogação no BD
                                                                      ↓
                                                          Carregar UserDetails
                                                                      ↓
                                                      Configurar SecurityContext
                                                                      ↓
                                                          Continuar Requisição
```

### Interceptação de Requisições

O `JwtAuthenticationFilter` intercepta todas as requisições HTTP e processa o token JWT antes de qualquer controller.

**Localização**: `br.com.archbase.security.filter.JwtAuthenticationFilter`

**Ordem de Execução**: Executado antes de `ArchbaseSecurityFilterChain`

```java
@Component
@Order(1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
                                   throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);  // Sem autenticação JWT
            return;
        }

        // Continua processamento...
    }
}
```

### Extração do Token

Token é extraído do header `Authorization` no formato padrão Bearer.

**Formato Esperado**:
```
Authorization: Bearer <token-jwt>
```

**Código de Extração**:
```java
final String jwt = authHeader.substring(7);  // Remove "Bearer "
final String userEmail = jwtService.extractUsername(jwt);
```

**Validações Iniciais**:
- Header `Authorization` presente
- Formato começa com "Bearer "
- Token não é vazio após extração

### Validação de Assinatura

O serviço JWT valida que o token foi assinado com a chave secreta correta do sistema.

**Localização**: `ArchbaseJwtService`

```java
private Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
}

private Key getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
}
```

**Processo**:
1. Decodifica a chave secreta de Base64
2. Cria parser JWT com a chave
3. Valida assinatura do token
4. Extrai claims se válido

**Exceções Possíveis**:
- `SignatureException`: Assinatura inválida (token adulterado)
- `MalformedJwtException`: Token mal formado
- `UnsupportedJwtException`: Algoritmo não suportado

### Validação de Expiração

Verifica se o token não está expirado comparando timestamp atual com claim `exp`.

**Localização**: `ArchbaseJwtService:100-112`

```java
public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
}

public boolean isTokenExpired(String token) {
    try {
        Date expiration = extractExpiration(token);
        return expiration.toInstant().isBefore(Instant.now());
    } catch (ExpiredJwtException e) {
        return true;
    }
}

public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
}
```

**Verificações**:
1. Extrai claim `exp` do token
2. Compara com timestamp atual (UTC)
3. Retorna `true` se expirado, `false` se válido

**Importante**: A validação de expiração é feita **antes** de consultar o banco, economizando recursos.

### Verificação no Banco de Dados

Confirma que o token não foi revogado manualmente no banco de dados.

```java
AccessTokenEntity storedToken = accessTokenRepository.findByToken(jwt)
    .orElseThrow(() -> new InvalidTokenException("Token não encontrado"));

if (storedToken.isExpired() || storedToken.isRevoked()) {
    throw new InvalidTokenException("Token inválido");
}
```

**Condições de Invalidade**:
- Token não existe em `AccessTokenEntity`
- Campo `expired = true`
- Campo `revoked = true`

**Casos de Uso de Revogação**:
- Logout manual
- Mudança de senha
- Desativação da conta
- Revogação administrativa

### Carregamento do UserDetails

Se o token é válido, carrega os detalhes completos do usuário associado.

```java
String username = jwtService.extractUsername(token);
UserDetails userDetails = userDetailsService.loadUserByUsername(username);

if (!jwtService.isTokenValid(token, userDetails)) {
    throw new InvalidTokenException("Token inválido para o usuário");
}
```

**Dados Carregados**:
- Credenciais do usuário
- Authorities (perfis e grupos)
- Permissões (diretas e herdadas)
- Status da conta (enabled, locked)

**Implementação**: `ArchbaseUserDetailsService` carrega usuário de `UserRepository`

### Configuração do SecurityContext

Define o usuário autenticado no contexto de segurança do Spring para a requisição atual.

```java
UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
    userDetails,
    null,  // Credenciais não necessárias após autenticação
    userDetails.getAuthorities()
);

authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

SecurityContextHolder.getContext().setAuthentication(authToken);
```

**Efeito**:
- `SecurityContextHolder` contém usuário autenticado
- Controllers podem acessar via `@AuthenticationPrincipal`
- `@HasPermission` pode verificar permissões
- Contexto é limpo automaticamente após requisição

**Acesso em Controllers**:
```java
@GetMapping("/profile")
public UserDto getProfile(@AuthenticationPrincipal UserEntity user) {
    return userMapper.toDto(user);
}
```

---

## Autenticação com API Token

Alternativa ao JWT para comunicação serviço-a-serviço ou integrações de longa duração.

### Características dos API Tokens

API Tokens diferem significativamente dos JWT access tokens:

| Característica | JWT Access Token | API Token |
|----------------|------------------|-----------|
| **Duração** | Curta (minutos/horas) | Longa (meses/anos) |
| **Geração** | Automática no login | Manual pelo administrador |
| **Ativação** | Imediata | Requer `activated=true` |
| **Refresh** | Sim (refresh token) | Não |
| **Revogação** | Logout ou expiração | Manual |
| **Uso Típico** | Usuários humanos | Serviços e integrações |
| **Persistência** | Sim (para revogação) | Sim (única fonte de verdade) |

### Criação de API Token

API Tokens são criados manualmente, geralmente por administradores ou através de interfaces dedicadas.

**Exemplo de Criação**:
```java
ApiTokenEntity apiToken = ApiTokenEntity.builder()
    .id(UUID.randomUUID().toString())
    .name("Integração Sistema Externo")
    .description("Token para integração com sistema de pagamento")
    .user(user)  // Usuário associado (herda permissões)
    .token(generateSecureToken())  // Token único e aleatório
    .activated(true)  // Deve ser explicitamente ativado
    .revoked(false)
    .expirationDate(LocalDateTime.now().plusYears(1))  // Longa duração
    .build();

apiTokenRepository.save(apiToken);
```

**Geração de Token Seguro**:
```java
private String generateSecureToken() {
    byte[] randomBytes = new byte[64];
    new SecureRandom().nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
}
```

**Resultado**: Token de 86 caracteres, seguro e URL-safe.

### Uso de API Token

Cliente inclui o token no header `Authorization` com prefixo `ApiToken` (ao invés de `Bearer`).

**Formato**:
```http
GET /api/v1/external/data HTTP/1.1
Host: api.exemplo.com
Authorization: ApiToken dGhpc2lzYXRva2VuZXhhbXBsZQ...
Content-Type: application/json
```

**Diferença de Formato**:
- JWT: `Authorization: Bearer <jwt>`
- API Token: `Authorization: ApiToken <token>`

### Validação de API Token

O filter identifica o tipo de autenticação pelo prefixo e valida conforme o tipo.

**Localização**: `ApiTokenAuthenticationFilter`

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain filterChain)
                               throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("ApiToken ")) {
        filterChain.doFilter(request, response);
        return;
    }

    final String apiToken = authHeader.substring(9);  // Remove "ApiToken "

    // Validação...
}
```

**Verificações Realizadas**:
1. Token existe no banco (`ApiTokenEntity`)
2. `activated = true` (token foi ativado)
3. `revoked = false` (token não foi revogado)
4. `expirationDate > now()` (não expirou)
5. Usuário associado existe e está ativo

**Exceções**:
- `InvalidApiTokenException`: Token inválido ou não encontrado
- `ApiTokenNotActivatedException`: Token existe mas não está ativado
- `ApiTokenRevokedException`: Token foi revogado
- `ApiTokenExpiredException`: Token expirou

### Gerenciamento de API Tokens

**Listar Tokens do Usuário**:
```java
List<ApiTokenEntity> tokens = apiTokenRepository.findByUser(user);
```

**Revogar Token**:
```java
ApiTokenEntity token = apiTokenRepository.findById(tokenId)
    .orElseThrow(() -> new NotFoundException("Token não encontrado"));

token.setRevoked(true);
apiTokenRepository.save(token);
```

**Renovar Token** (criar novo e revogar antigo):
```java
ApiTokenEntity oldToken = apiTokenRepository.findById(oldTokenId)
    .orElseThrow(() -> new NotFoundException("Token não encontrado"));

oldToken.setRevoked(true);
apiTokenRepository.save(oldToken);

ApiTokenEntity newToken = ApiTokenEntity.builder()
    .name(oldToken.getName())
    .description(oldToken.getDescription())
    .user(oldToken.getUser())
    .token(generateSecureToken())
    .activated(true)
    .revoked(false)
    .expirationDate(LocalDateTime.now().plusYears(1))
    .build();

apiTokenRepository.save(newToken);
```

---

## Fluxo de Redefinição de Senha

O fluxo de redefinição de senha permite que usuários recuperem acesso às suas contas de forma segura.

### Visão Geral do Fluxo

```
Solicitar Redefinição → Verificar Usuário → Revogar Tokens Antigos → Gerar Token
                                                                          ↓
                                                                    Enviar Email
                                                                          ↓
                                                             Usuário Submete Token
                                                                          ↓
                                                                 Validar Token
                                                                          ↓
                                                              Atualizar Senha
                                                                          ↓
                                                  Revogar Token + Invalidar Sessões
```

### Passo 1: Solicitação de Redefinição

Usuário fornece seu email através de um formulário de "Esqueci minha senha".

**Endpoint**: `POST /api/v1/auth/send-reset-password-email`

**Payload**:
```json
{
  "email": "usuario@exemplo.com"
}
```

**Código Controller**:
```java
@PostMapping("/send-reset-password-email")
public ResponseEntity<Void> sendResetPasswordEmail(@RequestBody ResetPasswordRequest request) {
    authenticationService.sendResetPasswordEmail(request.getEmail());
    return ResponseEntity.ok().build();
}
```

**Importante**: Sempre retorna sucesso (200 OK) mesmo se o email não existir, para evitar enumeration attack.

### Passo 2: Verificação do Usuário

Sistema verifica se o usuário existe e tem permissão para alterar senha.

**Localização**: `ArchbaseAuthenticationService:257-270`

```java
Optional<UserEntity> usuarioOptional = repository.findByEmail(email);

if (usuarioOptional.isEmpty()) {
    throw new ArchbaseValidationException(
        String.format("Usuário com email %s não foi encontrado.", email)
    );
}

UserEntity user = usuarioOptional.get();

if (!user.getAllowPasswordChange()) {
    throw new ArchbaseValidationException(
        String.format("Usuário com email %s não possui autorização para alterar a senha.", email)
    );
}
```

**Condições para Prosseguir**:
1. Usuário existe no banco
2. `allowPasswordChange = true` (permissão para alterar senha)
3. Conta está ativa (opcional, dependendo da política)

**Campo `allowPasswordChange`**: Permite controle granular sobre quem pode redefinir senha (útil para contas de serviço).

### Passo 3: Revogação de Tokens Anteriores

Qualquer token de redefinição anterior do usuário é revogado para segurança.

```java
private void revokeExistingTokens(UserEntity user) {
    List<PasswordResetTokenEntity> existingTokens =
        passwordResetTokenRepository.findByUser(user);

    existingTokens.forEach(token -> {
        token.setRevoked(true);
        token.setExpired(true);
    });

    passwordResetTokenRepository.saveAll(existingTokens);
}
```

**Razão**: Evita que tokens antigos enviados por email sejam reutilizados.

**Estratégia**: Apenas o token mais recente é válido.

### Passo 4: Geração do Token

Um token numérico de 13 caracteres é gerado para facilitar digitação.

**Localização**: `ArchbaseAuthenticationService:272-277`

```java
public String createPasswordResetToken(User user) {
    String passwordResetToken = TokenGeneratorUtil.generateNumericToken();

    PasswordResetToken token = new PasswordResetToken(passwordResetToken, user);
    passwordResetTokenPersistenceAdapter.save(token);

    return passwordResetToken;
}
```

**Implementação do Gerador**:
```java
public class TokenGeneratorUtil {
    private static final int TOKEN_LENGTH = 13;

    public static String generateNumericToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);

        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(random.nextInt(10));  // Dígitos 0-9
        }

        return token.toString();
    }
}
```

**Exemplo de Token Gerado**: `1234567890123`

**Características**:
- 13 dígitos numéricos
- Fácil de digitar (sem caracteres especiais)
- Suficientemente longo para segurança (~10^13 combinações)
- Armazenado como string no banco

### Passo 5: Envio do Email

Token é enviado para o email do usuário via `ArchbaseEmailService`.

```java
archbaseEmailService.sendResetPasswordEmail(
    email,
    passwordResetToken,
    user.getUsername(),
    user.getName()
);
```

**Template de Email** (exemplo):
```html
<!DOCTYPE html>
<html>
<head>
    <title>Redefinição de Senha</title>
</head>
<body>
    <h2>Olá, {{userName}}!</h2>
    <p>Você solicitou a redefinição de senha para a conta <strong>{{email}}</strong>.</p>
    <p>Seu código de redefinição é:</p>
    <h1 style="letter-spacing: 2px;">{{resetToken}}</h1>
    <p>Este código expira em <strong>1 hora</strong>.</p>
    <p>Se você não solicitou esta redefinição, ignore este email.</p>
</body>
</html>
```

**Configuração de Email**:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=seu-email@gmail.com
spring.mail.password=sua-senha-app
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Passo 6: Submissão do Token

Usuário submete o token recebido junto com a nova senha desejada.

**Endpoint**: `POST /api/v1/auth/reset-password`

**Payload**:
```json
{
  "email": "usuario@exemplo.com",
  "passwordResetToken": "1234567890123",
  "newPassword": "novaSenhaSegura456"
}
```

**Validações no Payload**:
- `email`: Formato válido, não vazio
- `passwordResetToken`: 13 dígitos numéricos
- `newPassword`: Mínimo 8 caracteres, atende política de senha

### Passo 7: Validação do Token

Sistema valida o token de redefinição com múltiplas verificações.

**Localização**: `ArchbaseAuthenticationService:288-310`

```java
PasswordResetToken token = passwordResetTokenPersistenceAdapter
    .findToken(user, request.getPasswordResetToken());

if (token == null) {
    throw new ArchbaseValidationException("Token de redefinição de senha inválido.");
}

token.updateExpired();  // Atualiza flag de expiração baseado em timestamp
passwordResetTokenPersistenceAdapter.save(token);

if (token.isExpired()) {
    throw new ArchbaseValidationException(
        "Token de redefinição de senha expirado, favor gerar novamente."
    );
}

if (token.isRevoked()) {
    throw new ArchbaseValidationException(
        "Token de redefinição de senha inválido, favor utilizar o token mais recente."
    );
}
```

**Verificações Realizadas**:
1. Token existe no banco
2. Token pertence ao usuário correto
3. Token não foi revogado (`revoked = false`)
4. Token não expirou (tempo < 1 hora desde criação)

**Método `updateExpired()`**:
```java
public void updateExpired() {
    if (expirationTime != null) {
        this.expired = Instant.now().toEpochMilli() > expirationTime;
    }
}
```

### Passo 8: Atualização da Senha

Senha é validada contra política, criptografada e atualizada no banco.

```java
// Validar política de senha
validatePasswordPolicy(request.getNewPassword());

// Criptografar nova senha
String encodedPassword = passwordEncoder.encode(request.getNewPassword());

// Atualizar usuário
user.setPassword(encodedPassword);
repository.save(user);
```

**Política de Senha** (configurável):
- Mínimo 8 caracteres
- Ao menos 1 letra maiúscula
- Ao menos 1 letra minúscula
- Ao menos 1 número
- Ao menos 1 caractere especial

**Algoritmo de Criptografia**: BCrypt (padrão do Spring Security)

### Passo 9: Revogação do Token de Redefinição

Token de redefinição é revogado imediatamente após uso para evitar reutilização.

```java
token.revokeToken();
passwordResetTokenPersistenceAdapter.save(token);
```

**Implementação de `revokeToken()`**:
```java
public void revokeToken() {
    this.revoked = true;
    this.expired = true;
}
```

**Importante**: Token só pode ser usado **uma vez**. Tentativas subsequentes falharão na validação (Passo 7).

### Passo 10: Invalidação de Access Tokens

Todos os tokens de acesso do usuário são invalidados para forçar novo login em todos os dispositivos.

```java
revokeAllUserTokens(user);
```

**Razão de Segurança**:
- Impede que atacantes com tokens antigos mantenham acesso
- Força usuário a fazer login novamente com nova senha
- Garante que apenas o usuário legítimo terá acesso

**Notificação** (opcional):
```java
archbaseEmailService.sendPasswordChangedNotification(
    user.getEmail(),
    user.getName()
);
```

---

## Configuração do Sistema de Autenticação

### Propriedades Básicas

```properties
# Chave secreta para assinatura JWT (Base64)
archbase.security.jwt.secret=YXJjaGJhc2Utc2VjcmV0LWtleS1leGFtcGxlLTEyMzQ1Njc4OTA=

# Duração do access token (milissegundos) - 1 hora
archbase.security.jwt.expiration=3600000

# Duração do refresh token (milissegundos) - 7 dias
archbase.security.jwt.refresh-expiration=604800000

# Permitir múltiplos logins simultâneos
archbase.security.allow-multiple-logins=false

# Duração do token de redefinição de senha (milissegundos) - 1 hora
archbase.security.password-reset-token.expiration=3600000
```

### Configuração de Email

```properties
# Servidor SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=seu-email@gmail.com
spring.mail.password=sua-senha-app

# Propriedades SMTP
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Remetente padrão
archbase.email.from=noreply@exemplo.com
archbase.email.from-name=Sistema Archbase
```

### Configuração de Política de Senha

```properties
# Habilitar validação de política de senha
archbase.security.password.policy.enabled=true

# Comprimento mínimo
archbase.security.password.policy.min-length=8

# Requer letra maiúscula
archbase.security.password.policy.require-uppercase=true

# Requer letra minúscula
archbase.security.password.policy.require-lowercase=true

# Requer número
archbase.security.password.policy.require-digit=true

# Requer caractere especial
archbase.security.password.policy.require-special=true

# Caracteres especiais aceitos
archbase.security.password.policy.special-chars=!@#$%^&*()_+-=[]{}|;:,.<>?
```

---

## Diagrama de Sequência

### Autenticação JWT Completa

```
Cliente                  Controller                Service              Repository            JWT Service
  |                          |                        |                      |                      |
  |--- POST /login --------->|                        |                      |                      |
  |                          |                        |                      |                      |
  |                          |--- authenticate() ---->|                      |                      |
  |                          |                        |                      |                      |
  |                          |                        |--- findByEmail() --->|                      |
  |                          |                        |<--- UserEntity ------|                      |
  |                          |                        |                      |                      |
  |                          |                        |--- generateToken() ----------------------->|
  |                          |                        |<--- JWT Token ---------------------------|
  |                          |                        |                      |                      |
  |                          |                        |--- save(token) ----->|                      |
  |                          |                        |<--- AccessToken -----|                      |
  |                          |                        |                      |                      |
  |                          |<--- AuthResponse ------|                      |                      |
  |<--- 200 OK + JWT --------|                        |                      |                      |
  |                          |                        |                      |                      |
```

### Validação de Token

```
Cliente                  Filter                 JWT Service          Repository           SecurityContext
  |                          |                        |                      |                      |
  |--- GET /api/users ------>|                        |                      |                      |
  |  Authorization: Bearer   |                        |                      |                      |
  |                          |                        |                      |                      |
  |                          |--- isTokenValid() ---->|                      |                      |
  |                          |                        |                      |                      |
  |                          |                        |--- findByToken() --->|                      |
  |                          |                        |<--- AccessToken -----|                      |
  |                          |<--- true --------------|                      |                      |
  |                          |                        |                      |                      |
  |                          |--- setAuthentication() -------------------------------->|
  |                          |                        |                      |                      |
  |                          |--- filterChain.doFilter() -------------------------------------------------------->
  |<--- 200 OK + Data -----------------------------------------------------------------------------|
  |                          |                        |                      |                      |
```

---

## Resumo

### Comparação de Tipos de Token

| Aspecto | Access Token (JWT) | Refresh Token (JWT) | API Token |
|---------|-------------------|---------------------|-----------|
| **Duração** | 15 min - 1 hora | 7 - 30 dias | Meses - Anos |
| **Persistido** | Sim (para revogação) | Não (stateless) | Sim (única fonte) |
| **Renovável** | Via refresh token | Não | Não |
| **Uso** | Requisições API | Renovar access token | Integrações serviços |
| **Revogação** | Sim (banco) | N/A | Sim (banco) |
| **Formato** | Bearer + JWT | Bearer + JWT | ApiToken + String |

### Melhores Práticas

1. **Segurança de Chaves**:
   - Usar chave secreta forte (mínimo 256 bits)
   - Rotacionar chave periodicamente
   - Armazenar em variáveis de ambiente, não no código

2. **Gestão de Tokens**:
   - Definir expiração curta para access tokens
   - Implementar renovação via refresh token
   - Revogar todos os tokens ao mudar senha

3. **API Tokens**:
   - Usar apenas para serviços, não usuários humanos
   - Implementar log de uso para auditoria
   - Rotacionar periodicamente

4. **Redefinição de Senha**:
   - Token de uso único
   - Expiração curta (1 hora)
   - Notificar mudanças de senha por email
   - Invalidar todas as sessões após reset

5. **Armazenamento no Cliente**:
   - Nunca em cookies sem HttpOnly
   - Preferir storage local seguro
   - Limpar tokens ao fazer logout

---

**Ver também**:
- [Sistema de Permissões](permissions-system.md) - Como permissões são avaliadas após autenticação
- [Multi-Tenancy](multi-tenancy.md) - Propagação de contexto de tenant em autenticação
- [Entidades de Segurança](../entities/security-entities.md) - Modelo de dados completo
- [Guia de Uso](../guides/authentication-guide.md) - Exemplos práticos de implementação

---

[← Anterior: Sistema de Permissões](permissions-system.md) | [Voltar ao Índice](../README.md) | [Próximo: Multi-Tenancy →](multi-tenancy.md)
