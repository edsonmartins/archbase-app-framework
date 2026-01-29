# Decisões de Integração Keycloak-Archbase

## Estrutura de Dados e Nomenclatura

---

## Contexto

Este documento documenta as **decisões arquiteturais definitivas** sobre estrutura de dados, nomenclatura e tratamento de informações para a integração entre **Keycloak** (autenticação) e **Archbase Framework** (autorização).

**Escopo**: Este documento foca exclusivamente em decisões sobre:
- Nomenclatura de groups
- Tratamento de hierarquia
- Metadados e formato de dados
- Algoritmos de sincronização incremental

**Nota**: A estratégia de sincronização entre Keycloak e Archbase (JIT, Scheduled, Event Listeners, etc.) será definida em documento separado.

---

## Resumo Executivo

### Decisões Tomadas

| # | Decisão | Descrição |
|---|----------|-----------|
| 1 | **Nomenclatura Unificada** | Padrão `{TIPO} | {detalhes}` na propriedade `name` de `GroupEntity` |
| 2 | **Tratamento de Hierarquia** | Manter path completo após o separador " | " |
| 3 | **Sincronização Incremental** | Algoritmo diff que adiciona/remove vínculos conforme necessário |
| 4 | **Soft Delete de Usuários** | Campo `accountDeactivated` em `UserEntity` |

### Tipos de Groups Suportados

| Tipo | Descrição | Exemplo |
|------|-----------|---------|
| `REALM` | Realm Role do Keycloak | `REALM | admin` |
| `CLIENT` | Client Role do Keycloak | `CLIENT | app-financeiro | visualizar` |
| `GROUP` | Keycloak Group (path) | `GROUP | Empresa/Financeiro` |

---

## Arquitetura Definida

### 1. Nomenclatura Unificada de Groups

#### Decisão Arquitetural
Todos os groups originados do Keycloak (seja Realm Roles, Client Roles ou Groups) seguirão um **padrão unificado de nomenclatura** aplicado à propriedade `name` da entidade `GroupEntity`.

#### Padrão Definido
```
{TIPO} | {detalhes}
```

**Separador**: ` | ` (espaço + barra vertical + espaço)

**Tipos suportados**:
- `REALM` - Para Realm Roles do Keycloak
- `CLIENT` - Para Client Roles do Keycloak
- `GROUP` - Para Keycloak Groups (paths hierárquicos)

#### Exemplos Práticos

**Realm Roles:**
```
Keycloak: Realm Role "admin"
Archbase: GroupEntity.name = "REALM | admin"

Keycloak: Realm Role "manager"
Archbase: GroupEntity.name = "REALM | manager"

Keycloak: Realm Role "user"
Archbase: GroupEntity.name = "REALM | user"
```

**Client Roles:**
```
Keycloak: Client Role "admin" (app-financeiro)
Archbase: GroupEntity.name = "CLIENT | app-financeiro | admin"

Keycloak: Client Role "visualizar" (app-financeiro)
Archbase: GroupEntity.name = "CLIENT | app-financeiro | visualizar"

Keycloak: Client Role "editar" (app-vendas)
Archbase: GroupEntity.name = "CLIENT | app-vendas | editar"
```

**Keycloak Groups:**
```
Keycloak: Group "/Empresa"
Archbase: GroupEntity.name = "GROUP | Empresa"

Keycloak: Group "/Empresa/Financeiro"
Archbase: GroupEntity.name = "GROUP | Empresa/Financeiro"

Keycloak: Group "/Empresa/Financeiro/Contas-a-Pagar"
Archbase: GroupEntity.name = "GROUP | Empresa/Financeiro/Contas-a-Pagar"
```

#### Racional da Decisão

1. **Clareza de Origem**: O tipo no início identifica imediatamente a origem do group
2. **Unicidade Garantida**: Separação por tipo evita conflitos entre nomes iguais de origens diferentes
3. **Parsing Simples**: Facilita extração de informações via split do separador
4. **Fácil Filtros**: Queries podem filtrar facilmente por tipo usando `LIKE "REALM | %"`

---

### 2. Tratamento de Hierarquia de Groups

#### Decisão Arquitetural
**Opção C: Manter Path Completo**

A hierarquia de Keycloak Groups é mantida integralmente após o separador " | ", preservando o contexto completo da estrutura organizacional.

#### Formato
```
GROUP | {path_completo_com_barras}
```

#### Exemplos

```
Keycloak: /Empresa
Archbase: GROUP | Empresa

Keycloak: /Empresa/Financeiro
Archbase: GROUP | Empresa/Financeiro

Keycloak: /Empresa/Financeiro/Contas-a-Pagar
Archbase: GROUP | Empresa/Financeiro/Contas-a-Pagar

Keycloak: /Projetos/ProjetoX/Equipe1
Archbase: GROUP | Projetos/ProjetoX/Equipe1
```

