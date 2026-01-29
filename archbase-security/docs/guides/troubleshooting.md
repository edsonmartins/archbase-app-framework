[← Anterior: Melhores Práticas](best-practices.md) | [Voltar ao Índice](../README.md)

---

# Troubleshooting - Solução de Problemas

## Introdução

Este guia fornece soluções para problemas comuns encontrados ao trabalhar com o módulo `archbase-security`. As soluções estão organizadas por categoria e incluem sintomas, causas possíveis e passos para resolução.

---

## 1. Problemas Comuns

### 1.1 Erro: "Token inválido" mesmo com token correto

**Sintomas**:
- Token parece válido mas autenticação falha
- Erro "Invalid JWT signature" nos logs
- Cliente recebe status 401 Unauthorized
- Token decodifica corretamente em jwt.io mas é rejeitado pela aplicação

**Causas Possíveis**:

1. **Chave secreta diferente entre geração e validação**
   - Token foi gerado com uma chave, mas aplicação está usando outra
   - Comum após reiniciar aplicação sem variáveis de ambiente configuradas

2. **Token gerado em um ambiente, validado em outro**
   - Token de desenvolvimento usado em produção
   - Múltiplas instâncias da aplicação com chaves diferentes

3. **Formato da chave incorreto**
   - Espaços ou quebras de linha na chave secreta
   - Codificação de caracteres incorreta

**Solução**:

```bash
# 1. Verificar chave configurada no ambiente
echo $JWT_SECRET

# 2. Verificar propriedade no application.properties
cat application.properties | grep jwt.secret

# 3. Verificar que a propriedade está sendo lida corretamente
# application.properties deve ter:
archbase.security.jwt.secret-key=${JWT_SECRET}

# 4. Testar com chave hardcoded (APENAS PARA DEBUG)
archbase.security.jwt.secret-key=test-key-for-debugging-only
```

**Validação**:

Adicione logging temporário na inicialização da aplicação:

```java
@Component
public class JwtSecretValidator implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${archbase.security.jwt.secret-key}")
    private String secretKey;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Log apenas os primeiros caracteres por segurança
        log.info("JWT Secret configurado: {}...", secretKey.substring(0, Math.min(10, secretKey.length())));
    }
}
```

---

### 1.2 Erro: "Access Denied" para administrador

**Sintomas**:
- Usuário tem `isAdministrator=true` mas acesso é negado
- Permissões não são bypassadas como esperado
- Administrador não consegue acessar recursos que deveria ter acesso total
- Log mostra "Permission denied" mesmo para admin

**Causas Possíveis**:

1. **Conta desativada** (`accountDeactivated=true`)
   - Conta foi explicitamente desativada
   - Comum após múltiplas falhas de login

2. **Conta bloqueada** (`accountLocked=true`)
   - Conta bloqueada por política de segurança
   - Pode ser bloqueio temporário ou permanente

3. **Método `isEnabled()` retorna `false`**
   - Usuário não passou por todas as validações de estado
   - Pode haver lógica customizada verificando outros campos

4. **Horário de acesso fora do permitido**
   - `AccessScheduleEntity` configurado com horários restritos
   - Válido mesmo para administradores

**Solução**:

```sql
-- 1. Verificar status completo da conta
SELECT
    id,
    user_name,
    bo_administrador,
    bo_conta_desativada,
    conta_bloqueada,
    bo_senha_expirada,
    dt_expiracao_senha
FROM seguranca
WHERE tp_seguranca = 'USUARIO' AND id = 'user-id-here';

-- 2. Reativar conta se necessário
UPDATE seguranca
SET
    bo_conta_desativada = 'N',
    conta_bloqueada = 'N',
    bo_senha_expirada = 'N'
WHERE id = 'user-id-here';

-- 3. Verificar horários de acesso
SELECT * FROM schedule_access
WHERE security_id = 'user-id-here';

-- 4. Limpar horários restritivos se necessário
DELETE FROM schedule_access
WHERE security_id = 'user-id-here';
```

**Debug Programático**:

