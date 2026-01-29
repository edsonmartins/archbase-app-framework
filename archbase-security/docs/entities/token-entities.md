[← Anterior: Sistema de Permissões](permission-entities.md) | [Voltar ao Índice](../README.md) | [Próximo: Sistema de Autenticação →](../architecture/authentication-system.md)

---

# Gerenciamento de Tokens

## Introdução

O módulo `archbase-security` utiliza três tipos de tokens para diferentes propósitos de autenticação e segurança:

1. **AccessTokenEntity** - Tokens JWT para autenticação de usuários (curta duração)
2. **ApiTokenEntity** - Tokens de API para serviços (longa duração)
3. **PasswordResetTokenEntity** - Tokens temporários para redefinição de senha

Este documento descreve em detalhes cada tipo de token, seus campos, ciclo de vida e uso apropriado.

---

## 1. AccessTokenEntity

**Localização**: `br.com.archbase.security.persistence.AccessTokenEntity`
**Tabela**: `SEGURANCA_TOKEN_ACESSO`

Armazena tokens JWT (JSON Web Tokens) para autenticação de usuários. Estes tokens são gerados durante o login e utilizados para autenticar requisições subsequentes.

### Campos

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_TOKEN_ACESSO | UUID | Identificador único |
| `token` | TOKEN | String(5000) | String do token JWT (único) |
| `tokenType` | TP_TOKEN | Enum | Tipo do token (padrão: BEARER) |
| `revoked` | TOKEN_REVOGADO | Boolean | Indica se o token foi revogado |
| `expired` | TOKEN_EXPIRADO | Boolean | Indica se o token expirou |
| `expirationTime` | TEMPO_EXPIRACAO | Long | Timestamp de expiração em milissegundos (epoch) |
| `expirationDate` | DH_EXPIRACAO | LocalDateTime | Data e hora de expiração |
| `user` | ID_USUARIO | UUID (FK) | Proprietário do token |

### Constraint Único

`UNIQUE (TOKEN)` - Cada token JWT deve ser único no sistema.

### Relacionamentos

| Relacionamento | Entidade | Tipo | Descrição |
|----------------|----------|------|-----------|
| `user` | UserEntity | ManyToOne | Usuário proprietário do token |

### TokenType Enum

```java
public enum TokenType {
    BEARER  // Tipo padrão para tokens JWT
}
```

**Uso**: No header HTTP: `Authorization: Bearer <token>`

### Ciclo de Vida

#### 1. Geração (Login)

Quando um usuário faz login com sucesso:

```java
// 1. Gerar token JWT
var jwtToken = jwtService.generateToken(user);

// 2. Calcular data de expiração
LocalDateTime expirationDateTime = convertToLocalDateTimeViaInstant(
    jwtService.extractExpiration(jwtToken.token())
);

// 3. Persistir no banco
AccessTokenEntity token = AccessTokenEntity.builder()
    .id(UUID.randomUUID().toString())
    .user(user)
    .token(jwtToken.token())
    .expirationTime(jwtToken.expiresIn())
    .expirationDate(expirationDateTime)
    .tokenType(TokenType.BEARER)
    .expired(false)
    .revoked(false)
    .build();
accessTokenRepository.save(token);
```

#### 2. Validação (A Cada Requisição)

O `JwtAuthenticationFilter` valida o token em cada requisição:

```java
// 1. Extrair token do header
String jwt = jwtService.extractToken(request);

// 2. Validar assinatura e expiração (JWT)
if (!jwtService.isTokenExpired(jwt)) {

    // 3. Verificar no banco se não foi revogado
    AccessTokenEntity token = accessTokenRepository.findByToken(jwt);
    if (token != null && !token.isRevoked() && !token.isExpired()) {
        // Token válido - autenticar usuário
    }
}
```

#### 3. Revogação

Tokens podem ser revogados em várias situações:

**Logout Manual**:
```java
accessToken.setRevoked(true);
accessTokenRepository.save(accessToken);
```

**Mudança de Senha**:
```java
// Revogar TODOS os tokens do usuário
List<AccessTokenEntity> userTokens = accessTokenRepository
    .findAllValidTokenByUser(user);
userTokens.forEach(token -> {
    token.setRevoked(true);
    token.setExpired(true);
});
accessTokenRepository.saveAll(userTokens);
```