#### Racional da Decisão

1. **Contexto Preservado**: Mantém toda a semântica hierárquica original
2. **Fácil Navegação**: Permite filtrar todos os groups de um departamento (ex: `LIKE "GROUP | Empresa/%"`)
3. **Ambiguidade Evitada**: Groups com mesmo nome em departamentos diferentes permanecem distintos
4. **Rastreabilidade**: Facilita auditoria e report baseados em estrutura organizacional

---

### 3. Sincronização de Mudanças

#### Decisão Arquitetural
**Opção B: Atualização Incremental (Diff)**

A cada sincronização de um usuário, o sistema compara os groups atuais com os groups presentes no token JWT e realiza apenas as diferenças necessárias.

#### Algoritmo Definido

```java
public class UserGroupSynchronizationService {

    /**
     * Sincroniza os groups de um usuário usando algoritmo incremental (diff).
     *
     * @param user Usuário a ser sincronizado
     * @param tokenGroups Lista de group names extraídos do token JWT
     */
    public void syncUserGroupsIncremental(UserEntity user, List<String> tokenGroups) {

        // 1. Buscar groups atuais do usuário no banco de dados
        List<String> currentGroupNames = user.getUserGroups().stream()
                .map(ug -> ug.getGroup().getName())
                .collect(Collectors.toList());

        // 2. Identificar groups a adicionar (presentes no token, mas não no banco)
        Set<String> groupsToAdd = new HashSet<>(tokenGroups);
        groupsToAdd.removeAll(currentGroupNames);

        // 3. Identificar groups a remover (presentes no banco, mas não no token)
        Set<String> groupsToRemove = new HashSet<>(currentGroupNames);
        groupsToRemove.removeAll(tokenGroups);

        // 4. Adicionar novos vínculos
        for (String groupName : groupsToAdd) {
            GroupEntity group = findOrCreateGroup(groupName);
            UserGroupEntity userGroup = new UserGroupEntity();
            userGroup.setUser(user);
            userGroup.setGroup(group);
            userGroupRepository.save(userGroup);

            log.info("Added group {} to user {}", groupName, user.getUsername());
        }

        // 5. Remover vínculos obsoletos
        for (String groupName : groupsToRemove) {
            GroupEntity group = groupRepository.findByName(groupName)
                    .orElseThrow(() -> new IllegalStateException("Group not found: " + groupName));

            userGroupRepository.deleteByUserAndGroup(user, group);

            log.info("Removed group {} from user {}", groupName, user.getUsername());
        }

        log.info("Group sync completed for user {}: +{} groups, -{} groups",
                user.getUsername(), groupsToAdd.size(), groupsToRemove.size());
    }

    /**
     * Busca um group existente ou cria um novo baseado no nome padronizado.
     */
    private GroupEntity findOrCreateGroup(String groupName) {
        return groupRepository.findByName(groupName)
                .orElseGet(() -> {
                    GroupEntity newGroup = new GroupEntity();
                    newGroup.setName(groupName);

                    // Extrair informações adicionais do nome se necessário
                    GroupTypeInfo info = parseGroupTypeName(groupName);
                    newGroup.setCode(generateCodeFromName(groupName));
                    newGroup.setDescription(info.getDescription());

                    return groupRepository.save(newGroup);
                });
    }

    /**
     * Faz o parse do nome do group para extrair tipo e detalhes.
     * Formato esperado: "TIPO | detalhes"
     */
    private GroupTypeInfo parseGroupTypeName(String groupName) {
        String[] parts = groupName.split(" \\| ", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid group name format: " + groupName);
        }

        GroupType type = GroupType.valueOf(parts[0]);
        String details = parts[1];

        return new GroupTypeInfo(type, details);
    }

    /**
     * Gera um código amigável a partir do nome do group.
     * Exemplo: "REALM | admin" -> "realm_admin"
     */
    private String generateCodeFromName(String groupName) {
        return groupName.toLowerCase()
                .replace(" | ", "_")
                .replace("/", "_");
    }
}

/**
 * Tipos de groups suportados.
 */
public enum GroupType {
    REALM,      // Realm Role do Keycloak
    CLIENT,     // Client Role do Keycloak
    GROUP,      // Keycloak Group
    MANUAL      // Criado manualmente no Archbase
}

/**
 * DTO para informações extraídas do nome do group.
 */
public record GroupTypeInfo(GroupType type, String details) {

    public String getDescription() {
        return switch (type) {
            case REALM -> "Realm Role: " + details;
            case CLIENT -> "Client Role: " + details;
            case GROUP -> "Keycloak Group: " + details;
            case MANUAL -> "Manual Group: " + details;
        };
    }
}
```

