# Planejamento de Integração Keycloak ↔ Archbase
## Análise de Estratégias de Sincronização

---

## Contexto

Este documento apresenta a análise das diferentes estratégias possíveis para integrar o **Keycloak** (autenticação) com o **Archbase Framework** (autorização), considerando a sincronização de usuários, groups e roles.

### Objetivo
- Autenticação centralizada no Keycloak
- Autorização gerenciada no Archbase
- Sincronização automática de usuários e estrutura organizacional
- Gestão manual de permissões no Archbase

---

## 1. Event Listeners (Keycloak)

### Descrição
O Keycloak possui um sistema de eventos que dispara notificações quando ações ocorrem. Você pode criar um **Event Listener Provider** customizado que reage a essas mudanças.

### Como Funciona
- Implementar um SPI (Service Provider Interface) customizado
- Deployar como JAR no diretório de providers do Keycloak
- Configurar para chamar API do Archbase ou webhook
- Processar eventos em tempo real

### Eventos Disponíveis

#### Admin Events
- Criação/edição/exclusão de usuários
- Alteração de roles
- Modificação de groups
- Atribuição/remoção de roles/groups

#### User Events
- Login
- Logout
- Registro
- Alteração de senha
- Falhas de autenticação

### Implementação Conceitual

```
┌─────────────────────────────────────────┐
│         KEYCLOAK SERVER                 │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   Admin altera role/group        │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   Event Listener (Custom SPI)    │  │
│  │   - Captura evento               │  │
│  │   - Extrai dados                 │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   HTTP Client                    │  │
│  │   - Chama API Archbase           │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│         ARCHBASE APP                    │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   REST Endpoint                  │  │
│  │   - Recebe notificação           │  │
│  │   - Sincroniza dados             │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Vantagens
- ✅ Sincronização em tempo real
- ✅ Não depende de login do usuário
- ✅ Captura todas as alterações administrativas
- ✅ Keycloak como fonte única de verdade
- ✅ Pode sincronizar usuários que nunca fizeram login
- ✅ Garante consistência imediata

### Desvantagens
- ❌ Requer desenvolvimento e deploy de provider customizado no Keycloak
- ❌ Acopla a infraestrutura (Keycloak precisa conseguir chamar o Archbase)
- ❌ Mais complexo de debugar e manter
- ❌ Necessita versionamento e compatibilidade com upgrades do Keycloak
- ❌ Requer acesso e permissões administrativas no servidor Keycloak
- ❌ Potencial ponto de falha se Archbase estiver indisponível

### Quando Usar
- ✓ Quando você tem controle total sobre a infraestrutura do Keycloak
- ✓ Quando precisa de sincronização imediata de todas as mudanças
- ✓ Quando há muitas alterações administrativas frequentes
- ✓ Em ambientes corporativos com equipe DevOps dedicada

### Complexidade
**Alta** - Requer conhecimento profundo do Keycloak SPI e infraestrutura

---

## 2. Just-in-Time (JIT) Provisioning

### Descrição
Usuários são criados/atualizados no Archbase **no momento do login**, baseado nas informações do token JWT recebido do Keycloak.

### Como Funciona
- Interceptar requisições autenticadas (Filter/Interceptor)
- Extrair dados do token JWT (claims: sub, email, roles, groups)
- Criar ou atualizar UserEntity no Archbase
- Criar GroupEntity automaticamente se não existirem
- Vincular usuário aos groups apropriados

### Implementação Conceitual

```
┌─────────────────────────────────────────┐
│         KEYCLOAK                        │
│  - Usuário autentica                    │
│  - Gera token JWT                       │
└─────────────────────────────────────────┘
                 │
                 ↓
        ┌────────────────┐
        │   Token JWT    │
        │  {             │
        │   sub: "123",  │
        │   roles: [...],│
        │   groups: [...] │
        │  }             │
        └────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│         ARCHBASE APP                    │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   JWT Filter/Interceptor         │  │
│  │   - Valida token                 │  │
│  │   - Extrai claims                │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   User Sync Service              │  │
│  │   - Busca UserEntity por         │  │
│  │     keycloakId (claim "sub")     │  │
│  │   - Se não existe, cria          │  │
│  │   - Atualiza dados (email, etc)  │  │
│  │   - Sincroniza groups            │  │
│  │   - Cache de 5-15 min            │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   Database                       │  │
│  │   - UserEntity criado/atualizado │  │
│  │   - Groups criados se necessário │  │
│  │   - Vínculos atualizados         │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Fluxo Detalhado