**Novo Login (quando `allowMultipleLogins=false`)**:
```java
if (!user.getAllowMultipleLogins()) {
    revokeAllUserTokens(user);  // Revogar tokens antigos
}
// Gerar novo token
```

#### 4. Expiração

Tokens expirados automaticamente com base em `expirationTime`:

```java
public boolean isTokenExpired(String token) {
    try {
        Date expiration = extractExpiration(token);
        return expiration.toInstant().isBefore(Instant.now());
    } catch (ExpiredJwtException e) {
        return true;
    }
}
```

**Importante**: Tokens expirados no JWT **e** tokens marcados como `expired=true` no banco são rejeitados.

### Estratégias de Revogação

| Situação | Ação | Motivo |
|----------|------|--------|
| Logout manual | Revogar token atual | Segurança |
| Mudança de senha | Revogar todos os tokens | Invalidar sessões antigas |
| Novo login (`allowMultipleLogins=false`) | Revogar todos os tokens | Permitir apenas uma sessão |
| Desativação de conta | Revogar todos os tokens | Bloquear acesso imediato |
| Suspeita de comprometimento | Revogar todos os tokens | Medida de segurança |

### Tempo de Expiração Padrão

Configurado via propriedade:

```properties
archbase.security.jwt.expiration=86400000  # 24 horas em milissegundos
```

**Recomendações**:
- **Produção**: 15-60 minutos para alta segurança, 1-4 horas para equilíbrio
- **Desenvolvimento**: 24 horas para conveniência

### Refresh Tokens

O sistema também suporta refresh tokens (gerados junto com access tokens):

```java
var refreshToken = jwtService.generateRefreshToken(user);
```

**Características**:
- Duração maior que access tokens (configurável)
- Usados para obter novos access tokens sem re-autenticação
- Não são persistidos no banco (apenas validados via assinatura JWT)

---

## 2. ApiTokenEntity

**Localização**: `br.com.archbase.security.persistence.ApiTokenEntity`
**Tabela**: `SEGURANCA_TOKEN_API`

Tokens de longa duração para comunicação serviço-a-serviço ou integrações externas. Diferente dos access tokens, API tokens não expiram automaticamente e devem ser explicitamente ativados.

### Campos

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_TOKEN_API | UUID | Identificador único |
| `name` | NOME | String(150) | Nome identificador do token |
| `description` | DESCRICAO | String(500) | Descrição do propósito do token |
| `user` | ID_SEGURANCA | UUID (FK) | Usuário associado ao token |
| `token` | TOKEN | String(500) | String do token (único) |
| `revoked` | BO_REVOGADO | Boolean | Indica se o token foi revogado |
| `expirationDate` | DH_EXPIRACAO | LocalDateTime | Data de expiração (opcional) |
| `activated` | BO_ATIVADO | Boolean | Indica se o token está ativado para uso |

### Constraint Único

`UNIQUE (TOKEN)` - Cada API token deve ser único no sistema.

### Relacionamentos

| Relacionamento | Entidade | Tipo | Descrição |
|----------------|----------|------|-----------|
| `user` | UserEntity | ManyToOne | Usuário associado (define permissões) |

### Diferenças em relação a AccessToken

| Aspecto | AccessToken (JWT) | ApiToken |
|---------|------------------|----------|
| **Duração** | Curta (minutos/horas) | Longa (meses/anos) |
| **Geração** | Automática no login | Manual |
| **Ativação** | Imediata | Requer `activated=true` |
| **Uso** | Autenticação de usuários | Autenticação de serviços |
| **Expiração** | Sempre expira | Opcional |
| **Refresh** | Sim (via refresh token) | Não |

### Ciclo de Vida

#### 1. Criação

```java
// Gerar token seguro
String secureToken = generateSecureRandomToken();

// Criar entidade
ApiTokenEntity apiToken = ApiTokenEntity.builder()
    .name("Integração Sistema Externo")
    .description("Token para integração com sistema de pagamento")
    .user(serviceUser)
    .token(secureToken)
    .activated(false)  // Inicia desativado por segurança
    .revoked(false)
    .expirationDate(LocalDateTime.now().plusYears(1))  // 1 ano
    .build();
apiTokenRepository.save(apiToken);
```