#### Racional da Decisão

1. **Eficiência**: Menos operações de banco de dados comparado à atualização completa
2. **Performance**: Apenas as diferenças são processadas
3. **Auditabilidade**: Logs claros do que foi adicionado/removido
4. **Consistência**: Garante que o estado do banco reflita exatamente o token atual

---

### 4. Tratamento de Usuários Removidos

#### Decisão Arquitetural
**Opção A: Soft Delete**

Usuários removidos do Keycloak não são deletados fisicamente do Archbase, mas marcados como desativados através do campo `accountDeactivated`.

#### Estrutura de Dados

```java
@Entity
@Table(name = "users")
public class UserEntity extends PersistenceEntityBase<UserEntity, UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String externalId; // claim "sub" do Keycloak

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Indica se a conta do usuário foi desativada.
     * Usuário pode ter sido removido do Keycloak ou ter sua conta desativada.
     */
    @Column(nullable = false)
    private Boolean accountDeactivated = false;

    @Column
    private LocalDateTime deactivatedAt;

    @Column(length = 255)
    private String deactivationReason; // "REMOVED_FROM_KEYCLOAK", "MANUAL", etc.

    // Outros campos...

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<UserGroupEntity> userGroups = new HashSet<>();

    public void deactivateAccount(String reason) {
        this.accountDeactivated = true;
        this.deactivatedAt = LocalDateTime.now();
        this.deactivationReason = reason;
    }

    public void activateAccount() {
        this.accountDeactivated = false;
        this.deactivatedAt = null;
        this.deactivationReason = null;
    }
}
```

#### Lógica de Soft Delete

```java
public class UserDeactivationService {

    /**
     * Marca um usuário como desativado quando detectado que ele foi removido do Keycloak.
     */
    public void markUserAsRemovedFromKeycloak(UserEntity user) {
        if (user.getAccountDeactivated()) {
            return; // Já está desativado
        }

        user.deactivateAccount("REMOVED_FROM_KEYCLOAK");
        userRepository.save(user);

        log.warn("User {} marked as deactivated (removed from Keycloak)",
                user.getUsername());

        // Opcional: Publicar evento de domínio
        eventPublisher.publishEvent(new UserDeactivatedEvent(user.getId()));
    }

    /**
     * Reativa um usuário caso ele tenha sido recriado no Keycloak.
     */
    public void reactivateUser(UserEntity user) {
        if (!user.getAccountDeactivated()) {
            return; // Já está ativo
        }

        user.activateAccount();
        userRepository.save(user);

        log.info("User {} reactivated", user.getUsername());

        // Opcional: Publicar evento de domínio
        eventPublisher.publishEvent(new UserReactivatedEvent(user.getId()));
    }
}
```

#### Racional da Decisão

1. **Preservação de Histórico**: Dados e relações do usuário são mantidos
2. **Auditoria**: Possível rastrear quando e por que um usuário foi desativado
3. **Reversibilidade**: Usuário pode ser reativado se recriado no Keycloak
4. **Integridade Referencial**: Não quebra chaves estrangeiras em outras tabelas

---

## Modelo de Dados

### Estrutura de GroupEntity

```java
@Entity
@Table(name = "groups")
public class GroupEntity extends PersistenceEntityBase<GroupEntity, UUID> {

    @Id
    private UUID id;

    /**
     * Nome do group seguindo o padrão unificado.
     * Formato: "TIPO | detalhes"
     * Exemplos: "REALM | admin", "CLIENT | app-financeiro | visualizar", "GROUP | Empresa/Financeiro"
     */
    @Column(nullable = false, unique = true, length = 500)
    private String name;

    /**
     * Código amigável derivado do nome.
     * Exemplo: "realm_admin", "client_app_financeiro_visualizar", "group_empresa_financeiro"
     */
    @Column(nullable = false, unique = true, length = 255)
    private String code;

    @Column(length = 1000)
    private String description;

    // Metadados de origem (opcional, para facilitar queries)
    @Column(nullable = false, length = 20)
    private String groupType; // "REALM", "CLIENT", "GROUP", "MANUAL"

    @Column(length = 255)
    private String originDetails; // Ex: "app-financeiro" para CLIENT roles

    @OneToMany(mappedBy = "group")
    private Set<UserGroupEntity> userGroups = new HashSet<>();

    /**
     * Extrai o tipo do group a partir do nome.
     * @return "REALM", "CLIENT", "GROUP", ou "MANUAL"
     */
    public String extractType() {
        int separatorIndex = name.indexOf(" | ");
        if (separatorIndex == -1) {
            return "MANUAL";
        }
        return name.substring(0, separatorIndex);
    }

    /**
     * Extrai os detalhes do group a partir do nome.
     * @return String após o separador " | "
     */
    public String extractDetails() {
        int separatorIndex = name.indexOf(" | ");
        if (separatorIndex == -1) {
            return name;
        }
        return name.substring(separatorIndex + 3);
    }
}
```