1. **Primeira requisição do usuário**
   - Token JWT validado
   - Claims extraídos
   - UserEntity criado com keycloakId = claim "sub"
   - GroupEntity criados para cada role/group no token
   - Vínculos UserGroupEntity criados
   - Dados salvos em cache (5-15 min)

2. **Requisições subsequentes (dentro do período de cache)**
   - Token validado
   - Cache hit → sincronização pulada
   - Performance otimizada

3. **Requisições após expiração do cache**
   - Token validado
   - Claims extraídos novamente
   - Comparação com estado atual
   - Atualização incremental se necessário
   - Cache renovado

### Vantagens
- ✅ Simples de implementar
- ✅ Não requer modificações no Keycloak
- ✅ Sem acoplamento de infraestrutura
- ✅ Sincroniza apenas usuários ativos (que fazem login)
- ✅ Sempre reflete o estado atual do token
- ✅ Fácil de debugar e manter
- ✅ Baixa complexidade operacional
- ✅ Funciona mesmo sem acesso administrativo ao Keycloak

### Desvantagens
- ❌ Usuários só são criados após primeiro login
- ❌ Delay na sincronização (só atualiza no próximo login)
- ❌ Não captura mudanças de usuários inativos
- ❌ Pode gerar overhead em cada requisição (mitigável com cache)
- ❌ Usuários que nunca logaram não existem no Archbase

### Estratégias de Cache

#### Opção 1: Cache em Memória (Simples)
```
Key: keycloakId
Value: Timestamp da última sincronização
TTL: 5-15 minutos
```

#### Opção 2: Campo no Banco (Persistente)
```
UserEntity.lastSyncedAt
- Atualizado a cada sincronização
- Verificado antes de sincronizar novamente
```

#### Opção 3: Cache Distribuído (Escalável)
```
Redis/Hazelcast
- Útil em ambientes com múltiplas instâncias
- Compartilha estado entre servidores
```

### Quando Usar
- ✓ Quando você não tem controle sobre o Keycloak
- ✓ Para aplicações com muitos usuários cadastrados, mas poucos ativos
- ✓ Quando simplicidade e manutenibilidade são prioridades
- ✓ **Recomendado para maioria dos casos**
- ✓ Startups e times pequenos
- ✓ Ambiente SaaS onde Keycloak é gerenciado externamente

### Complexidade
**Baixa** - Implementação direta em Spring Security

---

## 3. Scheduled Sync (Sincronização Agendada)

### Descrição
Um job agendado consulta periodicamente a **Keycloak Admin API** e sincroniza todos os usuários, roles e groups do realm.

### Como Funciona
- Usar Keycloak Admin Client (biblioteca oficial Java)
- Agendar job com Spring @Scheduled, Quartz, ou similar
- Buscar todos os usuários e suas atribuições
- Comparar com estado atual no Archbase
- Criar/atualizar/desativar conforme necessário

### Implementação Conceitual

```
┌─────────────────────────────────────────┐
│         ARCHBASE APP                    │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   Scheduled Job                  │  │
│  │   - Trigger: Cron (ex: 2h AM)    │  │
│  │   - Executa periodicamente       │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   Keycloak Admin Client          │  │
│  │   - Conecta com credenciais      │  │
│  │   - Lista todos usuários         │  │
│  │   - Lista roles e groups         │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│         KEYCLOAK API                    │
│  GET /admin/realms/{realm}/users        │
│  GET /admin/realms/{realm}/roles        │
│  GET /admin/realms/{realm}/groups       │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   Sync Service                   │  │
│  │   - Compara com DB atual         │  │
│  │   - Cria novos usuários          │  │
│  │   - Atualiza existentes          │  │
│  │   - Marca removidos como inativos│  │
│  │   - Atualiza vínculos            │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   Database                       │  │
│  │   - Estado sincronizado          │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Fluxo do Job

1. **Inicialização**
   - Job disparado pelo scheduler
   - Autentica com Keycloak Admin API
   - Obtém token de acesso administrativo

2. **Busca de Dados**
   - Lista todos os usuários do realm (paginado se necessário)
   - Para cada usuário, busca:
     - Realm roles atribuídas
     - Client roles atribuídas
     - Groups aos quais pertence

3. **Comparação e Sincronização**
   - Para cada usuário do Keycloak:
     - Busca correspondente no Archbase (por keycloakId)
     - Se não existe → cria UserEntity
     - Se existe → atualiza dados (email, etc)
     - Sincroniza groups
   
4. **Limpeza (Opcional)**
   - Usuários no Archbase que não existem mais no Keycloak
     - Opção 1: Marcar como inativos
     - Opção 2: Deletar (não recomendado)
     - Opção 3: Ignorar

5. **Log e Auditoria**
   - Registrar quantos usuários foram criados/atualizados
   - Alertar sobre erros
   - Métricas de performance

### Configurações Recomendadas

```yaml
keycloak:
  admin:
    server-url: https://keycloak.example.com
    realm: master
    client-id: admin-cli
    client-secret: ${KEYCLOAK_ADMIN_SECRET}
    