#### 2. Ativação

```java
// Ativar token (após aprovação)
apiToken.setActivated(true);
apiTokenRepository.save(apiToken);
```

**Importante**: Tokens com `activated=false` são rejeitados durante autenticação, mesmo que válidos.

#### 3. Uso

Cliente inclui o token no header HTTP:

```http
Authorization: ApiToken <token-value>
```

Ou alternativamente:

```http
X-Api-Token: <token-value>
```

#### 4. Validação

O `ApiTokenAuthenticationFilter` valida:

```java
public boolean isApiTokenValid(String token) {
    ApiTokenEntity apiToken = apiTokenRepository.findByToken(token);

    if (apiToken == null) return false;
    if (!apiToken.getActivated()) return false;
    if (apiToken.getRevoked()) return false;

    // Verificar expiração (se definida)
    if (apiToken.getExpirationDate() != null &&
        apiToken.getExpirationDate().isBefore(LocalDateTime.now())) {
        return false;
    }

    return true;
}
```

#### 5. Revogação

```java
// Revogar token (quando não mais necessário ou comprometido)
apiToken.setRevoked(true);
apiTokenRepository.save(apiToken);
```

### Casos de Uso

**Integrações de Sistema**:
```java
ApiTokenEntity integrationToken = ApiTokenEntity.builder()
    .name("ERP Integration")
    .description("Token para sincronização com sistema ERP")
    .user(systemUser)
    .token(generateToken())
    .activated(true)
    .expirationDate(null)  // Sem expiração
    .build();
```

**APIs de Terceiros**:
```java
ApiTokenEntity partnerToken = ApiTokenEntity.builder()
    .name("Partner XYZ API Access")
    .description("Token para parceiro acessar nossa API")
    .user(partnerUser)
    .token(generateToken())
    .activated(true)
    .expirationDate(LocalDateTime.now().plusMonths(6))
    .build();
```

**Serviços Internos**:
```java
ApiTokenEntity microserviceToken = ApiTokenEntity.builder()
    .name("Notification Service")
    .description("Token para microserviço de notificações")
    .user(notificationServiceUser)
    .token(generateToken())
    .activated(true)
    .expirationDate(null)  // Serviços internos sem expiração
    .build();
```

### Geração de Token Seguro

```java
private String generateSecureRandomToken() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
}
```

**Recomendações**:
- Mínimo 32 bytes de entropia
- Usar Base64 URL-safe encoding
- Armazenar de forma segura (variáveis de ambiente, secrets manager)

---

## 3. PasswordResetTokenEntity

**Localização**: `br.com.archbase.security.persistence.PasswordResetTokenEntity`
**Tabela**: `SEGURANCA_TOKEN_REDEFINICAO_SENHA`

Tokens temporários e de uso único para redefinição de senha. Estes tokens são curtos, numéricos e têm expiração rápida.

### Campos

| Campo | Coluna | Tipo | Descrição |
|-------|--------|------|-----------|
| `id` | ID_TOKEN_REDEFINICAO | UUID | Identificador único |
| `token` | TOKEN | String(13) | Token numérico de 13 caracteres |
| `revoked` | TOKEN_REVOGADO | Boolean | Indica se o token foi revogado |
| `expired` | TOKEN_EXPIRADO | Boolean | Indica se o token expirou |
| `expirationTime` | TEMPO_EXPIRACAO | Long | Tempo de expiração em milissegundos (epoch) |
| `user` | ID_USUARIO | UUID (FK) | Usuário solicitante |

### Constraint Único

`UNIQUE (TOKEN)` - Cada token de redefinição deve ser único.

### Relacionamentos

| Relacionamento | Entidade | Tipo | Descrição |
|----------------|----------|------|-----------|
| `user` | UserEntity | ManyToOne | Usuário que solicitou redefinição |

### Características

- **Token Curto**: 13 caracteres numéricos (fácil de digitar)
- **Tempo de Expiração**: 1 hora (padrão)
- **Uso Único**: Revogado automaticamente após ser utilizado
- **Revogação em Cascata**: Tokens anteriores são revogados ao gerar novo

### Fluxo Completo

#### Passo 1: Solicitação de Redefinição