### Estrutura de UserGroupEntity (Vínculo)

```java
@Entity
@Table(name = "user_groups", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "group_id"})
})
public class UserGroupEntity extends PersistenceEntityBase<UserGroupEntity, Long> {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Opcional: Para rastreabilidade
    @Column(length = 50)
    private String source; // "KEYCLOAK_REALM_ROLE", "KEYCLOAK_CLIENT_ROLE", "KEYCLOAK_GROUP", "MANUAL"

    public UserGroupEntity() {
        this.createdAt = LocalDateTime.now();
    }
}
```

### Exemplos de Dados

#### Tabela `groups`

| id | name | code | group_type | origin_details | description |
|----|------|------|------------|----------------|-------------|
| uuid-1 | `REALM | admin` | realm_admin | REALM | null | Realm Role: admin |
| uuid-2 | `REALM | manager` | realm_manager | REALM | null | Realm Role: manager |
| uuid-3 | `CLIENT | app-financeiro | admin` | client_app_financeiro_admin | CLIENT | app-financeiro | Client Role: app-financeiro | admin |
| uuid-4 | `CLIENT | app-financeiro | visualizar` | client_app_financeiro_visualizar | CLIENT | app-financeiro | Client Role: app-financeiro | visualizar |
| uuid-5 | `CLIENT | app-vendas | editar` | client_app_vendas_editar | CLIENT | app-vendas | Client Role: app-vendas | editar |
| uuid-6 | `GROUP | Empresa` | group_empresa | GROUP | null | Keycloak Group: Empresa |
| uuid-7 | `GROUP | Empresa/Financeiro` | group_empresa_financeiro | GROUP | null | Keycloak Group: Empresa/Financeiro |
| uuid-8 | `GROUP | Empresa/Financeiro/Contas-a-Pagar` | group_empresa_financeiro_contas_a_pagar | GROUP | null | Keycloak Group: Empresa/Financeiro/Contas-a-Pagar |

#### Tabela `user_groups`

| id | user_id | group_id | created_at | source |
|----|---------|----------|------------|--------|
| 1 | user-uuid-1 | uuid-1 | 2025-01-23 10:00:00 | KEYCLOAK_REALM_ROLE |
| 1 | user-uuid-1 | uuid-3 | 2025-01-23 10:00:00 | KEYCLOAK_CLIENT_ROLE |
| 1 | user-uuid-1 | uuid-7 | 2025-01-23 10:00:00 | KEYCLOAK_GROUP |

---

## Implementação

### 1. Utilitário de Conversão de Nomes

```java
/**
 * Utilitário para conversão entre entidades do Keycloak e formato de groups do Archbase.
 */
public class KeycloakGroupConverter {

    private static final String SEPARATOR = " | ";

    /**
     * Converte uma Realm Role do Keycloak para o formato de group do Archbase.
     */
    public static String realmRoleToGroupName(String realmRoleName) {
        return "REALM" + SEPARATOR + realmRoleName;
    }

    /**
     * Converte uma Client Role do Keycloak para o formato de group do Archbase.
     */
    public static String clientRoleToGroupName(String clientId, String clientRoleName) {
        return "CLIENT" + SEPARATOR + clientId + SEPARATOR + clientRoleName;
    }

    /**
     * Converte um Keycloak Group path para o formato de group do Archbase.
     */
    public static String keycloakGroupToGroupName(String groupPath) {
        // Remove a barra inicial se existir
        String path = groupPath.startsWith("/") ? groupPath.substring(1) : groupPath;
        return "GROUP" + SEPARATOR + path;
    }

    /**
     * Extrai o tipo do group a partir do nome formatado.
     */
    public static String extractType(String groupName) {
        int separatorIndex = groupName.indexOf(SEPARATOR);
        if (separatorIndex == -1) {
            return "MANUAL";
        }
        return groupName.substring(0, separatorIndex);
    }

    /**
     * Verifica se o nome segue o padrão esperado.
     */
    public static boolean isValidFormat(String groupName) {
        return groupName != null && groupName.contains(SEPARATOR);
    }

    /**
     * Gera um código a partir do nome do group.
     */
    public static String generateCode(String groupName) {
        return groupName.toLowerCase()
                .replace(SEPARATOR.trim(), "_")
                .replace("/", "_")
                .replace(" ", "_");
    }
}
```

### 2. Parser de Token JWT