sync:
  schedule:
    cron: "0 0 2 * * ?" # 2h da manhã, diariamente
  batch-size: 100 # Usuários por lote
  enabled: true
```

### Vantagens
- ✅ Sincronização completa de todos os usuários
- ✅ Não depende de login dos usuários
- ✅ Pode ser executado em horários de baixo tráfego
- ✅ Fácil de monitorar e logar
- ✅ Captura usuários inativos
- ✅ Permite limpeza de usuários removidos
- ✅ Útil para relatórios e auditoria

### Desvantagens
- ❌ Não é tempo real (delay baseado no intervalo)
- ❌ Pode sobrecarregar o Keycloak com muitos usuários
- ❌ Requer credenciais administrativas do Keycloak
- ❌ Consumo de recursos em sincronizações grandes
- ❌ Possível inconsistência entre sincronizações
- ❌ Complexidade de paginação com muitos usuários (>10k)

### Otimizações

#### Sincronização Incremental
```
- Armazenar timestamp da última sincronização
- Consultar apenas usuários modificados desde então
- Usar Keycloak Events API (se disponível)
```

#### Paginação Eficiente
```
- Processar em lotes (batch size: 50-100)
- Evitar carregar todos usuários em memória
- Usar cursores se API suportar
```

#### Paralelização
```
- Processar múltiplos lotes em paralelo
- Cuidado com rate limiting do Keycloak
- Thread pool configurável
```

### Quando Usar
- ✓ Quando você precisa de sincronização completa periódica
- ✓ Para relatórios e auditoria
- ✓ Como complemento ao JIT (sincroniza usuários inativos)
- ✓ Em ambientes com poucos usuários (<10k)
- ✓ Quando há necessidade de limpeza/desativação de usuários

### Complexidade
**Média** - Requer gestão de credenciais e tratamento de volumes

---

## 4. Keycloak User Storage SPI (Federation)

### Descrição
Implementar um **User Storage Provider** que faz o Keycloak buscar usuários diretamente do Archbase (inversão da direção de integração).

### Como Funciona
- Criar SPI customizado que implementa UserStorageProvider
- Deploy no Keycloak
- Keycloak consulta o Archbase para validação e dados de usuários
- Archbase vira fonte de verdade para alguns atributos

### Implementação Conceitual

```
┌─────────────────────────────────────────┐
│         KEYCLOAK                        │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   User tries to login            │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   User Storage SPI (Custom)      │  │
│  │   - Calls Archbase API           │  │
│  │   - Validates credentials        │  │
│  │   - Fetches user attributes      │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│         ARCHBASE APP                    │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   REST API                       │  │
│  │   - Validates user               │  │
│  │   - Returns user data            │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   Database                       │  │
│  │   - UserEntity (source of truth) │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Vantagens
- ✅ Integração bidirecional
- ✅ Keycloak pode consultar dados do Archbase
- ✅ Útil se já tem usuários no Archbase
- ✅ Permite validação customizada

### Desvantagens
- ❌ Muito complexo para este caso de uso
- ❌ Inverte o modelo (Keycloak deveria ser fonte de verdade)
- ❌ Requer desenvolvimento pesado no Keycloak
- ❌ Acoplamento forte entre sistemas
- ❌ Performance depende de latência entre sistemas
- ❌ **Não recomendado** para seu cenário

### Quando Usar
- ✓ Quando você quer fazer migração gradual de usuários legados
- ✓ Quando o Archbase já é fonte de verdade de usuários
- ✓ Quando há sistema legado que não pode ser substituído
- ✗ **Não aplicável ao seu caso** (Keycloak deve ser fonte de verdade)