```java
@GetMapping("/debug/admin-status/{userId}")
@PreAuthorize("hasRole('SUPER_ADMIN')") // Apenas para super admins
public Map<String, Object> debugAdminStatus(@PathVariable String userId) {
    UserEntity user = userRepository.findById(UUID.fromString(userId))
        .orElseThrow(() -> new EntityNotFoundException("User not found"));

    Map<String, Object> status = new HashMap<>();
    status.put("isAdministrator", user.getIsAdministrator());
    status.put("isEnabled", user.isEnabled());
    status.put("accountNonExpired", user.isAccountNonExpired());
    status.put("accountNonLocked", user.isAccountNonLocked());
    status.put("credentialsNonExpired", user.isCredentialsNonExpired());
    status.put("accountDeactivated", user.getAccountDeactivated());
    status.put("accountLocked", user.getAccountLocked());
    status.put("passwordExpired", user.getPasswordExpired());

    return status;
}
```

---

### 1.3 Erro: Contexto de tenant null em operações assíncronas

**Sintomas**:
- Operações síncronas funcionam corretamente
- Operações assíncronas falham com "Tenant context not found"
- `@Async` methods não têm acesso ao tenant
- Background jobs falham ao tentar acessar dados específicos do tenant
- Erro: `IllegalStateException: No tenant context available`

**Causa**:
- Contexto ThreadLocal não é propagado automaticamente para novas threads
- `ArchbaseTenantContext` usa ThreadLocal que é isolado por thread
- Spring `@Async` cria novas threads sem copiar o contexto

**Solução**:

Implemente `TaskDecorator` para propagar contexto:

```java
import br.com.archbase.ddd.context.ArchbaseTenantContext;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // Capturar tenant da thread atual
        String tenantId = ArchbaseTenantContext.getTenantId();
        String companyId = ArchbaseTenantContext.getCompanyId();
        String projectId = ArchbaseTenantContext.getProjectId();

        return () -> {
            try {
                // Restaurar contexto na nova thread
                if (tenantId != null) {
                    ArchbaseTenantContext.setTenantId(tenantId);
                }
                if (companyId != null) {
                    ArchbaseTenantContext.setCompanyId(companyId);
                }
                if (projectId != null) {
                    ArchbaseTenantContext.setProjectId(projectId);
                }

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

Configure o executor assíncrono:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");

        // Configurar decorator para propagação de contexto
        executor.setTaskDecorator(new TenantAwareTaskDecorator());

        executor.initialize();
        return executor;
    }
}
```

Use o executor configurado:

```java
@Service
public class NotificationService {

    @Async("taskExecutor")
    public void sendNotificationAsync(String message) {
        // Agora o tenant context está disponível
        String tenantId = ArchbaseTenantContext.getTenantId();
        log.info("Sending notification for tenant: {}", tenantId);
        // ... implementação
    }
}
```

---

### 1.4 Erro: "Token expired" imediatamente após login

**Sintomas**:
- Token expira segundos após ser gerado
- Tempo de expiração parece estar incorreto
- Token válido por apenas alguns segundos ao invés de horas/dias
- Cliente precisa fazer refresh imediatamente após login

**Causas Possíveis**:

1. **Configuração de expiração em formato errado**
   - Valor configurado em segundos ao invés de milissegundos
   - Configuração usa unidade errada

2. **Fuso horário incorreto**
   - Servidor com relógio dessincronizado
   - Container Docker sem timezone configurado

3. **Valor muito pequeno por erro de digitação**
   - Falta de zeros na configuração
   - Confusão entre minutos e milissegundos

**Solução**:

```properties
# FORMATO CORRETO - Valores em MILISSEGUNDOS

# ❌ ERRADO - 1 hora em segundos (token expira em 3.6 segundos!)
archbase.security.jwt.token-expiration=3600

# ✓ CORRETO - 1 hora em milissegundos
archbase.security.jwt.token-expiration=3600000

# Exemplos de configurações comuns:
# 15 minutos = 900000 ms
archbase.security.jwt.token-expiration=900000

# 1 hora = 3600000 ms
archbase.security.jwt.token-expiration=3600000

# 8 horas = 28800000 ms
archbase.security.jwt.token-expiration=28800000

# 24 horas = 86400000 ms
archbase.security.jwt.token-expiration=86400000

# Refresh token - 7 dias = 604800000 ms
archbase.security.jwt.refresh-expiration=604800000
```

**Validação do Token**:

Crie um endpoint de debug para verificar a expiração:

```java
@RestController
@RequestMapping("/api/debug")
@Profile("dev")
public class TokenDebugController {

    @Autowired
    private ArchbaseJwtService jwtService;

    @GetMapping("/token-expiration")
    public Map<String, Object> checkTokenExpiration(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");

        Date expiration = jwtService.extractExpiration(token);
        Date now = new Date();
        long timeToExpire = expiration.getTime() - now.getTime();

        Map<String, Object> info = new HashMap<>();
        info.put("currentTime", now);
        info.put("expirationTime", expiration);
        info.put("millisecondsToExpire", timeToExpire);
        info.put("minutesToExpire", timeToExpire / 1000 / 60);
        info.put("hoursToExpire", timeToExpire / 1000 / 60 / 60);
        info.put("isExpired", jwtService.isTokenExpired(token));

        return info;
    }
}
```

**Verificar Sincronização de Relógio**:

```bash
# Verificar data/hora do servidor
date

# Verificar timezone
timedatectl

# Para containers Docker, adicione ao docker-compose.yml:
# environment:
#   - TZ=America/Sao_Paulo
# volumes:
#   - /etc/localtime:/etc/localtime:ro
```

---

## 2. Dicas de Debug

### 2.1 Habilitar Logging Detalhado

Configure níveis de log apropriados para diagnosticar problemas:

```properties
# application.properties ou application-dev.properties

# ========================================
# LOGGING DE SEGURANÇA
# ========================================

# Módulo de segurança do Archbase (RECOMENDADO)
logging.level.br.com.archbase.security=DEBUG

# Spring Security (DETALHADO)
logging.level.org.springframework.security=DEBUG

# Contexto de tenant (MULTI-TENANCY)
logging.level.br.com.archbase.ddd.context=DEBUG

# SQL queries e parâmetros (BANCO DE DADOS)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Filtros HTTP (REQUISIÇÕES)
logging.level.org.springframework.web.filter=DEBUG

# JWT processing (TOKEN)
logging.level.io.jsonwebtoken=DEBUG
```

**Logging Estruturado para Produção**:

```properties
# application-prod.properties

# Menos verboso em produção
logging.level.br.com.archbase.security=INFO
logging.level.org.springframework.security=WARN

# Mas mantém eventos importantes
logging.level.br.com.archbase.security.audit=INFO
```

---

### 2.2 Debugar Avaliação de Permissões

Adicione logging detalhado no serviço de segurança para rastrear o processo de avaliação:

```java
@Service
@Slf4j
public class ArchbaseSecurityService {

    public boolean hasPermission(
            String userId,
            String action,
            String resource,
            String tenantId,
            String companyId,
            String projectId) {

        log.debug("=== INÍCIO DA AVALIAÇÃO DE PERMISSÃO ===");
        log.debug("Usuário ID: {}", userId);
        log.debug("Ação solicitada: {}", action);
        log.debug("Recurso solicitado: {}", resource);
        log.debug("Contexto - Tenant: {}, Company: {}, Project: {}",
            tenantId, companyId, projectId);

        // Buscar usuário
        UserEntity user = userRepository.findById(UUID.fromString(userId))
            .orElse(null);

        if (user == null) {
            log.warn("Usuário não encontrado: {}", userId);
            return false;
        }

        log.debug("Usuário encontrado: {}", user.getUserName());
        log.debug("É administrador: {}", user.getIsAdministrator());
        log.debug("Conta habilitada: {}", user.isEnabled());

        // Verificar bypass de administrador
        if (user.getIsAdministrator() && user.isEnabled()) {
            log.info("PERMISSÃO CONCEDIDA: Usuário é administrador");
            return true;
        }

        // Buscar permissões diretas do usuário
        List<PermissionEntity> userPermissions = permissionRepository
            .findBySecurityIdAndActionNameAndResourceName(
                UUID.fromString(userId), action, resource);

        log.debug("Permissões diretas encontradas: {}", userPermissions.size());

        // Buscar grupos do usuário
        List<GroupEntity> userGroups = groupRepository.findByUserId(UUID.fromString(userId));
        log.debug("Grupos do usuário: {}", userGroups.size());

        for (GroupEntity group : userGroups) {
            log.debug("  - Grupo: {} ({})", group.getName(), group.getId());
        }

        // Buscar permissões dos grupos
        List<PermissionEntity> groupPermissions = new ArrayList<>();
        for (GroupEntity group : userGroups) {
            List<PermissionEntity> perms = permissionRepository
                .findBySecurityIdAndActionNameAndResourceName(
                    group.getId(), action, resource);
            log.debug("Permissões do grupo {}: {}", group.getName(), perms.size());
            groupPermissions.addAll(perms);
        }

        // Combinar todas as permissões
        List<PermissionEntity> allPermissions = new ArrayList<>();
        allPermissions.addAll(userPermissions);
        allPermissions.addAll(groupPermissions);

        log.debug("Total de permissões para avaliar: {}", allPermissions.size());

        // Avaliar cada permissão contra o contexto
        for (PermissionEntity permission : allPermissions) {
            log.debug("Avaliando permissão ID: {}", permission.getId());
            log.debug("  Tenant: {} (contexto: {})", permission.getTenantId(), tenantId);
            log.debug("  Company: {} (contexto: {})", permission.getCompanyId(), companyId);
            log.debug("  Project: {} (contexto: {})", permission.getProjectId(), projectId);

            boolean matches = matchesContext(permission, tenantId, companyId, projectId);
            log.debug("  Correspondência: {}", matches);

            if (matches) {
                log.info("PERMISSÃO CONCEDIDA: Permissão ID {} corresponde ao contexto",
                    permission.getId());
                return true;
            }
        }

        log.warn("PERMISSÃO NEGADA: Nenhuma permissão correspondente encontrada");
        log.debug("=== FIM DA AVALIAÇÃO DE PERMISSÃO ===");
        return false;
    }

    private boolean matchesContext(
            PermissionEntity permission,
            String tenantId,
            String companyId,
            String projectId) {

        // Permissão global (todos nulos)
        if (permission.getTenantId() == null &&
            permission.getCompanyId() == null &&
            permission.getProjectId() == null) {
            log.debug("    -> Permissão GLOBAL");
            return true;
        }

        // Verificar tenant
        if (permission.getTenantId() != null) {
            if (!permission.getTenantId().equals(tenantId)) {
                log.debug("    -> Tenant não corresponde");
                return false;
            }
        }

        // Verificar company
        if (permission.getCompanyId() != null) {
            if (!permission.getCompanyId().equals(companyId)) {
                log.debug("    -> Company não corresponde");
                return false;
            }
        }

        // Verificar project
        if (permission.getProjectId() != null) {
            if (!permission.getProjectId().equals(projectId)) {
                log.debug("    -> Project não corresponde");
                return false;
            }
        }

        log.debug("    -> Contexto corresponde!");
        return true;
    }
}
```

---

### 2.3 Inspecionar Token JWT

#### Ferramentas Online

Use ferramentas para decodificar e validar tokens JWT:

- **https://jwt.io/** - Decodificador e debugger oficial
- **https://token.dev/** - Alternativa com recursos adicionais
- **https://jwt.ms/** - Microsoft JWT decoder

**IMPORTANTE**: Nunca cole tokens de produção em sites públicos!

#### Endpoint de Debug (Apenas Desenvolvimento)

```java
@RestController
@RequestMapping("/api/debug")
@Profile("dev") // APENAS EM DESENVOLVIMENTO
public class TokenDebugController {

    @Autowired
    private ArchbaseJwtService jwtService;

    @GetMapping("/token-info")
    public Map<String, Object> getTokenInfo(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");

        Map<String, Object> info = new HashMap<>();

        try {
            // Claims básicos
            info.put("username", jwtService.extractUsername(token));
            info.put("expiration", jwtService.extractExpiration(token));
            info.put("issuedAt", jwtService.extractIssuedAt(token));
            info.put("isExpired", jwtService.isTokenExpired(token));

            // Claims customizados
            Claims claims = jwtService.extractAllClaims(token);
            info.put("userId", claims.get("userId"));
            info.put("tenantId", claims.get("tenantId"));
            info.put("companyId", claims.get("companyId"));
            info.put("roles", claims.get("roles"));

            // Informações de tempo
            Date now = new Date();
            Date expiration = jwtService.extractExpiration(token);
            long timeToExpire = expiration.getTime() - now.getTime();

            info.put("currentTime", now);
            info.put("minutesToExpire", timeToExpire / 1000 / 60);
            info.put("hoursToExpire", timeToExpire / 1000 / 60 / 60);

        } catch (Exception e) {
            info.put("error", e.getMessage());
            info.put("errorType", e.getClass().getSimpleName());
        }

        return info;
    }

    @GetMapping("/validate-token")
    public Map<String, Object> validateToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String username) {

        String token = authHeader.replace("Bearer ", "");

        Map<String, Object> validation = new HashMap<>();
        validation.put("isValid", jwtService.validateToken(token, username));
        validation.put("username", jwtService.extractUsername(token));
        validation.put("isExpired", jwtService.isTokenExpired(token));

        return validation;
    }
}
```