```java
/**
 * Extrai informações de groups do token JWT do Keycloak.
 */
public class KeycloakTokenGroupExtractor {

    private static final String SEPARATOR = " | ";

    /**
     * Extrai todos os grupos (roles + groups) do token JWT como nomes formatados.
     */
    public List<String> extractGroupNames(Jwt jwt) {

        List<String> groupNames = new ArrayList<>();

        // 1. Extrair Realm Roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                for (String role : realmRoles) {
                    // Ignorar roles padrão do sistema
                    if (!isDefaultRole(role)) {
                        groupNames.add(KeycloakGroupConverter.realmRoleToGroupName(role));
                    }
                }
            }
        }

        // 2. Extrair Client Roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                String clientId = entry.getKey();
                Map<String, Object> clientData = (Map<String, Object>) entry.getValue();
                List<String> clientRoles = (List<String>) clientData.get("roles");

                if (clientRoles != null) {
                    for (String role : clientRoles) {
                        groupNames.add(KeycloakGroupConverter.clientRoleToGroupName(clientId, role));
                    }
                }
            }
        }

        // 3. Extrair Keycloak Groups
        List<String> groups = jwt.getClaim("groups");
        if (groups != null) {
            for (String groupPath : groups) {
                groupNames.add(KeycloakGroupConverter.keycloakGroupToGroupName(groupPath));
            }
        }

        return groupNames;
    }

    /**
     * Verifica se a role é uma role padrão do Keycloak que deve ser ignorada.
     */
    private boolean isDefaultRole(String roleName) {
        return Set.of(
                "default-roles-" + realmName,
                "offline_access",
                "uma_authorization"
        ).contains(roleName);
    }
}
```

### 3. Serviço de Sincronização Completo

```java
/**
 * Serviço principal de sincronização de usuários e groups do Keycloak.
 */
@Service
public class KeycloakUserSyncService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final KeycloakTokenGroupExtractor groupExtractor;
    private final UserDeactivationService deactivationService;

    private static final long SYNC_CACHE_TTL_MINUTES = 15;
    private final Cache<String, LocalDateTime> syncCache = Caffeine.newBuilder()
            .expireAfterWrite(SYNC_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .build();

    /**
     * Sincroniza um usuário baseado no token JWT (algoritmo incremental com cache).
     */
    @Transactional
    public void syncUserFromToken(Jwt jwt) {

        // 1. Extrair informações básicas do token
        String externalId = jwt.getSubject();
        String username = jwt.getClaim("preferred_username");
        String email = jwt.getClaim("email");

        // 2. Verificar cache para evitar sincronizações frequentes
        LocalDateTime lastSync = syncCache.getIfPresent(externalId);
        if (lastSync != null) {
            log.debug("User {} synced recently, skipping", username);
            return;
        }

        // 3. Buscar ou criar UserEntity
        UserEntity user = userRepository.findByExternalId(externalId)
                .orElseGet(() -> createNewUser(externalId, username, email));

        // 4. Atualizar dados básicos se necessário
        updateUserBasicData(user, username, email);

        // 5. Reativar usuário se estava desativado e agora existe no Keycloak
        if (user.getAccountDeactivated()) {
            deactivationService.reactivateUser(user);
        }

        // 6. Extrair groups do token
        List<String> tokenGroupNames = groupExtractor.extractGroupNames(jwt);

        // 7. Sincronizar groups (algoritmo incremental)
        syncUserGroupsIncremental(user, tokenGroupNames);

        // 8. Atualizar cache
        syncCache.put(externalId, LocalDateTime.now());

        log.info("User {} synchronized successfully with {} groups",
                username, tokenGroupNames.size());
    }

    /**
     * Cria um novo usuário baseado nas informações do token.
     */
    private UserEntity createNewUser(String externalId, String username, String email) {
        UserEntity user = new UserEntity();
        user.setExternalId(externalId);
        user.setUsername(username);
        user.setEmail(email);
        user.setAccountDeactivated(false);
        return userRepository.save(user);
    }

    /**
     * Atualiza dados básicos do usuário se mudaram.
     */
    private void updateUserBasicData(UserEntity user, String username, String email) {
        boolean needsUpdate = false;

        if (!user.getUsername().equals(username)) {
            user.setUsername(username);
            needsUpdate = true;
        }

        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            needsUpdate = true;
        }

        if (needsUpdate) {
            userRepository.save(user);
        }
    }

    /**
     * Sincronização incremental de groups (implementação detalhada na seção 3).
     */
    private void syncUserGroupsIncremental(UserEntity user, List<String> tokenGroupNames) {
        // Implementação conforme mostrado na seção "Sincronização de Mudanças"
        // ... (código omitido por brevidade)
    }
}
```

---

## Configuração

### YAML de Exemplo