### Complexidade
**Muito Alta** - Arquitetura complexa e contraintuitiva

---

## 5. Webhook/HTTP Endpoint (Integração Assíncrona)

### Descrição
Similar ao Event Listener, mas usando webhooks e processamento assíncrono via mensageria (Kafka, RabbitMQ, etc.).

### Como Funciona
- Configurar Event Listener que publica eventos em fila
- Archbase consome eventos da fila
- Processar sincronização de forma assíncrona
- Retry automático em caso de falha

### Implementação Conceitual

```
┌─────────────────────────────────────────┐
│         KEYCLOAK                        │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   Event Listener (Custom)        │  │
│  │   - Captura evento               │  │
│  │   - Publica em fila              │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│       MESSAGE BROKER                    │
│    (Kafka / RabbitMQ / SQS)             │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   Topic: user-events             │  │
│  │   - user.created                 │  │
│  │   - user.updated                 │  │
│  │   - role.assigned                │  │
│  │   - group.membership.changed     │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────┐
│         ARCHBASE APP                    │
│                                         │
│  ┌──────────────────────────────────┐  │
│  │   Event Consumer                 │  │
│  │   - Processa eventos             │  │
│  │   - Retry automático             │  │
│  │   - Dead letter queue            │  │
│  └──────────────────────────────────┘  │
│                 │                       │
│                 ↓                       │
│  ┌──────────────────────────────────┐  │
│  │   Sync Service                   │  │
│  │   - Processa assincronamente     │  │
│  │   - Atualiza database            │  │
│  └──────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Tipos de Eventos

```json
// user.created
{
  "eventType": "USER_CREATED",
  "userId": "123-456-789",
  "username": "joao.silva",
  "email": "joao.silva@example.com",
  "timestamp": "2025-01-23T10:30:00Z"
}

// role.assigned
{
  "eventType": "ROLE_ASSIGNED",
  "userId": "123-456-789",
  "roleType": "REALM_ROLE",
  "roleName": "admin",
  "timestamp": "2025-01-23T10:31:00Z"
}

// group.membership.added
{
  "eventType": "GROUP_MEMBERSHIP_ADDED",
  "userId": "123-456-789",
  "groupPath": "/Empresa/Financeiro",
  "timestamp": "2025-01-23T10:32:00Z"
}
```

### Vantagens
- ✅ Desacoplamento via mensageria
- ✅ Processamento assíncrono (não bloqueia Keycloak)
- ✅ Retry automático em caso de falha
- ✅ Escalável (múltiplos consumidores)
- ✅ Resiliência (dead letter queue)
- ✅ Auditoria completa de eventos
- ✅ Possibilidade de replay de eventos

### Desvantagens
- ❌ Requer infraestrutura adicional (Kafka, RabbitMQ, etc.)
- ❌ Maior complexidade operacional
- ❌ Ainda requer Event Listener no Keycloak
- ❌ Eventual consistency (não é tempo real imediato)
- ❌ Necessita monitoramento de filas
- ❌ Complexidade de troubleshooting

### Quando Usar
- ✓ Arquiteturas baseadas em eventos/microserviços
- ✓ Quando já existe infraestrutura de mensageria
- ✓ Para alta escalabilidade e resiliência
- ✓ Quando há múltiplos consumidores dos eventos
- ✓ Ambientes enterprise com equipe DevOps madura

### Complexidade
**Alta** - Requer infraestrutura e expertise em mensageria

---

## 6. Abordagem Híbrida (Recomendada)

### Descrição
Combinação estratégica de **JIT Provisioning** (primário) com **Scheduled Sync** (secundário opcional), aproveitando os pontos fortes de cada abordagem.

### Arquitetura Completa

```
┌───────────────────────────────────────────────────────────────┐
│                         KEYCLOAK                              │
│  - Fonte de verdade para usuários                             │
│  - Gerenciamento de roles e groups                            │
│  - Autenticação centralizada                                  │
└───────────────────────────────────────────────────────────────┘
                           │
                           │ (1) Autenticação
                           ↓
                  ┌─────────────────┐
                  │   Token JWT     │
                  │   - sub         │
                  │   - email       │
                  │   - roles       │
                  │   - groups      │
                  └─────────────────┘
                           │
                           │ (2) Requisição com token
                           ↓
