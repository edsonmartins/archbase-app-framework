[← Voltar ao Índice](../README.md) | [Próximo: Anotações e Uso →](annotations-and-usage.md)

---

# Configuração do Módulo archbase-security

> **Em revisão.** As propriedades `archbase.security.method.enabled` e `archbase.security.permission.cache.*`
> citadas abaixo **não existem** no código (não há cache de permissões) e não têm efeito. A referência
> completa e conferida das propriedades está no [`CLAUDE.md`](../../../CLAUDE.md#configuration-properties)
> e em [`deployment/security-hardening.md`](../../../deployment/security-hardening.md) até a reescrita deste guia.

## Introdução

Este documento descreve todas as propriedades de configuração disponíveis no módulo `archbase-security`, incluindo segurança, JWT, cache de permissões e multi-tenancy.

---

## 1. Configuração de Segurança

### Propriedades Básicas

```properties
# ========================================
# MÓDULO DE SEGURANÇA
# ========================================

# Habilita o módulo de segurança
archbase.security.enabled=true

# Habilita segurança no nível de método (@HasPermission, etc)
archbase.security.method.enabled=true
```

**Descrição**:
- **`archbase.security.enabled`**: Ativa/desativa todo o módulo de segurança
  - Padrão: `true`
  - Se `false`: Segurança é completamente desabilitada (apenas para desenvolvimento)

- **`archbase.security.method.enabled`**: Habilita anotações de segurança em métodos
  - Padrão: `true`
  - Se `false`: `@HasPermission` e outras anotações são ignoradas

**Uso Típico**:
- **Produção**: Ambas `true`
- **Testes de Integração**: Pode desabilitar para testes específicos
- **Desenvolvimento**: Manter `true` para evitar surpresas em produção

---

## 2. Configuração JWT

### Propriedades de Tokens

```properties
# ========================================
# JWT (JSON Web Tokens)
# ========================================

# As três são OBRIGATÓRIAS e não têm padrão: sem qualquer uma delas a aplicação não sobe.

# Chave de assinatura (HS256): Base64 padrão que decodifique para >= 32 bytes
archbase.security.jwt.secret-key=${JWT_SECRET}

# Expiração do access token, em milissegundos (ex.: 1 hora)
archbase.security.jwt.token-expiration=3600000

# Expiração do refresh token, em milissegundos (ex.: 7 dias)
archbase.security.jwt.refresh-expiration=604800000
```

### secret-key

**Descrição**: Chave secreta usada para assinar tokens JWT (HMAC SHA-256).

**Requisitos**:
- É **decodificada como Base64 padrão** (`Decoders.BASE64`): o alfabeto URL-safe (`-`, `_`) e texto livre
  são recusados
- Depois de decodificada, mínimo de 32 bytes (HS256) — cerca de 44 caracteres em Base64
- Deve ser aleatória e única para cada ambiente
- **NUNCA** commitar a chave de produção no código fonte

**Geração de Chave Segura**:

```bash
# Usando OpenSSL (48 bytes aleatórios)
openssl rand -base64 48

# Usando Python (Base64 padrão; NÃO use secrets.token_urlsafe)
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(48)).decode())"
```

**Boas Práticas**:
```properties
# ❌ Ruim (hardcoded)
archbase.security.jwt.secret-key=mypassword123

# ✓ Bom (variável de ambiente)
archbase.security.jwt.secret-key=${JWT_SECRET}

# ✓ Dev: fallback só no profile de desenvolvimento, e também em Base64 válido
# application-dev.properties
archbase.security.jwt.secret-key=${JWT_SECRET:ZGV2LW9ubHkta2V5LW5vdC1mb3ItcHJvZHVjdGlvbi11c2UtMTIzNDU2Nzg5MA==}
```

### token-expiration

**Descrição**: Duração do access token em milissegundos.

**Recomendações por Ambiente**:

| Ambiente | Duração Recomendada | Milissegundos | Motivo |
|----------|---------------------|---------------|--------|
| **Desenvolvimento** | 24 horas | 86400000 | Conveniência |
| **Homologação** | 4 horas | 14400000 | Simular produção |
| **Produção (Baixa Segurança)** | 4 horas | 14400000 | Equilíbrio |
| **Produção (Média Segurança)** | 1 hora | 3600000 | Recomendado |
| **Produção (Alta Segurança)** | 15-30 min | 900000-1800000 | Sistemas financeiros |

**Conversão de Tempo**:
```
15 minutos = 900000 ms
30 minutos = 1800000 ms
1 hora     = 3600000 ms
4 horas    = 14400000 ms
24 horas   = 86400000 ms
```

### refresh-expiration

**Descrição**: Duração do refresh token em milissegundos.

**Recomendações**:
- Deve ser **significativamente maior** que `token-expiration`
- Típico: 7-30 dias

| Duração | Milissegundos | Uso Típico |
|---------|---------------|------------|
| 7 dias | 604800000 | Padrão recomendado |
| 14 dias | 1209600000 | Aplicações móveis |
| 30 dias | 2592000000 | Conveniência máxima |

**Exemplo de Configuração Equilibrada**:
```properties
# Access token: 1 hora
archbase.security.jwt.token-expiration=3600000

# Refresh token: 7 dias
archbase.security.jwt.refresh-expiration=604800000
```

---

## 3. Cache de Permissões

### Propriedades de Cache

```properties
# ========================================
# CACHE DE PERMISSÕES
# ========================================

# Habilita cache de permissões para melhor performance
archbase.security.permission.cache.enabled=true

# TTL do cache em segundos (opcional)
archbase.security.permission.cache.ttl=3600

# Tamanho máximo do cache (opcional)
archbase.security.permission.cache.max-size=1000
```

### permission.cache.enabled

**Descrição**: Ativa/desativa cache de permissões.

**Comportamento**:
- `true`: Permissões consultadas são armazenadas em cache
- `false`: Cada verificação de permissão consulta o banco de dados

**Quando Habilitar**:
- ✓ **Produção** (sempre recomendado)
- ✓ **Homologação** (simular produção)
- ✗ **Testes** (pode causar inconsistências em testes)

### permission.cache.ttl

**Descrição**: Tempo de vida do cache em **segundos**.

**Valores Recomendados**:

| TTL | Segundos | Uso |
|-----|----------|-----|
| Curto | 300 (5 min) | Permissões mudam frequentemente |
| Médio | 1800 (30 min) | Equilíbrio |
| Longo | 3600 (1 hora) | Permissões estáveis |

**Importante**: Cache é **invalidado automaticamente** quando permissões são modificadas.

### permission.cache.max-size

**Descrição**: Número máximo de entradas no cache.

**Cálculo de Tamanho**:
```
max-size ≈ (número de usuários ativos) × (média de recursos por usuário)

Exemplo:
- 100 usuários ativos
- Cada usuário acessa 5 recursos diferentes
- max-size = 100 × 5 = 500
```

**Recomendações**:
- Pequena aplicação: 500-1000
- Média aplicação: 1000-5000
- Grande aplicação: 5000-10000

---

## 4. Multi-Tenancy

### Propriedades de Multi-Tenancy

```properties
# ========================================
# MULTI-TENANCY
# ========================================

# Habilita suporte multi-tenancy
archbase.multitenancy.enabled=true

# Pacotes para scan de entidades multi-tenant
archbase.multitenancy.scan-packages=br.com.yourcompany.domain
```

### multitenancy.enabled

**Descrição**: Ativa/desativa suporte multi-tenancy.

**Efeitos**:
- `true`:
  - Entidades com `tenantId` são filtradas automaticamente
  - `ArchbaseTenantContext` é utilizado
  - Isolamento de dados por tenant
- `false`:
  - Todos os usuários veem todos os dados
  - `tenantId` é ignorado

**Quando Usar**:
- ✓ **SaaS multi-inquilino**: `true`
- ✓ **Aplicações com múltiplas organizações**: `true`
- ✗ **Aplicação single-tenant**: `false`

### multitenancy.scan-packages

**Descrição**: Pacotes Java a serem escaneados para entidades multi-tenant.

**Formato**: Lista separada por vírgula.

**Exemplo**:
```properties
archbase.multitenancy.scan-packages=\
  br.com.mycompany.domain,\
  br.com.mycompany.security,\
  br.com.mycompany.reporting
```

**Importante**: Deve incluir **todos** os pacotes que contêm entidades que estendem `TenantPersistenceEntityBase`.

---

## 5. Exemplo de Configuração Completa

### Ambiente de Desenvolvimento

```properties
# application-dev.properties

# ========================================
# SEGURANÇA
# ========================================
archbase.security.enabled=true
archbase.security.method.enabled=true

# ========================================
# JWT
# ========================================
# Chave fixa de dev (NÃO usar em produção) — Base64 válido de 46 bytes
archbase.security.jwt.secret-key=ZGV2LW9ubHkta2V5LW5vdC1mb3ItcHJvZHVjdGlvbi11c2UtMTIzNDU2Nzg5MA==
archbase.security.jwt.token-expiration=86400000
archbase.security.jwt.refresh-expiration=604800000

# ========================================
# CACHE
# ========================================
# Cache desabilitado para facilitar debugging
archbase.security.permission.cache.enabled=false

# ========================================
# MULTI-TENANCY
# ========================================
archbase.multitenancy.enabled=true
archbase.multitenancy.scan-packages=br.com.mycompany.domain

# ========================================
# DATABASE
# ========================================
spring.datasource.url=jdbc:postgresql://localhost:5432/myapp_dev
spring.datasource.username=dev_user
spring.datasource.password=dev_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# ========================================
# LOGGING
# ========================================
logging.level.br.com.archbase.security=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Ambiente de Produção

```properties
# application-prod.properties

# ========================================
# SEGURANÇA
# ========================================
archbase.security.enabled=true
archbase.security.method.enabled=true

# ========================================
# JWT
# ========================================
# Chave vem de variável de ambiente (OBRIGATÓRIO)
archbase.security.jwt.secret-key=${JWT_SECRET}
# Access token: 1 hora
archbase.security.jwt.token-expiration=3600000
# Refresh token: 7 dias
archbase.security.jwt.refresh-expiration=604800000

# ========================================
# CACHE
# ========================================
archbase.security.permission.cache.enabled=true
archbase.security.permission.cache.ttl=1800
archbase.security.permission.cache.max-size=5000

# ========================================
# MULTI-TENANCY
# ========================================
archbase.multitenancy.enabled=true
archbase.multitenancy.scan-packages=br.com.mycompany.domain,br.com.mycompany.security

# ========================================
# DATABASE
# ========================================
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# ========================================
# LOGGING
# ========================================
logging.level.br.com.archbase.security=INFO
logging.level.org.springframework.security=WARN

# ========================================
# PERFORMANCE
# ========================================
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

---

## 6. Variáveis de Ambiente

### Configuração via Variáveis de Ambiente

**Recomendado para Produção** (12-Factor App):

```bash
# JWT
export JWT_SECRET="$(openssl rand -base64 48)"   # gere uma vez e guarde no gerenciador de segredos

# Database
export DATABASE_URL="jdbc:postgresql://prod-db.example.com:5432/myapp"
export DATABASE_USER="myapp_user"
export DATABASE_PASSWORD="strong_password_here"
```

**No application.properties**:
```properties
archbase.security.jwt.secret-key=${JWT_SECRET}
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
```

### Docker / Kubernetes

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - DATABASE_URL=jdbc:postgresql://db:5432/myapp
      - DATABASE_USER=myapp
      - DATABASE_PASSWORD_FILE=/run/secrets/db_password
```

---

## 7. Verificação de Configuração

### Checklist de Produção

- [ ] `archbase.security.jwt.secret-key` vem de variável de ambiente
- [ ] Chave JWT é Base64 padrão e decodifica para pelo menos 32 bytes
- [ ] `token-expiration` é ≤ 4 horas (14400000 ms)
- [ ] `refresh-expiration` é 7-30 dias
- [ ] `spring.jpa.hibernate.ddl-auto=validate` (nunca `update` em produção)
- [ ] `spring.jpa.show-sql=false`
- [ ] Logging em nível INFO ou WARN
- [ ] Multi-tenancy configurado corretamente se aplicável

---

**Ver também**:
- [Anotações e Uso](annotations-and-usage.md) - Como usar @HasPermission
- [Melhores Práticas](best-practices.md) - Recomendações de configuração
- [Troubleshooting](troubleshooting.md) - Problemas comuns de configuração

---

[← Voltar ao Índice](../README.md) | [Próximo: Anotações e Uso →](annotations-and-usage.md)