```yaml
archbase:
  security:
    enabled: true

  keycloak:
    # Configuração de conexão com o Keycloak
    issuer-uri: https://keycloak.example.com/realms/meu-realm
    realm: meu-realm
    resource: minha-aplicacao

    # Configuração de sincronização (estratégia a ser definida separadamente)
    sync:
      enabled: true

      # Cache de sincronização (para evitar sincronizações excessivas)
      cache:
        type: CAFFEINE  # ou REDIS para ambientes distribuídos
        ttl-minutes: 15

      # Configuração de groups
      groups:
        # Padrão de nomenclatura (já definido nesta arquitetura)
        naming-pattern: "TIPO | detalhes"
        separator: " | "

        # Roles padrão do Keycloak a ignorar
        ignored-default-roles:
          - "offline_access"
          - "uma_authorization"
          - "default-roles-*"

      # Configuração de usuários removidos
      user-deactivation:
        strategy: SOFT_DELETE  # Sempre usar soft delete
        mark-inactive-when-removed-from-keycloak: true

# Logging
logging:
  level:
    br.com.archbase.security.keycloak: DEBUG
    br.com.archbase.security.keycloak.sync: INFO
```

---

## Exemplos Práticos

### Fluxo Completo de Sincronização

#### Cenário: Usuário faz login pela primeira vez

**Token JWT recebido:**
```json
{
  "sub": "123e4567-e89b-12d3-a456-426614174000",
  "preferred_username": "joao.silva",
  "email": "joao.silva@empresa.com",
  "realm_access": {
    "roles": ["user", "manager"]
  },
  "resource_access": {
    "app-financeiro": {
      "roles": ["visualizar"]
    }
  },
  "groups": ["/Empresa", "/Empresa/Financeiro"]
}
```

**Processo:**

1. **Extração de Groups:**
   ```
   Realm Roles → ["REALM | user", "REALM | manager"]
   Client Roles → ["CLIENT | app-financeiro | visualizar"]
   Keycloak Groups → ["GROUP | Empresa", "GROUP | Empresa/Financeiro"]

   Todos: ["REALM | user", "REALM | manager",
           "CLIENT | app-financeiro | visualizar",
           "GROUP | Empresa", "GROUP | Empresa/Financeiro"]
   ```

2. **Criação de UserEntity:**
   ```java
   UserEntity {
       id: uuid-generated,
       externalId: "123e4567-e89b-12d3-a456-426614174000",
       username: "joao.silva",
       email: "joao.silva@empresa.com",
       accountDeactivated: false
   }
   ```

3. **Criação de GroupEntities (se não existirem):**
   ```
   GroupEntity { name: "REALM | user", code: "realm_user", groupType: "REALM" }
   GroupEntity { name: "REALM | manager", code: "realm_manager", groupType: "REALM" }
   GroupEntity { name: "CLIENT | app-financeiro | visualizar", ... }
   GroupEntity { name: "GROUP | Empresa", ... }
   GroupEntity { name: "GROUP | Empresa/Financeiro", ... }
   ```

4. **Criação de vínculos UserGroupEntity:**
   ```
   user.id + "REALM | user".id → UserGroupEntity
   user.id + "REALM | manager".id → UserGroupEntity
   user.id + "CLIENT | app-financeiro | visualizar".id → UserGroupEntity
   user.id + "GROUP | Empresa".id → UserGroupEntity
   user.id + "GROUP | Empresa/Financeiro".id → UserGroupEntity
   ```

#### Cenário: Usuário faz login novamente com roles alteradas

**Token JWT atualizado:**
```json
{
  "realm_access": {
    "roles": ["user", "admin"]  // "manager" removido, "admin" adicionado
  },
  "resource_access": {
    "app-financeiro": {
      "roles": ["visualizar", "editar"]  // "editar" adicionado
    }
  },
  "groups": ["/Empresa", "/Empresa/TI"]  // "/Empresa/Financeiro" removido, "/Empresa/TI" adicionado
}
```

**Sincronização Incremental (Diff):**

```
Groups atuais no banco:
  - "REALM | user"
  - "REALM | manager"
  - "CLIENT | app-financeiro | visualizar"
  - "GROUP | Empresa"
  - "GROUP | Empresa/Financeiro"

Groups do token:
  - "REALM | user"
  - "REALM | admin"
  - "CLIENT | app-financeiro | visualizar"
  - "CLIENT | app-financeiro | editar"
  - "GROUP | Empresa"
  - "GROUP | Empresa/TI"

Diff:
  Para adicionar: ["REALM | admin", "CLIENT | app-financeiro | editar", "GROUP | Empresa/TI"]
  Para remover: ["REALM | manager", "GROUP | Empresa/Financeiro"]

Ações:
  ✓ Criar vínculo com "REALM | admin"
  ✓ Criar vínculo com "CLIENT | app-financeiro | editar"
  ✓ Criar vínculo com "GROUP | Empresa/TI"
  ✗ Remover vínculo com "REALM | manager"
  ✗ Remover vínculo com "GROUP | Empresa/Financeiro"
```