┌───────────────────────────────────────────────────────────────┐
│                  ARCHBASE (Spring Boot App)                   │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  COMPONENTE 1: JIT Provisioning (Primário)              │ │
│  │                                                          │ │
│  │  ┌────────────────────────────────────────────┐         │ │
│  │  │  JWT Filter / Interceptor                  │         │ │
│  │  │  - Valida token                            │         │ │
│  │  │  - Extrai claims (sub, roles, groups)      │         │ │
│  │  └────────────────────────────────────────────┘         │ │
│  │                    │                                     │ │
│  │                    ↓                                     │ │
│  │  ┌────────────────────────────────────────────┐         │ │
│  │  │  Cache Check                               │         │ │
│  │  │  - Verifica se sincronizou recentemente    │         │ │
│  │  │  - TTL: 5-15 minutos                       │         │ │
│  │  └────────────────────────────────────────────┘         │ │
│  │                    │                                     │ │
│  │         ┌──────────┴─────────┐                          │ │
│  │         │                    │                          │ │
│  │    Cache HIT          Cache MISS                        │ │
│  │         │                    │                          │ │
│  │    Pula sync               │                          │ │
│  │                            ↓                          │ │
│  │         ┌────────────────────────────────────┐         │ │
│  │         │  User Sync Service                 │         │ │
│  │         │  - Busca/cria UserEntity           │         │ │
│  │         │  - Cria Groups se não existirem    │         │ │
│  │         │  - Atualiza UserGroupEntity        │         │ │
│  │         │  - Atualiza cache                  │         │ │
│  │         └────────────────────────────────────┘         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  COMPONENTE 2: Scheduled Sync (Secundário - OPCIONAL)   │ │
│  │                                                          │ │
│  │  ┌────────────────────────────────────────────┐         │ │
│  │  │  Scheduled Job (ex: 2h AM, diário)         │         │ │
│  │  │  - @Scheduled(cron = "0 0 2 * * ?")        │         │ │
│  │  └────────────────────────────────────────────┘         │ │
│  │                    │                                     │ │
│  │                    ↓                                     │ │
│  │  ┌────────────────────────────────────────────┐         │ │
│  │  │  Keycloak Admin Client                     │         │ │
│  │  │  - Lista todos usuários (paginado)         │         │ │
│  │  │  - Busca roles e groups                    │         │ │
│  │  └────────────────────────────────────────────┘         │ │
│  │                    │                                     │ │
│  │                    ↓                                     │ │
│  │  ┌────────────────────────────────────────────┐         │ │
│  │  │  Full Sync Service                         │         │ │
│  │  │  - Compara com estado atual                │         │ │
│  │  │  - Sincroniza usuários inativos            │         │ │
│  │  │  - Marca usuários removidos                │         │ │
│  │  │  - Gera relatório de auditoria             │         │ │
│  │  └────────────────────────────────────────────┘         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Database (PostgreSQL/MySQL)                            │ │
│  │  - UserEntity (keycloakId: unique)                      │ │
│  │  - GroupEntity (criados automaticamente)                │ │
│  │  - UserGroupEntity (vínculos atualizados)               │ │
│  │  - PermissionEntity (gestão manual)                     │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

### Divisão de Responsabilidades

#### JIT Provisioning (Componente Primário)
**Responsabilidades:**
- ✅ Sincronizar usuários ativos (que fazem login)
- ✅ Garantir dados atualizados para usuários frequentes
- ✅ Criar grupos automaticamente conforme necessário
- ✅ Performance otimizada com cache

**Quando atua:**
- A cada login de usuário
- Com cache para evitar overhead
- Atualizações incrementais

#### Scheduled Sync (Componente Secundário - OPCIONAL)
**Responsabilidades:**
- ✅ Sincronizar usuários que não fizeram login recentemente
- ✅ Detectar e marcar usuários removidos do Keycloak
- ✅ Auditoria e relatórios
- ✅ Limpeza e manutenção

**Quando atua:**
- Periodicamente (ex: diariamente às 2h AM)
- Em horários de baixo tráfego
- Sincronização completa

### Fluxo Combinado Detalhado

#### Cenário 1: Usuário Ativo Faz Login