#### Script de Shell para Decodificar JWT

```bash
#!/bin/bash
# decode-jwt.sh

TOKEN=$1

if [ -z "$TOKEN" ]; then
    echo "Uso: ./decode-jwt.sh <token>"
    exit 1
fi

# Remover Bearer se presente
TOKEN=${TOKEN#"Bearer "}

# Decodificar header
HEADER=$(echo $TOKEN | cut -d '.' -f 1)
echo "=== HEADER ==="
echo $HEADER | base64 -d 2>/dev/null | jq .

# Decodificar payload
PAYLOAD=$(echo $TOKEN | cut -d '.' -f 2)
echo -e "\n=== PAYLOAD ==="
echo $PAYLOAD | base64 -d 2>/dev/null | jq .

# Mostrar signature (não decodificada)
SIGNATURE=$(echo $TOKEN | cut -d '.' -f 3)
echo -e "\n=== SIGNATURE ==="
echo $SIGNATURE
```

---

## 3. Monitoramento

### 3.1 Métricas Recomendadas

Implemente métricas usando Spring Boot Actuator e Micrometer:

```java
@Component
public class SecurityMetrics {

    private final Counter authenticationAttempts;
    private final Counter authenticationSuccesses;
    private final Counter authenticationFailures;
    private final Counter permissionChecks;
    private final Counter permissionGrants;
    private final Counter permissionDenials;
    private final Timer authenticationTime;
    private final Timer permissionCheckTime;
    private final Gauge activeTokens;

    public SecurityMetrics(MeterRegistry registry, AccessTokenRepository tokenRepository) {

        // Contadores de autenticação
        this.authenticationAttempts = Counter.builder("security.authentication.attempts")
            .description("Total de tentativas de autenticação")
            .tag("type", "login")
            .register(registry);

        this.authenticationSuccesses = Counter.builder("security.authentication.successes")
            .description("Autenticações bem-sucedidas")
            .tag("type", "login")
            .register(registry);

        this.authenticationFailures = Counter.builder("security.authentication.failures")
            .description("Falhas de autenticação")
            .tag("type", "login")
            .register(registry);

        // Contadores de permissão
        this.permissionChecks = Counter.builder("security.permission.checks")
            .description("Total de verificações de permissão")
            .register(registry);

        this.permissionGrants = Counter.builder("security.permission.grants")
            .description("Permissões concedidas")
            .register(registry);

        this.permissionDenials = Counter.builder("security.permission.denials")
            .description("Permissões negadas")
            .register(registry);

        // Timers de performance
        this.authenticationTime = Timer.builder("security.authentication.time")
            .description("Tempo de processamento de autenticação")
            .register(registry);

        this.permissionCheckTime = Timer.builder("security.permission.check.time")
            .description("Tempo de verificação de permissão")
            .register(registry);

        // Gauge de tokens ativos
        this.activeTokens = Gauge.builder("security.tokens.active", tokenRepository, repo -> {
            return repo.countActiveTokens();
        })
        .description("Número de tokens ativos no sistema")
        .register(registry);
    }

    // Métodos de registro
    public void recordAuthenticationAttempt() {
        authenticationAttempts.increment();
    }

    public void recordAuthenticationSuccess() {
        authenticationSuccesses.increment();
    }

    public void recordAuthenticationFailure() {
        authenticationFailures.increment();
    }

    public void recordPermissionCheck() {
        permissionChecks.increment();
    }

    public void recordPermissionGrant() {
        permissionGrants.increment();
    }

    public void recordPermissionDenial() {
        permissionDenials.increment();
    }

    public Timer.Sample startAuthenticationTimer() {
        return Timer.start(registry);
    }

    public void stopAuthenticationTimer(Timer.Sample sample) {
        sample.stop(authenticationTime);
    }

    public Timer.Sample startPermissionCheckTimer() {
        return Timer.start(registry);
    }

    public void stopPermissionCheckTimer(Timer.Sample sample) {
        sample.stop(permissionCheckTime);
    }
}
```