#### Cenário: Usuário é removido do Keycloak

**Scheduled Sync detecta que usuário não existe mais:**

```
1. Scheduled Job lista todos usuários do Keycloak
2. Compara com usuários no Archbase (por externalId)
3. Detecta: UserEntity com externalId "123e4567-..." não existe mais no Keycloak
4. Executa:
   user.setAccountDeactivated(true)
   user.setDeactivatedAt(LocalDateTime.now())
   user.setDeactivationReason("REMOVED_FROM_KEYCLOAK")
5. Salva UserEntity
6. Vínculos UserGroupEntity são mantidos (para histórico)
```

### Casos de Uso Detalhados

#### UC-1: Realm Role como Base de Permissão

**Requisito:** Usuário com Realm Role "admin" deve ter permissão total.

**Configuração de Permissões no Archbase:**
```java
// Vincular permissões ao group "REALM | admin"
PermissionEntity adminFullPermission = new PermissionEntity();
adminFullPermission.setAction("MANAGE");
adminFullPermission.setResource("*");
adminFullPermission.setGroupId(groupRepository.findByName("REALM | admin").get().getId());
```

#### UC-2: Client Role por Aplicação

**Requisito:** Usuário com Client Role "editar" do app-financeiro pode editar relatórios financeiros.

**Configuração de Permissões:**
```java
// Vincular permissões ao group "CLIENT | app-financeiro | editar"
PermissionEntity financeiroEditPermission = new PermissionEntity();
financeiroEditPermission.setAction("EDIT");
financeiroEditPermission.setResource("RELATORIO_FINANCEIRO");
financeiroEditPermission.setGroupId(
    groupRepository.findByName("CLIENT | app-financeiro | editar").get().getId()
);
```

#### UC-3: Keycloak Group para Estrutura Organizacional

**Requisito:** Usuários no grupo "/Empresa/Financeiro" podem visualizar relatórios do departamento.

**Configuração de Permissões:**
```java
// Vincular permissões ao group "GROUP | Empresa/Financeiro"
PermissionEntity departamentoFinanceiro = new PermissionEntity();
departamentoFinanceiro.setAction("VIEW");
departamentoFinanceiro.setResource("RELATORIO_DEPARTAMENTAL");
departamentoFinanceiro.setDepartment("Financeiro");
departamentoFinanceiro.setGroupId(
    groupRepository.findByName("GROUP | Empresa/Financeiro").get().getId()
);
```

#### UC-4: Filtro de Groups por Tipo

**Consulta JPA example:**
```java
// Buscar todos os Realm Roles
List<GroupEntity> realmRoles = groupRepository.findByGroupType("REALM");

// Buscar todos os groups de um client específico
List<GroupEntity> financeiroRoles = groupRepository.findByGroupTypeAndOriginDetails(
    "CLIENT", "app-financeiro");

// Buscar todos os grupos de um departamento (usando LIKE)
List<GroupEntity> departamentoGroups = groupRepository.findByNameStartingWith(
    "GROUP | Empresa/Financeiro");
```

---

## Casos de Teste

### Testes de Nomenclatura

```java
class KeycloakGroupConverterTest {

    @Test
    void shouldConvertRealmRoleToGroupName() {
        String result = KeycloakGroupConverter.realmRoleToGroupName("admin");
        assertEquals("REALM | admin", result);
    }

    @Test
    void shouldConvertClientRoleToGroupName() {
        String result = KeycloakGroupConverter.clientRoleToGroupName("app-financeiro", "visualizar");
        assertEquals("CLIENT | app-financeiro | visualizar", result);
    }

    @Test
    void shouldConvertKeycloakGroupPathToGroupName() {
        String result = KeycloakGroupConverter.keycloakGroupToGroupName("/Empresa/Financeiro");
        assertEquals("GROUP | Empresa/Financeiro", result);
    }

    @Test
    void shouldExtractTypeFromGroupName() {
        assertEquals("REALM", KeycloakGroupConverter.extractType("REALM | admin"));
        assertEquals("CLIENT", KeycloakGroupConverter.extractType("CLIENT | app | role"));
        assertEquals("GROUP", KeycloakGroupConverter.extractType("GROUP | Empresa/Financeiro"));
        assertEquals("MANUAL", KeycloakGroupConverter.extractType("ManualGroup"));
    }

    @Test
    void shouldGenerateCodeFromName() {
        assertEquals("realm_admin", KeycloakGroupConverter.generateCode("REALM | admin"));
        assertEquals("client_app_financeiro_visualizar",
                     KeycloakGroupConverter.generateCode("CLIENT | app-financeiro | visualizar"));
        assertEquals("group_empresa_financeiro",
                     KeycloakGroupConverter.generateCode("GROUP | Empresa/Financeiro"));
    }
}
```