```
1. Usuário autentica no Keycloak
   → Recebe token JWT com roles e groups

2. Primeira requisição do dia ao Archbase
   → JWT Filter intercepta
   → Extrai claim "sub" (keycloakId)
   → Verifica cache: MISS (primeira vez no dia)
   → Dispara User Sync Service:
      • Busca UserEntity por keycloakId
      • Atualiza email se mudou
      • Extrai roles do token
      • Extrai groups do token
      • Cria GroupEntity se não existirem
      • Atualiza vínculos UserGroupEntity
      • Salva timestamp no cache (TTL: 15 min)
   → Requisição prossegue normalmente

3. Próximas requisições (dentro de 15 min)
   → JWT Filter intercepta
   → Verifica cache: HIT
   → Pula sincronização
   → Performance otimizada

4. Requisição após 15 min
   → Cache expirado
   → Sincronização incremental novamente
```

#### Cenário 2: Usuário Inativo (Scheduled Sync)

```
1. Usuário existe no Keycloak, mas não faz login há 30 dias
   → JIT não sincroniza (usuário não loga)

2. Scheduled Job executa (2h AM)
   → Lista todos usuários do Keycloak
   → Encontra usuário inativo
   → Sincroniza dados:
      • Cria UserEntity se não existe
      • Atualiza roles e groups
   → Usuário agora existe no Archbase

3. Administrador pode atribuir permissões manualmente
   → Mesmo que usuário não tenha logado ainda
```

#### Cenário 3: Usuário Removido do Keycloak

```
1. Administrador remove usuário do Keycloak
   → Usuário ainda existe no Archbase

2. JIT não detecta (usuário não tenta logar)

3. Scheduled Job executa
   → Lista usuários do Keycloak
   → Compara com usuários no Archbase
   → Detecta usuário que existe no Archbase mas não no Keycloak
   → Marca UserEntity.active = false (ou deleta, conforme política)
   → Gera log de auditoria

4. Sistema Archbase bloqueia acesso do usuário inativo
```

### Configuração Recomendada

```yaml
archbase:
  keycloak:
    # Configuração JIT
    jit:
      enabled: true
      cache:
        type: MEMORY # ou REDIS para múltiplas instâncias
        ttl-minutes: 15
      sync-on-login: true
    
    # Configuração Scheduled Sync (OPCIONAL)
    scheduled:
      enabled: false # Habilitar apenas se necessário
      cron: "0 0 2 * * ?" # 2h AM diariamente
      batch-size: 100
      mark-removed-as-inactive: true # ou delete: false
      
    # Admin API (necessário apenas para Scheduled Sync)
    admin:
      server-url: https://keycloak.example.com
      realm: meu-realm
      client-id: admin-cli
      client-secret: ${KEYCLOAK_ADMIN_SECRET}
```

### Vantagens da Abordagem Híbrida
- ✅ **Melhor dos dois mundos**
  - JIT: performance e simplicidade
  - Scheduled: completude e auditoria
  
- ✅ **Flexibilidade**
  - Pode usar apenas JIT inicialmente
  - Adicionar Scheduled Sync quando necessário
  
- ✅ **Baixo acoplamento**
  - JIT não requer acesso administrativo ao Keycloak
  - Scheduled é opcional e independente
  
- ✅ **Performance otimizada**
  - Cache reduz overhead do JIT
  - Scheduled roda em horário de baixo tráfego
  
- ✅ **Auditoria completa**
  - JIT loga sincronizações de usuários ativos
  - Scheduled gera relatórios completos

### Desvantagens
- ⚠️ Complexidade moderada (dois componentes)
- ⚠️ Requer configuração de credenciais admin (para Scheduled)
- ⚠️ Eventual consistency entre componentes

### Quando Usar
- ✓ **Recomendado para maioria dos cenários**
- ✓ Aplicações que querem simplicidade inicial (só JIT)
- ✓ Com opção de evolução para auditoria completa (+ Scheduled)
- ✓ Ambientes com mix de usuários ativos e inativos

### Complexidade
**Média** - Balanceada entre funcionalidade e manutenibilidade

---

## Comparação Resumida das Estratégias