**Integração com Serviço de Autenticação**:

```java
@Service
public class ArchbaseAuthenticationService {

    @Autowired
    private SecurityMetrics metrics;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        metrics.recordAuthenticationAttempt();
        Timer.Sample sample = metrics.startAuthenticationTimer();

        try {
            // Lógica de autenticação
            AuthenticationResponse response = performAuthentication(request);

            metrics.recordAuthenticationSuccess();
            return response;

        } catch (AuthenticationException e) {
            metrics.recordAuthenticationFailure();
            throw e;

        } finally {
            metrics.stopAuthenticationTimer(sample);
        }
    }
}
```

---

### 3.2 Alertas

Configure alertas para detectar problemas de segurança proativamente:

#### Alertas Críticos (Resposta Imediata)

1. **Taxa de falhas de autenticação elevada**
   - **Métrica**: `rate(security_authentication_failures_total[5m]) > 10`
   - **Condição**: Mais de 10 falhas por minuto em 5 minutos
   - **Ação**: Possível ataque de força bruta

2. **Spike em negações de permissão**
   - **Métrica**: `rate(security_permission_denials_total[5m]) > 50`
   - **Condição**: Mais de 50 negações por minuto em 5 minutos
   - **Ação**: Possível tentativa de privilege escalation

3. **Tokens expirados não limpos**
   - **Métrica**: `security_tokens_expired_not_cleaned > 10000`
   - **Condição**: Mais de 10.000 tokens expirados no banco
   - **Ação**: Job de limpeza não está funcionando

#### Alertas de Atenção (Monitoramento)

4. **Tempo de autenticação elevado**
   - **Métrica**: `histogram_quantile(0.95, security_authentication_time) > 2`
   - **Condição**: P95 acima de 2 segundos
   - **Ação**: Performance degradada

5. **Usuários com muitos tokens ativos**
   - **Métrica**: `security_tokens_per_user > 20`
   - **Condição**: Usuário com mais de 20 tokens ativos
   - **Ação**: Possível vazamento de tokens

6. **Taxa de sucesso de autenticação baixa**
   - **Métrica**: `security_authentication_successes / security_authentication_attempts < 0.8`
   - **Condição**: Menos de 80% de sucesso
   - **Ação**: Problema de integração ou UX

#### Exemplo de Configuração Prometheus

```yaml
# prometheus-alerts.yml

groups:
  - name: security
    interval: 30s
    rules:
      # Alerta crítico - Ataque de força bruta
      - alert: HighAuthenticationFailureRate
        expr: rate(security_authentication_failures_total[5m]) > 10
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Taxa elevada de falhas de autenticação"
          description: "{{ $value }} falhas por segundo nos últimos 5 minutos"

      # Alerta crítico - Tentativa de escalação de privilégios
      - alert: HighPermissionDenialRate
        expr: rate(security_permission_denials_total[5m]) > 50
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Spike em negações de permissão"
          description: "{{ $value }} negações por segundo nos últimos 5 minutos"

      # Alerta atenção - Performance degradada
      - alert: SlowAuthentication
        expr: histogram_quantile(0.95, rate(security_authentication_time_bucket[5m])) > 2
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Autenticação lenta"
          description: "P95 de tempo de autenticação: {{ $value }}s"

      # Alerta atenção - Limpeza de tokens não funcionando
      - alert: ExpiredTokensNotCleaned
        expr: security_tokens_expired_count > 10000
        for: 1h
        labels:
          severity: warning
        annotations:
          summary: "Tokens expirados acumulando"
          description: "{{ $value }} tokens expirados no banco de dados"
```

#### Dashboard Grafana