```java
public void sendResetPasswordEmail(String email) {
    // 1. Verificar usuário
    UserEntity user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ArchbaseValidationException("Usuário não encontrado"));

    // 2. Verificar permissão
    if (!user.getAllowPasswordChange()) {
        throw new ArchbaseValidationException(
            "Usuário não possui autorização para alterar senha"
        );
    }

    // 3. Revogar tokens anteriores
    revokeExistingPasswordResetTokens(user);

    // 4. Gerar novo token
    String token = generateNumericToken(13);

    PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
        .token(token)
        .user(user)
        .expirationTime(System.currentTimeMillis() + 3600000)  // 1 hora
        .expired(false)
        .revoked(false)
        .build();
    passwordResetTokenRepository.save(resetToken);

    // 5. Enviar email
    emailService.sendPasswordResetEmail(user.getEmail(), token);
}
```

#### Passo 2: Submissão do Token

```java
public void resetPassword(String token, String newPassword) {
    // 1. Buscar token
    PasswordResetTokenEntity resetToken =
        passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new ArchbaseValidationException("Token inválido"));

    // 2. Validar token
    if (resetToken.isRevoked() || resetToken.isExpired()) {
        throw new ArchbaseValidationException("Token inválido ou expirado");
    }

    if (resetToken.getExpirationTime() < System.currentTimeMillis()) {
        resetToken.setExpired(true);
        passwordResetTokenRepository.save(resetToken);
        throw new ArchbaseValidationException("Token expirado");
    }

    // 3. Atualizar senha
    UserEntity user = resetToken.getUser();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    // 4. Revogar token de reset
    resetToken.setRevoked(true);
    passwordResetTokenRepository.save(resetToken);

    // 5. Invalidar todos os access tokens
    revokeAllUserAccessTokens(user);
}
```

### Geração de Token Numérico

```java
private String generateNumericToken(int length) {
    SecureRandom random = new SecureRandom();
    StringBuilder token = new StringBuilder();

    for (int i = 0; i < length; i++) {
        token.append(random.nextInt(10));
    }

    return token.toString();
}
```

**Exemplo de Token**: `"4729183650274"`

### Formato de Email

```html
Olá [Nome do Usuário],

Você solicitou a redefinição de senha. Use o código abaixo para criar uma nova senha:

Código: 4729183650274

Este código expira em 1 hora.

Se você não solicitou esta redefinição, ignore este email.
```

### Segurança

**Medidas de Segurança**:
1. **Expiração Rápida**: 1 hora (configurável)
2. **Uso Único**: Token revogado após uso
3. **Revogação em Cascata**: Novos pedidos invalidam tokens anteriores
4. **Invalidação de Sessões**: Todos os access tokens são revogados após reset
5. **Verificação de Permissão**: Campo `allowPasswordChange` deve ser `true`

**Configuração de Expiração**:

```properties
# Em application.properties (se implementado)
archbase.security.password-reset.expiration=3600000  # 1 hora em ms
```

---

## Resumo Comparativo

| Aspecto | AccessToken | ApiToken | PasswordResetToken |
|---------|-------------|----------|-------------------|
| **Formato** | JWT (longo) | String aleatória | Numérico (13 chars) |
| **Duração** | Minutos/horas | Meses/anos | 1 hora |
| **Geração** | Automática (login) | Manual | Automática (pedido) |
| **Ativação** | Imediata | Requer ativação | Imediata |
| **Uso** | Autenticação de usuário | Autenticação de serviço | Redefinição de senha |
| **Reutilizável** | Sim (até expirar) | Sim (até revogar) | Não (uso único) |
| **Refresh** | Sim | Não | Não |

---

**Ver também**:
- [Fluxo de Autenticação JWT](../architecture/authentication-system.md) - Como access tokens são gerados
- [Configuração de Segurança](../guides/configuration.md) - Propriedades de tokens
- [Exemplos de Código](../guides/code-examples.md) - Implementações práticas
- [Melhores Práticas](../guides/best-practices.md) - Gerenciamento de tokens

---

[← Anterior: Sistema de Permissões](permission-entities.md) | [Voltar ao Índice](../README.md) | [Próximo: Sistema de Autenticação →](../architecture/authentication-system.md)