| Estratégia | Complexidade | Tempo Real | Requer Mods Keycloak | Requer Admin API | Sincroniza Inativos | Recomendação |
|------------|-------------|------------|---------------------|------------------|-------------------|--------------|
| **Event Listeners** | Alta | ✅ Sim | ✅ Sim | ❌ Não | ✅ Sim | ⭐⭐⭐ Se tiver acesso |
| **JIT Provisioning** | Baixa | ⚠️ No login | ❌ Não | ❌ Não | ❌ Não | ⭐⭐⭐⭐⭐ **Top** |
| **Scheduled Sync** | Média | ❌ Não | ❌ Não | ✅ Sim | ✅ Sim | ⭐⭐⭐ Complementar |
| **User Storage SPI** | Muito Alta | ✅ Sim | ✅ Sim | ❌ Não | ✅ Sim | ⭐ Não aplicável |
| **Webhook/Mensageria** | Alta | ✅ Sim | ✅ Sim | ❌ Não | ✅ Sim | ⭐⭐ Se tiver infra |
| **Híbrida (JIT + Scheduled)** | Média | ⚠️ Híbrido | ❌ Não | ⚠️ Opcional | ⚠️ Opcional | ⭐⭐⭐⭐⭐ **Ideal** |

---

## Decisões de Arquitetura Pendentes

Independente da estratégia escolhida, algumas decisões precisam ser tomadas:

### 1. Nomenclatura de Groups

#### Opção A: Nome Direto (Simples)
```
Realm Role "admin" → GroupEntity.code = "admin"
Realm Role "manager" → GroupEntity.code = "manager"
```
**Prós:** Simples, direto
**Contras:** Possível conflito com Client Roles de mesmo nome

#### Opção B: Prefixo por Tipo (Clareza)
```
Realm Role "admin" → GroupEntity.code = "REALM_admin"
Client Role "admin" (app-financeiro) → GroupEntity.code = "CLIENT_app-financeiro_admin"
Keycloak Group "/Empresa/Financeiro" → GroupEntity.code = "GROUP_Empresa_Financeiro"
```
**Prós:** Sem conflitos, origem clara
**Contras:** Nomes mais longos

#### Opção C: Client ID para Client Roles
```
Realm Role "admin" → GroupEntity.code = "admin"
Client Role "visualizar" (app-financeiro) → GroupEntity.code = "app-financeiro:visualizar"
Keycloak Group "/Empresa/Financeiro" → GroupEntity.code = "Empresa/Financeiro"
```
**Prós:** Balanço entre clareza e simplicidade
**Contras:** Convenção precisa ser documentada

### 2. Tratamento de Hierarquia de Groups

Keycloak suporta hierarquia: `/Empresa/Financeiro/Contas-a-Pagar`
Archbase não suporta hierarquia nativa

#### Opção A: Achatar com Separador
```
"/Empresa/Financeiro/Contas-a-Pagar" → "Empresa-Financeiro-Contas-a-Pagar"
```
**Prós:** Simples
**Contras:** Perde semântica hierárquica

#### Opção B: Usar Apenas Nome Final
```
"/Empresa/Financeiro/Contas-a-Pagar" → "Contas-a-Pagar"
```
**Prós:** Nomes mais curtos
**Contras:** Possível ambiguidade/conflito

#### Opção C: Manter Path Completo
```
"/Empresa/Financeiro/Contas-a-Pagar" → "Empresa/Financeiro/Contas-a-Pagar"
```
**Prós:** Mantém contexto completo
**Contras:** Nomes muito longos

#### Opção D: Criar Múltiplos Groups (Herança Simulada)
```
"/Empresa/Financeiro/Contas-a-Pagar" cria:
- GroupEntity "Empresa"
- GroupEntity "Financeiro"
- GroupEntity "Contas-a-Pagar"

Usuário vinculado aos 3 groups
```
**Prós:** Simula hierarquia via múltiplos vínculos
**Contras:** Complexidade, possível explosão de groups

### 3. Metadados de Origem

Adicionar campo para identificar origem do Group?

#### Opção A: Campo Enum
```java
@Entity
public class GroupEntity {
    private UUID id;
    private String code;
    private String name;
    
    @Enumerated(EnumType.STRING)
    private GroupOrigin origin; // REALM_ROLE, CLIENT_ROLE, KEYCLOAK_GROUP, MANUAL
    
    private String originDetails; // Ex: "app-financeiro" para client roles
}
```
**Prós:** Clareza total, útil para administradores
**Contras:** Requer migração de schema

#### Opção B: Convenção de Nomenclatura
```
Usar prefixos no code (REALM_, CLIENT_, GROUP_)
Parsing do nome revela origem
```
**Prós:** Sem mudança de schema
**Contras:** Menos robusto, depende de parsing

#### Opção C: Sem Metadados
```
Não diferenciar origem
Todos são simplesmente Groups
```
**Prós:** Simplicidade máxima
**Contras:** Impossível distinguir origem posteriormente