```json
{
  "dashboard": {
    "title": "Archbase Security Monitoring",
    "panels": [
      {
        "title": "Taxa de Autenticação",
        "targets": [
          {
            "expr": "rate(security_authentication_attempts_total[5m])",
            "legendFormat": "Tentativas"
          },
          {
            "expr": "rate(security_authentication_successes_total[5m])",
            "legendFormat": "Sucessos"
          },
          {
            "expr": "rate(security_authentication_failures_total[5m])",
            "legendFormat": "Falhas"
          }
        ]
      },
      {
        "title": "Verificações de Permissão",
        "targets": [
          {
            "expr": "rate(security_permission_checks_total[5m])",
            "legendFormat": "Verificações"
          },
          {
            "expr": "rate(security_permission_grants_total[5m])",
            "legendFormat": "Concedidas"
          },
          {
            "expr": "rate(security_permission_denials_total[5m])",
            "legendFormat": "Negadas"
          }
        ]
      },
      {
        "title": "Tokens Ativos",
        "targets": [
          {
            "expr": "security_tokens_active",
            "legendFormat": "Tokens Ativos"
          }
        ]
      },
      {
        "title": "Performance - P95",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(security_authentication_time_bucket[5m]))",
            "legendFormat": "Autenticação"
          },
          {
            "expr": "histogram_quantile(0.95, rate(security_permission_check_time_bucket[5m]))",
            "legendFormat": "Verificação Permissão"
          }
        ]
      }
    ]
  }
}
```

---

## 4. Checklist de Troubleshooting

Use este checklist quando encontrar problemas:

### Problemas de Autenticação

- [ ] Verificar se JWT secret está configurado corretamente
- [ ] Confirmar que os tempos de expiração estão em milissegundos
- [ ] Validar que o token não está expirado
- [ ] Verificar sincronização de relógio do servidor
- [ ] Conferir se o usuário existe no banco de dados
- [ ] Validar que a conta não está desativada ou bloqueada
- [ ] Verificar logs de erro detalhados
- [ ] Testar decodificação do token em jwt.io

### Problemas de Autorização

- [ ] Confirmar que o usuário tem a permissão necessária
- [ ] Verificar se o contexto (tenant/company/project) está correto
- [ ] Validar que grupos do usuário têm as permissões
- [ ] Conferir se horários de acesso estão configurados
- [ ] Verificar se cache de permissões está atualizado
- [ ] Validar que recursos e ações estão cadastrados
- [ ] Confirmar que anotação @HasPermission está correta
- [ ] Revisar logs de avaliação de permissões

### Problemas de Multi-Tenancy

- [ ] Verificar que multi-tenancy está habilitado
- [ ] Confirmar que tenant context está sendo setado
- [ ] Validar propagação de contexto em operações assíncronas
- [ ] Verificar configuração de TaskDecorator
- [ ] Confirmar que entidades herdam de TenantPersistenceEntityBase
- [ ] Validar que filtros JPA estão aplicados
- [ ] Revisar logs de contexto de tenant

### Problemas de Performance

- [ ] Verificar índices no banco de dados
- [ ] Validar que cache de permissões está habilitado
- [ ] Conferir pool de conexões do banco
- [ ] Revisar queries N+1
- [ ] Verificar se há locks no banco de dados
- [ ] Monitorar uso de memória e CPU
- [ ] Analisar métricas de tempo de resposta

---

## 5. Recursos Adicionais

### Documentação Relacionada

- [Configuração](configuration.md) - Propriedades e configurações
- [Sistema de Autenticação](../architecture/authentication-system.md) - Fluxos de autenticação
- [Sistema de Permissões](../architecture/permissions-system.md) - Avaliação de permissões
- [Multi-Tenancy](../architecture/multi-tenancy.md) - Propagação de contexto

### Ferramentas Úteis

- **JWT.io** - https://jwt.io/ - Decodificador de tokens
- **Spring Boot Actuator** - Monitoramento e métricas
- **Prometheus** - Coleta de métricas
- **Grafana** - Visualização de dashboards

### Suporte

Para questões adicionais ou suporte técnico:
- Documentação principal: [readme-security.md](../../readme-security.md)
- Repositório: [Archbase Framework](https://github.com/edsonmartins/archbase-app-framework)

---

[← Anterior: Melhores Práticas](best-practices.md) | [Voltar ao Índice](../README.md)