### Testes de Sincronização Incremental

```java
class UserGroupSynchronizationServiceTest {

    @Test
    void shouldAddNewGroupsToUser() {
        // Given
        UserEntity user = createUserWithGroups("REALM | user");
        List<String> tokenGroups = List.of("REALM | user", "REALM | admin");

        // When
        service.syncUserGroupsIncremental(user, tokenGroups);

        // Then
        assertEquals(2, user.getUserGroups().size());
        assertTrue(hasGroupWithName(user, "REALM | admin"));
    }

    @Test
    void shouldRemoveGroupsNotInToken() {
        // Given
        UserEntity user = createUserWithGroups("REALM | user", "REALM | manager");
        List<String> tokenGroups = List.of("REALM | user");

        // When
        service.syncUserGroupsIncremental(user, tokenGroups);

        // Then
        assertEquals(1, user.getUserGroups().size());
        assertFalse(hasGroupWithName(user, "REALM | manager"));
    }

    @Test
    void shouldHandleMultipleGroupTypes() {
        // Given
        UserEntity user = createUserWithGroups("REALM | user");
        List<String> tokenGroups = List.of(
            "REALM | user",
            "CLIENT | app-financeiro | visualizar",
            "GROUP | Empresa/Financeiro"
        );

        // When
        service.syncUserGroupsIncremental(user, tokenGroups);

        // Then
        assertEquals(3, user.getUserGroups().size());
        assertTrue(hasGroupWithName(user, "CLIENT | app-financeiro | visualizar"));
        assertTrue(hasGroupWithName(user, "GROUP | Empresa/Financeiro"));
    }
}
```

### Testes de Soft Delete

```java
class UserDeactivationServiceTest {

    @Test
    void shouldMarkUserAsDeactivated() {
        // Given
        UserEntity user = new UserEntity();
        user.setAccountDeactivated(false);

        // When
        service.markUserAsRemovedFromKeycloak(user);

        // Then
        assertTrue(user.getAccountDeactivated());
        assertEquals("REMOVED_FROM_KEYCLOAK", user.getDeactivationReason());
        assertNotNull(user.getDeactivatedAt());
    }

    @Test
    void shouldReactivateUser() {
        // Given
        UserEntity user = new UserEntity();
        user.setAccountDeactivated(true);
        user.setDeactivationReason("REMOVED_FROM_KEYCLOAK");

        // When
        service.reactivateUser(user);

        // Then
        assertFalse(user.getAccountDeactivated());
        assertNull(user.getDeactivationReason());
        assertNull(user.getDeactivatedAt());
    }
}
```

---

## Referências

### Documentos Relacionados

- **Estratégias de Sincronização**: `keycloak-archbase-integration-strategies.md`
  - Análise completa de estratégias (JIT, Scheduled, Event Listeners, etc.)

- **Guia de Entidades**: `entities-guide.md`
  - Estrutura detalhada das entidades do Archbase

### Arquivos de Código

- `KeycloakGroupConverter.java` - Utilitário de conversão de nomes
- `KeycloakTokenGroupExtractor.java` - Parser de token JWT
- `UserGroupSynchronizationService.java` - Serviço de sincronização incremental
- `UserDeactivationService.java` - Serviço de soft delete
- `KeycloakUserSyncService.java` - Serviço principal de sincronização

---

## Notas Importantes

1. **Estratégia de Sincronização Pendente**: Este documento define a estrutura de dados e algoritmos de sincronização incremental. A escolha entre JIT, Scheduled, Event Listeners (ou combinação deles) será definida em documento separado.

2. **Compatibilidade com Multi-Tenancy**: Caso o framework use multi-tenancy, considerar adicionar contexto de tenant na nomenclatura ou em metadados dos groups.

3. **Performance**: O cache de sincronização (TTL: 15 minutos) é essencial para evitar sobrecarga em requisições frequentes. Em ambientes distribuídos, usar cache distribuído (Redis).

4. **Evitar Conflitos**: O separador " | " (barra vertical com espaços) foi escolhido por ser pouco comum em nomes de roles e groups, reduzindo chances de conflitos.

5. **Índices de Banco**: Recomendado criar índices nos campos:
   - `GroupEntity.name` (unique)
   - `GroupEntity.code` (unique)
   - `GroupEntity.groupType`
   - `UserEntity.externalId` (unique)
   - `UserGroupEntity.user_id` + `UserGroupEntity.group_id` (composite unique)

---

**Versão:** 1.0
**Data:** 2025-01-23
**Status:** Definitivo - Decisões de Estrutura de Dados