### 4. Sincronização de Mudanças

#### Opção A: Atualização Completa (Replace)
```
A cada sincronização:
1. Remove todos vínculos UserGroupEntity do usuário
2. Cria novos vínculos baseados no token atual
```
**Prós:** Simples, sempre consistente
**Contras:** Perde histórico, pode causar overhead

#### Opção B: Atualização Incremental (Diff)
```
A cada sincronização:
1. Compara groups atuais com groups do token
2. Adiciona novos vínculos
3. Remove vínculos não presentes no token
```
**Prós:** Mais eficiente, menos writes
**Contras:** Lógica mais complexa

#### Opção C: Manter Histórico
```
Nunca remove vínculos
Adiciona campo UserGroupEntity.active
Desativa vínculos não presentes no token
```
**Prós:** Auditoria completa, histórico preservado
**Contras:** Tabela cresce indefinidamente

### 5. Tratamento de Usuários Removidos

#### Opção A: Soft Delete (Recomendado)
```java
@Entity
public class UserEntity {
    private Boolean accountDeactivated = false;
    private LocalDateTime deactivatedAt;
}

// Quando usuário não existe mais no Keycloak
user.setActive(true);
user.setDeactivatedAt(LocalDateTime.now());
```
**Prós:** Preserva histórico, auditoria, dados relacionados
**Contras:** Tabela cresce

#### Opção B: Hard Delete
```
// Remove UserEntity completamente
userRepository.delete(user);
```
**Prós:** Limpeza de dados
**Contras:** Perde histórico, quebra referências

#### Opção C: Ignorar
```
// Não faz nada, usuário permanece no Archbase
// Bloqueio ocorre naturalmente (token inválido)
```
**Prós:** Simplicidade
**Contras:** Dados órfãos

---

**Nomenclatura de Groups**:
   - `REALM_admin`, `CLIENT_app-financeiro_visualizar`, `GROUP_Financeiro`
   - Clareza total, sem conflitos
---

## Recursos e Referências

### Documentação Oficial
- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html)
- [Keycloak Server Developer Guide](https://www.keycloak.org/docs/latest/server_development/)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)

### Bibliotecas Úteis
- **Keycloak Admin Client**: `org.keycloak:keycloak-admin-client`
- **Spring Security OAuth2 Resource Server**: `spring-boot-starter-oauth2-resource-server`
- **JWT Decoder**: Incluído no Spring Security OAuth2

### Ferramentas de Desenvolvimento
- **Keycloak Docker**: Testes locais
- **Postman**: Testar Admin API
- **JWT.io**: Debug de tokens

---

## Apêndice: Exemplo de Token JWT

```json
{
  "exp": 1706024400,
  "iat": 1706020800,
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "iss": "https://keycloak.example.com/realms/meu-realm",
  "sub": "123e4567-e89b-12d3-a456-426614174000",
  "typ": "Bearer",
  "azp": "minha-aplicacao",
  "session_state": "xyz789",
  "preferred_username": "joao.silva",
  "email": "joao.silva@empresa.com",
  "email_verified": true,
  "name": "João Silva",
  "given_name": "João",
  "family_name": "Silva",
  
  "realm_access": {
    "roles": [
      "default-roles-meu-realm",
      "offline_access",
      "uma_authorization",
      "user",
      "manager"
    ]
  },
  
  "resource_access": {
    "minha-aplicacao": {
      "roles": [
        "app-user"
      ]
    },
    "app-financeiro": {
      "roles": [
        "visualizar_relatorios",
        "editar_relatorios",
        "exportar_dados"
      ]
    },
    "app-vendas": {
      "roles": [
        "criar_pedidos",
        "aprovar_pedidos"
      ]
    }
  },
  
  "groups": [
    "/Empresa",
    "/Empresa/Financeiro",
    "/Empresa/TI",
    "/Projetos/ProjetoX"
  ]
}
```

### Claims Relevantes para Sincronização

- **`sub`**: ID único do usuário (use como keycloakId) ✅
- **`preferred_username`**: Nome de usuário
- **`email`**: Email (pode não estar presente)
- **`realm_access.roles`**: Realm roles do usuário
- **`resource_access.{client}.roles`**: Client roles específicos
- **`groups`**: Groups do Keycloak (paths hierárquicos)

---

**Versão:** 1.0  
**Data:** 2025-01-23  
**Status:** Em Análise
